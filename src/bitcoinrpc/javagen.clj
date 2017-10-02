(ns bitcoinrpc.javagen
  (:require [bitcoinrpc.core :refer [btc-meta dbg]])
  (:import [java.io File]))


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




