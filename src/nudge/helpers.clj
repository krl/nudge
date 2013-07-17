(ns nudge.helpers
  (:use [clojure.set :only [union]]
        [nudge.types]
        [nudge.debug]))

(defn push-to-set [set value]
  (conj (or set #{}) value))

(defn union-to-set [set value]
  (union (or set #{}) value))

(defn push-to-set-in-map [map & kvs]
  (let [result 
        (reduce (fn [coll [key val]]            
                  (assoc coll 
                    key (conj (or (get map key) #{})
                              val)))
                map
                (partition 2 kvs))]
    result))

(defn generate-date []
  (java.util.Date.))

(defn generate-id []
  (java.util.UUID/randomUUID))
