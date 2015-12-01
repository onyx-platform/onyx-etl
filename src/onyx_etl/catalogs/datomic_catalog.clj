(ns onyx-etl.catalogs.datomic-catalog)

(defn datomic-transform-entry [db-partition key-map batch-size]
  {:onyx/name :prepare-datoms
   :onyx/fn :onyx-etl.functions.transformers/prepare-datoms
   :onyx/type :function
   :datomic/partition db-partition
   :datomic/key-map key-map
   :onyx/params [:datomic/partition :datomic/key-map]
   :onyx/batch-size batch-size
   :onyx/doc "Semantically transform the SQL rows to Datomic datoms"})

(defn datomic-output-entry [db-uri db-partition batch-size]
  {:onyx/name :write-to-datomic
   :onyx/plugin :onyx.plugin.datomic/write-bulk-datoms
   :onyx/type :output
   :onyx/medium :datomic
   :datomic/uri db-uri
   :datomic/partition db-partition
   :onyx/batch-size batch-size
   :onyx/doc "Transacts segments to storage"})
