(ns comfykafka.transient.core-2
  (:require [cljs.pprint :refer [pprint]]
            [cljs.core.async :refer [chan >! <! timeout]]
            [cljs.core.async :refer-macros [go go-loop]]))

;; Kinds of events:
;;
;; forward navigation start
;; {:type :nav|->
;;  :hotkey
;;  :id}
;;
;; forward navigation end
;; {:type :nav->|
;;  :hotkey
;;  :id}
;;
;; backward navigation start
;; {:type :nav<-|}

;; A keymap is comprised of:
;; [hotkey id desc & sub-keymaps]

(defn ^:private filter-first
  [pred coll]
  (->> coll
       (filter pred)
       first))

(defn ^:private try-pop
  "Like pop but does not throw an error if coll is empty."
  [coll]
  (if (empty? coll) coll (pop coll)))

(defn process-events
  "Given a keymap, process events run against the keymap"
  [keymap events]
  (let [states (atom [{:current keymap}])
        states-channel (chan)
        temp (atom {})]
    (add-watch states :changes (fn [_ _ _ new-state]
                                 (go (>! states-channel new-state))))
    (doseq [{:keys [type hotkey id]} events]
      (condp = type
        :nav|-> (if (nil? (:pending-nav-id @temp))
                  (swap! temp assoc :pending-nav-id id)
                  (print (str "Unexpected :nav|-> with ID: " id ". Ignoring...")))
        :nav->| (let [current-keymap (-> @states
                                         last
                                         :current)
                      [_ _ _ & sub-keymaps] current-keymap
                      new-keymap (filter-first #(= (first %) hotkey)
                                               sub-keymaps)]
                  (if (nil? new-keymap)
                    (print (str "Unexpected hotkey:"  hotkey ". Ignoring..."))
                    (do
                      ;; This can happen due to race conditions
                      ;;causing > 1 navigation sequences to be started
                      (when (not= (:pending-nav-id @temp) id)
                        (throw (js/Error (str "Unexpected :nav->| ID: " id))))
                      (swap! states conj {:previous current-keymap
                                          :current new-keymap})
                      (swap! temp dissoc :pending-nav-id))))
        :nav<-| (do (swap! states try-pop)
                    (swap! temp dissoc :pending-nav-id))
        (throw (js/Error (str "Unexpected event type: " type)))))
    @states))

(def keymap
  [nil :root "comfykafka"
   ["c" :connections/view "Connections"
    ["c" :connection/connect "Connect"]
    ["e" :connection/edit "Edit"
     ["l" :conection-edit/url "URL"]]]
   ["s" :settings/view "Settings"
    ["e" :settings/edit "Edit"]
    ["r" :settings/reset "Reset"]]])

(def events
  [{:type :nav|-> :hotkey "c" :id 1}
   {:type :nav->| :hotkey "c" :id 1}
   {:type :nav|-> :hotkey "e" :id 3}
   {:type :nav|-> :hotkey "e" :id 4} ;; This should get ignored
   {:type :nav->| :hotkey "e" :id 3}
   {:type :nav<-|}])

(def dbg_processed (process-events keymap events))
