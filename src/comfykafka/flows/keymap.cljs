(ns comfykafka.flows.keymap
  (:require [cljs.core.async
             :refer [chan >! <! timeout]
             :refer-macros [go]]
            [re-frame.core :as rf]))

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
     (condp = key-id
       (go (tap> (str "About to process: " key-id))
           (<! (timeout 1000))
           (>! events-channel event-after))))))
