(ns bitcoinrpc.btcjava
  (:require [bitcoinrpc.core :refer :all]))

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

(def-java-api)
