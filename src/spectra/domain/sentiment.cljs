(ns spectra.domain.sentiment)

;; Vector for consistent ordering in UI
(def sentiments [:blue :green :orange :red])

(def sentiment-labels
  {:blue "Sad"
   :green "Happy"
   :orange "Fizzy"
   :red "Upset"})

(defn valid-sentiment? [s]
  (some #{s} sentiments))

(defn sentiment->class [s]
  (when s (name s)))

(defn label [s]
  (get sentiment-labels s (name s)))
