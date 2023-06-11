package fr.networks.ugp.utils;

import fr.networks.ugp.readers.http.HTTPReader;
import fr.uge.ugegreed.Checker;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public class CheckerDownloader {
	private static final Logger logger = Logger.getLogger(Client.class.getName());

	private static class NonBlockingTCPClient {
		private final Selector selector;
		private final SocketChannel socketChannel;
		private final static int PORT = 80;
		private static final Charset ASCII_CHARSET = StandardCharsets.US_ASCII;
		private final ByteBuffer buffer = ByteBuffer.allocate(1_024);
		private ByteBuffer checkerBytes = null;
		private boolean isDownloaded;
		private HTTPReader reader = null;
		private String path = "";
		private String host = "";

		public NonBlockingTCPClient() throws IOException {
			selector = Selector.open();
			socketChannel = SocketChannel.open();
			socketChannel.configureBlocking(false);
			socketChannel.register(selector, SelectionKey.OP_CONNECT);
		}

		public void sendRequest() throws IOException {
			var request = "GET "+ path +" HTTP/1.1\r\nHost: " + host + "\r\n\r\n";
			socketChannel.write(ASCII_CHARSET.encode(request));
			socketChannel.register(selector, SelectionKey.OP_READ);

			reader = new HTTPReader(socketChannel, buffer);
		}

		private void getCheckerBytes() throws IOException {
			var header = reader.readHeader();
			ByteBuffer bodyBuffer;

			int contentLength = header.getContentLength();
			if (contentLength == -1) {
				bodyBuffer = reader.readChunks();
			} else {
				bodyBuffer = reader.readBytes(contentLength);
			}
			bodyBuffer.flip();
			checkerBytes = bodyBuffer;
			isDownloaded = true;
		}

		public void doConnect(SelectionKey key) throws IOException {
			if (!socketChannel.finishConnect()) {
				logger.warning("The selector give a bad hint");
				return; // selector gave a bad hint
			}
			key.interestOps(SelectionKey.OP_WRITE);
		}

		public ByteBuffer getResource(String host, String path) throws IOException {
			this.host = host;
			this.path = path;
			socketChannel.connect(new InetSocketAddress(this.host, PORT));
			while (!isDownloaded) {
				try {
					selector.select(this::treatKey);
				} catch (UncheckedIOException tunneled) {
					throw tunneled.getCause();
				}
			}

			return checkerBytes;
		}

		private void treatKey(SelectionKey key) {
			try {
				if (key.isValid() && key.isConnectable()) {
					doConnect(key);
				}
				if (key.isValid() && key.isWritable()) {
					sendRequest();
				}
				if (key.isValid() && key.isReadable()) {
					getCheckerBytes();
				}
			} catch (IOException ioe) {
				// lambda call in select requires to tunnel IOException
				close();
				throw new UncheckedIOException(ioe);
			}
		}

		public void close() {
			try {
				socketChannel.close();
				selector.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
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

		var protocol = url.getProtocol();

		if ("jar".equals(protocol)) {
			try {
				String jarUrl = url.toString();
				int jarIndex = jarUrl.indexOf(".jar!");

				if (jarIndex != -1) {
					String httpUrl = jarUrl.substring("jar:".length(), jarIndex + ".jar".length());
					String jarPath = jarUrl.substring(jarIndex + ".jar!".length());

					var client = new NonBlockingTCPClient();
					var bytes = client.getResource(new URL(httpUrl).getHost(), jarPath);
					client.close();

					return checkerFromJarBytes(bytes, className);
				}
			} catch (IOException e) {
				logger.info("Failed to retrieve JAR file from URL: " + url);
			}
		}

		return Optional.empty();
	}

	private static Optional<Checker> checkerFromJarBytes(ByteBuffer bytes, String className) {
		try {
			Path tempJarFile = Files.createTempFile("temp", ".jar");
			Files.write(tempJarFile, bytes.array(), StandardOpenOption.WRITE);

			try (JarFile jarFile = new JarFile(tempJarFile.toFile())) {
				JarEntry entry = jarFile.getJarEntry(className.replace('.', '/') + ".class");
				if (entry != null) {
					try (InputStream inputStream = jarFile.getInputStream(entry)) {
						byte[] classBytes = inputStream.readAllBytes();
						// Use a custom class loader to define the class
						ClassLoader classLoader = new ClassLoader() {
							@Override
							protected Class<?> findClass(String name) throws ClassNotFoundException {
								if (name.equals(className)) {
									return defineClass(name, classBytes, 0, classBytes.length);
								}
								return super.findClass(name);
							}
						};

						Class<?> checkerClass = classLoader.loadClass(className);
						Object instance = checkerClass.getDeclaredConstructor().newInstance();
						if (instance instanceof Checker) {
							return Optional.of((Checker) instance);
						}
					}
				}
			} catch (IOException e) {
				logger.info("Failed to read class from JAR: " + e.getMessage());
			} finally {
				Files.deleteIfExists(tempJarFile);
			}
		} catch (IOException e) {
			logger.info("Failed to create temporary JAR file: " + e.getMessage());
		} catch (ClassNotFoundException e) {
			logger.info("Failed to load class: " + className);
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			logger.info("Failed to create instance of class: " + className);
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
			logger.info("JarURL : " + jarURL);
			return retrieveCheckerFromURL(jarURL, className);
		} catch (MalformedURLException e) {
			logger.info("URL is malformed");
			return Optional.empty();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		var checker = checkerFromHTTP("http://www-igm.univ-mlv.fr/~carayol/Factorizer.jar", "fr.uge.factors.Factorizer").orElseThrow();
		System.out.println(checker.check(12L));
		/*checker = checkerFromDisk(Path.of("/Users/carayol/bb/progreseau/jars/Collatz.jar"), "fr.uge.collatz.Collatz").orElseThrow();
		System.out.println(checker.check(12L));*/
		checker = checkerFromHTTP("http://www-igm.univ-mlv.fr/~carayol/SlowChecker.jar", "fr.uge.slow.SlowChecker").orElseThrow();
		System.out.println(checker.check(12L));
	}
}
