(ns comfykafka.wrappers.persistent-db
  (:require [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            ["path" :as path]
            ["os" :as os]
            ["nedb" :as Datastore]
            [comfykafka.errors :refer [->AsyncError]]))

(def ^:private db-file-dir (.join path (.homedir os)
                                  ".comfykafka/nedb/"))
(def ^:private connections-db
  (new Datastore (clj->js {:filename (str db-file-dir "connections.db")
                           :autoload true})))

(defn ^:private resolve-db
  [db-identifier]
  (condp = db-identifier
    :connections connections-db))

;; TODO Spec to check that doc has :id
(defn upsert
  "Upserts `doc` into `db`. Returns a Promise containing the new doc"
  [db doc]
  (go (<p! (new js/Promise
                (fn [resolve reject]
                  (.update (resolve-db db)
                           (clj->js {"_id" (doc :id)})
                           (clj->js (-> doc
                                        (dissoc :id)
                                        (merge {"_id" (:id doc)})))
                           (clj->js {:upsert true})
                           (fn [err new-doc] (if err
                                               (reject (->AsyncError (.-message err)
                                                                     (.-stack err)))
                                               (resolve new-doc)))))))))

(defn get-all
  "Returns a Promise containing all the docs in `db`"
  [db]
  (go (<p! (new js/Promise
                (fn [resolve reject]
                  (.find (resolve-db db) (clj->js {})
                         (fn [err docs] (if err
                                          (reject (->AsyncError (.-message err)
                                                                (.-stack err)))
                                          (resolve docs)))))))))
