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
  {:id        123
   :alias     \"Production Cluster\"
   :url       \"my-kafka.com\"
   :username  \"abc\"
   :password  \"123\"}
  ```
  * prompts - Struct that decides which prompt (if any) is shown.
  Example:
  ```
  {:alias     false
   :url       true
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
      (seq->components (map (fn [[label key]] [label (config key)])
                            [["Alias" :alias]
                             ["URL" :url]
                             ["Username" :username]
                             ["Password" :password]])
                       (fn [idx [label value]] ; e.g. k = :url, v = "kafka-cluster.com"
                         [:box {:top (* idx 2) ; Essentially gives a </br>
                                :width "80%"
                                :left 0
                                :height "30%"
                                :style {:fg :white}
                                :content (str label ": " value)}])
                       {:with-index? true})
      (seq->components prompts
                       (fn [[k v]] ; e.g. k = :url, v = true
                         (when v [single-field-prompt {:heading (name k)
                                                       :initial-value (get config k)
                                                       :on-submit (get-in prompt-cbs [:on-submit k])
                                                       :on-cancel (get-in prompt-cbs [:on-cancel k])}])))])])
