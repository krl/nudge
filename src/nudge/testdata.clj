(ns nudge.testdata 
  (:use     [nudge.types])
  (:require [nudge.db :as db]
            [nudge.store :as store]))

(def testdata (store/memory-store))

(db/with-db testdata
  (db/write

   {:nudge/id   ::efnet
    :nudge/type :irc/network      
    :name      "efnet"}

   {:nudge/type :irc/server

    :irc/network (id ::efnet)

    :name      "irc.inet.tele.dk"
    :host      "irc.inet.tele.dk"}      

   {:nudge/id   ::kriget
    :nudge/type :irc/channel

    :network   (id ::efnet)
    :name      "#kriget"}

   {:nudge/type :irc/watcher

    :channel   ::kriget
    :status    :watching}))