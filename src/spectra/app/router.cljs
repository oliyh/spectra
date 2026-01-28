(ns spectra.app.router
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;; URL scheme (hash-based for GitHub Pages compatibility):
;; #/                          -> entry view, today, first subject
;; #/entry/:subject-id/:date   -> entry view for subject on date
;; #/calendar/:subject-id      -> calendar view for subject

(defn parse-hash []
  (let [hash (.-hash js/location)
        path (if (str/starts-with? hash "#") (subs hash 1) "")
        parts (vec (rest (str/split path #"/")))]
    (case (first parts)
      "entry" {:view :entry
               :subject-id (get parts 1)
               :date (get parts 2)}
      "calendar" {:view :calendar
                  :subject-id (get parts 1)}
      {:view :entry})))

(defn build-hash [{:keys [view subject-id date]}]
  (case view
    :entry (str "#/entry/" subject-id "/" date)
    :calendar (str "#/calendar/" subject-id)
    "#/"))

(defn navigate!
  "Update URL hash without reloading page"
  [route]
  (let [hash (build-hash route)]
    (set! (.-hash js/location) hash)))

(defn replace-url!
  "Replace current URL hash without adding to history"
  [route]
  (let [hash (build-hash route)]
    (.replaceState js/history nil "" hash)))

(defn current-route []
  {:view @(rf/subscribe [:nav/view])
   :subject-id @(rf/subscribe [:selected-subject-id])
   :date @(rf/subscribe [:current-date])})

;; Effect to update URL when state changes
(rf/reg-fx
 :router/navigate
 (fn [route]
   (navigate! route)))

(rf/reg-fx
 :router/replace
 (fn [route]
   (replace-url! route)))

;; Event to handle URL changes (back/forward buttons)
(rf/reg-event-fx
 :router/on-navigate
 (fn [{:keys [db]} [_ parsed]]
   (let [{:keys [view subject-id date]} parsed
         subjects (:subjects db)
         resolved-subject-id (or (when (and subject-id (contains? subjects subject-id))
                                   subject-id)
                                 (:selected-subject-id db)
                                 (first (keys subjects)))
         resolved-date (or date (:current-date db))]
     {:db (-> db
              (assoc :view (or view :entry))
              (assoc :selected-subject-id resolved-subject-id)
              (assoc :current-date resolved-date))
      :fx [[:firebase/fetch-entry [resolved-subject-id resolved-date]]]})))

;; Initialize router - call on app start
(defn init! []
  ;; Handle hash changes (back/forward buttons and direct hash changes)
  (.addEventListener js/window "hashchange"
                     (fn [_]
                       (rf/dispatch [:router/on-navigate (parse-hash)]))))

;; Apply initial URL state after subjects are loaded
(rf/reg-event-fx
 :router/apply-initial
 (fn [{:keys [db]} _]
   (let [parsed (parse-hash)
         {:keys [view subject-id date]} parsed
         subjects (:subjects db)
         resolved-subject-id (or (when (and subject-id (contains? subjects subject-id))
                                   subject-id)
                                 (first (keys subjects)))
         resolved-date (or date (:current-date db))]
     {:db (-> db
              (assoc :view (or view :entry))
              (assoc :selected-subject-id resolved-subject-id)
              (assoc :current-date resolved-date))
      :fx [[:firebase/fetch-entry [resolved-subject-id resolved-date]]
           [:router/replace {:view (or view :entry)
                             :subject-id resolved-subject-id
                             :date resolved-date}]]})))
