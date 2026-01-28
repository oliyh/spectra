(ns spectra.ui.entry
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [spectra.ui.components :as c]
            [spectra.domain.entry :as entry-domain]
            [spectra.domain.sentiment :as sentiment]
            [clojure.string :as str]
            [tick.core :as t]))

(defn format-date [date-str]
  (let [date (t/date date-str)
        day-of-week (str (t/day-of-week date))
        day (t/day-of-month date)
        month (str (t/month date))]
    {:day day
     :full (str day-of-week ", " day " " month)}))

(defn date-navigation []
  (let [current-date @(rf/subscribe [:current-date])
        {:keys [day full]} (format-date current-date)]
    [:div
     [:div.date-nav
      [:button.date-nav-btn {:on-click #(rf/dispatch [:date/prev])}
       [c/icon-chevron-left]]
      [:button.btn.btn-secondary {:on-click #(rf/dispatch [:date/today])}
       "Today"]
      [:button.date-nav-btn {:on-click #(rf/dispatch [:date/next])}
       [c/icon-chevron-right]]]
     [:div.date-display
      [:div.date-day day]
      [:div.date-full full]]]))

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

(def timeframes
  [{:id :whole-day :label "Whole Day"}
   {:id :before-school :label "Before"}
   {:id :morning-school :label "AM"}
   {:id :afternoon-school :label "PM"}
   {:id :after-school :label "After"}])

(def block-order [:before-school :morning-school :afternoon-school :after-school])

(defn timeframe-button [{:keys [id label sentiment selected? on-click]}]
  [:button.timeframe-btn
   {:class [(when sentiment (name sentiment))
            (when selected? "selected")]
    :on-click on-click}
   [:span.timeframe-label label]])

(defn get-timeframe-sentiment [entry timeframe-id]
  (if (= timeframe-id :whole-day)
    (:sentiment entry)
    (entry-domain/get-block-sentiment entry timeframe-id)))

(defn find-next-empty-block [entry current-block]
  "Find the next block without a sentiment, starting after current-block"
  (let [current-idx (.indexOf block-order current-block)
        remaining (if (>= current-idx 0)
                    (drop (inc current-idx) block-order)
                    block-order)]
    (first (filter #(nil? (entry-domain/get-block-sentiment entry %)) remaining))))

(defn find-first-empty-timeframe [entry]
  "Find the first timeframe without a sentiment"
  (or
   ;; Check whole-day first
   (when (nil? (:sentiment entry)) :whole-day)
   ;; Then check blocks in order
   (first (filter #(nil? (entry-domain/get-block-sentiment entry %)) block-order))
   ;; Default to whole-day if everything is filled
   :whole-day))

(defn timeframe-picker []
  (let [selected-timeframe (r/atom :whole-day)
        prev-context (r/atom nil)]
    (fn []
      (let [entry @(rf/subscribe [:current-entry])
            current-date @(rf/subscribe [:current-date])
            subject-id @(rf/subscribe [:selected-subject-id])
            context [current-date subject-id]
            ;; Reset selection when date or subject changes
            _ (when (not= @prev-context context)
                (reset! prev-context context)
                (reset! selected-timeframe (find-first-empty-timeframe entry)))
            selected @selected-timeframe
            current-sentiment (get-timeframe-sentiment entry selected)]
        [:div.card
         [:h2 "Select timeframe, then colour"]

         ;; Timeframe buttons
         [:div.timeframes
          (doall
           (for [{:keys [id label]} timeframes]
             ^{:key id}
             [timeframe-button
              {:id id
               :label label
               :sentiment (get-timeframe-sentiment entry id)
               :selected? (= selected id)
               :on-click #(reset! selected-timeframe id)}]))]

         ;; Color picker (always visible)
         [:div.color-picker.mt-md
          (doall
           (for [color sentiment/sentiments]
             ^{:key color}
             [:button.color-btn
              {:class [(name color)
                       (when (= color current-sentiment) "selected")]
               :on-click (fn []
                           (let [new-sentiment (if (= color current-sentiment) nil color)]
                             (if (= selected :whole-day)
                               (do
                                 (rf/dispatch [:entry/set-sentiment new-sentiment])
                                 ;; Auto-advance from whole-day to first empty block
                                 (when new-sentiment
                                   (when-let [next-block (first block-order)]
                                     (when (nil? (entry-domain/get-block-sentiment entry next-block))
                                       (reset! selected-timeframe next-block)))))
                               (do
                                 (rf/dispatch [:entry/set-block-sentiment selected new-sentiment])
                                 ;; Auto-advance to next empty block
                                 (when new-sentiment
                                   (when-let [next-block (find-next-empty-block entry selected)]
                                     (reset! selected-timeframe next-block)))))))}
              [:span.color-label (sentiment/sentiment-labels color)]]))]]))))

(defn to-kebab-case [s]
  (-> s
      str/trim
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" "-")
      (str/replace #"-+" "-")
      (str/replace #"^-|-$" "")))

(def collapsed-tag-limit 8)

(defn tags-section []
  (let [new-tag-input (r/atom "")
        tags-expanded? (r/atom false)
        input-visible? (r/atom false)]
    (fn []
      (let [entry @(rf/subscribe [:current-entry])
            subject @(rf/subscribe [:selected-subject])
            global-tags @(rf/subscribe [:tags])
            current-tags (or (:tags entry) #{})
            suggested-tags (vec (or (:suggested-tags subject) []))
            suggested-set (set suggested-tags)
            other-tags (->> global-tags
                            (remove suggested-set)
                            sort
                            vec)
            all-tags (concat suggested-tags other-tags)
            show-expand? (> (count all-tags) collapsed-tag-limit)
            visible-tags (if (or @tags-expanded? (not show-expand?))
                           all-tags
                           (take collapsed-tag-limit all-tags))
            input-value @new-tag-input]
        [:div.card
         [:h2 "Tags"]

         ;; Tags list
         [:div.tags
          (doall
           (for [tag visible-tags]
             ^{:key tag}
             [c/tag
              {:label tag
               :selected? (contains? current-tags tag)
               :on-click #(rf/dispatch [:entry/toggle-tag tag])}]))

          ;; Expand/collapse tags button
          (when show-expand?
            [:button.tag.tag-expand
             {:on-click #(swap! tags-expanded? not)}
             (if @tags-expanded?
               "−"
               (str "+" (- (count all-tags) collapsed-tag-limit)))])

          ;; Add new tag button (small +)
          (when-not @input-visible?
            [:button.tag.tag-add
             {:on-click #(reset! input-visible? true)}
             "+"])]

         ;; Custom tag input (only visible when toggled)
         (when @input-visible?
           [:div.tag-input-row.mt-sm
            [:input.tag-input
             {:type "text"
              :placeholder "new-tag-name"
              :value input-value
              :auto-focus true
              :on-change #(reset! new-tag-input (-> % .-target .-value))
              :on-key-down (fn [e]
                             (cond
                               (= (.-key e) "Enter")
                               (let [tag (to-kebab-case input-value)]
                                 (when (seq tag)
                                   (rf/dispatch [:entry/toggle-tag tag])
                                   (reset! new-tag-input "")
                                   (reset! input-visible? false)))
                               (= (.-key e) "Escape")
                               (do
                                 (reset! new-tag-input "")
                                 (reset! input-visible? false))))}]
            [:button.btn.btn-secondary.btn-sm
             {:on-click (fn []
                          (let [tag (to-kebab-case input-value)]
                            (when (seq tag)
                              (rf/dispatch [:entry/toggle-tag tag])))
                          (reset! new-tag-input "")
                          (reset! input-visible? false))}
             "Add"]
            [:button.btn.btn-secondary.btn-sm
             {:on-click (fn []
                          (reset! new-tag-input "")
                          (reset! input-visible? false))}
             "×"]])]))))

(defn notes-section []
  (let [entry @(rf/subscribe [:current-entry])
        notes (or (:notes entry) "")]
    [:div.card
     [:h2 "Notes"]
     [:textarea.notes-input
      {:value notes
       :placeholder "Add notes about the day..."
       :on-change #(rf/dispatch [:entry/set-notes (-> % .-target .-value)])}]]))

(defn entry-view []
  [:div
   [date-navigation]
   [subject-tabs]
   [timeframe-picker]
   [tags-section]
   [notes-section]])
