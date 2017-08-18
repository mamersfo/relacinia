(ns lacinia-hack.core
  (:gen-class)
  (:require [lacinia-hack.util :refer :all]
            [lacinia-hack.db :as db]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
            ;; [com.walmartlabs.lacinia.pedestal :refer [pedestal-service]]
            ;; [io.pedestal.http :as http]
            [cheshire.core :as json]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.request :refer [body-string]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]))

(defn pull-spec [n fs]
  (if-let [f (get fs (:fragment-name n))]
    (vec (map #(pull-spec % fs) (:selections f)))
    (if-let [s (:selections n)]
      {(:field n) (vec (flatten (map #(pull-spec % fs) s)))}
      (:field n))))

(defn resolve-selection
  [context args _value]
  (let [selection (:com.walmartlabs.lacinia/selection context)
        fragments (:fragments (:com.walmartlabs.lacinia.constants/parsed-query context))
        spec (-> (pull-spec selection fragments) first second)
        result (db/pull spec _value)]
    result))

(defn resolve-one
  [context args _value]
  (let [value (db/match (uuid (:id args)))]
    (resolve-selection context args value)))

(defn resolve-all
  [type context args _value]
  (let [value (db/match [:type type])]
    (resolve-selection context args value)))

(defn resolve-ref [m k]
  (if (contains-key? m k)    
    (if-let [ref (get m k)]
      (assoc m k (db/match (uuid (:id ref))))
      m)
    m))

(defn team!
  [context args _value]
  (let [input (-> (:input args)
                  (assoc :type :Team)
                  (resolve-ref :country))
        value (db/upsert! input)]
    (resolve-selection context args value)))

(defn player!
  [context args _value]
  (let [input (-> (:input args)
                  (assoc :type :Player)
                  (resolve-ref :country)
                  (resolve-ref :team))
        value (db/upsert! input)]
    (resolve-selection context args value)))

(defn country!
  [context args _value]
  (let [input (assoc (:input args) :type :Country)
        value (db/upsert! input)]
    (resolve-selection context args value)))

(def graphql-schema
  (-> (read-string (slurp (io/resource "graphql-schema.edn")))
      (attach-resolvers {:resolve-one    resolve-one
                         :countries      (partial resolve-all :Country)
                         :country        country!
                         :players        (partial resolve-all :Player)
                         :player         player!
                         :teams          (partial resolve-all :Team)
                         :team           team!
                         })
      schema/compile))

(defn setup
  []
  (let [uri "datomic:mem://test"]
    (db/delete-db! uri)
    (db/create-db! uri)
    (db/connect uri)
    (db/transact! (read-string (slurp (io/resource "datomic-schema.edn"))))
    (db/transact! (read-string (slurp (io/resource "datomic-data.edn"))))))

;; pedestal

(comment
  (defonce runnable-service (pedestal-service graphql-schema {:graphiql true :port 8080}))

  (defn -main
    [& args]
    (setup)
    (http/start runnable-service)))

;; ring

(defn execute
  [query]
  (lacinia/execute graphql-schema query nil nil))

(defn graphql-handler
  [compiled-schema]
  (fn [request]
    (let [body (json/parse-string (:body request) true)
          query (:query body)
          variables (:variables body)
          result (lacinia/execute compiled-schema query variables nil)
          result (json/generate-string result)]
      (println "query:" query "result:" result)
      {:status 200
       :headers {"Content-Type" "application/json"}
       :body result})))

(defn handler
  [request]
  (if (= (:uri request) "/graphql")
    ((graphql-handler graphql-schema) request)
    {:status 404}))

(defn wrap-body-string
  [handler]
  (fn [request]
    (let [s (body-string request)]
      (handler (assoc request :body s)))))

(def app
  (-> handler
      wrap-params
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post])
      wrap-body-string))
