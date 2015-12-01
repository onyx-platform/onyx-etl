(ns onyx-etl.lifecycles.sql-lifecycles)

(defn sql-reader-entries []
  [{:lifecycle/task :partition-keys
    :lifecycle/calls :onyx.plugin.sql/partition-keys-calls}
   {:lifecycle/task :read-rows
    :lifecycle/calls :onyx.plugin.sql/read-rows-calls}])
