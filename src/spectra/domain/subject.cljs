(ns spectra.domain.subject)

(defn new-subject [name]
  {:id (str (random-uuid))
   :name name
   :suggested-tags []})

(defn set-name [subject name]
  (assoc subject :name name))

(defn add-suggested-tag [subject tag]
  (update subject :suggested-tags conj tag))

(defn remove-suggested-tag [subject tag]
  (update subject :suggested-tags #(vec (remove #{tag} %))))

(defn set-suggested-tags [subject tags]
  (assoc subject :suggested-tags (vec tags)))
