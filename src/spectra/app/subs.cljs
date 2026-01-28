(ns spectra.app.subs
  (:require [re-frame.core :as rf]
            [spectra.domain.entry :as entry]))

;; Auth
(rf/reg-sub :auth/status (fn [db _] (get-in db [:auth :status])))
(rf/reg-sub :auth/user (fn [db _] (get-in db [:auth :user])))

;; Navigation
(rf/reg-sub :nav/view (fn [db _] (:view db)))
(rf/reg-sub :current-date (fn [db _] (:current-date db)))

;; Subjects
(rf/reg-sub :subjects (fn [db _] (vals (:subjects db))))
(rf/reg-sub :selected-subject-id (fn [db _] (:selected-subject-id db)))

(rf/reg-sub
 :selected-subject
 :<- [:subjects]
 :<- [:selected-subject-id]
 (fn [[subjects selected-id] _]
   (first (filter #(= (:id %) selected-id) subjects))))

;; Entries
(rf/reg-sub :entries (fn [db _] (:entries db)))

(rf/reg-sub
 :current-entry
 :<- [:entries]
 :<- [:selected-subject-id]
 :<- [:current-date]
 (fn [[entries subject-id date] _]
   (let [entry-id (entry/make-entry-id subject-id date)]
     (get entries entry-id))))

(rf/reg-sub
 :entries-for-subject
 :<- [:entries]
 (fn [entries [_ subject-id]]
   (filter #(= (:subject-id %) subject-id) (vals entries))))

(rf/reg-sub
 :entries-in-range
 :<- [:entries]
 (fn [entries [_ subject-id start-date end-date]]
   (->> (vals entries)
        (filter #(= (:subject-id %) subject-id))
        (filter #(and (>= (:date %) start-date)
                      (<= (:date %) end-date))))))

;; Tags
(rf/reg-sub :tags (fn [db _] (:tags db)))

;; Loading states
(rf/reg-sub :loading/subjects (fn [db _] (get-in db [:loading :subjects])))
(rf/reg-sub :loading/entries (fn [db _] (get-in db [:loading :entries])))
