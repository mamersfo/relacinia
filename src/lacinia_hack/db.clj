(ns lacinia-hack.db
  (:require [lacinia-hack.util :refer :all]
            [datomic.api :as d]
            [clojure.set :refer [difference]]
            [clojure.spec.alpha :as s]))

;; connection

(defonce connection (atom nil))

(defn connect
  [uri]
  (reset! connection (d/connect uri)))

(defn conn []
  (if (nil? @connection)
    (throw (RuntimeException. "No database connection."))
    @connection))

;; database

(defn create-db!
  [uri]
  (d/create-database uri))

(defn delete-db!
  [uri]
  (d/delete-database uri))

(defn db []
  (d/db (conn)))

;; match

(defn- var-fn [a]
  (symbol (str "?" (name a))))

(defn- fulltext-fn [a]
  (vector (list 'fulltext '$ a (var-fn a)) [['?e]]))

(defn- where-fn [a]
  (if (:db/fulltext (d/entity (db) a))
    (fulltext-fn a)
    (vector '?e a (var-fn a))))

(defn match
  [x]
  (s/assert (s/or :uuid uuid? :vector vector?) x)
  (cond
    (vector? x)
    (let [pairs (partition 2 x)
          keys (map first pairs)
          vals (map second pairs)
          vars (map var-fn keys)
          wheres (vec (map where-fn keys))
          query (vec (concat [:find '?e :in '$] vars [:where] wheres))]
      (map first (apply d/q query (db) vals)))
    (uuid? x)
    (ffirst (d/q '[:find ?e :in $ ?id :where [?e :id ?id]] (db) x))))

(defn transact! [data]
  (let [conn (conn)
        db (d/db conn)
        tx @(d/transact conn data)
        tempids (:tempids tx)]
    (map #(d/entity db (d/resolve-tempid db tempids (:db/id %))) data)))

(defn- datoms-for-many
  [e a v]
  (let [attr (d/entity (db) a)
        before (set (if (component? attr)
                      (map #(into {} (d/touch %)) (get e a))
                      (map value (get e a))))
        after (set v)
        omissions (difference before after)
        retract-ds (map #(vector :db/retract (:db/id e) a %) omissions)
        additions (difference after before)]
    (if (component? attr)
      (let [additions (map #(assoc % :db/id (d/tempid :db.part/user))
                            additions)
            add-ds (map #(vector :db/add (:db/id e) a (:db/id %)) additions)]
        (concat add-ds retract-ds additions))
      (let [add-ds (map #(vector :db/add (:db/id e) a %) additions)]
        (concat add-ds retract-ds)))))

(s/def ::entity entity?)

(defn- datom-to-update
  [e m k]
  (let [v (value (get m k))]
    (if (= (value (get e k)) v)
      nil
      [:db/add (:db/id e) k v])))

(defn- datom-to-retract
  [e k]
  (when-let [v (value (get e k))]
    [:db/retract (:db/id e) k v]))

(defn diff
  [e m]
  (s/assert ::entity e)
  (let [m (dissoc m :id :type)
        ks (keys m)
        ;; cardinality many
        many-ks (set (filter #(-> m % sequential?) ks))
        many-ds (map #(datoms-for-many e % (get m %)) many-ks)
        many-ds (apply concat many-ds)
        ;; retractions        
        ks (difference (set ks) many-ks)
        retract-ks (set (filter #(nil? (get m %)) ks))
        retract-ds (map #(datom-to-retract e %) retract-ks)
        ;; addition        
        ks (difference (set ks) retract-ks)
        update-ks (difference (set ks) retract-ks)
        update-ds (map #(datom-to-update e m %) update-ks)]
    (filter identity (apply concat (list many-ds retract-ds update-ds)))))

(defn upsert!
  [m]
  (if-let [id (:id m)]
    ;; update
    (let [e (d/entity (db) (match (uuid id)))
          data (diff e m)]
      (if data @(d/transact (conn) data))
      (:db/id e))
    ;; insert
    (let [m (assoc (select-keys m (filter #(identity (get m %)) (keys m)))
                   :db/id (d/tempid :db.part/user)
                   :id (d/squuid))
          result @(d/transact (conn) (vector m))]
      (d/resolve-tempid (db) (:tempids result) (:db/id m)))))

(defn delete!
  [id]
  (if-let [found (match id)]
    @(d/transact (conn) [[:db.fn/retractEntity (:db/id found)]])))

(defn pull
  [s v]
  (cond
    (seq?  v) (d/pull-many (db) s (map value v))
    (long? v) (d/pull      (db) s (value v))))
