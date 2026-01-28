(ns spectra.ui.calendar
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [spectra.ui.components :as c]
            [spectra.domain.entry :as entry-domain]
            [spectra.domain.sentiment :as sentiment]
            [tick.core :as t]))

(def weekdays ["Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"])

(def min-occurrences 3)

(defn analyze-entries
  "Analyze entries to find tag-sentiment correlations"
  [entries]
  (let [entries-with-data (filter #(and (entry-domain/display-sentiment %)
                                        (seq (:tags %)))
                                  entries)
        ;; Build tag -> sentiment counts
        tag-sentiment-pairs (for [entry entries-with-data
                                  :let [sentiment (entry-domain/display-sentiment entry)]
                                  tag (:tags entry)]
                              [tag sentiment])
        ;; Count occurrences
        tag-sentiment-counts (reduce (fn [acc [tag sentiment]]
                                       (update-in acc [tag sentiment] (fnil inc 0)))
                                     {}
                                     tag-sentiment-pairs)
        ;; Count sentiment occurrences (for entries with tags)
        sentiment-counts (frequencies (map entry-domain/display-sentiment entries-with-data))
        ;; Count tag occurrences
        tag-counts (reduce (fn [acc entry]
                             (reduce #(update %1 %2 (fnil inc 0)) acc (:tags entry)))
                           {}
                           entries-with-data)]
    {:tag-sentiment-counts tag-sentiment-counts
     :sentiment-counts sentiment-counts
     :tag-counts tag-counts
     :total-entries (count entries-with-data)}))

(defn get-tag-insights
  "For each tag, find which sentiment it correlates with most"
  [{:keys [tag-sentiment-counts tag-counts]}]
  (->> tag-sentiment-counts
       (map (fn [[tag sentiment-map]]
              (let [total (get tag-counts tag 0)
                    [top-sentiment top-count] (apply max-key val sentiment-map)
                    percentage (when (pos? total)
                                 (Math/round (* 100 (/ top-count total))))]
                {:tag tag
                 :sentiment top-sentiment
                 :count top-count
                 :total total
                 :percentage percentage})))
       (filter #(>= (:total %) min-occurrences))
       (filter #(>= (:percentage %) 60))  ; Only show strong correlations
       (sort-by :total >)))

(defn get-sentiment-insights
  "For each sentiment, find the most common tags"
  [{:keys [tag-sentiment-counts sentiment-counts]}]
  (->> sentiment/sentiments
       (map (fn [sentiment]
              (let [total (get sentiment-counts sentiment 0)
                    tags-for-sentiment (->> tag-sentiment-counts
                                            (map (fn [[tag smap]]
                                                   [tag (get smap sentiment 0)]))
                                            (filter #(>= (second %) 2))
                                            (sort-by second >)
                                            (take 3))]
                {:sentiment sentiment
                 :total total
                 :top-tags tags-for-sentiment})))
       (filter #(>= (:total %) min-occurrences))
       (filter #(seq (:top-tags %)))))

(defn insights-panel [{:keys [entries]}]
  (let [analysis (analyze-entries entries)
        tag-insights (get-tag-insights analysis)
        sentiment-insights (get-sentiment-insights analysis)]
    [:div.insights-panel
     [:h3 "Insights"]
     (if (< (:total-entries analysis) min-occurrences)
       [:p.insights-empty "Add more entries with tags to see patterns"]
       [:div
        (when (seq tag-insights)
          [:div.insight-section
           [:h4 "Tag patterns"]
           [:ul.insight-list
            (doall
             (for [{:keys [tag sentiment percentage]} tag-insights]
               ^{:key tag}
               [:li
                [:span.tag-name tag]
                " → "
                [:span.sentiment-badge {:class (name sentiment)}
                 (sentiment/label sentiment)]
                [:span.percentage (str " (" percentage "%)")]]))]])
        (when (seq sentiment-insights)
          [:div.insight-section
           [:h4 "Common tags by mood"]
           (doall
            (for [{:keys [sentiment top-tags]} sentiment-insights]
              ^{:key sentiment}
              [:div.sentiment-tags
               [:span.sentiment-badge {:class (name sentiment)}
                (sentiment/label sentiment)]
               [:span.tag-list
                (interpose ", " (map first top-tags))]]))])])]))

(defn sentiment-legend []
  [:div.sentiment-legend
   (doall
    (for [s sentiment/sentiments]
      ^{:key s}
      [:div.legend-item
       [:span.legend-color {:class (name s)}]
       [:span.legend-label (sentiment/label s)]]))])

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
         [:div.calendar-layout
          [:div.card.calendar-card
           [calendar-header
            {:date @view-date
             :on-prev (fn []
                        (let [new-date (t/<< current-date (t/of-months 1))]
                          (reset! view-date (str new-date))))
             :on-next (fn []
                        (let [new-date (t/>> current-date (t/of-months 1))]
                          (reset! view-date (str new-date))))}]
           [sentiment-legend]
           [calendar-grid-view
            {:year year
             :month month
             :entries entries
             :on-day-click (fn [date]
                             (rf/dispatch [:date/set date])
                             (rf/dispatch [:nav/set-view :entry]))}]]
          [:div.card.insights-card
           [insights-panel {:entries entries}]]]]))))
