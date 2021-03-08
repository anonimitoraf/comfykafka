(ns comfykafka.components.connections
  (:require [reagent.core :as r]
            [comfykafka.components.generic :refer [single-field-prompt]]))

(defn configurator
  "
  Component for configuring a Connection.

  * config - Connection configuration. For creation, expected to be nil. For example:
  ```
  {:url       \"my-kafka.com\"
   :username  \"abc\"
   :password  \"123\"}
  ```

  * prompt - Struct that decides which prompt (if any) is shown. For example:
    ```
    {:url       true
     :username  false
     :password  false}
    ```
  "
  [config prompt]
  [:box {:top 0
         :height "75%"
         :left 0
         :width "100%-2"
         :border {:type :line}
         :style {:border {:fg :yellow}}}
   (when (:url config)
     (print "Show URL value?")
     [:box {:top 0
            :width "80%"
            :left 0
            :height "30%"
            :style {:fg :white}
            :content (str "URL: " (:url config))}])
   (when (:url prompt)
     [single-field-prompt {:heading "URL"
                           :initial-value (:url config)
                           :on-submit #(print "on-submit" %)
                           :on-cancel #(print "on-cancel")}])])
