(ns org.altlaw.util.merge-fields
  (:refer clojure.set))

(defn value-sets
  "Takes a map, converts all values, whether collections or single
  values, to sets."
  [map]
  (reduce (fn [m [k v]]
              (assoc m k (if (coll? v) (set v) #{v})))
          {}
          map))

(def *multivalued*
     #{:dockets :citations :titles :files :hashes
       :incites :outcites :links})

(defn singleize
  "For use in a reduce over a map of keys to sets.  For keys that
  should have single values, uses the first value in the set.  For
  keys that should have multiple values, converts the set to a
  vector."
  [map [k v]]
  (if (seq v)  ; not empty
    (assoc map k
           (if (*multivalued* k)
             (vec v)
             (first v)))
    map))

(defn merge-fields
  "Merges fields from documents, creating collections or single values
  where appropriate."
  [docs]
  (reduce singleize {}
          (apply merge-with union (map value-sets docs))))
