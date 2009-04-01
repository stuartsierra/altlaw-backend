(ns org.altlaw.util.string)

(defn truncate
  "If s is longer than max characters, truncates it to max characters
  and add an ellipsis (...)."
  [s max]
  (if (< (count s) max)
    s
    (str (subs s 0 max) "...")))

