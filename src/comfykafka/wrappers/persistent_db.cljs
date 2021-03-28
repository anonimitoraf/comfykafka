(ns comfykafka.wrappers.persistent-db
  (:require [cljs.core.async
             :refer [chan >! <!]
             :refer-macros [go go-loop]]
            ["path" :as path]
            ["os" :as os]
            ["nedb" :as Datastore]
            [comfykafka.errors :refer [->AsyncError]]))

(defrecord DBError [msg details])

(def dbs (atom {}))

(defn ^:private resolve-db
  [db-identifier]
  (or (db-identifier @dbs)
      (let [db (new Datastore (clj->js {:filename
                                        (str (.join path (.homedir os) ".comfykafka/nedb/")
                                             (name db-identifier) ".db")
                                        :autoload true}))]
        (swap! dbs assoc db-identifier db)
        db)))

;; TODO Spec to check that doc has :id
(defn upsert
  "Upserts `doc` into `db`.
  Returns a channel which emits either:
  * The upserted doc is successful
  * An `->AsyncError` if not"
  [db doc]
  (let [ch (chan)]
    (.update (resolve-db db)
             (clj->js {"_id" (doc :id)})
             (clj->js (-> doc
                          (dissoc :id)
                          (merge {"_id" (:id doc)})))
             (clj->js {:upsert true
                       :multi false
                       :returnUpdatedDocs true})
             (fn [err _affected-count affected-docs]
               (go (>! ch (if (nil? err)
                            (js->clj affected-docs)
                            (->DBError (.-message err) (.-stack err)))))))
    ch))

(defn get-all
  "Returns a channel which emits either:
  * All the docs in the `db`
  * An `->AsyncError` if not"
  [db]
  (let [ch (chan)]
    (.find (resolve-db db) (clj->js {})
           (fn [err docs]
             (go (>! ch (if (nil? err)
                          (map js->clj (js->clj docs))
                          (->DBError (.-message err) (.-stack err)))))))
    ch))
