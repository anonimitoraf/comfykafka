(ns comfykafka.wrappers.js)

(defn sleep
  "Returns a Promise which resolves after `ms`"
  [ms]
  (new js/Promise (fn [resolve]
                    (js/setTimeout resolve ms))))
