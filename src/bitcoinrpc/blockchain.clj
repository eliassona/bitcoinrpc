(ns bitcoinrpc.blockchain
  "Functions using the blockchain.info API"
  (:require [clj-http.client :as client]
            [clojure.data.json :as json])
  )

(defn blockchain-call [arg]
  (let [res (client/get (format "https://blockchain.info/%s" arg))]
    (if (= (:status res) 200) 
      (-> res :body json/read-str)
      (throw (IllegalStateException. (pr-str res))))))


(defn exchange-rates [] (blockchain-call "ticker"))

(defn dollar-rate [] (((exchange-rates) "USD") "last"))

(defn tobtc [value to-currency]
  (blockchain-call (format "tobtc?currency=%s&value=%s" to-currency value)))

(defn blockchain-query-call [arg] (blockchain-call (format "q/%s" arg)))

(defmacro def-query [& args]
  `(do 
     ~@(map (fn [a] `(defn ~a [] (blockchain-query-call ~(str a)))) args)))


;realtime
(def-query 
  getdifficulty 
  getblockcount 
  latesthash
  bcperblock
  totalbc
  probability
  hashestowin
  nextretarget
  avgtxsize 
  avgtxvalue
  interval 
  eta
  avgtxnumber)


;misc

(def-query 
  unconfirmedcount
  marketcap
  hashrate
  rejected)
  
  


(defn _24hrprice [] (blockchain-query-call "24hrprice"))
(defn _24hrtransactioncount  [] (blockchain-query-call "24hrtransactioncount "))
(defn _24hrbtcsent [] (blockchain-query-call "24hrbtcsent"))


(defn block-of [height] (blockchain-call (format "block-height/%s?format=json" height)))

(defn latest-block [] (blockchain-call "latestblock"))
