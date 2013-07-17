(ns nudge.types
  (:use [nudge.debug])
  (:require [digest]))

(def debug-db (atom nil))

(defrecord Vhash [hash])

(defmethod clojure.core/print-method Vhash
  [value writer]
  (try
    (.write writer (str "Vhash["
                        (str (or (nudge.store/fetch @debug-db [value :value])
                                 (:hash value)))
                        "]"))
    (catch Exception e      
      (.write writer (str e)))))

(defn vhash? [value]
  (= nudge.types.Vhash (type value)))

(defn vhash
  "value hash is based on pr-str representation, since serializers could change"
  [value]
  (Vhash. (-> value pr-str digest/sha1)))

(defrecord IDRef [id])

(defn id-ref [id]
  (IDRef. id))

(defn id-ref? [value]
  (= nudge.types.IDRef (type value)))

(defprotocol Derefable
  (deref-value [this db]))
