(ns comfykafka.flows.connection
  (:require [re-frame.core :as rf]
            [comfykafka.utils :refer [find-first]]))

(rf/reg-event-db
 ::add
 (fn [db [_ connections]]
   (update db :connection/registry concat connections)))

(rf/reg-event-db
 ::select
 (fn [db [_ connection-name]]
   (assoc db :connection/selected-name connection-name)))

(rf/reg-event-db
 ::save
 (fn [db [_ connection]]
   (assoc-in db [:connection/registry
                 (:connection/name connection)
                 connection])))

;; ______________________________ Subs ______________________________

(rf/reg-sub
 ::registry
 (fn [db]
   (:connection/registry db)))

(rf/reg-sub
 ::selected-name
 (fn [db]
   (:connection/selected-name db)))

(rf/reg-sub
 ::selected
 (fn [_]
   [(rf/subscribe [::registry])
    (rf/subscribe [::selected-name])])
 (fn [[registry connection-name]]
   (find-first registry #(= (:connection/name %) connection-name))))
