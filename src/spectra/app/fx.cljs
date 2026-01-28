(ns spectra.app.fx
  (:require [re-frame.core :as rf]
            [spectra.adapters.auth :as auth]
            [spectra.adapters.repository :as repo]))

;; These will be set during initialization
(defonce ^:private adapters (atom {:auth nil :repo nil}))

(defn set-adapters! [auth-adapter repo-adapter]
  (reset! adapters {:auth auth-adapter :repo repo-adapter}))

;; Auth effects
(rf/reg-fx
 :firebase/init-auth
 (fn [_]
   (when-let [auth-adapter (:auth @adapters)]
     (auth/on-auth-state-changed
      auth-adapter
      #(rf/dispatch [:auth/state-changed %])))))

(rf/reg-fx
 :firebase/sign-in-google
 (fn [_]
   (when-let [auth-adapter (:auth @adapters)]
     (-> (auth/sign-in-with-google! auth-adapter)
         (.catch #(js/console.error "Sign in failed:" %))))))

(rf/reg-fx
 :firebase/sign-out
 (fn [_]
   (when-let [auth-adapter (:auth @adapters)]
     (auth/sign-out! auth-adapter))))

;; Repository effects
(rf/reg-fx
 :firebase/fetch-subjects
 (fn [_]
   (when-let [repo (:repo @adapters)]
     (-> (repo/fetch-subjects repo)
         (.then #(rf/dispatch [:subjects/loaded %]))
         (.catch #(js/console.error "Failed to fetch subjects:" %))))))

(rf/reg-fx
 :firebase/save-subject
 (fn [subject]
   (when-let [repo (:repo @adapters)]
     (-> (repo/save-subject! repo subject)
         (.catch #(js/console.error "Failed to save subject:" %))))))

(rf/reg-fx
 :firebase/fetch-entries
 (fn [[subject-id start-date end-date]]
   (when-let [repo (:repo @adapters)]
     (-> (repo/fetch-entries repo subject-id start-date end-date)
         (.then #(rf/dispatch [:entries/loaded %]))
         (.catch #(js/console.error "Failed to fetch entries:" %))))))

(rf/reg-fx
 :firebase/fetch-entry
 (fn [[subject-id date]]
   (when-let [repo (:repo @adapters)]
     (-> (repo/fetch-entry repo subject-id date)
         (.then #(rf/dispatch [:entry/loaded %]))
         (.catch #(js/console.error "Failed to fetch entry:" %))))))

(rf/reg-fx
 :firebase/save-entry
 (fn [entry]
   (when-let [repo (:repo @adapters)]
     (-> (repo/save-entry! repo entry)
         (.catch #(js/console.error "Failed to save entry:" %))))))

;; Tags effects
(rf/reg-fx
 :firebase/fetch-tags
 (fn [_]
   (when-let [repo (:repo @adapters)]
     (-> (repo/fetch-tags repo)
         (.then #(rf/dispatch [:tags/loaded %]))
         (.catch #(js/console.error "Failed to fetch tags:" %))))))

(rf/reg-fx
 :firebase/save-tag
 (fn [tag]
   (when-let [repo (:repo @adapters)]
     (-> (repo/save-tag! repo tag)
         (.catch #(js/console.error "Failed to save tag:" %))))))
