(ns comfykafka.transient.actions)

(defn todo
  "Placeholder for TODO tasks"
  [& args]
  (print (str "TODO: " args)))

(defn show-connections
  []
  (todo :show-connections))

(defn do-connect
  [connection]
  (todo :do-connect connection))

(defn create-connection
  [config]
  (todo :create-connection config))

(defn edit-connection
  [config]
  (todo :edit-connection config))

(defn save-connection
  [connection]
  (todo :save-connection connection))

(defn delete-connection
  [config]
  (todo :delete-connection config))

(defn delete-connection-prompt-answer
  [y-or-n]
  (todo :delete-connection-prompt-answer y-or-n))
