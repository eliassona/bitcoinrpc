(ns bitcoinrpc.core
  (:use [clojure.pprint])
  (:require [clojure.data.json :as json]
            [instaparse.core :as insta]
            [clj-http.client :as client]
            [clojure.repl :refer [source dir doc]]
            [clojure.spec :as s]
            [clojure.spec.gen :as gen]))


(defn btc-rpc [method & args]
  (let [res 
        (-> (client/post "http://localhost:18332" 
                         {:body (json/write-str 
                                  {:method method
                                   :params args
                                   :id "foo"})
                          :headers {"Content-Type" "application/json; charset=utf-8"}
                          :basic-auth ["" "tjabba"]
                          :throw-entire-message? true}) :body json/read-str)]
    (if-let
      [e (res "error")]
      (throw (IllegalStateException. e))
      (res "result"))))


(s/def ::height (s/and integer? #(pos? %) ))

(defn help 
  ([]
    (.split (btc-rpc "help") "\n"))
  ([cmd]
    (.split (btc-rpc "help" cmd) "\n")))

(defn get-description [name]
  (loop [r (rest (help name))]
    (if (empty? (first r))
      (recur (rest r))
      (first r))))

(defmacro def-rpc [name & args]
  `(defn ~name ~(get-description name) [~@args] (btc-rpc ~(str name) ~@args))) 

(defmacro def-rpc-opt [name & args]
  `(defn ~name ~(get-description name) 
     ([~@args] (btc-rpc ~(str name) ~@args))
     ([~@(butlast args)] (btc-rpc ~(str name) ~@(butlast args)))
     )) 

;;--------RPC calls-----------



(s/fdef getbestblockhash :ret string?)
(def-rpc getbestblockhash)

(s/fdef getblock :args (s/alt :verbose (s/cat :block-hash string?), :choice (s/cat :block-hash string?, :verbose boolean?)) :ret map?)
(def-rpc-opt getblock block-hash verbose)

(s/fdef getblockchaininfo :ret map?)
(def-rpc getblockchaininfo)

(s/fdef getblockcount :ret ::height)
(def-rpc getblockcount)


(s/fdef getblockhash
  :args ::height      
  :ret string?)
(def-rpc getblockhash height)




;;--------------------------------


(defn get-block-hashes []
  (map (partial btc-rpc "getblockhash") (range (btc-rpc "getblockcount"))))

(defn block-of [block-hash] (getblock block-hash))



(defn print-it [a-list]
  (doseq [l a-list] (println l)))

(defn print-help 
  ([]
    (print-it (.split (btc-rpc "help") "\n")))
  ([cmd]
    (print-it (.split (btc-rpc "help" cmd) "\n"))))


  
;;---------------------------------------------------------------------------------
;experimental

(def help-parser
  (insta/parser
    "BTC = (<'=='> SPACE TITLE SPACE <'=='> NEW-LINE FUNCTIONS)*
     FUNCTIONS = (FUNCTION NEW-LINE)*
     FUNCTION = SYMBOL SPACE ARGS
     ARGS = ((ARG SPACE)* ARG)*
     ARG = (<'\"'> SYMBOL <'\"'>) | SYMBOL | (<'('> OPTIONAL-SPACE SYMBOL OPTIONAL-SPACE <')'>)
     <TITLE> = SYMBOL
     NEW-LINE = '\n' | Epsilon
     SYMBOL = #'[a-zA-Z_]'  #'\\w'*
     <SPACE> = <#'[ \t\n,]+'>
     <OPTIONAL-SPACE> = <#'[ \t\n]*'>
"))



