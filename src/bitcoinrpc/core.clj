(ns bitcoinrpc.core
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
            [com.gfredericks.debug-repl :refer [break! unbreak!]]
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
         :password (sys-prop-of "bitcoinrpc.password" "tjabba"), 
         :url (sys-prop-of "bitcoinrpc.url" "http://localhost:18332")}))

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
(def not-working #{"createrawtransaction" "listunspent" "sendmany"})
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



;; java interface
;;TODO



(defn method-decl-of [fn-map]
  (let [n (:name fn-map)]
    (mapv (fn [x] [n (mapv (fn [_] 'Object) (rest x)) 'Object]) (:arglists fn-map))
    ))
(defn method-name-of [n]
  (symbol (str "-" n)))

(defn method-overload-of [name args]
  `(~(-> args rest (conj 'this) vec) (~name (.config ~'this) ~@(rest args))))

(defn method-impl-of [fn-map]
  (let [n (:name fn-map)]
    `(defn ~(method-name-of n) ~@(map (partial method-overload-of n) (:arglists fn-map)))))

(defn get-meta []
  (btc-meta)
  #_(-> (btc-meta) (nth 2) list))

(defmacro def-java-api []
  `(do 
     (gen-class
       :name bitcoinrpc.BtcJava
       :state ~'config
       :methods ~(vec (mapcat method-decl-of (get-meta)))
       :init ~'init
       :constructors {[java.util.Map] []})
          
     ~@(map method-impl-of (get-meta))
     (defn ~'-init [config#]
       [[] config#])))
(def-java-api)


  

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


#_(gen-class
  :name bitcoinrpc.BtcJava
  :prefix "-"
  :methods [[foo [] String]
            [foo [String] String]]
  )

#_(defn -foo 
   ([this]
   (str (class this)))
   ([this a]
   (str (class this))))


(compile 'bitcoinrpc.core)
