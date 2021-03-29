(ns comfykafka.flows.connection
  (:require [re-frame.core :as rf]
            [comfykafka.utils :refer [filter-first]]))

(rf/reg-event-db
 ::add
 (fn [db [_ connections]]
   (tap> connections)
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

;; ______________________________ Subs ______________________________

(rf/reg-sub
 ::registry
 (fn [db]
   (get-in db [:connection :registry])))

(rf/reg-sub
 ::selected-id
 (fn [db]
   (get-in db [:connection :selected-id])))

(rf/reg-sub
 ::selected
 (fn [_]
   [(rf/subscribe [::registry])
    (rf/subscribe [::selected-id])])
 (fn [[registry id]]
   (filter-first #(= (:id %) id) registry)))
