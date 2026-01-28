(ns spectra.app.db
  (:require [tick.core :as t]))

(defn today []
  (str (t/date)))

(def default-db
  {:view :entry                    ;; :entry or :calendar
   :auth {:status :loading         ;; :loading, :signed-out, :signed-in
          :user nil}
   :current-date (today)
   :selected-subject-id nil
   :subjects {}                    ;; id -> subject
   :entries {}                     ;; "subject-id_date" -> entry
   :tags #{}                       ;; set of all known tags
   :loading {:subjects false
             :entries false}})
