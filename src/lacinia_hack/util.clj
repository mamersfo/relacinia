(ns lacinia-hack.util
  (:require [clojure.spec.alpha :as s]
            [ring.util.request :refer [body-string]]
            [datomic.api :as d]))

(defn uuid?
  [x]
  (instance? java.util.UUID x))

(defn uuid
  ([x]
   (if (uuid? x)
     x
     (if (string? x)
       (java.util.UUID/fromString x)
       nil)))
  ([]
   (d/squuid)))

(defn long?
  [x]
  (instance? java.lang.Long x))

(defn contains-key?
  [m k]
  (contains? (set (keys m)) k))

(defn entity?
  [x]
  (instance? datomic.query.EntityMap x))

(defn value
  [v]
  ;; (if (or (instance? datomic.query.EntityMap v) (map? v)) (:db/id v) v)
  (or (:db/id v) v))

(defn component? [a]
  (and (entity? a) (:db/isComponent a) (= :db.type/ref (:db/valueType a))))

(defn wrap-body-string
  [handler]
  (fn [request]
    (let [s (body-string request)]
      (handler (assoc request :body s)))))

