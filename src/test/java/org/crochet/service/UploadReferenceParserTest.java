package org.crochet.service;

import org.crochet.exception.BadRequestException;
import org.crochet.util.UploadReferenceParser;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.crochet.service.UploadTestSupport.*;

class UploadReferenceParserTest {
    @Test
    void matchesOnlyConfiguredCdnBoundaryAndDecodesEscapes() {
        var parser = new UploadReferenceParser(properties());
        assertThat(parser.managedKey("//cdn.example.test/" + KEY)).contains(KEY);
        assertThat(parser.managedKey("https:\\u002f\\u002fcdn.example.test/" + KEY)).contains(KEY);
        assertThat(parser.managedKey(CDN + "/" + KEY.replace("development", "%64evelopment") + "?a=1&amp;b=2#x"))
                .contains(KEY);
        assertThat(parser.managedKey("https://cdn.example.test.evil.test/" + KEY)).isEmpty();
        assertThat(parser.managedKey("https://external.example.test/" + KEY)).isEmpty();
    }

    @Test
    void cdnSubpathRequiresBoundaryAndOtherEnvironmentIsRejectedAfterDecoding() {
        var properties = properties();
        properties.setCdnUrl(CDN + "/assets/");
        var parser = new UploadReferenceParser(properties);
        assertThat(parser.managedKey(CDN + "/assets/" + KEY)).contains(KEY);
        assertThat(parser.managedKey(CDN + "/assets-other/" + KEY)).isEmpty();
        assertThatThrownBy(() -> parser.managedKey(CDN + "/assets/%70roduction/blogs/images/file.png"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rejectsTraversalButPreservesLegacyDisplayNamesAndUrls() {
        var parser = new UploadReferenceParser(properties());
        assertThatThrownBy(() -> parser.managedKey(CDN + "/development/%2e%2e/production/a.png"))
                .isInstanceOf(BadRequestException.class);
        assertThat(parser.fileKey("100% handmade.png")).isEqualTo("100% handmade.png");
        assertThat(parser.managedKey("https://firebasestorage.googleapis.com/v0/b/old/o/a%2Fb.png?alt=media")).isEmpty();
        assertThat(parser.managedKey(CDN + "/old-folder/legacy.png")).contains("old-folder/legacy.png");
    }

    @Test
    void failsClosedWhenABrowserEquivalentFormResolvesToAManagedObject() {
        var parser = new UploadReferenceParser(properties());
        assertThatThrownBy(() -> parser.managedKey("https://%63dn.example.test/" + KEY))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> parser.managedKey("https://user@%63dn.example.test/" + KEY))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> parser.managedKey(CDN + "/x/../" + KEY))
                .isInstanceOf(BadRequestException.class);
        assertThat(parser.managedKey("https://%65xternal.example.test/" + KEY)).isEmpty();
        var subpath = properties();
        subpath.setCdnUrl(CDN + "/assets/");
        assertThat(new UploadReferenceParser(subpath).managedKey(CDN + "/x/../other.png")).isEmpty();
    }

    @Test
    void decodesOnlyEnvironmentPrefixedFileNames() {
        var parser = new UploadReferenceParser(properties());
        assertThat(parser.fileKey(KEY.replace("/", "%2F"))).isEqualTo(KEY);
        assertThat(parser.fileKey("production 100% cotton.png")).isEqualTo("production 100% cotton.png");
        assertThat(parser.fileKey("development%20dress 100%.png")).isEqualTo("development%20dress 100%.png");
        assertThatThrownBy(() -> parser.fileKey(
                KEY.replace("development", "production").replace("/", "%2F")))
                .isInstanceOf(ResponseStatusException.class);
    }
}
