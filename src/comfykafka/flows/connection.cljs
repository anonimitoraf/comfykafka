(ns comfykafka.flows.connection
  (:require [cljs.core.async
             :refer [<!]
             :refer-macros [go]]
            [clojure.string :refer [split]]
            [comfykafka.utils :refer [filter-first]]
            [comfykafka.wrappers.kafka :refer [make-client
                                               make-admin]]
            [re-frame.core :as rf]))

(rf/reg-event-db
 ::add
 (fn [db [_ connections]]
   (update-in db [:connection :registry] concat connections)))

(rf/reg-event-db
 ::select
 (fn [db [_ connection-id]]
   (assoc-in db [:connection :selected-id] connection-id)))

(rf/reg-event-db
 ::save
 (fn [db [_ connection]]
   (assoc-in db [:connection :registry (:id connection)]
             connection)))

(defn ^:private get-registry
  [db]
  (get-in db [:connection :registry]))

(defn ^:private get-selected-id
  [db]
  (get-in db [:connection :selected-id]))

(defn ^:private resolve-selected
  [registry id]
  (filter-first #(= (:id %) id) registry))

(rf/reg-sub ::registry get-registry)

(rf/reg-sub ::selected-id get-selected-id)

(rf/reg-sub
 ::selected
 (fn [_]
   [(rf/subscribe [::registry])
    (rf/subscribe [::selected-id])])
 (fn [[registry id]]
   (resolve-selected registry id)))

(defn do-connect
  "Creates and connects a Kafka client and a Kafka admin.
  Returns `[kafka-client kafka-admin]`."
  [db]
  (go
    (let [connection (resolve-selected
                      (get-registry db)
                      (get-selected-id db))
          client (make-client (split (:url connection) #","))
          admin (<! (make-admin client))]
      [client admin])))
