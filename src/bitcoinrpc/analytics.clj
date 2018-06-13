(ns bitcoinrpc.analytics
  "Bitcoin Core analytics"
  (:use [clojure.pprint]
        [clojure.repl])
  (:require [bitcoinrpc.core :refer :all]))


(def coinbase-tx (apply str (repeat 32 "0")))

(defn tx-history-of [txid]
  (let [tx (-> txid getrawtransaction decoderawtransaction)
        tx-id (tx "txid")]
    (if (= tx-id coinbase-tx)
      tx
      (let [vin (tx "vin")]
        tx))))
        ;(lazy-seq (cons tx (tx-history-of  
  
(defn blocks-of [the-key]
  (letfn [(block-fn [block-hash]
          (let [b (getblock block-hash)]
            (if-let [bh (b the-key)]
              (lazy-seq (cons b (block-fn bh)))
              (list b))))]
    block-fn))

(defn block-hash-str-of [prefix] (str prefix "blockhash"))

(defn blocks-backward
  "Get a lazy seq of blocks going backward in time"
  ([block-hash]
  ((blocks-of (block-hash-str-of "prevous")) block-hash))
  ([]
    (blocks-backward (getbestblockhash))))

(defn blocks-forward
  "Get a lazy seq of blocks going forward in time"
  ([block-hash]
  ((blocks-of (block-hash-str-of "next")) block-hash))
  ([]
    (blocks-backward (getblockhash 0))))
  
        
  
