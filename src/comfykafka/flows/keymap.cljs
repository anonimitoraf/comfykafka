(ns comfykafka.flows.keymap
  (:require [cljs.core.async
             :refer [>! <! timeout]
             :refer-macros [go]]
            [re-frame.core :as rf]))

(rf/reg-fx
 ::process-key!
 (fn [[db key-id after]]
   (condp = key-id
     (go (tap> (str "About to process: " key-id))
         (<! (timeout 1000))
         (after)))))

(rf/reg-event-fx
 ::process-key
 (fn [{:keys [db]} [_ key-id after]]
   {::process-key! [db key-id after]}))
