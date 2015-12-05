(ns {{app-name}}.launcher.local-runner
  (:require [com.stuartsierra.component :as component]
            [onyx-etl-support.core :refer [parse-cli-opts]]
            [{{app-name}}.launcher.dev-system :as s]
            [onyx.plugin.datomic]
            [onyx.plugin.sql]
            [onyx.api])
  (:gen-class))

(defn -main [& args]
  (let [{:keys [success msgs job]} (parse-cli-opts args)]
    (if success
      (let [n-peers 4
            dev-env (component/start (s/onyx-dev-env n-peers))
            peer-config (s/load-peer-config (:onyx-id dev-env))
            job-id (:job-id (onyx.api/submit-job peer-config job))]
        (println)
        (println "Executing the following Onyx job:")
        (println)
        (clojure.pprint/pprint job)
        (onyx.api/await-job-completion peer-config job-id)
        (println)
        (println "Onyx data transfer job complete. Check onyx.log for any exceptions or errors.")
        (println)
        (component/stop dev-env)
        (shutdown-agents))
      (doseq [m msgs]
        (println m)))))
