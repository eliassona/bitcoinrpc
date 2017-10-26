(defproject bitcoinrpc "0.1.0-SNAPSHOT"
  :description "Clojure and Java API for Bitcoin RPC"
  :url "https://github.com/eliassona/bitcoinrpc"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :java-source-paths ["java/src"]
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [clj-http "3.7.0"]
                 [com.gfredericks/debug-repl "0.0.9"]
                 [org.clojure/data.json "0.2.6"]
                 [instaparse "1.4.7"]
                 [org.clojure/test.check "0.9.0"]]
  :aot [bitcoinrpc.btcjava]
  :repl-options
  {:nrepl-middleware
    [com.gfredericks.debug-repl/wrap-debug-repl]})
