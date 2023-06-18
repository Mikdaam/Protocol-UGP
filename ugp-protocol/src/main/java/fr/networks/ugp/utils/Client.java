package fr.networks.ugp.utils;


import fr.uge.ugegreed.Checker;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;


public class Client {
    private static final Logger logger = Logger.getLogger(Client.class.getName());


    /**
     * This method downloads the jar file from the given url
     * and creates an instance of the class assuming it implements the
     * fr.uge.ugegreed.Checker interface.
     *
     * This method can both be used retrieve the class from a local jar file
     * or from a jar file provided by an HTTP server. The behavior depends
     * on the url parameter.
     * @param url the url of the jar file
     * @param className the fully qualified name of the class to load
     * @return an instance of the class if it exists
     */
    public static Optional<Checker> retrieveCheckerFromURL(URL url, String className) {
        Objects.requireNonNull(url);
        Objects.requireNonNull(className);
        var urls = new URL[]{url};
        var urlClassLoader = new URLClassLoader(urls,Thread.currentThread().getContextClassLoader());
        try {
            var clazz = Class.forName(className, true, urlClassLoader);
            var constructor = clazz.getDeclaredConstructor();
            var instance = constructor.newInstance();
            return Optional.of((Checker) instance);
        } catch (ClassNotFoundException e) {
            logger.info("The class %s was not found in %s. The jarfile might not be present at the given URL.".formatted(className, url));
            return Optional.empty();
        } catch (NoSuchMethodException e) {
            logger.info("Class %s in jar %s cannot be cast to fr.uge.ugegreed.Checker".formatted(className, url));
            return Optional.empty();
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            logger.info("Failed to create an instance of %s".formatted(className));
            return Optional.empty();
        }
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
        var checker = checkerFromHTTP("http://www-igm.univ-mlv.fr/~carayol/Factorizer.jar","fr.uge.factors.Factorizer").orElseThrow();
        System.out.println(checker.check(12L));
        /*checker = checkerFromDisk(Path.of("/Users/carayol/bb/progreseau/jars/Collatz.jar"),"fr.uge.collatz.Collatz").orElseThrow();
        System.out.println(checker.check(12L));*/
        checker = checkerFromHTTP("http://www-igm.univ-mlv.fr/~carayol/SlowChecker.jar","fr.uge.slow.SlowChecker").orElseThrow();
        System.out.println(checker.check(12L));
    }
}
