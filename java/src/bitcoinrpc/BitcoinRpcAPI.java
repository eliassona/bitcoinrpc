package bitcoinrpc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Keyword;

public class BitcoinRpcAPI {

	private final Map<Keyword, Object> config;
	private static final String BTC_CORE = "bitcoinrpc.core";
	private static final String CLOJURE_CORE = "clojure.core";

	public BitcoinRpcAPI(String user, String password, String url) {
		config = new HashMap<>();
		config.put(Keyword.intern(null, "user"), user);
		config.put(Keyword.intern(null, "password"), password);
		config.put(Keyword.intern(null, "url"), url);
		final IFn require = Clojure.var(CLOJURE_CORE, "require");
		require.invoke(Clojure.read(BTC_CORE));
	}
	
	public String getbestblockhash() {
		return (String) btcFnOf("getbestblockhash").invoke(config);
	}
	public Map<Object, Object> getblockchaininfo() {
		return (Map<Object, Object>) btcFnOf("getblockchaininfo").invoke(config);
	}
	public String getblockcount() {
		return (String) btcFnOf("getblockcount").invoke(config);
	}
	public String getblockhash(long height) {
		return (String) btcFnOf("getblockhash").invoke(config, height);
	}
	
	public Map<Object, Object> getblockheader(String hash) {
		return (Map<Object, Object>) btcFnOf("getblockhash").invoke(config, hash);
	}
	public Map<Object, Object> getblockheader(String hash, boolean verbose) {
		return (Map<Object, Object>) btcFnOf("getblockhash").invoke(config, hash, verbose);
	}
	public List<Object> getchaintips() {
		return (List<Object>) btcFnOf("getchaintips").invoke(config);
	}
	public double getdifficulty() {
		return (double) btcFnOf("getdifficulty").invoke(config);
	}
	
	public Object getmempoolancestors(String txid) {
		return btcFnOf("getmempoolancestors").invoke(config, txid);
	}
	public Object getmempoolancestors(String txid, boolean verbose) {
		return btcFnOf("getmempoolancestors").invoke(config, txid, verbose);
	}
	public Object getmempooldescendants(String txid) {
		return btcFnOf("getmempooldescendants").invoke(config, txid);
	}
	public Object getmempooldescendants(String txid, boolean verbose) {
		return btcFnOf("getmempooldescendants").invoke(config, txid, verbose);
	}
	public Object getmempoolentry(String txid) {
		return btcFnOf("getmempoolentry").invoke(config, txid);
	}
	public Object getmempoolinfo() {
		return btcFnOf("getmempoolinfo").invoke(config);
	}
	public Object getrawmempool() {
		return btcFnOf("getrawmempool").invoke(config);
	}
	public Object getrawmempool(boolean verbose) {
		return btcFnOf("getrawmempool").invoke(config, verbose);
	}
	public Object gettxout(String txid, int n) {
		return btcFnOf("getrawmempool").invoke(config, txid, n);
	}
	public Object gettxout(String txid, int n, boolean include_mempool) {
		return btcFnOf("getrawmempool").invoke(config, txid, n, include_mempool);
	}
	public Object gettxoutproof(String txid, int n) {
		return btcFnOf("gettxoutproof").invoke(config, txid, n);
	}
	
/*	
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
	(def-rpc prioritisetransaction txid priority_delta fee_delta)
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
	(def-rpc-opt createrawtransaction 2 txids addresses locktime)

	(def-rpc decoderawtransaction hexstring)
	(def-rpc decodescript hexstring)
	(def-rpc-opt fundrawtransaction 1 hexstring options)
	(def-rpc-opt getrawtransaction 1 txid verbose)
	(def-rpc-opt sendrawtransaction 1 hexstring allowhighfees)
	(def-rpc-opt signrawtransaction 1 hexstring txids privatekeys sighashtype)

	;== Util ==
	(def-rpc createmultisig nrequired the-keys)
	(def-rpc estimatefee nblocks)
	(def-rpc estimatepriority nblocks)
	(def-rpc estimatesmartfee nblocks)
	(def-rpc estimatesmartpriority nblocks)

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
	(def-rpc-opt listunspent 0 minconf maxconf  addresses include_unsafe)
	(def-rpc-opt lockunspent 0 unlock todo)
	(def-rpc-opt move 3 fromaccount toaccount amount minconf comment)
	(def-rpc removeprunedfunds txid)
	(def-rpc-opt sendfrom 3 fromaccount toaddress amount  minconf comment comment_to)
	(def-rpc-opt sendmany 2 fromaccount addess-map minconf comment addresses)

	  

	(def-rpc-opt sendtoaddress 2 address amount comment comment_to subtractfeefromamount)
	(def-rpc setaccount address account)
	(def-rpc settxfee amount)
	(def-rpc signmessage address message)
*/	
	public static IFn btcFnOf(String name) {
		return Clojure.var(BTC_CORE, name);
	}
	
	@Override
	public String toString() {
		return config.toString();
	}
	
	
	public static void main(String[] args) {
		final BitcoinRpcAPI btc = new BitcoinRpcAPI("", "tjabba", "http://localhost:18332");
		System.out.println(btc.getdifficulty());
	}

}
