package fr.networks.ugp.utils;

import fr.uge.ugegreed.Checker;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public class CheckerDownloader {
	private static final Logger logger = Logger.getLogger(Client.class.getName());

	private static class NonBlockingTCPClient {
		private final Selector selector;
		private final SocketChannel socketChannel;

		public NonBlockingTCPClient(String host, int port) throws IOException {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.connect(new InetSocketAddress(host, port));
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
		}

		public void sendRequest(String request) throws IOException {
			ByteBuffer buffer = ByteBuffer.wrap(request.getBytes(StandardCharsets.UTF_8));
			socketChannel.write(buffer);
			socketChannel.register(selector, SelectionKey.OP_READ);
		}

		public String receiveResponse() throws IOException {
			StringBuilder responseBuilder = new StringBuilder();
			ByteBuffer buffer = ByteBuffer.allocate(1024);

			while (true) {
				int numBytes = socketChannel.read(buffer);
				if (numBytes == -1) {
					break;
				}

				buffer.flip();
				responseBuilder.append(StandardCharsets.UTF_8.decode(buffer));
				buffer.clear();
			}

			return responseBuilder.toString();
		}

		public void close() throws IOException {
			socketChannel.close();
			selector.close();
		}
	}

	/**
	 * This method downloads the jar file from the given url
	 * and creates an instance of the class assuming it implements the
	 * fr.uge.ugegreed.Checker interface.
	 * <p>
	 * This method can both be used to retrieve the class from a local jar file
	 * or from a jar file provided by an HTTP server. The behavior depends
	 * on the url parameter.
	 *
	 * @param url        the url of the jar file
	 * @param className  the fully qualified name of the class to load
	 * @return an instance of the class if it exists
	 */
	public static Optional<Checker> retrieveCheckerFromURL(URL url, String className) {
		Objects.requireNonNull(url);
		Objects.requireNonNull(className);

		if ("file".equals(url.getProtocol())) {
			// Local file, use checkerFromDisk method
			try {
				var jarPath = Path.of(url.toURI());
				return checkerFromDisk(jarPath, className);
			} catch (URISyntaxException e) {
				logger.info("Invalid file URI");
				return Optional.empty();
			}
		} else if ("http".equals(url.getProtocol())) {
			// HTTP URL, use checkerFromHTTP method
			try {
				String host = url.getHost();
				int port = url.getPort();
				String path = url.getPath();

				NonBlockingTCPClient client = new NonBlockingTCPClient(host, port);

				String request = String.format("GET %s HTTP/1.1\r\nHost: %s\r\n\r\n", path, host);
				client.sendRequest(request);

				String response = client.receiveResponse();
				client.close();

				int emptyLineIndex = response.indexOf("\r\n\r\n");
				if (emptyLineIndex >= 0) {
					byte[] bytecode = response.substring(emptyLineIndex + 4).getBytes(StandardCharsets.ISO_8859_1);

					// Assuming the class bytecode has been received successfully

				} else {
					logger.info("Failed to retrieve jar file. Invalid HTTP response");
				}
			} catch (IOException e) {
				logger.info("Failed to retrieve jar file from URL: %s".formatted(url));
			}
		}

		return Optional.empty();
	}

	public static Optional<Checker> checkerFromDisk(Path jarPath, String className) {
		try {
			var url = jarPath.toUri().toURL();
			return retrieveCheckerFromURL(url, className);
		} catch (MalformedURLException e) {
			logger.info("URL is malformed");
			return Optional.empty();
		}
	}

	public static Optional<Checker> checkerFromHTTP(String url, String className) {
		try {
			var jarURL = new URL("jar", "", url + "!/");
			return retrieveCheckerFromURL(jarURL, className);
		} catch (MalformedURLException e) {
			logger.info("URL is malformed");
			return Optional.empty();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		var checker = checkerFromHTTP("http://www-igm.univ-mlv.fr/~carayol/Factorizer.jar", "fr.uge.factors.Factorizer").orElseThrow();
		System.out.println(checker.check(12L));
		checker = checkerFromDisk(Path.of("/Users/carayol/bb/progreseau/jars/Collatz.jar"), "fr.uge.collatz.Collatz").orElseThrow();
		System.out.println(checker.check(12L));
		checker = checkerFromHTTP("http://www-igm.univ-mlv.fr/~carayol/SlowChecker.jar", "fr.uge.slow.SlowChecker").orElseThrow();
		System.out.println(checker.check(12L));
	}
}
