(ns {{app-name}}.launcher.local-runner
  (:require [com.stuartsierra.component :as component]
            [onyx-etl-support.core :refer [parse-cli-opts write-to-ns]]
            [onyx-etl-support.functions.transformers] 
            [{{app-name}}.launcher.dev-system :as s]
            [onyx.plugin.datomic]
            [onyx.plugin.sql]
            [onyx.api])
  (:gen-class))

(defn write-to-file! [job-file job]
  (write-to-ns (format "src/{{underscored-name}}/launcher/%s" job-file)
               "{{app-name}}.launcher."
               (clojure.string/replace (apply str (drop-last 4 job-file)) #"_" "-")
               "{{app-name}}.launcher.dev-system"
               job))

(defn -main [& args]
  (let [{:keys [success dry-run? msgs job job-file]} (parse-cli-opts args)]
    (cond (and success dry-run?)
          (do (println)
              (println "Executing in dry-run mode, would have run the following Onyx job:")
              (println)
              (clojure.pprint/pprint job)
              (when job-file
                (write-to-file! job-file job)
                (println (format "Wrote Onyx job to %s" job-file))))

          success
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
            (when job-file
              (write-to-file! job-file job)
              (println (format "Wrote Onyx job to %s" job-file)))
            (component/stop dev-env)
            (shutdown-agents))
          :else
          (doseq [m msgs]
            (println m)))))
