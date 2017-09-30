(ns bitcoinrpc.core
  (:use [clojure.pprint])
  (:require [clojure.data.json :as json]
            [instaparse.core :as insta]
            [clj-http.client :as client]
            [clojure.repl :refer [source source-fn dir doc]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.set :refer [subset?]]
            [clojure.core.server :as sock-repl]
            [clojure.main :as m]
            )
  (:import [java.io File]))

(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     x#))

(def ^:private id (atom 0))


(defn btc-rpc-fn [method config & args]
  (let [{:keys [user password url]} config
        res 
        (-> (client/post url 
                         {:body (json/write-str 
                                  {:method method
                                   :params args
                                   :id (str "id" (swap! id inc))})
                          :headers {"Content-Type" "application/json; charset=utf-8"}
                          :basic-auth [user password]
                          :throw-entire-message? true}) :body json/read-str)]
    (if-let
      [e (res "error")]
      (throw (IllegalStateException. e))
      (res "result"))))

(def config (atom {:user "", :password "tjabba", :url "http://localhost:18332"}))


(def hex-string? (partial re-matches #"[0-9a-fA-F]+"))

(s/def ::height (s/and integer? #(pos? %)))
(s/def ::hex-string hex-string?)
(s/def ::address string?)
(s/def ::signature string?)

(defn split [s] (.split s "\n"))



(defn split-help 
  ([]
    (split (btc-rpc-fn "help" @config)))
  ([cmd]
    (split (btc-rpc-fn "help" @config cmd))))

(defn get-description [name]
  (loop [r (rest (split-help name))]
    (if (empty? (first r))
      (recur (rest r))
      (first r))))

(defn added-config [args] (conj args 'config))


(defmacro def-rpc [name & args]
  `(defn ~name ~(get-description name) [~@(added-config args)] (btc-rpc-fn ~(str name) ~'config ~@args))) 

(defmacro def-rpc-opt [name n & args]
  `(defn ~name ~(get-description name) 
     ~@(map (comp #(concat % `((btc-rpc-fn ~(str name) ~@(first %)))) list vec #(added-config (take % args))) (range n (+ 1 (count args))))
     ))

(defn btc-meta []
  (filter (fn [x] (:doc x)) (map (comp meta val) (ns-publics *ns*))))



;;----------------------------------------------------------------------------------------------

;;--------RPC calls-----------

;== Blockchain ==

(s/fdef getbestblockhash :ret string?)
(def-rpc getbestblockhash)

(s/fdef getblock :args (s/alt :verbose (s/cat :block-hash string?), :choice (s/cat :block-hash string?, :verbose boolean?)) :ret map?)
(def-rpc-opt getblock 1 block-hash verbose)

(s/fdef getblockchaininfo :ret map?)
(def-rpc getblockchaininfo)

(s/fdef getblockcount :ret ::height)
(def-rpc getblockcount)


(s/fdef getblockhash
  :args ::height      
  :ret ::hex-string)
(def-rpc getblockhash height)

(s/fdef getblockheader 
  :args (s/alt :verbose (s/cat :hash string?), :choice (s/cat :hash string?, :verbose boolean?))
  :ret map?)
(def-rpc-opt getblockheader 1 hash verbose)

(s/fdef getchaintips :ret vector?) 
(def-rpc getchaintips)

(s/fdef getdifficulty :ret integer?) 
(def-rpc getdifficulty)

(def-rpc-opt getmempoolancestors 1 txid verbose)
(def-rpc-opt getmempooldescendants 1 txid verbose)
(def-rpc getmempoolentry txid)
(def-rpc getmempoolinfo)
(def-rpc-opt getrawmempool 0 verbose)
(def-rpc-opt gettxout 2 txid n include_mempool)

(s/fdef gettxoutproof :args (s/alt :verbose (s/cat :txids vector?), :choice (s/cat :txids vector?, :blockhash boolean?)) :ret ::hex-string)
(def-rpc gettxoutproof txids blockhash)

(def-rpc gettxoutsetinfo)
(def-rpc preciousblock blockhash)
(def-rpc pruneblockchain)
(def-rpc-opt verifychain 0 checklevel nblocks)
(def-rpc verifytxoutproof proof)

;== Control ==
(def-rpc getinfo)
(def-rpc getmemoryinfo)
;help ( "command" )
(def-rpc stop)

;== Generating ==
(def-rpc-opt generate 1 nblocks maxtries)
(def-rpc-opt generatetoaddress 1 nblocks address maxtries)

;== Mining ==
(def-rpc-opt getblocktemplate 0 TemplateRequest )
(def-rpc getmininginfo)
(def-rpc-opt getnetworkhashps 0 nblocks height)
#_(def-rpc prioritisetransaction txid priority_delta fee_delta)
(def-rpc-opt submitblock 1 hexdata jsonparametersobject)

;== Network ==
(def-rpc addnode node action)
(def-rpc clearbanned)
(def-rpc disconnectnode address) 
(def-rpc-opt getaddednodeinfo 0 node)
(def-rpc getconnectioncount)
(def-rpc getnettotals)
(def-rpc getnetworkinfo)
(def-rpc getpeerinfo)
(def-rpc listbanned)
(def-rpc ping)

(s/def ::ban-action #(or (= % "add") (= % "remove")))
(s/fdef setban :args (s/alt :default (s/cat :subnet string?, :action ::ban-action) :optional (s/cat :subnet string?, :action ::ban-action, :bantime integer?, :absolute boolean?))) 
(def-rpc-opt setban 2 subnet ban-action bantime absolute)
(def-rpc setnetworkactive b)

;== Rawtransactions ==
(s/def ::txid-opt (s/and map? #(subset? #{"txid" "vout"} (into #{} (keys %))) #(string? (% "txid")) #(integer? (% "vout"))))
(s/def ::txid-opts (s/* ::txid-opt))
(s/def ::address-amount (s/and map? #(every? string? (keys %)) #(every? double? (vals %))))
(s/fdef createrawtransaction 
        :args (s/alt 
                :no-lock (s/cat :input ::txid-opts, :output ::address-amount)
                :lock (s/cat :input ::txid-opts, :output ::address-amount, :locktime integer?))
        :ret ::hex-string) 
#_(def-rpc-opt createrawtransaction 2 txids addresses locktime)

(def-rpc decoderawtransaction hexstring)
(def-rpc decodescript hexstring)
(def-rpc-opt fundrawtransaction 1 hexstring options)
(def-rpc-opt getrawtransaction 1 txid verbose)
(def-rpc-opt sendrawtransaction 1 hexstring allowhighfees)
(def-rpc-opt signrawtransaction 1 hexstring txids privatekeys sighashtype)

;== Util ==
(def-rpc createmultisig nrequired the-keys)
(def-rpc estimatefee nblocks)
;(def-rpc estimatepriority nblocks)
;(def-rpc estimatesmartfee nblocks)
;(def-rpc estimatesmartpriority nblocks)

(s/fdef signmessagewithprivkey :args (s/cat :privkey string?, :message string?) :ret ::signature)
(def-rpc signmessagewithprivkey privkey message)

(s/fdef validateaddress :args (s/cat :address ::address) :ret map?)
(def-rpc validateaddress address)

(def-rpc verifymessage address signature message)

;== Wallet ==
(def-rpc abandontransaction txid)
(def-rpc-opt addmultisigaddress 2 nrequired the-keys account)
(def-rpc addwitnessaddress address)
(def-rpc backupwallet destination)
(def-rpc-opt bumpfee 1 txid options) 
(def-rpc dumpprivkey address)
(def-rpc dumpwallet filename)
(def-rpc encryptwallet passphrase)
(def-rpc getaccount address)
(def-rpc getaccountaddress account)
(def-rpc getaddressesbyaccount account)
(def-rpc-opt getbalance 0 account minconf include_watchonly)
(def-rpc-opt getnewaddress 0 account)
(def-rpc getrawchangeaddress)
(def-rpc-opt getreceivedbyaccount 1 account minconf)
(def-rpc-opt getreceivedbyaddress 1 address minconf)
(def-rpc-opt gettransaction 1 txid include_watchonly)
(def-rpc getunconfirmedbalance)
(def-rpc getwalletinfo)
(def-rpc-opt importaddress 1 address label rescan p2sh)
(def-rpc importmulti requests options)
(def-rpc-opt importprivkey 1 bitcoinprivkey label rescan)
(def-rpc importprunedfunds)
(def-rpc-opt importpubkey 1 pubkey label rescan)
(def-rpc importwallet filename)
(def-rpc-opt keypoolrefill 0 newsize)
(def-rpc-opt listaccounts 0 minconf include_watchonly)
(def-rpc listaddressgroupings)
(def-rpc listlockunspent)
(def-rpc-opt listreceivedbyaccount 0 minconf include_empty include_watchonly)
(def-rpc-opt listreceivedbyaddress 0 minconf include_empty include_watchonly)
(def-rpc-opt listsinceblock 0 blockhash target_confirmations include_watchonly)
(def-rpc-opt listtransactions 0 account count skip include_watchonly)
#_(def-rpc-opt listunspent 0 minconf maxconf  addresses include_unsafe)
(def-rpc-opt lockunspent 0 unlock todo)
(def-rpc-opt move 3 fromaccount toaccount amount minconf comment)
(def-rpc removeprunedfunds txid)
(def-rpc-opt sendfrom 3 fromaccount toaddress amount  minconf comment comment_to)
#_(def-rpc-opt sendmany 2 fromaccount addess-map minconf comment addresses)

  

(def-rpc-opt sendtoaddress 2 address amount comment comment_to subtractfeefromamount)
(def-rpc setaccount address account)
(def-rpc settxfee amount)
(def-rpc signmessage address message)



;;--------------------------------


(defn get-block-hashes [] (map getblockhash (range (getblockcount))))

(defn block-of [block-hash] (getblock block-hash))

(defn get-all-tx [])
  

(defn print-it [a-list]
  (doseq [l a-list] (println l)))

(defmacro print-help 
  ([]
    (print-it (split-help)))
  ([cmd]
    (print-it (split-help cmd))))

(defn get-rpcs [] (map (fn [v] (symbol (first (.split v " ")))) (filter #(not (.startsWith % "==")) (split-help))))

(defn print-rpcs [] (doseq [s (get-rpcs)] (println s)))
  



;;---------------------------------------------------------------------------------
;experiment

(def help-parser
  (insta/parser
    "BTC = TITLE | FUNCTION | Epsilon
     TITLE = (<'=='> SPACE SYMBOL SPACE <'=='>)
     FUNCTION = SYMBOL | (SYMBOL SPACE ARGS)
     ARGS = ((ARG SPACE)* ARG) | Epsilon
     <ARG> = MAP-ARG | LIST-ARG | STRING-SYMBOL | SYMBOL  | OPTIONAL-ARG | ENUM
     ENUM = ((SYMBOL <'|'>)* SYMBOL) | Epsilon
     OPTIONAL-ARG = (<'('> OPTIONAL-SPACE (ARG | ARGS) OPTIONAL-SPACE <')'>)
     LIST-ARG = <'['> ARG <',...'> <']'> 
     MAP-ARG = (<'{'>) ((KEY-VALUE <','>)* KEY-VALUE) (<'}'>)
     KEY-VALUE = ARG <':'> ARG
     OPEN-KEY-VALUE = (KEY-VALUE <',...'>)
     STRING-SYMBOL = ('\"' ARG '\"')
     
     NEW-LINE = '\n' | Epsilon
     SYMBOL = #'[a-zA-Z_]'  #'\\w'*
     <SPACE> = <#'[ \t\n,]+'>
     <OPTIONAL-SPACE> = <#'[ \t\n]*'>
"))

(defn opt-arg-index-of [[i a]]
  (if (and (vector? a) (= (first a) :OPTIONAL-ARG))
    i
    nil))

(defn arg-index-of [args] 
  (reduce 
    (fn [acc v] 
      (if acc acc (opt-arg-index-of v))) nil args))

(defn function ([name arg-list] 
  (let [args (map-indexed (fn [i a] [i a]) arg-list)
        arg-ix (arg-index-of args)
        arg-names (flatten (map (comp #(if (vector? %) (second %) %) second) args))]
    (if arg-ix
      `(def-rpc-opt ~name ~arg-ix ~@arg-names)
      `(def-rpc ~name ~@arg-names))))
  ([name] (function name [])))


(def ast->clj-map 
  { 
   :FUNCTION function
   :SYMBOL (comp symbol str)
   :STRING-SYMBOL (fn [_ a _] a)
   :ARGS (fn [& args] args)
   :TITLE str
   :MAP-ARG (fn [& args] (symbol (str (reduce (fn [acc [_ k v]] (format "%s%s_%s" (if acc (str acc "-") "") k v)) nil args) "-map")))
   :LIST-ARG #(symbol (format "%s-list" %))
   :ENUM (fn [& args] (reduce (fn [acc v] (format "%s%s" (if acc (str acc "-") "") v)) nil args))
   :BTC (fn 
          ([a] a)
          ([] nil))
   })

(defn ast->clj [ast]
  (insta/transform
    ast->clj-map 
    ast))

(def not-working #{"prioritisetransaction" "createrawtransaction" "listunspent" "sendmany"})

(defn valid-fn [l] 
  (let [n (first (.split l " "))]
    (not (contains? not-working n))))

(defn parse [l]
  (let [r (help-parser l)]
    (if (insta/failure? r) 
      (throw (IllegalStateException. (pr-str (insta/get-failure r)))) 
      (ast->clj r))))

(defn def-rpcs [] 
  (let [e (conj 
            (map parse (filter valid-fn (split-help)))
            'do)]
    e))

;;--------------------------------------------------------

(defn repl-init []
  (sock-repl/repl-init)
  (require ['bitcoinrpc.core :as 'btc])
  (use 'clojure.pprint)
  (require ['clojure.repl :refer ['doc 'source]])
  )


(defn repl []
  (m/repl 
    :init repl-init
    :read sock-repl/repl-read
    :eval (fn [expr]
            (eval expr)))
  )


(defn port []
  (read-string (System/getProperty "mz.btc.port" "5566")))

(defn start-repl-server []
  (sock-repl/start-server {:port (port), :name "mz-btc-repl", :accept 'bitcoinrpc.core/repl}))




;;-------------------------java gen---------------------------

(defn normalize-name [s] (.replace (str s) "-" "_"))

(defn arg-def-of [args]
  (reduce (fn [acc v] (if (empty? acc) (format "final Object %s" v) (format "%s, final Object %s" acc v))) "" args))

(defn arg-vals-of [args]
  (reduce (fn [acc v] (if (empty? acc) (format "%s" v) (format "%s, %s" acc v))) "" args))

(defn java-fn-name-of [i m] (format "btc%s%s" (normalize-name (:name m)) (if (> i 0) i "")))

(defn java-method-of 
  ([m]
   (reduce (fn [acc v] (format "%s\n%s" acc v)) (flatten (map-indexed (fn [i v] (java-method-of i m (rest v))) (:arglists m)))))
  ([i m args]
    (let [n (java-fn-name-of i m)
          args (map normalize-name args)
          arg-vals (arg-vals-of args)]
      ["/**" 
      (str " *" (:doc m))
      "**/"
      (format "public Object %s(%s) {" n (arg-def-of args))
      (format "return btcFnOf(\"%s\"%s);" (:name m) (if (empty? arg-vals) "" (format ", %s" arg-vals)))
      "}"
      ])))


(defn java-executor-source-of [package classname base-classname]
  (reduce (fn [acc v] (format "%s\n%s" acc v)) [
  (format "package %s;" package)
  (format "public class %s extends %s {" classname base-classname)
  (format "public %s() throws Exception { super(); }" classname)
  (format "public %s(final String user, final String password, final String url) throws Exception {" classname)
  "   super(user, password, url);"
  "}"
  (reduce (fn [acc v] (format "%s\n%s" acc v)) (map java-method-of (btc-meta)))
  "}"
  ]
  ))

(defn java-signature-of [i m]
  (format "buildSignatureFromThreadSafeMethod(\"%s\")" (java-fn-name-of i m))
  )

(defn meta-with-index []
  (mapcat (fn [x] (map (fn [i] [i x]) (range (-> x :arglists count)))) (btc-meta)))

(defn java-plugin-source-of [package classname]
  (reduce (fn [acc v] (format "%s\n%s" acc v)) [
  (format "package %s;" package)
  "import com.digitalroute.devkit.apl.DRAPLPlugin;"
  "import com.digitalroute.devkit.apl.DRAPLFunctionSignature;"
  (format "public class %sPlugin extends DRAPLPlugin {" classname)
  "@Override"
  "public Class<?> getExecutorClass() {"
  (format "  return %sExecutor.class;" classname)
  "}"
  "@Override"
  "public DRAPLFunctionSignature[] getFunctions() {"
  "return new DRAPLFunctionSignature[] {"
  (reduce (fn [acc v] (format "%s,\n%s" acc v)) (map (fn [[i x]] (java-signature-of i x)) (meta-with-index)))
  
  "};"
  "}"
  "}"
   ]
  ))


(defn gen-java-executor-source! [dest package classname base-classname]
  (let [dir (File. dest)
        p-dir (File. dir (.replace package "." "/"))
        filename (File. (dbg p-dir) (format "%s.java" classname))]
  (spit (dbg filename)
        (java-executor-source-of package classname base-classname))))

(defn gen-java-plugin-source! [dest package classname]
  (let [dir (File. dest)
        p-dir (File. dir (.replace package "." "/"))
        filename (File. (dbg p-dir) (format "%sPlugin.java" classname))]
  (spit (dbg filename)
        (java-plugin-source-of package classname))))



(comment 
  (gen-java-plugin-source! 
  "/Users/anderse/src/mz-dev2/mz-main/mediationzone/packages/bitcoin/src/main/java" 
  "com.digitalroute.mz.bitcoin.agent" "BitcoinRpc" )
  (gen-java-executor-source! 
  "/Users/anderse/src/mz-dev2/mz-main/mediationzone/packages/bitcoin/src/main/java" 
  "com.digitalroute.mz.bitcoin.agent" "BitcoinRpcExecutor" "AbstractBitcoinRpc")
  )