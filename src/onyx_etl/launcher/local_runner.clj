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
            [onyx.api]))

(defn build-workflow [from to]
  (cond (and (= from :sql) (= to :datomic))
        onyx-etl.workflows.sql-to-datomic/sql-to-datomic-workflow

        :else
        (throw (ex-info (format "onyx-etl doesn't know how to move data from %s to %s."
                                from to)
                        {:from from :to to}))))

(defn find-input-lifecycles [medium]
  (cond (= medium :sql)
        (onyx-etl.lifecycles.sql-lifecycles/sql-reader-entries)

        :else
        (throw (ex-info (format "onyx-etl doesn't have input lifecycles for %s" medium) {:medium medium}))))

(defn find-output-lifecycles [medium]
  (cond (= medium :datomic)
        (onyx-etl.lifecycles.datomic-lifecycles/datomic-bulk-writer-entries)

        :else
        (throw (ex-info (format "onyx-etl doesn't have output lifecycles for %s" medium) {:medium medium}))))

(defn find-input-catalog-entries [medium opts]
  (cond (= medium :sql)
        (onyx-etl.catalogs.sql-catalog/sql-input-entries
         (:jdbc-spec opts)
         (:sql-table opts)
         (:sql-id-column opts)
         (:sql-rows-per-segment opts)
         (:input-batch-size opts))

        :else
        (throw (ex-info (format "onyx-etl doesn't have input catalog entries for %s" medium) {:medium medium}))))

(defn find-output-catalog-entries [medium opts]
  (cond (= medium :datomic)
        (let [key-map (read-string (slurp (clojure.java.io/resource (:datomic-key-file opts))))]
          (onyx-etl.catalogs.datomic-catalog/datomic-output-entries
           (:datomic-uri opts)
           (:datomic-partition opts)
           key-map
           (:function-batch-size opts)
           (:output-batch-size opts)))

        :else
        (throw (ex-info (format "onyx-etl doesn't have output catalog entries for %s" medium) {:medium medium}))))

(defn build-lifecycles [from to]
  (let [inputs (find-input-lifecycles from)
        outputs (find-output-lifecycles to)]
    (concat inputs outputs)))

(defn build-catalog [from to opts]
  (let [inputs (find-input-catalog-entries from opts)
        outputs (find-output-catalog-entries to opts)]
    (concat inputs outputs)))

(def cli-options
  [["-f" "--from <medium>" "Input storage medium"
    :parse-fn #(keyword %)
    :validate [#(some #{%} #{:sql}) "Must be one of #{:sql}"]]

   ["-t" "--to <medium>" "Output storage medium"
    :parse-fn #(keyword %)
    :validate [#(some #{%} #{:datomic}) "Must be one of #{:datomic}"]]

   [nil "--input-batch-size <n>" "Batch size of the input task"
    :parse-fn #(Integer/parseInt %)
    :validate [pos? "Must be a positive integer"]]

   [nil "--transform-batch-size <n>" "Batch size of the transformation task"
    :parse-fn #(Integer/parseInt %)
    :validate [pos? "Must be a positive integer"]]

   [nil "--output-batch-size <n>" "Batch size of the output task"
    :parse-fn #(Integer/parseInt %)
    :validate [pos? "Must be a positive integer"]]

   [nil "--datomic-uri <uri>" "Datomic URI"]
   [nil "--datomic-partition <part>" "Datomic partition to use"]
   [nil "--datomic-key-file <file>" "Absolute or relative path to a Datomic key transformation file. See this project's README.md."]

   [nil "--sql-classname <JDBC classname>" "The SQL JDBC spec classname"]
   [nil "--sql-subprotocol <JDBC subprotocol>" "The SQL JDBC spec subprotocol"]
   [nil "--sql-subname <JDBC subname>" "The SQL JDBC spec subname"]
   [nil "--sql-user <user>" "The user to log in to the SQL database as"]
   [nil "--sql-password <password>" "The password to authenticate the user"]
   [nil "--sql-table <table>" "The SQL table to read from"]
   [nil "--sql-id-column <column-name>" "The SQL column in the table to partition by"]
   [nil "--sql-rows-per-segments <n>" "The number of rows to compact into a single segment at read time"
    :parse-fn #(Integer/parseInt %)]

   ["-h" "--help"]])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (clojure.string/join \newline errors)))

(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond (:help options)
          (doseq [s (clojure.string/split summary #"\n")]
            (println s))

          errors (error-msg errors)

          :else
          (let [cluster-id (java.util.UUID/randomUUID)
                n-peers 3
                opts (parse-opts args cli-options)
                from (:from (:options opts))
                to (:to (:options opts))
                workflow (build-workflow from to)
                lifecycles (build-lifecycles from to)
                catalog (build-catalog from to (:options opts))
                dev-env (s/onyx-dev-env n-peers)
                peer-config (onyx-etl.launcher.dev-system/load-peer-config cluster-id)
                job-id (:job-id (onyx.api/submit-job
                                 peer-config
                                 {:workflow workflow
                                  :catalog catalog
                                  :lifecycles lifecycles}))]
            (onyx.api/await-job-completion peer-config job-id)
            (.addShutdownHook (Runtime/getRuntime)
                              (Thread.
                               (fn []
                                 (component/stop dev-env)
                                 (shutdown-agents))))))))
