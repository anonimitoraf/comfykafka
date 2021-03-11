(ns comfykafka.utils)

(defn find-first
  [coll pred]
  (->> coll
       (filter pred)
       first))
