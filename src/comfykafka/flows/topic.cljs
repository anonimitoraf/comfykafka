(ns comfykafka.flows.topic
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 ::select
 (fn [db [_ name]]
   (assoc-in db [:topic :selected-name] name)))

(rf/reg-sub
 ::registry
 (fn [db] (get-in db [:topic :registry])))

(rf/reg-sub
 ::selected-name
 (fn [db] (get-in db [:topic :selected-name])))
