package ch.admin.bj.swiyu.trust.management.modules.common.path;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.api.Test;

class PathSupportTest {

    @Test
    void join_BaseUriWithoutTrailingSlash_SegmentWithoutLeadingSlash() {
        assertThat(PathSupport.join(URI.create("https://example.com/base"), "path")).isEqualTo(
            URI.create("https://example.com/base/path")
        );
    }

    @Test
    void join_BaseUriWithTrailingSlash_SegmentWithoutLeadingSlash() {
        assertThat(PathSupport.join(URI.create("https://example.com/base/"), "path")).isEqualTo(
            URI.create("https://example.com/base/path")
        );
    }

    @Test
    void join_BaseUriWithoutTrailingSlash_SegmentWithLeadingSlash() {
        assertThat(PathSupport.join(URI.create("https://example.com/base"), "/path")).isEqualTo(
            URI.create("https://example.com/base/path")
        );
    }

    @Test
    void join_BaseUriWithTrailingSlash_SegmentWithLeadingSlash() {
        assertThat(PathSupport.join(URI.create("https://example.com/base/"), "/path")).isEqualTo(
            URI.create("https://example.com/base/path")
        );
    }

    @Test
    void join_BaseUriIsRoot_SegmentWithLeadingSlash() {
        assertThat(PathSupport.join(URI.create("https://example.com/"), "/path")).isEqualTo(
            URI.create("https://example.com/path")
        );
    }

    @Test
    void join_BaseUriIsRoot_SegmentWithoutLeadingSlash() {
        assertThat(PathSupport.join(URI.create("https://example.com/"), "path")).isEqualTo(
            URI.create("https://example.com/path")
        );
    }
}
