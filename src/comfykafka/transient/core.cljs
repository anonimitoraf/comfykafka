(ns comfykafka.transient.core
  (:require [clojure.string :refer [join]]
            [cljs.core.async :refer [go]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [comfykafka.keys :refer [with-keys]]
            [comfykafka.core :refer [screen]]
            [comfykafka.flows.connection :as cfc]
            [comfykafka.components.connections :as ccc]
            [comfykafka.transient.keys :as k]
            [comfykafka.transient.actions]))

(defn- handle-maybe-promise
  "Awaits `maybe-promise` then executes `thunk-after`"
  [maybe-promise thunk-after]
  (go (when (.-then maybe-promise) (<p! maybe-promise))
      ;; Easiest way to test if awaiting works
      ;; (tap> "After (maybe) waiting for maybe-promise")
      (thunk-after)))

(defn- process-keymap-inner
  [keymap on-navigate parent-keymap-id history]
  (let [[key id _ & sub-keymaps] keymap
        processed-sub-keymaps (when (not-empty sub-keymaps)
                                (map (fn [sub-keymap] (process-keymap-inner sub-keymap
                                                                            on-navigate
                                                                            id history))
                                     sub-keymaps))]
    (reduce #(merge-with  comp %1 %2)
            {key #(when (= parent-keymap-id
                           ;; History is a list of keymaps which look like:
                           ;;  [key id desc & sub-keymapsngs]
                           ;; We want to get the keymap ID
                           (-> @history first second))
                    (handle-maybe-promise
                     (on-navigate [keymap :forward])
                     ;; This generates a list, not a vector
                     ;; So, history is "stack"
                     (fn [] (swap! history conj keymap))))}
            processed-sub-keymaps)))

(defn process-keymap
  "
  Generates `blessed` keybindings given a `keymap`
    * keymap - See (TODO: Link namespace when it's finalized).
    * on-navigate - Function invoked for navigating forward or backward.
                    Takes 1 arg, which is the keymap and the direction:
                      [keymap navigation-direction]
                    Can be an async fn that returns a JS Promise.
  Returns:
  ```
  {:keymap  - `blessed` keybindings
   :within-keymap-history? - Fn with 1 arg: `keymap-d`.
                             Returns `true` if `keymap-id` is within navigation history
   :current-keymap? - Fn with with 1 arg: `keymap-d`.
                      Returns `true` if `keymap-id` is the currently selected
   :go-back - Fn that, when called, navigates backwards}
  ```
  "
  [keymap on-navigate]
  (let [history (atom nil)]
    {:keymap (process-keymap-inner keymap on-navigate nil history)
     :within-keymap-history? (fn [keymap-id]
                               (->> @history
                                    (map second)
                                    (filter (partial = keymap-id))
                                    empty? not))
     :current-keymap? (fn [keymap-id]
                        (->> @history
                             first
                             second
                             (= keymap-id)))
     :go-back
     ;; Get the second item in history because the first is the current keymap
     #(let [go-back-to (-> @history second)]
        (when go-back-to
          (handle-maybe-promise
           (on-navigate [go-back-to :backward])
           (fn [] (swap! history pop)))))}))

(defn test-component
  [debug-ui]
  (r/with-let [state (r/atom {:show-keymap-helper? true})
               {:keys [keymap
                       within-keymap-history?
                       current-keymap?
                       go-back]} (process-keymap
                                  k/workflow-keymap
                                  (fn [[selected-keymap direction]]
                                    (condp = direction
                                      :forward (swap! state assoc :selected selected-keymap)
                                      :backward (swap! state assoc :selected selected-keymap)
                                      (throw (js/Error (str "Unexpected direction: " direction))))
                                    ;; HACK A way to aide cancellation of prompts
                                    ;; (avoids an extra key-press: just "g" instead of "escape g")
                                    (swap! state assoc :inhibit-prompts? false)))
               meta-keymap {["?"] #(swap! state update :show-keymap-helper? not)
                            ["!"] #(swap! state update :show-debug-ui? not)
                            ["g"] go-back}]
    (with-keys @screen (merge meta-keymap keymap)
      [:<>
       [:box#main {:top 0
                   :height "75%"
                   :left 0
                   :width "100%"}
        ;; Listing connections
        (when (within-keymap-history? :connections/view)
          [ccc/selector
           {:top 0 :height "100%" :left 0 :width "25%"}
           {:focused? (current-keymap? :connections/view)}
           @(rf/subscribe [::cfc/registry])
           #(rf/dispatch [::cfc/select %])])
        ;; Editing connection/s
        (when (within-keymap-history? :connection/edit)
          [ccc/configurator
           {:top 0 :height "100%" :left "25%" :width "25%"}
           {:focused? (current-keymap? :connection/edit)}
           @(rf/subscribe [::cfc/selected])
           {:url      (current-keymap? :connection/edit-url)
            :username (current-keymap? :connection/edit-username)
            :password (current-keymap? :connection/edit-password)}
           {:on-submit {}
            :on-cancel {:url go-back
                        :username go-back
                        :password go-back}}])]
       (when (:show-keymap-helper? @state)
         [:box {:top "75%"
                :height "25%+1"
                :left 0
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
                  :style {:fg :cyan}}]])
       (when (:show-debug-ui? @state) debug-ui)])))
