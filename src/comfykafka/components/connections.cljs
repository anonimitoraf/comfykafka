(ns comfykafka.components.connections
  (:require [reagent.core :as r]
            [comfykafka.components.generic :refer [single-field-prompt
                                                   seq->components]]))

(defn configurator
  " Component for configuring a Connection.
  * config - Connection configuration. For creation, expected to be nil
  Example:
  ```
  {:url       \"my-kafka.com\"
   :username  \"abc\"
   :password  \"123\"}
  ```
  * prompts - Struct that decides which prompt (if any) is shown.
  Example:
  ```
  {:url       true
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
  [config prompts prompt-cbs]
  [:box {:top 0
         :height "75%"
         :left 0
         :width "100%-2"
         :border {:type :line}
         :style {:border {:fg :yellow}}}
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
                                                    :on-cancel (get-in prompt-cbs [:on-cancel k])}])))])
