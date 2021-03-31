(ns comfykafka.components.generic
  (:require [comfykafka.core :refer [screen]]
            [comfykafka.keys :refer [with-keys]]
            [comfykafka.theme :as theme]
            [reagent.core :as r])
  (:require-macros [comfykafka.components.generic]))

(defn with-color
  "Generates a colored string via blessed tags"
  [color text]
  (str "{" (name color) "-fg}" text))

(defn ^:private make-label
  [label focused?]
  (and label
       (str " " (with-color (if (focused?)
                              theme/default-text-focused
                              theme/default-text)
                  label)
            " ")))

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

(defn box
  "Wrapper around the Box component which reduces repetition"
  [label position {:keys [focused?] :as opts} & content]
  [:box (merge {:border {:type :line}
                :style {:border {:fg (if (focused?)
                                       theme/default-container-border-focused
                                       theme/default-container-border)}}
                :focused (focused?)
                :tags true}
               position
               (when label {:label (make-label label focused?)}))
   (map #(with-meta % {:key (gensym "key-")}) content)])

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

(defn list-box
  "
  Args:
  * choices   - list of {:label :value}
  * selected  - currently selected value
  * on-select - void fn with 1 arg: value of current choice
  * opts
   * focused?
   * position
   * label
  "
  [choices selected on-select {:keys [focused? position label]}]
  (r/with-let [selected-index (r/atom nil)
               do-select (fn [direction]
                           (swap! selected-index
                                  (fn [i] (mod ((condp = direction
                                                  :up dec
                                                  :down inc)
                                                (or i -1))
                                               (count choices))))
                           (on-select (-> choices
                                          (nth @selected-index)
                                          :value)))]
    (with-keys @screen {["up" "k"] #(when (focused?) (do-select :up))
                        ["down" "j"] #(when (focused?) (do-select :down))}
      [:box (merge {:label (make-label label focused?)
                    :border {:type :line}
                    :tags true
                    :style {:border {:fg (if (focused?)
                                           theme/default-container-border-focused
                                           theme/default-container-border)}}}

                   position)
       (for [[idx {:keys [value label]}] (map-indexed vector choices)]
         [:box {:key idx
                :top idx
                :style (if (= value selected)
                         theme/list-item-selected
                         theme/list-item-unselected)
                :height 1
                :content label}])])))
