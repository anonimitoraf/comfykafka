(ns comfykafka.wrappers.persistent-db
  (:require [clojure.walk :refer [keywordize-keys]]
            [cljs.core.async :refer [chan >! <! close!] :refer-macros [go]]
            ["path" :as path]
            ["os" :as os]
            ["nedb" :as Datastore]))

(defrecord DBError [msg details])

(def ^:private dbs (atom {}))

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
  * An `->DBError` if not"
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
                            (->DBError (.-message err) (.-stack err))))
                   (close! ch))))
    ch))

(defn get-all
  "Returns a channel which emits either:
  * All the docs in the `db`
  * An `->DBError` if not"
  [db]
  (let [ch (chan)]
    (.find (resolve-db db) (clj->js {})
           (fn [err docs]
             (go (>! ch (if (nil? err)
                          (map (fn [doc]
                                 (-> (keywordize-keys doc)
                                     (merge {:id (doc "_id")})
                                     (dissoc :_id)))
                               (js->clj docs))
                          (->DBError (.-message err) (.-stack err))))
                 (close! ch))))
    ch))

(comment (upsert :connections {:id "local-3"
                               :url "localhost:9094"}))
(comment (go (tap> (<! (get-all :connections)))))
