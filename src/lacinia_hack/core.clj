(ns lacinia-hack.core
  (:gen-class)
  (:require [lacinia-hack.util :refer :all]
            [lacinia-hack.db :as db]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers]]
            [cheshire.core :as json]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.params :refer [wrap-params]]
            [compojure.core :refer [defroutes POST]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]))

(defn pull-spec [sn fs]
  (if-let [f (get fs (:fragment-name sn))]
    (vec (map #(pull-spec % fs) (:selections f)))
    (if-let [s (:selections sn)]
      {(:field sn) (vec (flatten (map #(pull-spec % fs) s)))}
      (:field sn))))

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
  [context args _value]
  (let [type (-> context
                 :com.walmartlabs.lacinia/selection
                 :field-definition
                 :type
                 :type
                 :type)
        value (db/match [:type type])]
    (resolve-selection context args value)))

(defn resolve-ref [m k]
  (if (contains-key? m k)    
    (if-let [ref (get m k)]
      (assoc m k (db/match (uuid (:id ref))))
      m)
    m))

(defn resolve-input
  ([m ks]
   (if (seq ks)
     (resolve-input
      (if-let [id (:id (get m (first ks)))]
        (assoc m (first ks) (db/match (uuid id)))
        m)
      (rest ks))
     m))
  ([m]
   (resolve-input m (keys m))))

(defn upsert!
  [context args _value]
  (let [type (-> context
                 :com.walmartlabs.lacinia/selection
                 :field-definition
                 :type
                 :type)
        input (-> (resolve-input (:input args))
                  (assoc :type type))
        value (db/upsert! input)]
    (resolve-selection context args value)))

(def graphql-schema
  (-> (read-string (slurp (io/resource "graphql-schema.edn")))
      (attach-resolvers {:resolve-one    resolve-one
                         :countries      resolve-all
                         :country        upsert!
                         :players        resolve-all
                         :player         upsert!
                         :teams          resolve-all
                         :team           upsert!
                         })
      schema/compile))

(defn execute
  [query vars]
  (lacinia/execute graphql-schema query vars nil))

(defn query-and-vars
  [request]
  (let [content-type (:content-type request)]
    (condp = content-type
      "application/graphql" [(:body request) (get (:query-params request) "variables")]
      "application/json"    (let [json (json/parse-string (:body request) true)]
                              [(:query json) (:variables json)])
      (RuntimeException. "Unexpected content-type:" content-type))))

(defn graphql-handler
  [compiled-schema]
  (fn [request]
    (try
      (let [[query vars] (query-and-vars request)
            result (json/generate-string (execute query vars))]
        {:status 200
         :headers {"Content-Type" "application/json"}
         :body result})
      (catch Throwable e
        (.printStackTrace e)
        {:status 500
         :headers {"Content-Type" "application/json"}
         :body (json/generate-string {:message (.getMessage e)})}))))

(defroutes routes
  (POST "/graphql" request
        ((graphql-handler graphql-schema) request))
  (route/resources "/")
  (route/not-found "Page not found"))

(def app
  (-> (handler/site routes)
      wrap-params
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post])
      wrap-body-string))

(defn setup
  []
  (let [uri "datomic:mem://test"]
    (db/delete-db! uri)
    (db/create-db! uri)
    (db/connect uri)
    (db/transact! (read-string (slurp (io/resource "datomic-schema.edn"))))
    (db/transact! (read-string (slurp (io/resource "datomic-data.edn"))))))
