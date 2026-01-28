(ns spectra.adapters.auth-firebase
  (:require [spectra.adapters.auth :as auth]
            ["firebase/auth" :as firebase-auth]))

(defn- firebase-user->user [firebase-user]
  (when firebase-user
    {:uid (.-uid firebase-user)
     :email (.-email firebase-user)
     :display-name (.-displayName firebase-user)
     :photo-url (.-photoURL firebase-user)}))

(defrecord FirebaseAuth [auth google-provider]
  auth/Auth

  (current-user [_]
    (firebase-user->user (.-currentUser auth)))

  (sign-in-with-google! [_]
    (firebase-auth/signInWithPopup auth google-provider))

  (sign-out! [_]
    (firebase-auth/signOut auth))

  (on-auth-state-changed [_ callback]
    (firebase-auth/onAuthStateChanged auth #(callback (firebase-user->user %)))))

(defn create-auth [auth google-provider]
  (->FirebaseAuth auth google-provider))
