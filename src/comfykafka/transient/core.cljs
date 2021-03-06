(ns comfykafka.transient.core
  (:require [cljs.pprint :refer [pprint]]
            [reagent.core :as r]
            [comfykafka.keys :refer [with-keys]]
            [comfykafka.core :refer [screen]]
            [comfykafka.transient.keys :as k]
            [comfykafka.transient.actions]))

(defn process-keymap
  "
  Generates `blessed` keybindings given a `keymap`

    * keymap - See (TODO: Link namespace when it's finalized).
    * on-navigate - Function invoked for navigating forward or backward.
                    Takes 1 arg, a list: [active-key-id navigation-direction sub-keymap]
    * go-back-key - key for going back to the previous keymap.
  "
  ([keymap go-back-key on-navigate]
   (let [[_ _ & sub-keymaps] keymap
         state (atom nil)]
     (merge (process-keymap
             keymap
             go-back-key
             on-navigate
             nil state)
            {go-back-key #(let [go-back-to (-> @state :history first)]
                            (when go-back-to
                              (on-navigate [go-back-to :backward sub-keymaps])
                              (swap! state update :history pop)))})))
  ([keymap go-back-key on-navigate
    parent-keymap-id state]
   (let [[key keymap-id & sub-keymaps] keymap
         processed-sub-keymaps (when (not-empty sub-keymaps)
                                 (map (fn [sub-keymap] (process-keymap
                                                        sub-keymap
                                                        go-back-key
                                                        on-navigate
                                                        keymap-id
                                                        state))
                                      sub-keymaps))]
     (reduce #(merge-with comp %1 %2)
             {key #(when (= parent-keymap-id (-> @state :history first))
                     (on-navigate [keymap-id :forward sub-keymaps])
                     ;; This generates a list, not a vector
                     ;; So, history is "stack"
                     (swap! state update :history conj keymap-id))}
             processed-sub-keymaps))))

;; (def keymap
;;   ["a" :a
;;    ["b" :b
;;     ["a" :a2]]])

;; (def dbg_processed (process-keymap keymap print "g"))

(defn test-component
  [_]
  (r/with-let [show-keymap-helper? (r/atom false)
               keybindings (merge (process-keymap
                                   k/workflow-keymap
                                   k/go-back-key
                                   #(print (with-out-str (pprint %))))
                                  {["?"] #(swap! show-keymap-helper? not)})]
    (with-keys @screen keybindings
      [:box {:top 0
             :height "100%"
             :left 0
             :width "100%"
             :border {:type :line}
             :style {:border {:fg :white}}}
       ;; HACK: Hiding of component is via moving it well outside the viewport
       [:box {:top (if @show-keymap-helper? "75%" "1000%")
              :height "30%"
              :left -1
              :width "100%"
              :border {:type :line}
              :style {:border {:fg :cyan}}
              :content "Hello this is a test"}
        [:line {:top 1
                :orientation :horizontal
                :style {:fg :cyan}}]]])))
