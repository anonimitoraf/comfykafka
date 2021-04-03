(ns comfykafka.flows.keymap
  (:require [cljs.core.async
             :refer [chan >! <! timeout]
             :refer-macros [go]]
            [comfykafka.flows.connection :as connection-flows]
            [comfykafka.wrappers.kafka :refer [list-topics]]
            [re-frame.core :as rf]
            [re-frame.db :as rf-db]))

(rf/reg-event-db
 ::init-events-ch
 (fn [db _]
   (assoc-in db [:keymap :events/channel] (chan 1024))))

(rf/reg-sub
 ::events-ch
 (fn [db _]
   (get-in db [:keymap :events/channel])))

(rf/reg-event-fx
 ::process-key
 (fn [{:keys [db]} [_ key-id event-after]]
   {::process-key! [db key-id event-after]}))

(rf/reg-fx
 ::process-key!
 (fn [[db key-id event-after]]
   (let [events-channel (get-in db [:keymap :events/channel])]
     (go (tap> (str "About to process: " key-id))
         (condp = key-id

           :connection/connect
           (let [[client admin] (<! (connection-flows/do-connect db))]
             (reset! rf-db/app-db
                     (-> db
                         (assoc-in [:connection :client] client)
                         (assoc-in [:connection :admin] admin))))

           :topics/view
           (let [admin (get-in db [:connection :admin])]
             (reset! rf-db/app-db
                     (assoc-in db [:topic :registry] (<! (list-topics admin)))))

           (<! (timeout 1000))) ; Simulate some async delay
         (tap> (str "Finished processing: " key-id))
         (>! events-channel event-after)))))
