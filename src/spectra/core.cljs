(ns spectra.core
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [re-frame.core :as rf]
            [spectra.app.events]
            [spectra.app.subs]
            [spectra.app.fx :as fx]
            [spectra.app.router :as router]
            [spectra.adapters.auth-firebase :as auth-fb]
            [spectra.adapters.repository-firebase :as repo-fb]
            [spectra.ui.shell :as shell]
            ["firebase/app" :as firebase-app]
            ["firebase/auth" :as firebase-auth]
            ["firebase/firestore" :as firebase-firestore]))

;; Firebase configuration - replace with your own values
;; or load from a config endpoint
(def firebase-config
  #js {:apiKey "AIzaSyCeRmTDV7y8zPpBDpFeo8EECkaHtRNFTQY"
       :authDomain "spectra-f4242.firebaseapp.com"
       :projectId "spectra-f4242"
       :storageBucket "spectra-f4242.firebasestorage.app"
       :messagingSenderId "319424744452"
       :appId "1:319424744452:web:90a624e9a6cb7e72857951"
       :measurementId "G-TFP2E14HBE"})

(defonce firebase-app-instance (atom nil))
(defonce root (atom nil))

(defn init-firebase! []
  (when-not @firebase-app-instance
    (let [app (firebase-app/initializeApp firebase-config)
          auth (firebase-auth/getAuth app)
          db (firebase-firestore/getFirestore app)
          google-provider (firebase-auth/GoogleAuthProvider.)]
      (reset! firebase-app-instance app)
      (fx/set-adapters!
       (auth-fb/create-auth auth google-provider)
       (repo-fb/create-repository db)))))

(defn app []
  [shell/app-shell])

(defn ^:dev/after-load reload! []
  (rf/clear-subscription-cache!)
  (when @root
    (.render @root (r/as-element [app]))))

(defn init []
  (init-firebase!)
  (router/init!)
  (rf/dispatch-sync [:app/initialize])
  (let [container (js/document.getElementById "app")]
    (reset! root (rdom/create-root container))
    (.render @root (r/as-element [app]))))
