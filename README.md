# bitcoinrpc

A Clojure library for bitcoin RPC

## Usage


First install bitcoin core somewhere in your path i.e /usr/bin.
If you want to use bitcoinrpc with default settings add/edit the file bitcoin.conf (on mac located in ~/Library/Application Support/Bitcoin) with the following:
```bash
rpcpassword=tjabba
```

Start bitcoind (using regtest mode here to be on the safe side)!

```bash
bitcoind -regtest -daemon
```

Clojure usage:
```clojure
(use 'bitcoinrpc.core)
=> (getblockhash 0) ;get genesisblock
"0f9188f13cb7b2c71f2a335e3a4fc328bf5beb436012afca590b1a11466e2206"
=> (getinfo) ;get some info about the bitcoin server
{:errors "", :protocolversion 70015, :difficulty 4.656542373906925E-10, :relayfee 1.0E-5, :keypoolsize 100, :keypoololdest 1504971951, :testnet false, :paytxfee 0.0, :balance 199.9998616, :proxy "", :timeoffset 0, :blocks 104, :connections 0, :walletversion 130000, :version 140200}
```

## License

Copyright © 2017 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
