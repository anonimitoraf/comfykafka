(ns comfykafka.transient.keys)

(def topic-keymap
  ["t" :topics/view "Topics"
   ["o" :topic/overview "Topic overview"]

   ["w" :topic/write "Write a Message"
    ["x" :message/write-external "Open external editor"]
    ["y" :topic/write-confirm "Yes, write"]
    ["n" :topic/write-cancel "Cancel"]]

   ["r" :topic/read "Read Messages"
    ["p" :partitions/select "Select a Partition"
     ["m" :message/view "Inspect Message"
      ["c" :message/copy "Copy"]
      ["x" :message/read-external "Open external editor"]]]]])

(def workflow-keymap
  ["c" :connections/view "Connections"
   ["c" :connection/connect "Connect"
    topic-keymap]
   ["n" :connection/create "New"
    ["s" :connection/save "Save"]]
   ["e" :connection/edit "Edit"
    ["l" :connection/edit-url "URL"]
    ["u" :connection/edit-username "username"]
    ["p" :connection/edit-password "password"]
    ["s" :connection/save "Save"]]
   ["d" :connection/delete "Delete"
    ["y" :connection/delete-confirm "Yes, delete"]
    ["n" :connection/delete-cancel "Cancel"]]])
