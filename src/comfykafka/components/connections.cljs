(ns comfykafka.components.connections
  (:require [reagent.core :as r]
            [comfykafka.keys :refer [with-keys]]
            [comfykafka.core :refer [screen]]
            [comfykafka.theme :as theme]
            [comfykafka.components.generic :refer [box
                                                   single-field-prompt
                                                   seq->components]]))

(defn configurator
  "Component for configuring a Connection. Args:
  * position
  * opts - Example:
  ```
  {focused? - Fn that decides whether this component is focused.
              Primarily for theming and hotkeys}
  * config - Connection configuration. For creation, expected to be nil
  Example:
  ```
  {:url       \"my-kafka.com\"
   :name      \"Production Cluster\"
   :username  \"abc\"
   :password  \"123\"}
  ```
  * prompts - Struct that decides which prompt (if any) is shown.
  Example:
  ```
  {:url       true
   :name      false
   :username  false
   :password  false}
  ```
  * prompt-cbs - Struct that defines the callbacks for each prompt.
  Example:
  ```
  {:on-submit {:url (fn [val] (print \"Submitted URL value of: \" val))
               ... }}
  ```
  "
  [position {:keys [focused?] :as opts} config prompts prompt-cbs]
  [box nil position opts
   (if (nil? config)
     [:text "No connection selected"]
     [:<>
      (seq->components config
                       (fn [idx [k v]] ; e.g. k = :url, v = "kafka-cluster.com"
                         [:box {:top (* idx 2) ; Essentially gives a </br>
                                :width "80%"
                                :left 0
                                :height "30%"
                                :style {:fg :white}
                                :content (str (name k) ": " v)}])
                       {:with-index? true})
      (seq->components prompts
                       (fn [[k v]] ; e.g. k = :url, v = true
                         (when v [single-field-prompt {:heading (name k)
                                                       :initial-value (get config k)
                                                       :on-submit (get-in prompt-cbs [:on-submit k])
                                                       :on-cancel (get-in prompt-cbs [:on-cancel k])}])))])])

(defn selector
  "
  * connections - a list of connection structs. Example:
  ```
  {:name 'prod cluster'
   :url 'kafka-cluster.com'
   :username'abc'
   :password '123'}
  ```
  * opts - Example:
  ```
  {focused? - Fn that decides whether this component is focused.
              Primarily for theming and hotkeys}
  ```
  * on-move - callback for navigation, 1 arg: which is the connection name
  "
  [position
   {:keys [focused?] :as opts}
   connections
   selected-name
   on-move]
  (r/with-let [selected-index (r/atom nil)
               do-move (fn [direction]
                         (swap! selected-index
                                (fn [i] (mod ((condp = direction
                                                :up dec
                                                :down inc)
                                              (or i -1))
                                             (count connections))))
                         (on-move (-> connections
                                      (nth @selected-index)
                                      :connection/name)))]
    (with-keys @screen {["up" "k"] #(when (focused?) (do-move :up))
                        ["down" "j"] #(when (focused?) (do-move :down))}
      [:box (merge {:label " Connections "
                    :border {:type :line}
                    :style {:border {:fg (if (focused?)
                                           theme/default-container-border-focused
                                           theme/default-container-border)}}}

                   position)
       (for [[idx {:keys [:connection/name]}] (map-indexed vector connections)]
         [:box {:key idx
                :top idx
                :style (if (= name selected-name)
                         theme/list-item-selected
                         theme/list-item-unselected)
                :height 1
                :content name}])])))
