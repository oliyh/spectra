(ns spectra.ui.components
  (:require [spectra.domain.sentiment :as sentiment]))

(defn spinner []
  [:div.loading
   [:div.spinner]])

(defn sentiment-button [{:keys [color selected? on-click]}]
  [:button.sentiment-btn
   {:class [(name color) (when selected? "selected")]
    :on-click on-click
    :aria-label (sentiment/sentiment-labels color)}])

(defn sentiment-picker [{:keys [selected on-select]}]
  [:div.sentiment-picker
   (for [color sentiment/sentiments]
     ^{:key color}
     [sentiment-button
      {:color color
       :selected? (= selected color)
       :on-click #(on-select color)}])])

(defn sentiment-dot [{:keys [sentiment]}]
  [:span.sentiment-dot {:class (or (some-> sentiment name) "none")}])

(defn tag [{:keys [label selected? on-click]}]
  [:button.tag
   {:class (when selected? "selected")
    :on-click on-click}
   label])

(defn icon-entry []
  [:svg.nav-icon {:viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2"}
   [:rect {:x "3" :y "4" :width "18" :height "18" :rx "2"}]
   [:line {:x1 "16" :y1 "2" :x2 "16" :y2 "6"}]
   [:line {:x1 "8" :y1 "2" :x2 "8" :y2 "6"}]
   [:line {:x1 "3" :y1 "10" :x2 "21" :y2 "10"}]])

(defn icon-calendar []
  [:svg.nav-icon {:viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2"}
   [:rect {:x "3" :y "4" :width "18" :height "18" :rx "2"}]
   [:line {:x1 "16" :y1 "2" :x2 "16" :y2 "6"}]
   [:line {:x1 "8" :y1 "2" :x2 "8" :y2 "6"}]
   [:line {:x1 "3" :y1 "10" :x2 "21" :y2 "10"}]
   [:rect {:x "7" :y "14" :width "3" :height "3"}]
   [:rect {:x "14" :y "14" :width "3" :height "3"}]])

(defn icon-chevron-left []
  [:svg {:viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :width "20" :height "20"}
   [:polyline {:points "15 18 9 12 15 6"}]])

(defn icon-chevron-right []
  [:svg {:viewBox "0 0 24 24" :fill "none" :stroke "currentColor" :stroke-width "2" :width "20" :height "20"}
   [:polyline {:points "9 18 15 12 9 6"}]])

(defn google-icon []
  [:svg {:width "18" :height "18" :viewBox "0 0 18 18"}
   [:path {:fill "#4285F4" :d "M17.64 9.2c0-.637-.057-1.251-.164-1.84H9v3.481h4.844c-.209 1.125-.843 2.078-1.796 2.717v2.258h2.908c1.702-1.567 2.684-3.874 2.684-6.615z"}]
   [:path {:fill "#34A853" :d "M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 009 18z"}]
   [:path {:fill "#FBBC05" :d "M3.964 10.71A5.41 5.41 0 013.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 000 9c0 1.452.348 2.827.957 4.042l3.007-2.332z"}]
   [:path {:fill "#EA4335" :d "M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 00.957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58z"}]])
