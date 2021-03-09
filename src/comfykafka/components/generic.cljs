(ns comfykafka.components.generic
  (:require [reagent.core :as r]
            [comfykafka.theme :as theme])
  (:require-macros [comfykafka.components.generic]))

;; (defn text-box
;;   []
;;   (r/with-let [ref (r/atom nil)]
;;     ;; (def dbg_this this)
;;     [:textbox {:ref (fn [r]
;;                       (when r
;;                         (def dbg_r r)
;;                         (print "ref called")
;;                         (reset! ref r)))}]))

;; (defn text-box
;;   [{:keys [focused? initial-value]}]
;;   (let [ref (r/atom nil)]
;;     (r/create-class
;;      {:display-name "text-box"
;;       :component-did-mount
;;       (fn [_] (when @ref
;;                 (.setValue @ref (str initial-value))
;;                 (when focused? (.focus @ref))))

;;       :component-did-update
;;       (fn [_ _] (when focused? (.focus @ref)))

;;       :reagent-render
;;       (fn [{:keys [_focused? _initial-value]}]
;;         [:textbox {:left "50%"
;;                    :width "50%"
;;                    :border {:type :line}
;;                    :style {:border {:fg theme/text-box-container-border}}
;;                    :ref #(when % (reset! ref %))
;;                    :onSubmit (fn [input] (print input))
;;                    :onCancel (fn [input] (print input))
;;                    :inputOnFocus true}])})))

;; (defn my-component
;;   [x y z]
;;   (let [some (local but shared state)      ;; <-- closed over by lifecycle fns
;;         can  (go here)]
;;     (reagent/create-class                 ;; <-- expects a map of functions
;;      {:display-name  "my-component"      ;; for more helpful warnings & errors

;;       :component-did-mount               ;; the name of a lifecycle function
;;       (fn [this]
;;         (println "component-did-mount")) ;; your implementation

;;       :component-did-update              ;; the name of a lifecycle function
;;       (fn [this old-argv]                ;; reagent provides you the entire "argv", not just the "props"
;;         (let [new-argv (rest (reagent/argv this))]
;;           (do-something new-argv old-argv)))

;;         ;; other lifecycle funcs can go in here
;;       :reagent-render        ;; Note:  is not :render
;;       (fn [x y z]           ;; remember to repeat parameters
;;         [:div (str x " " y " " z)])})))

;; (reagent/render
;;  [my-component 1 2 3]         ;; pass in x y z
;;  (.-body js/document))

;; ;; or as a child in a larger Reagent component

;; (defn homepage []
;;   [:div
;;    [:h1 "Welcome"]
;;    [my-component 1 2 3]]) ;; Be sure to put the Reagent class in square brackets to force it to render!

(defn single-field-prompt
  [{:keys [heading
           initial-value
           on-submit
           on-cancel]}]
  (r/with-let [focused? (r/atom false)]
    [:textbox {:top "20%"
               :height 3
               :left "10%"
               :width "80%"
               :border {:type :line}
               :style {:border {:fg theme/text-box-container-border}}
               :label (str " " heading " ")
               :ref (fn [^js ref]
                      (when (and ref (not @focused?))
                        (reset! focused? true)
                        (.focus ref)
                        (.setValue ref (str initial-value))))
               :inputOnFocus true
               :onSubmit on-submit
               :onCancel on-cancel}]))

(defn plain-box
  "A plain box for debugging purposes. Imagine a `div` with some text on it"
  [content]
  [:box {:top "25%"
         :height "50%"
         :left "25%"
         :width "50%"
         :border {:type :line}
         :style {:border {:fg :red}}
         :label " SOME PLAIN BOX "
         :content content}])
