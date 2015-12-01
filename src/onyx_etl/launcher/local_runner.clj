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

(def cli-options
  [["-f" "--from <medium>" "Input storage medium"
    :parse-fn #(keyword %)
    :validate [#(some #{%} #{:sql}) "Must be one of #{:sql}"]]

   ["-t" "--to <medium>" "Output storage medium"
    :parse-fn #(keyword %)
    :validate [#(some #{%} #{:datomic}) "Must be one of #{:datomic}"]]

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

(defn -main [& args]
  (let [cluster-id (java.util.UUID/randomUUID)
        n-peers 3
        dev-env (s/onyx-dev-env n-peers)]
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread.
                       (fn []
                         (component/stop dev-env)
                         (shutdown-agents))))))
