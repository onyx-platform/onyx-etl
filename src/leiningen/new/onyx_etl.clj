(ns leiningen.new.onyx-etl
  (:require [leiningen.new.templates :refer [renderer name-to-path ->files]]
            [leiningen.core.main :as main]))

(def render (renderer "onyx_etl"))

(def files
  ["LICENSE"
   "README.md"
   "resources/env-config.edn"
   "resources/peer-config.edn"
   "env/dev/user.clj"
   "project.clj"
   "src/onyx_etl/launcher/dev_system.clj"
   "src/onyx_etl/launcher/local_runner.clj"])

(defn render-files [files name data]
  (mapv (juxt (fn [path]
                (let [name* (clojure.string/replace name #"-" "_")]
                  (clojure.string/replace path #"onyx_etl" name*)))
              (fn [file-path] (render file-path data)))
        files))

(defn onyx-etl
  "Creates a new onyx-etl application template"
  [name & args]
  (let [path (name-to-path name)
        onyx-version "0.8.2"
        data {:name name
              :onyx-version onyx-version
              :app-name name
              :sanitized path}
        render-instructions (render-files files name data)]
    (main/info "Creating a new onyx-etl application template...")
    (apply ->files data render-instructions)))
