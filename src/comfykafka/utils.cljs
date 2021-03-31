(ns comfykafka.utils
  (:require [re-frame.core :as rf]))

(defn filter-first
  [pred coll]
  (->> coll
       (filter pred)
       first))

(defn try-pop
  "Like pop but does not throw an error if coll is empty."
  [coll]
  (if (empty? coll) coll (pop coll)))

(defn event> [req] (rf/dispatch req))

(defn <sub [req] @(rf/subscribe req))
