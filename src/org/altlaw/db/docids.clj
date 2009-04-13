(ns org.altlaw.util.docids)

(def #^{:private true} *bucket* "altlaw.org")

(defn- path [collection]
  (str "v5/docids/" collection ".tsv.gz"))

(defn load-docids [collection]
  (read-tsv-stream (get-object-stream *bucket* (path collection))))

(defn save-docids [collection])

(def get-docid-map (memoize (load-docids)))

(defn get-docid [collection key]
  (get @(get-docid-map collection) key))

(defn set-docid [collection keys docid]
  (dosync (alter @(get-docid-map collection)
                 merge (zipmap keys (repeat docid)))))

(defn request-docid [collection keys])

