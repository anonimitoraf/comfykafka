(ns comfykafka.events
  "Event handlers for re-frame dispatch events.
  Used to manage app db updates and side-effects.
  https://github.com/Day8/re-frame/blob/master/docs/EffectfulHandlers.md"
  (:require [re-frame.core :as rf]))

; Below are very general effect handlers. While you can use these to get
; going quickly you are likely better off creating custom handlers for actions
; specific to your application.

(rf/reg-event-db
 :init
 (fn [db [_ opts terminal-size]]
   {:opts opts
    :router/view :home
    :terminal/size terminal-size
    ;; :connection/registry [{:id "prod cluster"
    ;;                        :connection/url "kafka-cluster.prod"}
    ;;                       {:id "qa cluster"
    ;;                        :connection/url "kafka-cluster.qa"}]
    }))

(rf/reg-event-db
 :update
 (fn [db [_ data]]
   (merge db data)))

(rf/reg-event-db
 :set
 (fn [db [_ data]]
   data))
