(defproject tab-tree-svc "0.1.0-SNAPSHOT"
  :min-lein-version "2.7.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [compojure "1.5.2"]
                 [hiccup "1.0.5"]
                 [ring-server "0.4.0"]
                 [ring/ring-json "0.4.0"]
                 [clj-datastore "0.3.6-SNAPSHOT"]
                 [cheshire "5.3.1"]
                 [google-apps-clj "0.6.1"]
                 ]
  :plugins [[lein-ring "0.8.12"]]
  :ring {:handler tab-tree-svc.handler/app
         :init tab-tree-svc.handler/init
         :destroy tab-tree-svc.handler/destroy}
  :profiles
  {:uberjar {:aot :all}
   :production
   {:ring
    {:open-browser? false, :stacktraces? false, :auto-reload? false}}
   :dev
   {:dependencies [[ring-mock "0.1.5"] [ring/ring-devel "1.5.1"]]}})
