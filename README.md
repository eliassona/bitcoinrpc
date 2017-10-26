# bitcoinrpc

A Clojure/Java library for bitcoin RPC.
The complete bitcoin API is extracted and generated into clojure and java from bitcoind-help data. It has only been tested with bitcoind v0.14.2 on MAC-OS.

## Usage


First install bitcoin core somewhere in your path i.e /usr/bin.
I added the following password to bitcoin.conf (on mac located in ~/Library/Application Support/Bitcoin) with the following:
```bash
rpcpassword=tjabba
```

Start bitcoind (using regtest mode here to be on the safe side)!

```bash
bitcoind -regtest -daemon
```

Note, it probably works without password, but that hasn't been tested.
The code below uses the atom config which contains a map with the default settings for user, password and url.


### Clojure
Add the following line to your leinigen dependencies:
```clojure
[bitcoinrpc "0.1.0-SNAPSHOT"]
```

```clojure
(use 'bitcoinrpc.core)
=> (getblockhash @config 0) ;get genesisblock
"0f9188f13cb7b2c71f2a335e3a4fc328bf5beb436012afca590b1a11466e2206"
=> (getinfo @config ) ;get some info about the bitcoin server
{:errors "", :protocolversion 70015, :difficulty 4.656542373906925E-10, :relayfee 1.0E-5, :keypoolsize 100, :keypoololdest 1504971951, :testnet false, :paytxfee 0.0, :balance 199.9998616, :proxy "", :timeoffset 0, :blocks 104, :connections 0, :walletversion 130000, :version 140200}
```
See the function names of the RPC API
```clojure
=> (print-rpcs)
```
```bash
getbestblockhash
getblock
getblockchaininfo
getblockcount
getblockhash
getblockheader
getchaintips
getdifficulty
getmempoolancestors
getmempooldescendants
getmempoolentry
getmempoolinfo
getrawmempool
gettxout
gettxoutproof
gettxoutsetinfo
preciousblock
pruneblockchain
verifychain
verifytxoutproof

getinfo
getmemoryinfo
help
stop

generate
generatetoaddress

getblocktemplate
getmininginfo
getnetworkhashps
prioritisetransaction
submitblock

addnode
clearbanned
disconnectnode
getaddednodeinfo
getconnectioncount
getnettotals
getnetworkinfo
getpeerinfo
listbanned
ping
setban
setnetworkactive

createrawtransaction
decoderawtransaction
decodescript
fundrawtransaction
getrawtransaction
sendrawtransaction
signrawtransaction

createmultisig
estimatefee
estimatepriority
estimatesmartfee
estimatesmartpriority
signmessagewithprivkey
validateaddress
verifymessage

abandontransaction
addmultisigaddress
addwitnessaddress
backupwallet
bumpfee
dumpprivkey
dumpwallet
encryptwallet
getaccount
getaccountaddress
getaddressesbyaccount
getbalance
getnewaddress
getrawchangeaddress
getreceivedbyaccount
getreceivedbyaddress
gettransaction
getunconfirmedbalance
getwalletinfo
importaddress
importmulti
importprivkey
importprunedfunds
importpubkey
importwallet
keypoolrefill
listaccounts
listaddressgroupings
listlockunspent
listreceivedbyaccount
listreceivedbyaddress
listsinceblock
listtransactions
listunspent
lockunspent
move
removeprunedfunds
sendfrom
sendmany
sendtoaddress
setaccount
settxfee
signmessage
```

To see details about a function in the API, for example 'listunspent'
```clojure
=> (doc listunspent)
```
```bash
-------------------------
bitcoinrpc.core/listunspent
([config] [config minconf] [config minconf maxconf] [config minconf maxconf addresses] [config minconf maxconf addresses include_unsafe])
  Returns array of unspent transaction outputs```
```
To see details about the function from bitcoin core.

```clojure
=> (print-help listunspent)
```
```bash
lot of text here......
```

Send 10 btc to a new address in the default wallet.

```clojure
=> (sendtoaddress @config (getnewaddress @config) 10.0)
"0f95510160151a03ec0c8122448357aa67c085a5da2acabaa60ee7f288a35443"
=> (generate @config 1) ;generate a block to confirm tx.
["09f72bc3819457594ba360cdb171503330eaf6415308a1c8f1e1eec6bedd20f4"]
```

Now you can view the newly create transaction with listunspent.


Perform a simple raw transaction.
```clojure
=> (def utxo (first (listunspent @config))) ;get an unspent tx
 
=> (def raw-tx (createrawtransaction @config [{"txid" (utxo "txid")
                                       "vout" (utxo "vout")}]
                                     {(getnewaddress @config) 49.9999})) ;create a raw transaction

=> (def signed-tx (signrawtransaction @config raw-tx)) ;sign the tx

=> (sendrawtransaction @config (signed-tx "hex")) ;send it to bitcoin core
"88d73d56e2527c042858cbd0ff37cb8daafa3a2302849353ac8f65b30d1d7a1a" 

=> (generate @config 1) ;generate a block to confirm tx.
["32d7b44dd17b3adcbf3d61798d763d0b5db240507fa4cae056612b3e27b16a08"]
```
Now you can view the newly created transaction with listunspent.

### Java

If you'd rather use java, there is a java class called BtcJava, it is AOT compiled into the jar. It is compiled using bitcoind v0.14.2. The java class is generated from the clojure API so the method names have the same name as the clojure function names. The methods have all one argument less though, the first argument. It is passed in the constructor of BtcJava instead. Currently all parameters and return types are of type Object. In a later version of this library it might be possible to extract the type info from bitcoind RPC and use in the generated java class.   
Example usage:

```java

import bitcoinrpc.BtcJava;

public class BtcJavaTest {
	public static void main(final String[] args) {
		final BtcJava btc = new BtcJava("", "tjabba", "http://localhost:18332");
		System.out.println(btc.getblockhash(0)); //get the genesis block
		System.out.println(btc.listunspent());   //list unspent transactions
	}
}

```




## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
