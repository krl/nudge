(ns nudge.convert
  (:use     [nudge.debug]
            [clojure.set :only [union]]
            [nudge.helpers]
            [nudge.types]))

(defn- atomic? [value]
  (and (not (coll? value))
       value))

(defn- replace-value
  "replace all values in value map with references"
  [value]
  (cond (or (vhash?  value)
            (atomic? value))
        value

        (map? value)
        (into {} (map (fn [[key val]]
                        [(vhash key)
                         (vhash val)])
                      value))
        :else
        (into (empty value)
              (map vhash value))))

(defn- replace-values-in-hv-map [hash-value-map]
  (into {} (map (fn [[key val]]
                  [key (replace-value val)])
                hash-value-map)))

(defn convert-value
  "converts a value into a vhash-value mapping"
  [value]
  (let [merger 
        (apply merge
               (cond (atomic? value)
                     nil
                     
                     (map? value)
                     (map (fn [[key val]]
                            (merge (convert-value key)
                                   (convert-value val)))
                          value)

                     (coll? value)
                     (map convert-value value)))]
    (if-not (atomic? value)
      (assoc merger (vhash value) value)
      merger)))



(defn get-references
  "takes one hash-value pair and returns the appropriate index."
  [hash-value-map]
  (reduce (fn [references [hash value]]
            (if (map? value)
              (reduce (fn [index key-val]
                        (push-to-set-in-map index
                                            [(vhash key-val) :key-val-in]
                                            hash))
                      references
                      value)
              references))
          {}
          hash-value-map))

(defn merge-recursive
  ([a b & rest]
     (apply merge-recursive
            (merge-recursive a b)
            rest))
  ([a b]
     (cond (or (nil? a) (nil? b))
           (or a b)

           (= a b)
           a

           (and (set? a))
           (union a b)

           (and (map? a))
           (reduce (fn [merger [key val]]
                     (assoc merger 
                       key (merge-recursive val (get a key))))
                   a b)))
  ([a] a))

(defn convert-values
  "converts a seq of values into a map of it's hashed components"
  [values]
  (apply merge-recursive (map convert-value values)))