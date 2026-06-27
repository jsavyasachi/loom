(defproject net.clojars.savya/loom "1.3.0"
  :min-lein-version "2.0.0"
  :description "Graph library for Clojure and ClojureScript"
  :license {:name "Eclipse Public License 1.0"
            :url "https://www.eclipse.org/legal/epl-v10.html"}
  :deploy-repositories [["releases" {:url "https://repo.clojars.org"
                                     :username :env/clojars_username
                                     :password :env/clojars_password
                                     :sign-releases false}]]
  :dependencies [[org.clojure/clojure "1.12.0" :scope "provided"]
                 [org.clojure/data.priority-map "1.2.1"]
                 [tailrecursion/cljs-priority-map "1.2.1"]]
  :url "https://github.com/jsavyasachi/loom"
  :test-selectors {:default (fn [m] (not (:test-check-slow m)))
                   :all (constantly true)
                   :test-check-slow :test-check-slow}

  :aliases {"all" ["with-profile" "+clojure-1-10:+clojure-1-11:+clojure-1-12"]
            "test-all" ["do" "clean," "test" ":all," "cljs-test"]
            "cljs-test" ["doo" "node" "node-test" "once"]
            "release" ["do" "clean," "with-profile" "default" "deploy" "releases"]}

  :profiles {:dev [:cljs
                   {:dependencies [[org.clojure/test.check "1.1.3"]]
                    :plugins [[com.jakemccrary/lein-test-refresh "0.26.0"]]
                    :repl-options {:init (set! *print-length* 50)}}]

             :clojure-1-10 {:dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]]}
             :clojure-1-11 {:dependencies [[org.clojure/clojure "1.11.4" :scope "provided"]]}
             :clojure-1-12 {:dependencies [[org.clojure/clojure "1.12.0" :scope "provided"]]}

             :cljs {:dependencies [[org.clojure/clojurescript "1.12.145"]]
                    :plugins [[lein-cljsbuild "1.1.8" :exclusions [org.clojure/clojure]]
                              [lein-doo "0.1.11"]]
                    :doo {:build "node-dev"}
                    :cljsbuild {:builds
                                {"node-dev"
                                 {:source-paths ["src", "test"]
                                  :compiler {:output-to "target/loom.js"
                                             :optimizations :none
                                             :pretty-print true
                                             :target :nodejs
                                             :main loom.test.runner}}
                                 "node-test"
                                 {:id "min"
                                  :source-paths ["src", "test"]
                                  :compiler {:output-to "target/loom.js"
                                             :optimizations :advanced
                                             :pretty-print false
                                             :target :nodejs
                                             :main loom.test.runner}}}}}})
