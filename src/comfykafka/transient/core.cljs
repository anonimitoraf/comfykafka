(ns comfykafka.transient.core
  (:require [clojure.string :refer [join]]
            [cljs.core.async :refer [go go-loop chan >! <! timeout]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [comfykafka.keys :refer [with-keys]]
            [comfykafka.core :refer [screen]]
            [comfykafka.flows.connection :as cfc]
            [comfykafka.components.connections :as ccc]
            [comfykafka.transient.keys :as k]
            [comfykafka.transient.actions]))

(defn ^:private gen-nav-seq [] (gensym "nav-seq:"))

(defn ^:private process-keymap-inner
  "Generates a blessed-keymap"
  [keymap ingress egress parent-keymap-id history]
  (let [[key id _ & sub-keymaps] keymap
        processed-sub-keymaps (when (not-empty sub-keymaps)
                                (map (fn [sub-keymap] (process-keymap-inner sub-keymap
                                                                            ingress egress
                                                                            id history))
                                     sub-keymaps))]
    (reduce #(merge-with comp %1 %2)
            {key #(when (= parent-keymap-id
                           ;; History is a list of keymaps which look like:
                           ;;  [key id desc & sub-keymapsngs]
                           ;; We want to get the keymap ID
                           (-> @history first second))
                    ;; Make sure only the initial keypress is valid
                    ;; for a given navigation seq
                    (let [awaited-id (r/atom nil)]
                      ;; Emit keymap events
                      (go
                        (let [nav-id (gen-nav-seq)]
                          ;; Start navigation seq
                          (>! egress {:type :nav-forward-start
                                      :id nav-id
                                      :value [keymap :forward]})
                          (reset! awaited-id nav-id)))
                      ;; Capture keymap events
                      (go
                        (let [{:keys [type id] :as event} (<! ingress)]
                          (print "Received ingress event:" event)
                          (condp = type
                            ;; Wait for the consumer to end the nagivation seq
                            :nav-forward-end (do
                                               ;; "Block" until the awaited ID arrives
                                               (when (not= id @awaited-id)
                                                 (throw (js/Error (str "Unexpected ingress event ID: " id))))
                                               ;; This generates a list, not a vector
                                               ;; So, history is "stack"
                                               (swap! history conj keymap)
                                               ;; Allow keypresses again
                                               (reset! awaited-id nil))
                            :nav-backward-start (swap! history pop)
                            (throw (js/Error (str "Unexpected ingress event type: " type))))))))}
            processed-sub-keymaps)))

(defn process-keymap
  "
  Generates `blessed` keybindings given a `keymap`
    * keymap - See (TODO: Link namespace when it's finalized).
    * ingress - Channel for accepting keymap event requests.
                Primarily for navigation (with dependence to async logic).
  Returns:
  ```
  {:keymap  - `blessed` keybindings
   :ingress - Channel for receiving keymap events.
   :egress - Channel for emitting keymap events.
   :within-keymap-history? - Fn with 1 arg: `keymap-d`.
                             Returns `true` if `keymap-id` is within navigation history
   :current-keymap? - Fn with with 1 arg: `keymap-d`.
                      Returns `true` if `keymap-id` is the currently selected
  ```
  "
  [keymap]
  (let [history (atom nil)
        ingress (chan)
        egress (chan)]
    {:keymap (process-keymap-inner keymap ingress egress nil history)
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
     :ingress ingress
     :egress egress}))

(defn process-keymap-events
  [ingress egress after-egress]
  (go-loop []
    (let [{:keys [type id] :as event} (<! egress)]
      (<! (timeout 1000))
      (print "Received egress event:" {:type type :id id})
      (after-egress event)
      (condp = type
        :nav-forward-start (>! ingress {:type :nav-forward-end
                                        :id id})))
    (recur)))

(defn test-component
  [debug-ui]
  (r/with-let [state (r/atom {:show-keymap-helper? true})
               {:keys [keymap
                       within-keymap-history?
                       current-keymap?
                       ingress egress]} (process-keymap k/workflow-keymap)
               go-back #(go (>! ingress {:type :nav-backward-start
                                         :id (gen-nav-seq)}))
               meta-keymap {["?"] #(swap! state update :show-keymap-helper? not)
                            ["!"] #(swap! state update :show-debug-ui? not)
                            ["g"] go-back}]
    (process-keymap-events
     ingress
     egress (fn [{:keys [value]}]
              (let [[keymap _direction] value]
                (swap! state assoc :selected keymap))))
    (with-keys @screen (merge meta-keymap keymap)
      [:<>
       [:box#main {:top 0
                   :height "75%"
                   :left 0
                   :width "100%"}
        ;; Box for listing/choosing a connection
        (when (within-keymap-history? :connections/view)
          [ccc/selector
           {:top 0 :height "100%" :left 0 :width "10%"}
           {:focused? #(current-keymap? :connections/view)}
           @(rf/subscribe [::cfc/registry])
           @(rf/subscribe [::cfc/selected-name])
           (fn [connection-name]
             (rf/dispatch [::cfc/select connection-name]))])
        ;; Box for configuring a connection
        (when (within-keymap-history? :connections/view)
          [ccc/configurator
           {:top 0 :height "100%" :left "10%" :width "30%"}
           {:focused? #(current-keymap? :connection/edit)}
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
