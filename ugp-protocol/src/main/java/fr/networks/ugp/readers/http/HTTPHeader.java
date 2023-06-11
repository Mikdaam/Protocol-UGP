package fr.networks.ugp.readers.http;

import java.nio.charset.Charset;
import java.util.*;

import static fr.networks.ugp.readers.http.HTTPException.ensure;

public class HTTPHeader {

    /**
     * Supported versions of the HTTP Protocol
     */

    private static final String[] LIST_SUPPORTED_VERSIONS = new String[] { "HTTP/1.0", "HTTP/1.1", "HTTP/2.0" };
    public static final Set<String> SUPPORTED_VERSIONS = Set.of(LIST_SUPPORTED_VERSIONS);

    private final String response;
    private final String version;
    private final int code;
    private final Map<String, String> fields;

    private HTTPHeader(String response, String version, int code, Map<String, String> fields) throws HTTPException {
        this.response = response;
        this.version = version;
        this.code = code;
        this.fields = Collections.unmodifiableMap(fields);
    }

    public static HTTPHeader create(String response, Map<String, String> fields) throws HTTPException {
        String[] tokens = response.split(" ");
        // Treatment of the response line
        ensure(tokens.length >= 2, "Badly formed response:\n" + response);
        var version = tokens[0];
        ensure(HTTPHeader.SUPPORTED_VERSIONS.contains(version), "Unsupported version in response:\n" + response);
        var code = 0;
        try {
            code = Integer.parseInt(tokens[1]);
            ensure(code >= 100 && code < 600, "Invalid code in response:\n" + response);
        } catch (NumberFormatException e) {
            ensure(false, "Invalid response:\n" + response);
        }
        var fieldsCopied = new HashMap<String, String>();
        for (var s : fields.keySet()) {
            fieldsCopied.put(s.toLowerCase(), fields.get(s).trim());
        }
        return new HTTPHeader(response, version, code, fieldsCopied);
    }

    public String getResponse() {
        return response;
    }

    public String getVersion() {
        return version;
    }

    public int getCode() {
        return code;
    }

    public Map<String, String> getFields() {
        return fields;
    }

    /**
     * @return the value of the Content-Length field in the header -1 if the field
     *         does not exists
     * @throws HTTPException when the value of Content-Length is not a number
     */
    public int getContentLength() throws HTTPException {
        var contentString = fields.get("content-length");
        if (contentString == null) {
            return -1;
        }
        try {
            return Integer.valueOf(contentString.trim());
        } catch (NumberFormatException e) {
            throw new HTTPException("Invalid Content-Length field value :\n" + contentString);
        }
    }

    /**
     * @return the Content-Type null if there is no Content-Type field
     */
    public Optional<String> getContentType() {
        var contentString = fields.get("content-type");
        if (contentString == null) {
            return Optional.empty();
        }
        return Optional.of(contentString.split(";")[0].trim());
    }

    /**
     * @return the charset corresponding to the Content-Type field or empty if
     *         charset is unknown or unavailable on the JVM
     */
    public Optional<Charset> getCharset() {
        Charset cs = null;
        var contentString = fields.get("content-type");
        if (contentString == null) {
            return Optional.empty();
        }
        for (var token : contentString.split(";")) {
            if (token.contains("charset=")) {
                try {
                    cs = Charset.forName(token.split("=")[1].trim());
                } catch (Exception e) {
                    // If the Charset is unknown or unavailable, the method return empty
                }
                return Optional.of(cs);
            }
        }
        return Optional.empty();
    }

    /**
     * @return true if the header correspond to a chunked response
     */
    public boolean isChunkedTransfer() {
        return fields.containsKey("transfer-encoding") && fields.get("transfer-encoding").trim().equals("chunked");
    }

    public String toString() {
        return response + "\n" + version + " " + code + "\n" + fields.toString();
    }
}