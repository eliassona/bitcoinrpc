package bitcoinrpc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

public class BtcMain {
	private static final String BITCOINRPC_CORE = "bitcoinrpc.core";
	private static final String CLOJURE_CORE = "clojure.core";

	public static void main(final String[] args) throws IOException {
		final IFn require = Clojure.var(CLOJURE_CORE, "require");
		require.invoke(Clojure.read(BITCOINRPC_CORE));
	
		final IFn startReplServer = Clojure.var(BITCOINRPC_CORE, "start-repl-server");
		System.out.println("start repl...");
		System.out.println(startReplServer.invoke());
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			if ("exit".equals(in.readLine())) {
				return;
			}
		}
	}
}
