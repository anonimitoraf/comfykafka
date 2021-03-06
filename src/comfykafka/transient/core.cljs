(ns comfykafka.transient.core
  (:require
   [cljs.core.async :refer [chan >! <!] :refer-macros [go go-loop]]
   [clojure.string :refer [join]]
   [comfykafka.components.connections :as connection-components]
   [comfykafka.components.generic :refer [list-box]]
   [comfykafka.core :refer [screen]]
   [comfykafka.flows.connection :as connection-flows]
   [comfykafka.flows.keymap :as keymap-flows]
   [comfykafka.flows.topic :as topic-flows]
   [comfykafka.keys :refer [with-keys]]
   [comfykafka.transient.keys :refer [connections-keymap settings-keymap]]
   [comfykafka.utils :refer [event> filter-first try-pop <sub]]
   [reagent.core :as r]))

(def keymap
  "
  A keymap is comprised of:
  [hotkey id desc & sub-keymaps]
  "
  ["*" :root nil
   connections-keymap
   settings-keymap])

(defn process-events
  "
  Given a keymap, process events run against the keymap.
  Returns a channel that streams the resulting states.

  Kinds of events:
  * forward navigation start
    {:type - :nav|->
     :hotkey
     :id
     :hook - (optional) fn of type: key-id => void}

  * forward navigation end
    {:type :nav->|
    :hotkey
    :id}

  * backward navigation start
    {:type :nav<-|}

  Other args:
  * on-nav|-> - Falled (1 arg: key-id) when a `nav|->` event is received.
  "
  [keymap events]
  (let [states (atom [])
        states-channel (chan)
        misc-state (atom {})]
    (add-watch states :changes (fn [_ _ _ new-state]
                                 (go (>! states-channel new-state))))
    ;; We emit the initial state after `add-watch`
    (swap! states conj {:current keymap})
    (go-loop []
      (let [{:keys [type hotkey id hook]} (<! events)
            current-keymap (-> @states last :current)
            [_ _ _ & sub-keymaps] current-keymap
            new-keymap (filter-first #(= (first %) hotkey)
                                     sub-keymaps)
            [_ new-key-id _ & _] new-keymap]
        (condp = type
          :nav|-> (if (nil? (:pending-nav-id @misc-state))
                    (do (swap! misc-state assoc :pending-nav-id id)
                        (and hook (hook new-key-id)))
                    (print (str "Unexpected :nav|-> with ID: " id ". Ignoring...")))
          :nav->| (if (nil? new-keymap)
                    (print (str "Unexpected hotkey:"  hotkey ". Ignoring..."))
                    (do
                      ;; This can happen due to race conditions
                      ;;causing > 1 navigation sequences to be started
                      (when (not= (:pending-nav-id @misc-state) id)
                        (throw (js/Error (str "Unexpected :nav->| ID: " id))))
                      (swap! states conj {:previous current-keymap
                                          :current new-keymap})
                      (swap! misc-state dissoc :pending-nav-id)))
          :nav<-| (do (swap! states try-pop)
                      (swap! misc-state dissoc :pending-nav-id))
          (throw (js/Error (str "Unexpected event type: " type)))))
      (recur))
    states-channel))

(defn gen-nav-seq-id [] (gensym "nav-seq-id:"))

(defn acc-hotkeys
  "Accumulate all the hotkeys in the keymap. Duplicates are removed."
  [keymap]
  (let [[hotkey _ _ & sub-keymaps] keymap]
    (if (nil? sub-keymaps)
      hotkey ; base-case
      (concat [hotkey] (->> sub-keymaps
                            (map acc-hotkeys)
                            flatten
                            dedupe)))))

(defn make-keybindings
  "
  This is a dumb function that just emits hotkeys
  to the events channel.
  "
  [keymap events]
  (->> (acc-hotkeys keymap)
       (map (fn [hotkey]
              {[hotkey] #(go (let [id (gen-nav-seq-id)]
                               (>! events
                                   {:type :nav|->
                                    :hotkey hotkey
                                    :id id
                                    :hook (fn [key-id]
                                            (tap> (str "Pressed: " key-id))
                                            (event> [::keymap-flows/process-key key-id
                                                     {:type :nav->|
                                                      :hotkey hotkey
                                                      :id id}]))})))}))
       (apply merge)))

