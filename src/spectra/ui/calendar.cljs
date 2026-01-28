(ns spectra.ui.calendar
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [spectra.ui.components :as c]
            [spectra.domain.entry :as entry-domain]
            [tick.core :as t]))

(def weekdays ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"])

(defn month-year [date]
  (let [d (t/date date)]
    (str (t/month d) " " (t/year d))))

(defn days-in-month [year month]
  (let [ym (t/year-month (t/new-date year month 1))]
    (.lengthOfMonth ym)))

(defn first-day-offset [year month]
  (let [first-day (t/new-date year month 1)
        dow (t/int (t/day-of-week first-day))]
    (dec dow)))

(defn calendar-header [{:keys [date on-prev on-next]}]
  [:div.calendar-header
   [:h2 (month-year date)]
   [:div.calendar-nav
    [:button.date-nav-btn {:on-click on-prev} [c/icon-chevron-left]]
    [:button.date-nav-btn {:on-click on-next} [c/icon-chevron-right]]]])

(defn calendar-grid-view [{:keys [year month entries on-day-click]}]
  (let [num-days (days-in-month year month)
        offset (first-day-offset year month)
        entries-by-date (into {} (map (juxt :date identity) entries))]
    [:div
     [:div.calendar-weekdays
      (doall
       (for [day weekdays]
         ^{:key day}
         [:div.calendar-weekday day]))]
     [:div.calendar-grid
      (doall
       (for [i (range offset)]
         ^{:key (str "empty-" i)}
         [:div.calendar-day.empty]))
      (doall
       (for [day (range 1 (inc num-days))]
         (let [date-str (str (t/new-date year month day))
               entry (get entries-by-date date-str)
               sentiment (entry-domain/display-sentiment entry)]
           ^{:key day}
           [:div.calendar-day
            {:class (if sentiment (name sentiment) "none")
             :on-click #(on-day-click date-str)}
            day])))]]))

(defn subject-tabs []
  (let [subjects @(rf/subscribe [:subjects])
        selected-id @(rf/subscribe [:selected-subject-id])]
    [:div.subject-tabs
     (doall
      (for [{:keys [id name]} subjects]
        ^{:key id}
        [:button.subject-tab
         {:class (when (= id selected-id) "active")
          :on-click #(rf/dispatch [:subjects/select id])}
         name]))]))

(defn calendar-view []
  (let [view-date (r/atom (str (t/date)))
        prev-fetch-key (r/atom nil)]
    (fn []
      (let [current-date (t/date @view-date)
            year (t/int (t/year current-date))
            month (t/int (t/month current-date))
            subject-id @(rf/subscribe [:selected-subject-id])
            start-date (str (t/new-date year month 1))
            end-date (str (t/new-date year month (days-in-month year month)))
            fetch-key [subject-id start-date end-date]
            ;; Fetch entries when view changes
            _ (when (and subject-id (not= @prev-fetch-key fetch-key))
                (reset! prev-fetch-key fetch-key)
                (rf/dispatch [:entries/fetch-range start-date end-date]))
            entries @(rf/subscribe [:entries-in-range subject-id start-date end-date])]
        [:div
         [subject-tabs]
         [:div.card
          [calendar-header
           {:date @view-date
            :on-prev (fn []
                       (let [new-date (t/<< current-date (t/of-months 1))]
                         (reset! view-date (str new-date))))
            :on-next (fn []
                       (let [new-date (t/>> current-date (t/of-months 1))]
                         (reset! view-date (str new-date))))}]
          [calendar-grid-view
           {:year year
            :month month
            :entries entries
            :on-day-click (fn [date]
                            (rf/dispatch [:date/set date])
                            (rf/dispatch [:nav/set-view :entry]))}]]]))))
