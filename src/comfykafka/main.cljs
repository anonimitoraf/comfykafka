(ns comfykafka.main
  "Main application entrypoint. Defines root UI view, cli-options,
  arg parsing logic, and initialization routine"
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.core.async :refer [<!] :refer-macros [go]]
            [mount.core :refer [defstate] :as mount]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [comfykafka.core :refer [render screen]]
            [comfykafka.demo.views :refer [demo]]
            [comfykafka.events]
            [comfykafka.resize :refer [size]]
            [comfykafka.subs]
            [comfykafka.views :as views]
            [comfykafka.wrappers.persistent-db :as persistent-db]
            [comfykafka.flows.connection :as connection-flow]
            [comfykafka.flows.keymap :as keymap-flow]))

(defn ui
  "Root ui view.
  Takes no arguments.
  Returns hiccup demo element to run the demo app."
  [_]
  [demo])

(def cli-options
  [["-p" "--port PORT" "port number"
    :default 80
    :parse-fn #(js/Number %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-v" nil "Verbosity level"
    :id :verbosity
    :default 0
    :update-fn inc]
   ["-h" "--help"]])

(defn args->opts
  "Takes a list of arguments.
  Returns a map of parsed CLI args."
  [args]
  (parse-opts args cli-options))

(defn init!
  "Initialize the application.
  Takes a root UI view function that returns a hiccup element and optionally
  a map of parsed CLI args.
  Returns rendered reagent view."
  [view & {:keys [opts]}]
  (mount/start)
  (rf/dispatch-sync [:init (:options opts) (size @screen)])
  (rf/dispatch-sync [::keymap-flow/init-events-ch])
  (go
    (rf/dispatch [::connection-flow/add
                  (<! (persistent-db/get-all :connections))]))
  (-> (r/reactify-component view)
      (r/create-element #js {})
      (render @screen)))

(defn main!
  "Main application entrypoint function. Initializes app, renders root UI view
  and initializes the re-frame app db.
  Takes list of CLI args.
  Returns rendered reagent view."
  [& args]
  (init! ui :opts (args->opts args)))

(set! *main-cli-fn* main!)
