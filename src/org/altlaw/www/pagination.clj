(ns org.altlaw.www.pagination
  (:require [org.altlaw.www.references :as r]))

(def *page-size* 10)

(def *window-size* 5)

(defn calc-last-page [total-hits page-size]
  (int (Math/ceil (/ total-hits (float *page-size*)))))

(defn calc-window-start [current-page window-size]
  (if (< (- current-page window-size) 1)
    1
    (- current-page window-size)))

(defn calc-window-end [current-page window-size last-page]
  (if (> (+ current-page *window-size*) last-page)
    last-page
    (+ current-page *window-size*)))

(defn pagination-html [ref current total-hits]
  (let [last-page (calc-last-page total-hits *page-size*)
        window-start (calc-window-start current *window-size*)
        window-end (calc-window-end current *window-size* last-page)]
    (with-out-str
      (print "<div class=\"pagination\"><ul>")
      (print "<li>Pages:</li>")
      (if (= current 1)
        (print "<li class=\"disablepage\">&#171; previous</li>")
        (printf "<li class=\"prevpage\"><a href=\"%s\">&#171; previous</a></li>"
                (str (r/page-ref ref (dec current)))))
      (when (not= window-start 1)
        (printf "<li><a href=\"%s\">1</a></li>" (str (r/page-ref ref 1)))
        (print "<li>...</li>"))
      (doseq [n (range window-start (inc window-end))]
        (if (= n current)
          (printf "<li class=\"currentpage\">%d</li>" n)
          (printf "<li><a href=\"%s\">%d</a></li>" (str (r/page-ref ref n)) n)))
      (when (not= window-end last-page)
        (print "<li>...</li>")
        (printf "<li><a href=\"%s\">%d</a></li>"
                (str (r/page-ref ref last-page)) last-page))
      (if (= current last-page)
        (print "<li class=\"disablepage\">next &#187;</li>")
        (printf "<li class=\"prevpage\"><a href=\"%s\">next &#187;</a></li>"
                (str (r/page-ref ref (inc current)))))
      (print "</ul></div>"))))

