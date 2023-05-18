package fr.networks.ugp.utils;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.StringJoiner;

public class Helpers {
	/***
	 * Theses methods are here to help understanding the behavior of the selector
	 ***/

	private static String interestOpsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		int interestOps = key.interestOps();
		var joiner = new StringJoiner("|");
		if ((interestOps & SelectionKey.OP_ACCEPT) != 0)
			joiner.add("OP_ACCEPT");
		if ((interestOps & SelectionKey.OP_READ) != 0)
			joiner.add("OP_READ");
		if ((interestOps & SelectionKey.OP_WRITE) != 0)
			joiner.add("OP_WRITE");
		return joiner.toString();
	}

	public static void printKeys(Selector selector) {
		var selectionKeySet = selector.keys();
		if (selectionKeySet.isEmpty()) {
			System.out.println("The selector contains no key : this should not happen!");
			return;
		}
		System.out.println("The selector contains:");
		for (var key : selectionKeySet) {
			var channel = key.channel();
			if (channel instanceof ServerSocketChannel) {
				System.out.println("\tKey for ServerSocketChannel : " + interestOpsToString(key));
			} else {
				var sc = (SocketChannel) channel;
				System.out.println("\tKey for Client " + remoteAddressToString(sc) + " : " + interestOpsToString(key));
			}
		}
	}

	private static String remoteAddressToString(SocketChannel sc) {
		try {
			return sc.getRemoteAddress().toString();
		} catch (IOException e) {
			return "???";
		}
	}

	public static void printSelectedKey(SelectionKey key) {
		var channel = key.channel();
		if (channel instanceof ServerSocketChannel) {
			System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
		} else {
			var sc = (SocketChannel) channel;
			System.out.println(
					"\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
		}
	}

	private static String possibleActionsToString(SelectionKey key) {
		if (!key.isValid()) {
			return "CANCELLED";
		}
		var joiner = new StringJoiner(" and ");
		if (key.isAcceptable())
			joiner.add("ACCEPT");
		if (key.isReadable())
			joiner.add("READ");
		if (key.isWritable())
			joiner.add("WRITE");
		return joiner.toString();
	}
}