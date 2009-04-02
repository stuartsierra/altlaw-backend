(ns org.altlaw.util.simpledb
  (:import (com.xerox.amazonws.sdb SimpleDB ItemAttribute)
           (java.util ArrayList)))

(defn- keystr [x]
  (if (keyword? x) (name x) (str x)))

(defn- make-attr-list [keyvals]
  (let [attrs (ArrayList.)]
    (doseq [[k v] keyvals]
      (if (coll? v)
        (do (.add attrs (ItemAttribute. (keystr k) (keystr (first v)) true))
            (doseq [vv (rest v)]
              (.add attrs (ItemAttribute. (keystr k) (keystr vv) false))))
        (.add attrs (ItemAttribute. (keystr k) (keystr v) true))))
    attrs))

(defn- parse-attr-list [attrs]
  (reduce (fn [m attr]
            (let [k (.getName attr), v (.getValue attr)]
              (assoc m k
                     (let [old-v (m k)]
                       (if old-v
                         (if (coll? old-v)
                           (conj old-v v)
                           (vector old-v v))
                         v)))))
          {} attrs))

(def #^{:doc "Returns the (cached) SimpleDB object."}
     get-simpledb
     (memoize (fn []
                (SimpleDB. (System/getenv "AWS_ACCESS_KEY_ID")
                           (System/getenv "AWS_SECRET_ACCESS_KEY")))))

(def #^{:doc "Returns the (cached) SimpleDB Domain object for name."}
     get-domain
     (memoize (fn [name] (.getDomain (get-simpledb) (keystr name)))))

(defn- get-item
  "Returns the SimpleDB item object with the given name in the
  domain."
  [domain-name item-name]
  (.getItem (get-domain domain-name) (keystr item-name)))

(defn put-attrs
  "Stores attributes (a map of key=>item or key=>collection) in the
  item under the domain."
  [domain-name item-name keyvals]
  (let [item (get-item domain-name item-name)]
    (.putAttributes item (make-attr-list keyvals))))

(defn get-attrs
  "Returns attributes (a map of key=>item or key=>collection) in the
  item under the domain."
  ([item] (parse-attr-list (.getAttributes item)))
  ([domain-name item-name]
     (let [item (get-item domain-name item-name)]
       (parse-attr-list (.getAttributes item)))))

(defn delete-item [domain-name item-name]
  (.deleteItem (get-domain domain-name) (keystr item-name)))

(defn list-items
  ([domain-name]
     (.getItemList (.listItems (get-domain domain-name))))
  ([domain-name query-string]
     (.getItemList (.listItems (get-domain domain-name)
                               query-string))))

(defn select-items
  ([domain-name query-string]
     (select-items domain-name query-string nil))
  ([domain-name query-string next-token]
     (reduce (fn [m [k v]] (assoc m k (parse-attr-list v)))
             {} (.getItems (.selectItems (get-domain domain-name)
                                         query-string next-token)))))

(defn delete-where [domain-name query-string]
  (doseq [item (list-items domain-name query-string)]
    (delete-item domain-name (.getIdentifier item))))
