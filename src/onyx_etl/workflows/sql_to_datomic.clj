(ns onyx-etl.workflows.sql-to-datomic)

(def sql-to-datomic-workflow
  [[:partition-keys :read-rows]
   [:read-rows :prepare-datoms]
   [:prepare-datoms :write-to-datomic]])
