(ns onyx-etl.functions.transformers
  (:require [datomic.api :as d]))

(defn prepare-datoms [db-partition ks segment]
  (let [base {:db/id (d/tempid db-partition)}
        new-keys (reduce-kv #(assoc %1 %2 (get segment %3)) {} ks)]
    {:tx [(merge base new-keys)]}))
