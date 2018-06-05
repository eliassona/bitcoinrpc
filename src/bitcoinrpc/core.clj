(ns bitcoinrpc.core
  "Bitcoin RPC API"
  (:use [clojure.pprint]
        [clojure.repl])
  
  (:require [clojure.data.json :as json]
            [instaparse.core :as insta]
            [clj-http.client :as client]
            [clojure.repl :refer [source source-fn dir doc]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.set :refer [subset?]]
            [clojure.core.server :as sock-repl]
            [clojure.main :as m]
            [clojure.set :as set]
            ;[com.gfredericks.debug-repl :refer [break! unbreak!]]
            ))

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

(defn sys-prop-of [name default-value]
  (System/getProperty name default-value))

(def config 
  (atom {:user (sys-prop-of "bitcoinrpc.user" ""), 
         :password (sys-prop-of "bitcoinrpc.password"), 
         :url (sys-prop-of "bitcoinrpc.url" "http://localhost:8332")}))

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


(defmacro def-rpc [name & args]
  `(defn ~name ~(get-description name) [~@args] (btc-rpc-fn ~(str name) (deref ~'config) ~@args))) 

(defmacro def-rpc-opt [name n & args]
  `(defn ~name ~(get-description name) 
     ~@(map (comp #(concat % `((btc-rpc-fn ~(str name) (deref ~'config) ~@(first %)))) list vec #(take % args)) (range n (+ 1 (count args))))
     ))

(defn btc-meta []
  (filter (fn [x] (:doc x)) (map (comp meta val) (ns-publics 'bitcoinrpc.core))))

(defn argname-set 
 []
 (into #{} (reduce concat (map (comp last :arglists) (btc-meta)))))

(defn arg->fns [arg]
 (filter 
   identity 
   (map
     (fn [f] 
       (let [args (into #{} (-> f :arglists last))]
         (when (contains? args arg)
           (:name f))))
     (btc-meta))))
  

(def arg->type
  {
   'minconf :long
   'maxconf :long
   'addresses :list
   'include_unsafe :boolean
   'verbose :boolean
   'address :string
   'account :string
   'fromaccount :string
   'toaddress :string
   'comment :string
   'comment_to :string
   'amount :string
   'include_empty :boolean
   'include_watchonly :boolean
   'blockhash :string
   'target_confirmations :long
   'count :long
   'skip :long
   'pubkey :string
   'label :string
   'rescan :boolean
   'filename :string
   'newsize :long
   'passphrase :string
   'message :string
   'txid :string
   'options :map
   'nrequired :long
   'keys :list
   'destination :string
   'script :string
   'p2sh :boolean
   'privkey :string
   'height :long
   'subtractfeefromamount :boolean
   'toaccount :string
   'nblocks :long
   'priority-delta :long
   'fee-delta :long
   'maxtries :long
   'requests :list
   'addess-map :object
   'subtractfeefrom :list
   'txids :list
   'hash :string
   'bitcoinprivkey :string
   'proof :string
   'checklevel :long
   'key-list :list
   'hexstring :string
   'signature :string
   'sighashtype :string
   'locktime :long
   'include_mempool :boolean
   'allowhighfees :boolean
   'TemplateRequest :object
   'add-remove :string
   'subnet :string
   'command :string
   'absolute :boolean
   'add-remove-onetry :string
   'config :object
   'txid-list :list
   'privatekey1-list :list
   'bantime :long
   'node :string
   'hexdata :string
   'unlock :boolean
   'txid_id-vout_n-scriptPubKey_hex-redeemScript_hex-map-list :list
   'jsonparametersobject :object
   'txid_txid-vout_n-map-list :list
   'n :long
   'true-false :boolean
   'iswitness :boolean
   'exclude :boolean
   'include_removed :boolean 
   'replaceable :boolean
   'oldpassphrase :string
   'newpassphrase :string
   'verbosity :long
   'conf_target :long
   'timeout :long
   'start_height :long
   'stop_height :long
   'estimate_mode :string
   'include :list
   'hexstring-list :list ;what is this?
   'address_type :string
   'mode :string
   'dummy :object
   'dummy-value :object
   })



(def fn->type 
  {'abandontransaction :object
   'addmultisigaddress :string
   'addnode :object
   'addwitnessaddress :string
   'backupwallet :object
   'bumpfee :map
   'clearbanned :object
   'createmultisig :map
   'createrawtransaction :string
   'decoderawtransaction :map
   'decodescript :map
   'disconnectnode :object
   'dumpprivkey :string
   'dumpwallet :object
   'encryptwallet :object
   'estimatefee :long
   'estimatepriority :long
   'estimatesmartfee :map
   'estimatesmartpriority :map
   'fundrawtransaction :map
   'generate :list
   'generatetoaddress :list
   'getaccount :string
   'getaccountaddress :string
   'getaddednodeinfo :list
   'getaddressesbyaccount :list
   'getbalance :double
   'getbestblockhash :string
   'getblock :object
   'getblockchaininfo :map
   'getblockcount :long
   'getblockhash :string
   'getblockheader :object
   'getblocktemplate :map
   'getchaintips :list
   'getconnectioncount :long
   'getdifficulty :double
   'getinfo :map
   'getmemoryinfo :map
   'getmempoolancestors :object
   'getmempooldescendants :object
   'getmempoolentry :map
   'getmempoolinfo :map
   'getmininginfo :map
   'getnettotals :map
   'getnetworkhashps :double
   'getnetworkinfo :map
   'getnewaddress :string
   'getpeerinfo :list
   'getrawchangeaddress :string
   'getrawmempool :object
   'getrawtransaction :object
   'getreceivedbyaccount :double
   'getreceivedbyaddress :double
   'gettransaction :map
   'gettxout :map
   'gettxoutproof :string
   'gettxoutsetinfo :map
   'getunconfirmedbalance :double
   'getwalletinfo :map
   'help :string
   'importaddress :object
   'importmulti :list
   'importprivkey :object
   'importprunedfunds :object
   'importpubkey :object
   'importwallet :object
   'keypoolrefill :object
   'listaccounts :map
   'listaddressgroupings :list
   'listbanned :list
   'listlockunspent :list
   'listreceivedbyaccount :list
   'listreceivedbyaddress :list
   'listsinceblock :map
   'listtransactions :list
   'listunspent :list
   'lockunspent :boolean
   'move :boolean
   'ping :object
   'preciousblock :object
   'prioritisetransaction :boolean
   'pruneblockchain :long
   'removeprunedfunds :object
   'sendfrom :string
   'sendmany :string
   'sendrawtransaction :string
   'sendtoaddress :string
   'setaccount :object
   'setban :object
   'setnetworkactive :object
   'settxfee :boolean
   'signmessage :string
   'signmessagewithprivkey :string
   'signrawtransaction :map
   'stop :object
   'submitblock :object
   'validateaddress :map
   'verifychain :boolean
   'verifymessage :boolean
   'verifytxoutproof :list
   })

(defn missing-arg-types [] (set/difference (argname-set) (into #{} (keys arg->type))))

(defn assert-missing-types [the-set]
 (when (not (empty? the-set))
   (throw (IllegalStateException. (format "Missing types for %s arguments: %s" (count the-set) (pr-str the-set))))))

(assert-missing-types (missing-arg-types))




;;--------------------------------

(defn print-it [a-list]
  (doseq [l a-list] (println l)))

(defmacro print-help 
  ([]
    (print-it (split-help)))
  ([cmd]
    (print-it (split-help cmd))))

(defn get-rpcs [] (map (fn [v] (symbol (first (.split v " ")))) (filter #(not (.startsWith % "==")) (split-help))))

(defn print-rpcs [] (doseq [s (get-rpcs)] (println s)))

(defn retname-set [] (into #{} (get-rpcs)))
(defn missing-return-types [] (set/difference (retname-set) (into #{} (keys fn->type))))
;(assert-missing-types (missing-return-types))


;;---------------------------------------------------------------------------------

(def rpc-parser
  (insta/parser
    "BTC = TITLE | FUNCTION | Epsilon
     TITLE = (<'=='> SPACE SYMBOL SPACE <'=='>)
     FUNCTION = SYMBOL | (SYMBOL SPACE ARGS)
     ARGS = ((ARG SPACE)* ARG OPTIONAL-SPACE) | Epsilon
     <ARG> = MAP-ARG | LIST-ARG | STRING-SYMBOL | SYMBOL  | OPTIONAL-ARG | ENUM | ALT-LIST-ARG
     ENUM = ((SYMBOL <'|'>)+ SYMBOL)
     OPTIONAL-ARG = (<'('> OPTIONAL-SPACE (ARG | ARGS) OPTIONAL-SPACE <')'>)
     LIST-ARG = <'['> ARG <',...'> <']'> 
     ALT-LIST-ARG = <'<'> (SYMBOL SPACE)* SYMBOL <'>'> 
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
   :ALT-LIST-ARG (fn [& args] (symbol (reduce (fn [acc v] (format "%s%s" (if acc (str acc "-") "") v)) nil args)))
   :ENUM (fn [& args] (symbol (reduce (fn [acc v] (format "%s%s" (if acc (str acc "-") "") v)) nil args)))
   :BTC (fn 
          ([a] a)
          ([] nil))
   })

(defn ast->clj [ast]
  (insta/transform
    ast->clj-map 
    ast))


;;the parser can't handle these RPC function yet.
(def not-working #{"createrawtransaction" "listunspent" "sendmany" "disconnectnode"})
;define them hardcoded
(def-rpc-opt createrawtransaction 2 txids addresses locktime)
(def-rpc-opt listunspent 0 minconf maxconf  addresses include_unsafe)
(def-rpc-opt sendmany 2 fromaccount addess-map minconf comment addresses)

(defn valid-fn [l] 
  (let [n (first (.split l " "))]
    (not (contains? not-working n))))

(defn parse [l]
  (let [r (rpc-parser l)]
    (if (insta/failure? r) 
      (throw (IllegalStateException. (pr-str (insta/get-failure r)))) 
      (ast->clj r))))

(defn def-rpcs []
  (eval 
    (conj 
      (map parse (filter valid-fn (split-help)))
      'do)))


(def-rpcs)





  

;;---------------socket repl-------------------------------

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




