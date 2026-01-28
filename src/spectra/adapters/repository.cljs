(ns spectra.adapters.repository)

(defprotocol Repository
  "Protocol for data persistence"
  (fetch-subjects [this] "Returns a channel/promise with all subjects")
  (save-subject! [this subject] "Saves a subject, returns channel/promise")
  (delete-subject! [this subject-id] "Deletes a subject, returns channel/promise")
  (fetch-entries [this subject-id start-date end-date] "Returns entries for subject in date range")
  (fetch-entry [this subject-id date] "Returns single entry or nil")
  (save-entry! [this entry] "Saves an entry, returns channel/promise")
  (delete-entry! [this entry-id] "Deletes an entry, returns channel/promise")
  (fetch-tags [this] "Returns all known tags")
  (save-tag! [this tag] "Saves a tag to the global tags collection"))
