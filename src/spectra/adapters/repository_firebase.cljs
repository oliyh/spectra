(ns spectra.adapters.repository-firebase
  (:require [clojure.string :as str]
            [spectra.adapters.repository :as repo]
            ["firebase/firestore" :as firestore]))

(defn- doc->clj [doc]
  (when (.-exists doc)
    (let [data (js->clj (.data doc) :keywordize-keys true)]
      (assoc data :id (.-id doc)))))

(defn- entry->firestore [entry]
  #js {:subject-id (:subject-id entry)
       :date (:date entry)
       :sentiment (some-> (:sentiment entry) name)
       :blocks (clj->js (into {}
                              (map (fn [[k v]]
                                     [(name k) {:sentiment (some-> (:sentiment v) name)}])
                                   (:blocks entry))))
       :tags (clj->js (vec (:tags entry)))
       :notes (or (:notes entry) "")})

(defn- firestore->entry [data]
  (let [;; Extract subject-id and date from document ID if missing
        {:keys [id]} data
        [subject-id date] (when id (str/split id #"_" 2))]
    (-> data
        (update :subject-id #(or % subject-id))
        (update :date #(or % date))
        (update :sentiment #(when % (keyword %)))
        (update :blocks #(into {}
                               (map (fn [[k v]]
                                      [(keyword k)
                                       {:sentiment (when-let [s (:sentiment v)] (keyword s))}])
                                    %)))
        (update :tags #(set (or % []))))))

(defn- subject->firestore [subject]
  #js {:name (:name subject)
       :suggested-tags (clj->js (vec (or (:suggested-tags subject) [])))})

(defn- firestore->subject [data]
  (-> data
      (update :suggested-tags #(vec (or % [])))))

(defrecord FirebaseRepository [db]
  repo/Repository

  (fetch-subjects [_]
    (-> (firestore/getDocs (firestore/collection db "subjects"))
        (.then (fn [snapshot]
                 (mapv #(firestore->subject (doc->clj %)) (.-docs snapshot))))
        (.catch (fn [err]
                  (js/console.error "fetch-subjects error:" err)
                  []))))

  (save-subject! [_ subject]
    (-> (firestore/setDoc
         (firestore/doc db "subjects" (:id subject))
         (subject->firestore subject))
        (.catch (fn [err]
                  (js/console.error "save-subject error:" err)))))

  (delete-subject! [_ subject-id]
    (firestore/deleteDoc
     (firestore/doc db "subjects" subject-id)))

  (fetch-entries [_ subject-id start-date end-date]
    (-> (firestore/getDocs
         (firestore/query
          (firestore/collection db "entries")
          (firestore/where "subject-id" "==" subject-id)
          (firestore/where "date" ">=" start-date)
          (firestore/where "date" "<=" end-date)))
        (.then (fn [snapshot]
                 (mapv #(firestore->entry (doc->clj %)) (.-docs snapshot))))
        (.catch (fn [err]
                  (js/console.error "fetch-entries error:" err)
                  []))))

  (fetch-entry [_ subject-id date]
    (let [entry-id (str subject-id "_" date)]
      (-> (firestore/getDoc (firestore/doc db "entries" entry-id))
          (.then (fn [doc]
                   (when-let [data (doc->clj doc)]
                     (firestore->entry data))))
          (.catch (fn [err]
                    (js/console.error "fetch-entry error:" err)
                    nil)))))

  (save-entry! [_ entry]
    (-> (firestore/setDoc
         (firestore/doc db "entries" (:id entry))
         (entry->firestore entry))
        (.then (fn [] (js/console.log "Entry saved:" (:id entry))))
        (.catch (fn [err]
                  (js/console.error "save-entry error:" err)))))

  (delete-entry! [_ entry-id]
    (firestore/deleteDoc
     (firestore/doc db "entries" entry-id)))

  (fetch-tags [_]
    (-> (firestore/getDocs (firestore/collection db "tags"))
        (.then (fn [snapshot]
                 (mapv #(.-id %) (.-docs snapshot))))
        (.catch (fn [err]
                  (js/console.error "fetch-tags error:" err)
                  []))))

  (save-tag! [_ tag]
    (-> (firestore/setDoc
         (firestore/doc db "tags" tag)
         #js {:created-at (.now js/Date)})
        (.catch (fn [err]
                  (js/console.error "save-tag error:" err))))))

(defn create-repository [db]
  (->FirebaseRepository db))
