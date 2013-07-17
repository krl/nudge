(ns nudge.db
  (:use     [nudge.helpers]
            [nudge.debug]
            [nudge.types]
            [clojure.set :only [union]]
            [clojure.pprint :only [pprint]])
  (:require [nudge.convert :as convert]
            [clojure.java.io :as io]
            [nudge.store :as store]
            [nudge.filters :as filters]))

(defrecord Transaction [nudge id])

(def ^:dynamic *current-transaction*)

(defmacro transact
  "start a subtransaction"
  [& body]
  `(dosync
   ~@body))

(defmacro with-db
  "set db"
  [db & body]
  `(binding [*current-transaction* (Transaction. ~db (generate-id))]
     (reset! nudge.types/debug-db ~db)
     (dosync
      ~@body)))

(defn get-db []
  (or (:nudge *current-transaction*)
      (throw (Exception. "must be called within a transaction"))))

(defn write
  "writes values to nudge, takes care of filtering and indexing"
  [& values]
  (let [db               (get-db)        
        filtered-values  (map (partial filters/conform db) values)
        converted-values (convert/convert-values filtered-values)
        references       (convert/get-references converted-values)]
    
    ;; store the values
    (doseq [[hash value] converted-values]
      (store/store db [hash :value] value))

    ;; index id of documents (only)
    (doseq [doc filtered-values]
      (let [key        [(:nudge/id doc) :id-value]
            old-values (or (store/fetch db key) '())
            value-hash (vhash doc)]
        (when-not (= value-hash (first old-values))
          (store/store db
                     key
                     (conj old-values value-hash)))))

    ;; index references to all values
    (doseq [[key val] references]
      (let [old-val (store/fetch db key)]
        (store/store db key (union old-val val))))

    filtered-values))

(defn get-document-by-id [id]
  (let [db      (get-db)
        current (first (store/fetch db [id :id-value]))]
    (when current
      (deref-value current db))))

;; extending derefs

(extend nudge.types.IDRef
  Derefable
  {:deref-value (fn [this db]
                  (or
                   (get-document-by-id (:id this))
                   this))})

(extend nudge.types.Vhash
  Derefable
  {:deref-value (fn [this db]
                  (deref-value
                   (store/fetch db [this :value])
                   db))})

(extend clojure.lang.PersistentArrayMap
  Derefable
  {:deref-value (fn [this db]
                  (into {} (map (fn [[key val]]
                                  [(deref-value key db)
                                   (deref-value val db)])
                                this)))})

(extend java.lang.Comparable
  Derefable
  {:deref-value (fn [this db]
                  this)})