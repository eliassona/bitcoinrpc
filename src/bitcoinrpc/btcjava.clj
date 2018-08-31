(ns bitcoinrpc.btcjava
  "Bitcoin RPC Java API"
  (:use [clojure.pprint])
  (:require [bitcoinrpc.core :refer :all]))

(def kw->java-type
  {:long 'long
   :string 'String
   :list 'java.util.List
   :map 'java.util.Map
   :boolean 'boolean
   :object 'Object
   :double 'double})


(defn java-arg-type-of [arg]
  (-> arg arg->type kw->java-type))
(defn java-return-type-of [arg]
  (-> arg fn->type kw->java-type))

(defn method-decl-of [fn-map]
  (let [n (:name fn-map)]
    (mapv (fn [x] [n (mapv java-arg-type-of (rest x)) (java-return-type-of n)]) (:arglists fn-map))
    ))
(defn method-name-of [n]
  (symbol (str "-" n)))

(defn method-overload-of [name args]
  `(~(-> args rest (conj 'this) vec) (~name (.config ~'this) ~@(rest args))))

(defn method-impl-of [fn-map]
  (let [n (:name fn-map)]
    `(defn ~(method-name-of n) ~@(map (partial method-overload-of n) (:arglists fn-map)))))

(defn get-meta []
  (btc-meta))

(defmacro def-java-api []
  `(do 
     (gen-class
       :name bitcoinrpc.BtcJava
       :main true
       :state ~'config
       :methods ~(vec (mapcat method-decl-of (get-meta)))
       :init ~'init
       :constructors {[String String String] []})
     ~@(map method-impl-of (get-meta))
     (defn ~'-init [user# password# url#]
       [[] {:user user#, :password password#, :url url#}])
     ))

;(def-java-api)
