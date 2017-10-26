package bitcoinrpc;

public class BtcFactory {
	public static void main(String[] args) {
		System.out.println(new BtcJava("", "tjabba", "http://localhost:18332").getbalance());
	}
}
