(ns nudge.query
  (:require [nudge.store :as store]            
            [nudge.db    :as db])
  (:use     [nudge.debug]
            [nudge.types]))

(defrecord Restraint [name])

(defn restraint* [name]
  (Restraint. name))

(defmacro ?
  "logic variables to restrain queries"
  [symbol]
  (assert (symbol? symbol) "Restraint name must be a symbol.")
  `(restraint* '~symbol))

(defn restraint? [value]
  (= (class value) nudge.query.Restraint))

(defn subquery? [keyword]
  (= (first (name keyword)) \*))

(defn direct? [val]
  (and (not (= (type val) nudge.query.Restraint))))

(defn doc [id]
  {:nudge/id id})

(defn bind
  "takes a query map and binds a value in it.
returns a new query-map with a bound restraint"
  [query-map key restraint value]
  (if-let [bound-to (get-in query-map [:bindings restraint])]
    (when (= bound-to value)
      query-map)
    (assoc-in query-map [:bindings restraint] value)))

(defn sort-conditions
  "takes a query and returns a sorted list of condititons"
  [query]
  (sort-by (fn [[key val]]
             (cond
               ;; fully defined
               (and (not (subquery? key))
                    (keyword? key)
                    (direct? val)) 
               -2
               (restraint? val) 
               -1
               :else 0))
           query))

(declare query)

(defn validate
  "validates a document and returns the updated doc or nil"
  [query-map]
  (let [[key condition-value] (first (:conditions query-map))
        doc-value            (get (:doc query-map) key)]
    (cond (empty? (:conditions query-map))
          query-map

          (= doc-value condition-value)
          (recur (assoc query-map
                   :conditions
                   (rest (:conditions query-map))))
          
          (restraint? condition-value)
          (when-let [bound (bind query-map
                                 key
                                 condition-value
                                 doc-value)]
            (recur (assoc bound
                     :conditions
                     (rest (:conditions query-map)))))
          
          (subquery? key)
          (let [subquery {:conditions (sort-conditions condition-value)
                          :bindings   (:bindings query-map)}
                result (query subquery)]
            (recur 
             (-> query-map
                 (assoc-in [:doc key] result)
                 (assoc :conditions (rest (:conditions query-map))))))
            
            ;; when we have a map, we have to recursively validate its parts
          (map? condition-value)
          (when-let [valid (validate (assoc query-map
                                       :doc        doc-value
                                       :conditions 
                                       (sort-conditions condition-value)))]
            (recur (-> query-map
                       (assoc-in [:doc key] (:doc valid))
                       (assoc :bindings (:bindings valid))
                       (assoc :conditions
                         (rest (:conditions query-map)))))))))

(defn query
  "Takes a query map and returns all documents matching it."
  [query-map]
  (let [db       (db/get-db)
        key-val  (first (:conditions query-map))
        traverse (map #(deref-value % db)
                      (store/fetch db [(vhash key-val) :key-val-in]))]
    (into #{} (map :doc
                   (keep #(and (:nudge/id %) ; only documents
                               (validate (assoc query-map :doc %)))
                         traverse)))))

(defn assoc-maybe [map kvs]
  (if-not (empty? kvs)
    (apply assoc map kvs)
    map))

(defn q
  [& kvs-and-query-form]
  (assert (odd? (count kvs-and-query-form)) "q always takes an odd number of arguments, kv-pairs and body.")
  (query
   (assoc-maybe {:conditions (sort-conditions (last kvs-and-query-form))}
                (partition 2 kvs-and-query-form))))

