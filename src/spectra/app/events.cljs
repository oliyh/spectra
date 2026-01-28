(ns spectra.app.events
  (:require [re-frame.core :as rf]
            [spectra.app.db :as db]
            [spectra.domain.entry :as entry]
            [spectra.domain.subject :as subject]
            [tick.core :as t]))

;; Initialization
(rf/reg-event-fx
 :app/initialize
 (fn [_ _]
   {:db db/default-db
    :fx [[:firebase/init-auth]]}))

;; Auth events
(rf/reg-event-fx
 :auth/state-changed
 (fn [{:keys [db]} [_ user]]
   (if user
     {:db (-> db
              (assoc-in [:auth :status] :signed-in)
              (assoc-in [:auth :user] user))
      :fx [[:firebase/fetch-subjects]
           [:firebase/fetch-tags]]}
     {:db (-> db
              (assoc-in [:auth :status] :signed-out)
              (assoc-in [:auth :user] nil))})))

(rf/reg-event-fx
 :auth/sign-in
 (fn [_ _]
   {:fx [[:firebase/sign-in-google]]}))

(rf/reg-event-fx
 :auth/sign-out
 (fn [_ _]
   {:fx [[:firebase/sign-out]]}))

;; Navigation
(rf/reg-event-fx
 :nav/set-view
 (fn [{:keys [db]} [_ view]]
   (let [new-db (assoc db :view view)]
     {:db new-db
      :fx [[:router/navigate {:view view
                              :subject-id (:selected-subject-id new-db)
                              :date (:current-date new-db)}]]})))

;; Date navigation
(rf/reg-event-fx
 :date/set
 (fn [{:keys [db]} [_ date]]
   (let [subject-id (:selected-subject-id db)]
     {:db (assoc db :current-date date)
      :fx [[:firebase/fetch-entry [subject-id date]]
           [:router/navigate {:view (:view db)
                              :subject-id subject-id
                              :date date}]]})))

(rf/reg-event-fx
 :date/prev
 (fn [{:keys [db]} _]
   (let [new-date (str (t/<< (t/date (:current-date db)) (t/of-days 1)))
         subject-id (:selected-subject-id db)]
     {:db (assoc db :current-date new-date)
      :fx [[:firebase/fetch-entry [subject-id new-date]]
           [:router/navigate {:view (:view db)
                              :subject-id subject-id
                              :date new-date}]]})))

(rf/reg-event-fx
 :date/next
 (fn [{:keys [db]} _]
   (let [new-date (str (t/>> (t/date (:current-date db)) (t/of-days 1)))
         subject-id (:selected-subject-id db)]
     {:db (assoc db :current-date new-date)
      :fx [[:firebase/fetch-entry [subject-id new-date]]
           [:router/navigate {:view (:view db)
                              :subject-id subject-id
                              :date new-date}]]})))

(rf/reg-event-fx
 :date/today
 (fn [{:keys [db]} _]
   (let [new-date (db/today)
         subject-id (:selected-subject-id db)]
     {:db (assoc db :current-date new-date)
      :fx [[:firebase/fetch-entry [subject-id new-date]]
           [:router/navigate {:view (:view db)
                              :subject-id subject-id
                              :date new-date}]]})))

;; Subject events
(rf/reg-event-fx
 :subjects/loaded
 (fn [{:keys [db]} [_ subjects]]
   (let [subjects-map (into {} (map (juxt :id identity) subjects))]
     {:db (-> db
              (assoc :subjects subjects-map)
              (assoc-in [:loading :subjects] false))
      :fx [[:dispatch [:router/apply-initial]]]})))

(rf/reg-event-fx
 :subjects/select
 (fn [{:keys [db]} [_ subject-id]]
   {:db (assoc db :selected-subject-id subject-id)
    :fx [[:firebase/fetch-entry [subject-id (:current-date db)]]
         [:router/navigate {:view (:view db)
                            :subject-id subject-id
                            :date (:current-date db)}]]}))

(rf/reg-event-fx
 :subjects/add
 (fn [{:keys [db]} [_ name]]
   (let [new-subject (subject/new-subject name)]
     {:db (assoc-in db [:subjects (:id new-subject)] new-subject)
      :fx [[:firebase/save-subject new-subject]]})))

;; Entry events
(rf/reg-event-db
 :entries/loaded
 (fn [db [_ entries]]
   (let [entries-map (into {} (map (juxt :id identity) entries))]
     (-> db
         (update :entries merge entries-map)
         (assoc-in [:loading :entries] false)))))

(rf/reg-event-db
 :entry/loaded
 (fn [db [_ entry]]
   (if entry
     (assoc-in db [:entries (:id entry)] entry)
     db)))

(defn- get-or-create-entry [db subject-id date]
  (let [entry-id (entry/make-entry-id subject-id date)]
    (or (get-in db [:entries entry-id])
        (entry/new-entry subject-id date))))

(rf/reg-event-fx
 :entry/set-sentiment
 (fn [{:keys [db]} [_ sentiment]]
   (let [{:keys [selected-subject-id current-date]} db
         updated-entry (-> (get-or-create-entry db selected-subject-id current-date)
                          (entry/set-sentiment sentiment))]
     {:db (assoc-in db [:entries (:id updated-entry)] updated-entry)
      :fx [[:firebase/save-entry updated-entry]]})))

(rf/reg-event-fx
 :entry/set-block-sentiment
 (fn [{:keys [db]} [_ block sentiment]]
   (let [{:keys [selected-subject-id current-date]} db
         updated-entry (-> (get-or-create-entry db selected-subject-id current-date)
                          (entry/set-block-sentiment block sentiment))]
     {:db (assoc-in db [:entries (:id updated-entry)] updated-entry)
      :fx [[:firebase/save-entry updated-entry]]})))

(rf/reg-event-fx
 :entry/toggle-tag
 (fn [{:keys [db]} [_ tag]]
   (let [{:keys [selected-subject-id current-date tags]} db
         updated-entry (-> (get-or-create-entry db selected-subject-id current-date)
                          (entry/toggle-tag tag))
         is-new-tag? (not (contains? tags tag))
         adding-tag? (contains? (:tags updated-entry) tag)]
     {:db (-> db
              (assoc-in [:entries (:id updated-entry)] updated-entry)
              (cond-> (and is-new-tag? adding-tag?)
                (update :tags conj tag)))
      :fx (cond-> [[:firebase/save-entry updated-entry]]
            (and is-new-tag? adding-tag?)
            (conj [:firebase/save-tag tag]))})))

(rf/reg-event-fx
 :entry/set-notes
 (fn [{:keys [db]} [_ notes]]
   (let [{:keys [selected-subject-id current-date]} db
         updated-entry (-> (get-or-create-entry db selected-subject-id current-date)
                          (entry/set-notes notes))]
     {:db (assoc-in db [:entries (:id updated-entry)] updated-entry)
      :fx [[:firebase/save-entry updated-entry]]})))

;; Fetch entries for calendar view
(rf/reg-event-fx
 :entries/fetch-range
 (fn [{:keys [db]} [_ start-date end-date]]
   {:db (assoc-in db [:loading :entries] true)
    :fx [[:firebase/fetch-entries [(:selected-subject-id db) start-date end-date]]]}))

;; Tags events
(rf/reg-event-db
 :tags/loaded
 (fn [db [_ tags]]
   (assoc db :tags (set tags))))
