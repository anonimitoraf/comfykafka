(ns comfykafka.transient.core
  (:require [comfykafka.keys :refer [with-keys]]
            [comfykafka.core :refer [screen]]
            ;; [comfykafka.transient.keybindings :as k]
            [comfykafka.transient.actions]))

(defn process-keymap
  "
  Generates `blessed` keybindings given a `keymap`

    * keymap - See (TODO: Link namespace when it's finalized)
    * on-keypress - fn invoked for a key being pressed. Takes 1 arg: The key's ID
    * go-back-key - key for going back to the previous keymap
  "
  ([keymap on-keypress go-back-key]
   (let [state (atom nil)]
     (merge (process-keymap
             keymap
             on-keypress
             go-back-key
             nil state)
            {go-back-key #(swap! state update :history pop)})))
  ([keymap on-keypress go-back-key
    parent-keymap-id state]
   (let [[key keymap-id & sub-keymaps] keymap
         processed-sub-keymaps (when (not-empty sub-keymaps)
                                 (map (fn [sub-keymap] (process-keymap
                                                        sub-keymap
                                                        on-keypress
                                                        go-back-key
                                                        keymap-id
                                                        state))
                                      sub-keymaps))]
     (reduce #(merge-with comp %1 %2)
             {key #(when (= parent-keymap-id (-> @state :history first))
                     (on-keypress keymap-id)
                     ;; This generates a list, not a vector
                     ;; So, history is "stack"
                     (swap! state update :history conj keymap-id))}
             processed-sub-keymaps))))

;; TODO A way to go back to the parent keymap

(def keymap
  ["a" :a
   ["b" :b
    ["a" :a2]]])

(def dbg_processed (process-keymap keymap print "g"))

(defn test-component
  []
  (with-keys @screen (process-keymap keymap print "g")
    [:box {:content "Hello this is a test"}]))