(defn within-keymap-states?
  [states keymap-id]
  (->> (map :current states)
       (reduce (fn [acc [_ id _]] (conj acc id)) [])
       (some #{keymap-id})))

(defn current-keymap?
  [states keymap-id]
  (let [[_ id _] (-> states last :current)]
    (=  id keymap-id)))

(defn test-component
  [debug-ui]
  (r/with-let [state (r/atom {:show-keymap-helper? true})
               keymap-events (<sub [::keymap-flows/events-ch])
               keymap-states (process-events keymap keymap-events)
               keybindings (make-keybindings keymap keymap-events)
               _ (go-loop []
                   (swap! state assoc :keymap-states (<! keymap-states))
                   (recur))
               go-back #(go (>! keymap-events {:type :nav<-|}))
               meta-keybindings {["?"] #(swap! state update :show-keymap-helper? not)
                                 ["!"] #(swap! state update :show-debug-ui? not)
                                 ["g"] go-back}]
    (with-keys @screen (merge keybindings meta-keybindings)
      (let [within-keymap-states?* #(within-keymap-states? (:keymap-states @state) %)
            current-keymap?* #(current-keymap? (:keymap-states @state) %)]
        [:<>
         [:box#main {:top 0
                     :height "75%"
                     :left 0
                     :width "100%"}
          ;; Box for listing/choosing a connection
          (when (within-keymap-states?* :connections/view)
            ;; TODO Move these into r/with-let
            (let [choices (->> (<sub [::connection-flows/registry])
                               (map (fn [c] {:label (c :alias)
                                             :value (c :id)})))
                  selected (<sub [::connection-flows/selected-id])
                  do-select #(event> [::connection-flows/select %])
                  position {:top 0 :height "40%" :left 0 :width "20%"}
                  focused? #(current-keymap?* :connections/view)]
              [list-box choices selected do-select {:focused? focused?
                                                    :position position
                                                    :label "Connections"}]))
          ;; Box for configuring a connection
          (when (within-keymap-states?* :connections/view)
            (let [selected (<sub [::connection-flows/selected])]
              [connection-components/configurator
               {:top "40%" :height "60%" :left 0 :width "20%"}
               {:focused? #(current-keymap?* :connection/edit)
                :label (:alias selected)}
               selected
               {:url      (current-keymap?* :connection-edit/url)
                :username (current-keymap?* :connection-edit/username)
                :password (current-keymap?* :connection-edit/password)}
               {:on-submit {}
                :on-cancel {:url go-back
                            :username go-back
                            :password go-back}}]))
          ;; Box for listing/choosing a topic
          (when (within-keymap-states?* :topics/view)
            (let [choices (->> (<sub [::topic-flows/registry])
                               (map (fn [t] {:label t :value t})))
                  selected (<sub [::topic-flows/selected-name])
                  do-select #(event> [::topic-flows/select %])
                  position {:top 0 :height "100%" :left "20%" :width "20%"}
                  focused? #(current-keymap?* :topics/view)]
              [list-box choices selected do-select {:focused? focused?
                                                    :position position
                                                    :label "Topics"}]))]
         (when (:show-keymap-helper? @state)
           [:box {:top "75%"
                  :height "25%+1"
                  :left 0
                  :width "100%"
                  :border {:type :line}
                  :style {:border {:fg :cyan}}
                  :content (let [[_ _ _ & sub-keymaps] (-> @state :keymap-states
                                                           last :current)
                                 breadcrumbs (->> (:keymap-states @state)
                                                  (reduce (fn [acc {:keys [current]}]
                                                            (let [[_ _ desc] current]
                                                              (conj acc desc)))
                                                          [])
                                                  (filter (comp not nil?))
                                                  (join " > "))]
                             (str breadcrumbs "\n" "\n"
                                  (->> sub-keymaps
                                       ;; TODO Utilize colored strings
                                       (map (fn [[key _ desc]] (str key " - " desc)))
                                       (join "\n"))))}
            [:line {:top 1
                    :orientation :horizontal
                    :style {:fg :cyan}}]])
         (when (:show-debug-ui? @state) debug-ui)]))))
