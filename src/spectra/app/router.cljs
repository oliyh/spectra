(ns spectra.app.router
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;; URL scheme:
;; /                          -> entry view, today, first subject
;; /entry/:subject-id/:date   -> entry view for subject on date
;; /calendar/:subject-id      -> calendar view for subject

(defn parse-path []
  (let [path (.-pathname js/location)
        parts (vec (rest (str/split path #"/")))]
    (case (first parts)
      "entry" {:view :entry
               :subject-id (get parts 1)
               :date (get parts 2)}
      "calendar" {:view :calendar
                  :subject-id (get parts 1)}
      {:view :entry})))

(defn build-path [{:keys [view subject-id date]}]
  (case view
    :entry (str "/entry/" subject-id "/" date)
    :calendar (str "/calendar/" subject-id)
    "/"))

(defn navigate!
  "Update URL without reloading page"
  [route]
  (let [path (build-path route)]
    (.pushState js/history nil "" path)))

(defn replace-url!
  "Replace current URL without adding to history"
  [route]
  (let [path (build-path route)]
    (.replaceState js/history nil "" path)))

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
  ;; Handle back/forward buttons
  (.addEventListener js/window "popstate"
                     (fn [_]
                       (rf/dispatch [:router/on-navigate (parse-path)]))))

;; Apply initial URL state after subjects are loaded
(rf/reg-event-fx
 :router/apply-initial
 (fn [{:keys [db]} _]
   (let [parsed (parse-path)
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
