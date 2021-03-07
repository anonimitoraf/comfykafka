(ns comfykafka.transient.core
  (:require [clojure.string :refer [join]]
            [cljs.pprint :refer [pprint]]
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
                    Takes 1 arg, which is the keymap and the direction
                      [keymap navigation-direction]
    * go-back-key - key for going back to the previous keymap.
  "
  ([keymap go-back-key on-navigate]
   (let [state (atom nil)]
     (merge (process-keymap
             keymap
             go-back-key
             on-navigate
             nil state)
            ;; Get the second item in history because the first is the current keymap
            {go-back-key #(let [go-back-to (-> @state :history second)]
                            (when go-back-to
                              (on-navigate [go-back-to :backward])
                              (swap! state update :history pop)))})))
  ([keymap go-back-key on-navigate
    parent-keymap-id state]
   (let [[key id _ & sub-keymaps] keymap
         processed-sub-keymaps (when (not-empty sub-keymaps)
                                 (map (fn [sub-keymap] (process-keymap
                                                        sub-keymap
                                                        go-back-key
                                                        on-navigate
                                                        id
                                                        state))
                                      sub-keymaps))]
     (reduce #(merge-with  comp %1 %2)
             {key #(do
                     (when (= parent-keymap-id
                              ;; History is a list of keymaps which look like:
                              ;;  [key id desc & sub-keymaps]
                              ;; We want to get the keymap ID
                              (-> @state :history first second))
                       (tap> {:parent parent-keymap-id})
                       (tap> {:state @state})
                       (on-navigate [keymap :forward])
                       ;; This generates a list, not a vector
                       ;; So, history is "stack"
                       (swap! state update :history conj keymap)))}
             processed-sub-keymaps))))

(defn test-component
  [debug-ui]
  (r/with-let [state (r/atom {})
               keymap-misc {["?"] #(swap! state update :show-keymap-helper? not)
                            ["!"] #(swap! state update :show-debug-ui? not)}
               keymap-workflow (process-keymap
                                k/workflow-keymap
                                k/go-back-key
                                (fn [[selected-keymap _]]
                                  ;; (tap> selected-keymap)
                                  (swap! state assoc :selected selected-keymap)))
               keymap (merge keymap-misc keymap-workflow)]
    (with-keys @screen keymap
      [:box {:top 0
             :height "100%"
             :left 0
             :width "100%"
             :border {:type :line}
             :style {:border {:fg :white}}}
       ;; HACK: Hiding of component is via moving it well outside the viewport
       [:box {:top (if (:show-keymap-helper? @state) "75%" "1000%")
              :height "30%"
              :left -1
              :width "100%"
              :border {:type :line}
              :style {:border {:fg :cyan}}
              :content (let [[_ _ desc & sub-keymaps] (:selected @state)]
                         (str desc "\n" "\n"
                              (->> sub-keymaps
                                   ;; TODO Utilize colored strings
                                   (map (fn [[key _ desc]] (str key " - " desc)))
                                   (join "\n"))))}
        [:line {:top 1
                :orientation :horizontal
                :style {:fg :cyan}}]]
       ;; HACK This is a hack to make the debug box toggle-able
       (if (:show-debug-ui? @state) debug-ui [:<>])])))
