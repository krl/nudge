(ns irc
  (:use     [nudge.types]
            [nudge.debug]
            [nudge.query :only [q ?]]
            [nudge.store :only [memory-store]]
            [irclj.process :only [process-line]])
  (:require [irclj.core   :as irc]
            [irclj.events :as events]
            [nudge.db :as db]))

;; setup test data

(def State (atom {}))

(do
  (def testdata (memory-store))

  (db/with-db testdata
    (db/write

     {:nudge/id   :efnet
      :nudge/type :irc/network
      :nick      "ohatad"
      :name      "efnet"}

     {:nudge/type :irc/server

      :irc/network (id-ref :efnet)
      :host      "irc.inet.tele.dk"}      

     {:nudge/id   :kriget
      :nudge/type :irc/channel

      :network   (id-ref :efnet)
      :name      "#kriget"}

     {:nudge/type :irc/watcher
      :channel   (id-ref :kriget) })))

(db/with-db testdata 
  (q {:nudge/type :irc/watcher
      :network   {:nudge/id       (? network-id)
                  :*servers {:nudge/type :irc/server
                             :irc/network (? network-id)}}}))

(db/with-db testdata 
  (q {:nudge/id              :efnet
      :*servers {:nudge/type :irc/server
                 :irc/network (? network-id)}}))


(defn setup-irc-watchers []
  (doseq [watcher (q {:nudge/type :irc/watcher})]
    (let [network (-> watcher :network)]
      (when-not (get @State network))
      (swap! @State
             assoc-in [network :connection]
             (irc/connect (dbg (:host network))
                          (dbg (:port network 6667))
                          (dbg (:nick network)))))))


;; (irc/join connection "#kriget")

;; (irc/message connection "#kriget" "ok!")

;; (defmethod process-line "PRIVMSG" [msg c]
;;   (irc/message c (:nick msg) (pr-str msg)))
