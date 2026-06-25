package ch.admin.bj.swiyu.trust.management.modules.common.path;

import java.net.URI;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PathSupport {

    /**
     * Utility method to join pathes without taking care of leading or trailing slashes.
     */
    public static URI join(URI uri, String segment) {
        var baseUri = uri.toString();
        if (baseUri.endsWith("/")) {
            baseUri = baseUri.substring(0, baseUri.length() - 1); // Remove the last trailing slash
        }
        var normalizedSegment = segment.startsWith("/") ? segment.substring(1) : segment; // Remove leading slash from segment
        return URI.create(baseUri + "/" + normalizedSegment); // Concatenate with a single slash
    }
}
