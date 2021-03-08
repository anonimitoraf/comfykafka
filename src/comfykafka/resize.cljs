(ns comfykafka.resize
  (:require
   [re-frame.core :as rf]))

(defn size
  "Get hash-map with the rows and cols of the screen size."
  [^js screen]
  {:rows (.-rows screen)
   :cols (.-cols screen)})

(defn setup
  [^js screen]
  (.on screen "resize"
       (fn handle-resize
         [_]
         (rf/dispatch [:update {:terminal/size (size screen)}]))))
