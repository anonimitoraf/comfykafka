(ns comfykafka.wrappers.kafka
  (:require [clojure.core.async
             :refer [chan >! <! close!]
             :refer-macros [go]]
            [clojure.core.async.interop :refer-macros [<p!]]
            ["kafkajs" :refer [Kafka]]))

(defn make-kafka-client
  [broker-urls]
  (new Kafka (clj->js {:brokers broker-urls})))

;; ______________________________ Admin ______________________________

(defprotocol IAdmin
  (list-topics [this] "List Kafka Topics")
  (create-topic [this name] "Create a Kafka Topic"))

(defrecord Admin [^js impl]
  IAdmin
  (list-topics [_]
    (let [ch (chan)]
      (go (>! ch (js->clj (<p! (.listTopics impl))))
          (close! ch))
      ch))
  (create-topic [_ name]
    (let [ch (chan)]
      (go (>! ch (<p! (.createTopics
                       impl
                       (clj->js {:topics [{:topic name}]}))))
          (close! ch))
      ch)))

(defn make-admin
  "Returns a one-off channel that emits an Admin"
  [^js client]
  (let [ch (chan)
        admin (.admin client)]
    (go (<p! (.connect admin))
        (>! ch (Admin. admin))
        (close! ch))
    ch))

(comment (go (let [_ (tap> "before make-admin")
                   admin (<! (-> ["localhost:9092"]
                                 make-kafka-client
                                 make-admin))]
               (tap> (<! (list-topics admin)))
               ;; (tap> (<! (create-topic admin "test-topic-1")))
               )))
