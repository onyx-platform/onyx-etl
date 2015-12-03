(ns onyx-etl.launcher.local-runner
  (:require [clojure.java.io :refer [resource]]
            [clojure.tools.cli :refer [parse-opts]]
            [com.stuartsierra.component :as component]
            [onyx-etl.launcher.dev-system :as s]
            [onyx-etl.lifecycles.datomic-lifecycles]
            [onyx-etl.lifecycles.sql-lifecycles]
            [onyx-etl.functions.transformers]
            [onyx-etl.catalogs.datomic-catalog]
            [onyx-etl.catalogs.sql-catalog]
            [onyx-etl.workflows.sql-to-datomic]
            [onyx.plugin.datomic]
            [onyx.plugin.sql]
            [onyx.api]))

(def default-batch-size 20)

(def default-sql-rows-per-segment 50)

(defmulti build-workflow
  (fn [from to]
    [from to]))

(defmulti find-input-lifecycles
  (fn [medium]
    medium))

(defmulti find-output-lifecycles
  (fn [medium]
    medium))

(defmulti find-input-catalog-entries
  (fn [medium opts]
    medium))

(defmulti find-output-catalog-entries
  (fn [medium opts]
    medium))

(defmethod build-workflow [:sql :datomic]
  [from to]
  onyx-etl.workflows.sql-to-datomic/sql-to-datomic-workflow)

(defmethod find-input-lifecycles :sql
  [medium]
  (onyx-etl.lifecycles.sql-lifecycles/sql-reader-entries))

(defmethod find-output-lifecycles :datomic
  [medium]
  (onyx-etl.lifecycles.datomic-lifecycles/datomic-bulk-writer-entries))

(defmethod find-input-catalog-entries :sql
  [medium opts]
  (onyx-etl.catalogs.sql-catalog/sql-input-entries
   {:classname (:sql-classname opts)
    :subprotocol (:sql-subprotocol opts)
    :subname (:sql-subname opts)
    :user (:sql-user opts)
    :password (:sql-password opts)}
   (:sql-table opts)
   (:sql-id-column opts)
   (:sql-rows-per-segment opts)
   (:input-batch-size opts)))

(defmethod find-output-catalog-entries :datomic
  [medium opts]
  (when-let [kf (:datomic-key-file opts)]
    (when-let [key-map (read-string (slurp kf))]
      (onyx-etl.catalogs.datomic-catalog/datomic-output-entries
       (:datomic-uri opts)
       (:datomic-partition opts)
       key-map
       (:transform-batch-size opts)
       (:output-batch-size opts)))))

(defmethod build-workflow :default
  [from to])

(defmethod find-input-lifecycles :default
  [medium])

(defmethod find-output-lifecycles :default
  [medium])

(defmethod find-input-catalog-entries :default
  [medium opts])

(defmethod find-output-catalog-entries :default
  [medium opts])

(def cli-options
  [["-f" "--from <medium>" "Input storage medium"
    :missing "--from is a required parameter, it was missing or incorrect"
    :parse-fn #(keyword %)
    :validate [#(some #{%} #{:sql}) "Must be one of #{:sql}"]]

   ["-t" "--to <medium>" "Output storage medium"
    :missing "--to is a required parameter, it was missing or incorrect"
    :parse-fn #(keyword %)
    :validate [#(some #{%} #{:datomic}) "Must be one of #{:datomic}"]]

   [nil "--input-batch-size <n>" "Batch size of the input task"
    :parse-fn #(Integer/parseInt %)
    :default default-batch-size
    :validate [pos? "Must be a positive integer"]]

   [nil "--transform-batch-size <n>" "Batch size of the transformation task"
    :parse-fn #(Integer/parseInt %)
    :default default-batch-size
    :validate [pos? "Must be a positive integer"]]

   [nil "--output-batch-size <n>" "Batch size of the output task"
    :parse-fn #(Integer/parseInt %)
    :default default-batch-size
    :validate [pos? "Must be a positive integer"]]

   [nil "--datomic-uri <uri>" "Datomic URI"]
   [nil "--datomic-partition <part>" "Datomic partition to use"
    :parse-fn #(keyword %)]
   [nil "--datomic-key-file <file>" "Absolute or relative path to a Datomic key transformation file. See this project's README.md."]

   [nil "--sql-classname <JDBC classname>" "The SQL JDBC spec classname"]
   [nil "--sql-subprotocol <JDBC subprotocol>" "The SQL JDBC spec subprotocol"]
   [nil "--sql-subname <JDBC subname>" "The SQL JDBC spec subname"]
   [nil "--sql-user <user>" "The user to log in to the SQL database as"]
   [nil "--sql-password <password>" "The password to authenticate the user"]
   [nil "--sql-table <table>" "The SQL table to read from"
    :parse-fn #(keyword %)]
   [nil "--sql-id-column <column-name>" "The SQL column in the table to partition by"
    :parse-fn #(keyword %)]
   [nil "--sql-rows-per-segment <n>" "The number of rows to compact into a single segment at read time"
    :default default-sql-rows-per-segment
    :parse-fn #(Integer/parseInt %)]

   ["-h" "--help"]])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (clojure.string/join \newline errors)))

(defn -main [& args]
  (let [{:keys [options arguments errors summary] :as xx} (parse-opts args cli-options)]
    (cond (:help options)
          (doseq [s (clojure.string/split summary #"\n")]
            (println s))

          errors (println (error-msg errors))

          :else
          (let [n-peers 4
                opts (parse-opts args cli-options)
                from (:from (:options opts))
                to (:to (:options opts))
                workflow (build-workflow from to)
                
                input-catalog-entries (find-input-catalog-entries from (:options opts))
                output-catalog-entries (find-output-catalog-entries to (:options opts))
                catalog (concat input-catalog-entries output-catalog-entries)

                input-lifecycle-entries (find-input-lifecycles from)
                output-lifecycle-entries (find-output-lifecycles to)
                lifecycles (concat input-lifecycle-entries output-lifecycle-entries)]

            (when-not workflow
              (println (format "onyx-etl doesn't support moving data from %s to %s. Aborting." from to))
              (System/exit 1))

            (when-not input-catalog-entries
              (println (format "onyx-etl doesn't have input catalog entries for %s. Aborting." from))
              (System/exit 1))

            (when-not output-catalog-entries
              (println (format "onyx-etl doesn't have output catalog entries for %s. Aborting." to))
              (System/exit 1))

            (when-not input-lifecycle-entries
              (println (format "onyx-etl doesn't have input lifecycles for %s" from))
              (System/exit 1))

            (when-not output-lifecycle-entries
              (println (format "onyx-etl doesn't have output lifecycles for %s" to))
              (System/exit 1))

            (clojure.pprint/pprint {:workflow workflow
                                    :catalog catalog
                                    :lifecycles lifecycles
                                    :task-scheduler :onyx.task-scheduler/balanced})

            (let [dev-env (component/start (s/onyx-dev-env n-peers))
                  peer-config (onyx-etl.launcher.dev-system/load-peer-config (:onyx-id dev-env))
                  job-id (:job-id (onyx.api/submit-job
                                   peer-config
                                   {:workflow workflow
                                    :catalog catalog
                                    :lifecycles lifecycles
                                    :task-scheduler :onyx.task-scheduler/balanced}))]
              (onyx.api/await-job-completion peer-config job-id)
              (.addShutdownHook (Runtime/getRuntime)
                                (Thread.
                                 (fn []
                                   (component/stop dev-env)
                                   (shutdown-agents)))))))))
