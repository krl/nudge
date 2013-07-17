(ns nudge.filters
  (:use [nudge.helpers]
        [nudge.types]))

(def filters* (atom '{}))

(defmacro deffilter [name docstring binding & body]
  (assert (= (count binding) 2) "deffilter must take [db doc] as parameters")
  `(swap! filters* assoc '~name
          (fn ~binding ~@body)))

(defn conform
  "makes a value conform to schemas or returns false"
  [db value]
  (reduce (fn [val [name f]]
            (f db val))
          value
          @filters*))

;; core document schemas

(deffilter id-and-creation
  "A document is defined as a map with a :nudge/id property. 
This rule adds an id and creation time if none is available."
  [db doc]
  (if-not (:nudge/id doc)
    (assoc doc 
      :nudge/id      (generate-id)
      :nudge/created (generate-date))
    doc))

;; (deffilter type-namespacing
;;   "The :nudge:type property must be present and namespaced"
;;   [db doc]
;;   (if-let [type (:nudge/type doc)]
;;     (assert 
;;     doc)))