(ns comfykafka.transient.keybindings)

(def topic-keymap
  ["t" :topics/view
   ["o" :topic/overview]

   ["w" :topic/write
    ["x" :message/write-external]
    ["y" :topic/write-confirm]
    ["n" :topic/write-cancel]]

   ["r" :topic/read
    ["p" :partitions/select
     ["m" :message/view
      ["c" :message/copy]
      ["x" :message/read-external]]]]])

(def keymap
  ["c" :connections/view
   ["c" :connection/connect
    topic-keymap]
   ["n" :connection/create
    ["s" :connection/save]]
   ["e" :connection/edit
    ["s" :connection/save]]
   ["d" :connection/delete
    ["y" :connection/delete-confirm]
    ["n" :connection/delete-cancel]]])
