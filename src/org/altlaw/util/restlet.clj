(ns org.altlaw.util.restlet
  (:use org.altlaw.util.log)
  (:import (org.restlet Component)
           (org.restlet.data Protocol)))

(defn make-component [port app-instance]
  (let [com (Component.)]
    (.. com getServers (add Protocol/HTTP port))
    (.. com getClients (add Protocol/FILE))
    (let [child-context (.createChildContext (.getContext com))]
      (.setContext app-instance child-context))
    (.. com getDefaultHost (attach app-instance))
    com))

(defmacro with-server [port app-instance & body]
  `(let [server# (make-component ~port ~app-instance)]
     (.start server#)
     (try ~@body
          (finally (.stop server#)
                   (.. server# getServers clear)))))
