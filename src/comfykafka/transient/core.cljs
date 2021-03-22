(ns comfykafka.transient.core
  (:require [clojure.string :refer [join]]
            [cljs.core.async
             :refer [chan >! <! timeout]
             :refer-macros [go go-loop]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [comfykafka.utils :refer [filter-first try-pop]]
            [comfykafka.keys :refer [with-keys]]
            [comfykafka.core :refer [screen]]
            [comfykafka.flows.connection :as cfc]
            [comfykafka.components.connections :as ccc]
            [comfykafka.transient.keys :as k]
            [comfykafka.transient.actions]))

(def keymap
  "
  A keymap is comprised of:
  [hotkey id desc & sub-keymaps]
  "
  ["*" :root "comfykafka"
   ["c" :connections/view "Connections"
    ["c" :connection/connect "Connect"]
    ["e" :connection/edit "Edit"
     ["l" :conection-edit/url "URL"]]]
   ["s" :settings/view "Settings"
    ["e" :settings/edit "Edit"]
    ["r" :settings/reset "Reset"]]])

(defn process-events
  "
  Given a keymap, process events run against the keymap.
  Returns a channel that streams the resulting states.

  Kinds of events:
  - forward navigation start
    {:type :nav|->
     :hotkey
     :id}

  - forward navigation end
    {:type :nav->|
    :hotkey
    :id}

  - backward navigation start
    {:type :nav<-|}
  "
  [keymap events]
  (let [states (atom [{:current keymap}])
        states-channel (chan)
        misc-state (atom {})]
    (add-watch states :changes (fn [_ _ _ new-state]
                                 (go (>! states-channel new-state))))
    (go-loop []
      (let [{:keys [type hotkey id]} (<! events)]
        (condp = type
          :nav|-> (if (nil? (:pending-nav-id @misc-state))
                    (swap! misc-state assoc :pending-nav-id id)
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
                        (when (not= (:pending-nav-id @misc-state) id)
                          (throw (js/Error (str "Unexpected :nav->| ID: " id))))
                        (swap! states conj {:previous current-keymap
                                            :current new-keymap})
                        (swap! misc-state dissoc :pending-nav-id))))
          :nav<-| (do (swap! states try-pop)
                      (swap! misc-state dissoc :pending-nav-id))
          (throw (js/Error (str "Unexpected event type: " type)))))
      (recur))
    states-channel))

;; (def events
;;   [{:type :nav|-> :hotkey "c" :id 1}
;;    {:type :nav->| :hotkey "c" :id 1}
;;    {:type :nav|-> :hotkey "e" :id 3}
;;    {:type :nav|-> :hotkey "e" :id 4} ;; This should get ignored
;;    {:type :nav->| :hotkey "e" :id 3}
;;    {:type :nav<-|}])

;; (def dbg_processed (process-events keymap events))

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

;; (acc-hotkeys keymap)

(defn gen-nav-seq-id [] (gensym "nav-seq-id:"))

(defn make-keybindings
  "
  This is a dumb function that just emits hotkeys
  to the events channel.
  "
  [keymap events]
  (->> (acc-hotkeys keymap)
       (map (fn [hotkey]
              {[hotkey] #(go (let [id (gen-nav-seq-id)]
                               (>! events {:type :nav|->
                                           :hotkey hotkey
                                           :id id})
                               (<! (timeout 1000))
                               (>! events {:type :nav->|
                                           :hotkey hotkey
                                           :id id})))}))
       (apply merge)))

;; (make-keybindings keymap (chan))

(defn test-component
  [debug-ui]
  (r/with-let [state (r/atom {:show-keymap-helper? true})
               keymap-events (chan 1024)
               keybindings (make-keybindings keymap keymap-events)
               meta-keybindings {["?"] #(swap! state update :show-keymap-helper? not)
                                 ["!"] #(swap! state update :show-debug-ui? not)
                                 ["g"] #()}
               keymap-states (process-events keymap keymap-events)]
    ;; Visualizing the states via REPL
    (go-loop []
      (def dbg_state (<! keymap-states))
      (recur))
    (with-keys @screen (merge keybindings meta-keybindings)
      [:<>
       ;; [:box#main {:top 0
       ;;             :height "75%"
       ;;             :left 0
       ;;             :width "100%"}
       ;;  ;; Box for listing/choosing a connection
       ;;  (when (within-keymap-history? :connections/view)
       ;;    [ccc/selector
       ;;     {:top 0 :height "100%" :left 0 :width "10%"}
       ;;     {:focused? #(current-keymap? :connections/view)}
       ;;     @(rf/subscribe [::cfc/registry])
       ;;     @(rf/subscribe [::cfc/selected-name])
       ;;     (fn [connection-name]
       ;;       (rf/dispatch [::cfc/select connection-name]))])
       ;;  ;; Box for configuring a connection
       ;;  (when (within-keymap-history? :connections/view)
       ;;    [ccc/configurator
       ;;     {:top 0 :height "100%" :left "10%" :width "30%"}
       ;;     {:focused? #(current-keymap? :connection/edit)}
       ;;     @(rf/subscribe [::cfc/selected])
       ;;     {:url      (current-keymap? :connection/edit-url)
       ;;      :username (current-keymap? :connection/edit-username)
       ;;      :password (current-keymap? :connection/edit-password)}
       ;;     {:on-submit {}
       ;;      :on-cancel {:url go-back
       ;;                  :username go-back
       ;;                  :password go-back}}])]
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
