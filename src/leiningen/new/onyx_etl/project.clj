(defproject {{app-name}} "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.onyxplatform/onyx "0.8.2"]
                 [org.onyxplatform/onyx-datomic "0.8.2.4"]
                 [org.onyxplatform/onyx-sql "0.8.2.1"]
                 [org.onyxplatform/onyx-etl-support "0.8.2.0"]
                 [com.datomic/datomic-free "0.9.5327" :exclusions [joda-time]]
                 [mysql/mysql-connector-java "5.1.27"]]
  :profiles {:uberjar {:aot :all
                       :main {{app-name}}.launcher.local-runner
                       :uberjar-name "{{app-name}}-standalone.jar"}
             :dev {:dependencies [[org.clojure/tools.namespace "0.2.11"]]
                   :source-paths ["env/dev" "src"]}})
