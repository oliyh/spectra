(ns spectra.ui.shell
  (:require [re-frame.core :as rf]
            [spectra.ui.components :as c]
            [spectra.ui.entry :as entry]
            [spectra.ui.calendar :as calendar]))

(defn auth-screen []
  [:div.auth-screen
   [:div.auth-logo "🌈"]
   [:h1.auth-title "Spectra"]
   [:p.auth-subtitle "Track daily sentiment patterns"]
   [:button.google-btn
    {:on-click #(rf/dispatch [:auth/sign-in])}
    [c/google-icon]
    "Sign in with Google"]])

(defn loading-screen []
  [:div.auth-screen
   [:div.auth-logo "🌈"]
   [c/spinner]])

(defn bottom-nav []
  (let [view @(rf/subscribe [:nav/view])]
    [:nav.bottom-nav
     [:div.nav-item
      {:class (when (= view :entry) "active")
       :on-click #(rf/dispatch [:nav/set-view :entry])}
      [c/icon-entry]
      [:span "Today"]]
     [:div.nav-item
      {:class (when (= view :calendar) "active")
       :on-click #(rf/dispatch [:nav/set-view :calendar])}
      [c/icon-calendar]
      [:span "Calendar"]]]))

(defn main-content []
  (let [view @(rf/subscribe [:nav/view])
        subjects @(rf/subscribe [:subjects])]
    (if (empty? subjects)
      [:div.card.text-center
       [:p.text-muted "No subjects yet"]
       [:p "Add someone to track in the settings"]]
      (case view
        :entry [entry/entry-view]
        :calendar [calendar/calendar-view]
        [entry/entry-view]))))

(defn app-shell []
  (let [auth-status @(rf/subscribe [:auth/status])]
    (case auth-status
      :loading [loading-screen]
      :signed-out [auth-screen]
      :signed-in [:div
                  [main-content]
                  [bottom-nav]])))
