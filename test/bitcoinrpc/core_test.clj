(ns bitcoinrpc.core-test
  (:require [clojure.test :refer :all]
            [bitcoinrpc.core :refer :all])
  (:import [bitcoinrpc BtcJava]))

;;These are not unit tests, they depend on bitcoind running on the local machine

(deftest a-test
  (let [j-btc (BtcJava. "" "tjabba" "http://localhost:18332")]
    (is (= (getbalance @config) (.getbalance j-btc)))
    (is (= (listunspent @config) (.listunspent j-btc)))
  ))
