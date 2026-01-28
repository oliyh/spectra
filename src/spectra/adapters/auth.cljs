(ns spectra.adapters.auth)

(defprotocol Auth
  "Protocol for authentication"
  (current-user [this] "Returns current user map or nil")
  (sign-in-with-google! [this] "Initiates Google sign-in, returns promise")
  (sign-out! [this] "Signs out current user, returns promise")
  (on-auth-state-changed [this callback] "Calls callback with user on auth changes, returns unsubscribe fn"))
