(defproject lacinia-hack "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.walmartlabs/lacinia "0.20.0"]
                 [cheshire "5.7.1"]
                 [ring "1.6.2"]
                 [ring-cors "0.1.10"]
                 [compojure "1.6.0"]
                 [com.datomic/datomic-free "0.9.5561.54"]]
  :plugins [[lein-ring "0.12.0"]]
  :ring {:handler lacinia-hack.core/app
         :init lacinia-hack.core/setup
         :nrepl {:start? true :port 4555}}
  :main lacinia-hack.core)
