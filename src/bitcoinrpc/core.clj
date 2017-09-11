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

;blockchain

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

(s/fdef getblockheader 
  :args (s/alt :verbose (s/cat :hash string?), :choice (s/cat :hash string?, :verbose boolean?)))
(def-rpc-opt getblockheader hash verbose)

(s/fdef getchaintips :ret vector?) 
(def-rpc getchaintips)

(s/fdef getdifficulty :ret integer?) 
(def-rpc getdifficulty)

(def-rpc-opt getmempoolancestors txid verbose)
(def-rpc-opt getmempooldescendants txid verbose)
(def-rpc getmempoolentry txid)
(def-rpc getmempoolinfo)
(def-rpc-opt getrawmempool verbose)
(def-rpc-opt gettxout txid n include_mempool)

;(s/def gettxoutproof ["txid",...] ( blockhash )
(def-rpc gettxoutproof txids blockhash)

(def-rpc gettxoutsetinfo)
(def-rpc preciousblock blockhash)
(def-rpc pruneblockchain)
;verifychain ( checklevel nblocks )
(def-rpc verifytxoutproof proof)


;== Control ==
(def-rpc getinfo)
(def-rpc getmemoryinfo)
;help ( "command" )
(def-rpc stop)

;== Generating ==
(def-rpc-opt generate nblocks maxtries)
(def-rpc-opt generatetoaddress nblocks address maxtries)

;== Mining ==
(def-rpc-opt getblocktemplate TemplateRequest )
(def-rpc getmininginfo)
;getnetworkhashps ( nblocks height )
;prioritisetransaction <txid> <priority delta> <fee delta>
(def-rpc-opt submitblock hexdata jsonparametersobject)

;== Network ==
(def-rpc addnode node action)
(def-rpc clearbanned)
(def-rpc disconnectnode address) 
(def-rpc-opt getaddednodeinfo node)
(def-rpc getconnectioncount)
(def-rpc getnettotals)
(def-rpc getnetworkinfo)
(def-rpc getpeerinfo)
(def-rpc listbanned)
(def-rpc ping)
;setban "subnet" "add|remove" (bantime) (absolute)
(def-rpc setnetworkactive b)

;== Rawtransactions ==
;createrawtransaction [{"txid":"id","vout":n},...] {"address":amount,"data":"hex",...} ( locktime )
(def-rpc decoderawtransaction hexstring)
(def-rpc decodescript hexstring)
(def-rpc-opt fundrawtransaction hexstring options)
(def-rpc-opt getrawtransaction txid  verbose)
(def-rpc-opt sendrawtransaction hexstring allowhighfees)
;signrawtransaction "hexstring" ( [{"txid":"id","vout":n,"scriptPubKey":"hex","redeemScript":"hex"},...] ["privatekey1",...] sighashtype )

;== Util ==
(def-rpc createmultisig nrequired the-keys)
(def-rpc estimatefee nblocks)
(def-rpc estimatepriority nblocks)
(def-rpc estimatesmartfee nblocks)
(def-rpc estimatesmartpriority nblocks)
(def-rpc signmessagewithprivkey privkey message)
(def-rpc validateaddress address)
(def-rpc verifymessage address signature message)

;== Wallet ==
(def-rpc abandontransaction txid)
(def-rpc-opt addmultisigaddress nrequired the-keys account)
(def-rpc addwitnessaddress address)
(def-rpc backupwallet destination)
(def-rpc-opt bumpfee txid options) 
(def-rpc dumpprivkey address)
(def-rpc dumpwallet filename)
(def-rpc encryptwallet passphrase)
(def-rpc getaccount address)
(def-rpc getaccountaddress account)
(def-rpc getaddressesbyaccount account)
;getbalance ( "account" minconf include_watchonly )
(def-rpc-opt getnewaddress account)
(def-rpc getrawchangeaddress)
(def-rpc-opt getreceivedbyaccount account minconf)
(def-rpc-opt getreceivedbyaddress address minconf)
(def-rpc-opt gettransaction txid include_watchonly)
(def-rpc getunconfirmedbalance)
(def-rpc getwalletinfo)
;importaddress "address" ( "label" rescan p2sh )
(def-rpc importmulti requests options)
;importprivkey "bitcoinprivkey" ( "label" ) ( rescan )
(def-rpc importprunedfunds)
;importpubkey "pubkey" ( "label" rescan )
(def-rpc importwallet filename)
(def-rpc-opt keypoolrefill newsize)
;listaccounts ( minconf include_watchonly)
(def-rpc listaddressgroupings)
(def-rpc listlockunspent)
;listreceivedbyaccount ( minconf include_empty include_watchonly)
;listreceivedbyaddress ( minconf include_empty include_watchonly)
;listsinceblock ( "blockhash" target_confirmations include_watchonly)
;listtransactions ( "account" count skip include_watchonly)
;listunspent ( minconf maxconf  ["addresses",...] [include_unsafe] )
;lockunspent unlock ([{"txid":"txid","vout":n},...])
;move "fromaccount" "toaccount" amount ( minconf "comment" )
(def-rpc removeprunedfunds txid)
;sendfrom "fromaccount" "toaddress" amount ( minconf "comment" "comment_to" )
;sendmany "fromaccount" {"address":amount,...} ( minconf "comment" ["address",...] )
;sendtoaddress "address" amount ( "comment" "comment_to" subtractfeefromamount )
(def-rpc setaccount address account)
(def-rpc settxfee amount)
(def-rpc signmessage address message)


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



