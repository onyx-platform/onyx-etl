(defproject onyx-etl "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.onyxplatform/onyx "0.8.2"]
                 [com.datomic/datomic-free "0.9.5327"]]
  :profiles {:uberjar {:aot [onyx-etl.launcher.aeron-media-driver
                             onyx-etl.launcher.launch-prod-peers]}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]
                   :source-paths ["env/dev" "src"]}})
