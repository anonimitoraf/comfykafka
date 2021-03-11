(ns comfykafka.components.connections
  (:require [comfykafka.components.generic :refer [box
                                                   single-field-prompt
                                                   seq->components]]))

(defn configurator
  " Component for configuring a Connection.
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
  [box " Connection " position opts
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
  * connections - a list of connection structs.
  Example of a connection:
  ```
  {:name 'prod cluster'
   :url 'kafka-cluster.com'
   :username'abc'
   :password '123'}
  ```

  * on-select - callback with 1 arg: the selected connection's name.
  "
  [position {:keys [focused?] :as opts} connections on-select]
  [box " Connections " position opts
   [:list {:keys true
           :items (map :connection/name connections)
           ;; FIXME The first item gets highlighted despite not being selected
           :style {:selected {:fg :green}}
           :focused true
           ;; TODO Restrict duplicate IDs
           :on-select (fn [selected-id]
                        ;; (tap> (.-content selected-id))
                        (on-select (.-content selected-id)))}]])
