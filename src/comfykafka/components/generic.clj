(ns comfykafka.components.generic)

(defmacro seq->components
  "Map a `seq` into components using the `render-fn`.
  One nice thing about this is that each seq elem is
  automatically given a unique `:key` metadata.

  * opts - {:with-index? - Decides whether the mapping is done via map-indexed}"
  ([seq render-fn] `(seq->components ~seq ~render-fn {}))
  ([seq render-fn opts]
   `(doall
     (let [f# (if (:with-index? ~opts) map-indexed map)]
       ;; (tap> f#)
       (map #(with-meta % {:key (gensym "key-")})
            (f# ~render-fn ~seq))))))
