(ns comfykafka.transient.keys)

(def topics-keymap
  ["t" :topics/view "Topics"
   ["i" :topic/inspect "Inspect"]])

(def connections-keymap
  ["c" :connections/view "Connections"
   ["c" :connection/connect "Connect"
    topics-keymap]

   ["n" :connection/new "New"
    ["l" :connection-edit/url "URL"]
    ["u" :connection-edit/username "Username"]
    ["p" :connection-edit/password "Password"]]

   ["e" :connection/edit "Edit"
    ["l" :connection-edit/url "URL"]
    ["u" :connection-edit/username "Username"]
    ["p" :connection-edit/password "Password"]]])

(def settings-keymap
  ["s" :settings/view "Settings"
   ["e" :settings/edit "Edit"]
   ["r" :settings/reset "Reset"]])
