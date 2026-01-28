(ns spectra.domain.entry
  (:require [spectra.domain.sentiment :as sentiment]))

(def blocks [:before-school :morning-school :afternoon-school :after-school])

(def block-labels
  {:before-school "Before"
   :morning-school "AM"
   :afternoon-school "PM"
   :after-school "After"})

(defn make-entry-id [subject-id date]
  (str subject-id "_" date))

(defn parse-entry-id [entry-id]
  (let [[subject-id date] (clojure.string/split entry-id #"_" 2)]
    {:subject-id subject-id :date date}))

(defn new-entry [subject-id date]
  {:id (make-entry-id subject-id date)
   :subject-id subject-id
   :date date
   :sentiment nil
   :blocks {}
   :tags #{}
   :notes ""})

(defn set-sentiment [entry sentiment]
  (if (or (nil? sentiment) (sentiment/valid-sentiment? sentiment))
    (assoc entry :sentiment sentiment)
    entry))

(defn set-block-sentiment [entry block sentiment]
  (if (and (contains? (set blocks) block)
           (or (nil? sentiment) (sentiment/valid-sentiment? sentiment)))
    (if sentiment
      (assoc-in entry [:blocks block :sentiment] sentiment)
      (update entry :blocks dissoc block))
    entry))

(defn get-block-sentiment [entry block]
  (get-in entry [:blocks block :sentiment]))

(defn add-tag [entry tag]
  (update entry :tags conj tag))

(defn remove-tag [entry tag]
  (update entry :tags disj tag))

(defn toggle-tag [entry tag]
  (if (contains? (:tags entry) tag)
    (remove-tag entry tag)
    (add-tag entry tag)))

(defn set-notes [entry notes]
  (assoc entry :notes notes))

(defn has-data? [entry]
  (or (:sentiment entry)
      (seq (:blocks entry))
      (seq (:tags entry))
      (seq (:notes entry))))

(defn display-sentiment
  "Returns the effective sentiment to display (overall or most common block)"
  [entry]
  (or (:sentiment entry)
      (when-let [block-sentiments (seq (keep #(get-in entry [:blocks % :sentiment]) blocks))]
        (first block-sentiments))))
