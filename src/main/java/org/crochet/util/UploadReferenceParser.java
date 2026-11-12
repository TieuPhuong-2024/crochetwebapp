package org.crochet.util;

import org.crochet.exception.BadRequestException;
import org.crochet.properties.R2Properties;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class UploadReferenceParser {
    private static final Pattern UNICODE_ESCAPE = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
    private static final Pattern CSS_URL = Pattern.compile("url\\(\\s*(['\"]?)(.*?)\\1\\s*\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENVIRONMENT_KEY = Pattern.compile("^(?:development|production)(?:/|%2[fF])");
    private final R2Properties properties;

    public UploadReferenceParser(R2Properties properties) {
        this.properties = properties;
    }

    public Optional<String> managedKey(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        URI base = properties.cdnUri();
        if (base == null) {
            return Optional.empty();
        }
        String normalized = Parser.unescapeEntities(unescape(value), false).trim();
        if (normalized.startsWith("//")) {
            normalized = base.getScheme() + ":" + normalized;
        }
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException ex) {
            if (normalized.toLowerCase().contains(base.getHost().toLowerCase())) {
                throw new BadRequestException("Invalid managed upload URL");
            }
            return Optional.empty();
        }
        String host = uri.getHost();
        if (host == null) {
            // Java cannot parse percent-encoded hosts, but browsers resolve them normally.
            if (base.getScheme().equalsIgnoreCase(uri.getScheme())
                    && base.getHost().equalsIgnoreCase(rawAuthorityHost(uri.getRawAuthority()))) {
                throw new BadRequestException("Invalid managed upload URL");
            }
            return Optional.empty();
        }
        if (!base.getHost().equalsIgnoreCase(host)
                || !base.getScheme().equalsIgnoreCase(uri.getScheme()) || port(base) != port(uri)) {
            return Optional.empty();
        }
        String rawPath = decodePath(uri.getRawPath());
        String path = removeDotSegments(rawPath);
        String prefix = base.getPath().replaceAll("/+$", "") + "/";
        if (!path.startsWith(prefix)) {
            return Optional.empty();
        }
        if (!path.equals(rawPath)) {
            throw new BadRequestException("Invalid managed upload path");
        }
        String key = path.substring(prefix.length());
        if (uri.getUserInfo() != null || key.contains("\\") || key.startsWith("/")
                || key.chars().anyMatch(Character::isISOControl)
                || List.of(key.split("/", -1)).stream().anyMatch(segment -> segment.equals(".") || segment.equals(".."))) {
            throw new BadRequestException("Invalid managed upload path");
        }
        rejectOtherEnvironment(key);
        return key.isEmpty() ? Optional.empty() : Optional.of(key);
    }

    public String fileKey(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        Optional<String> urlKey = managedKey(name);
        if (urlKey.isPresent()) {
            return urlKey.get();
        }
        String key = Parser.unescapeEntities(unescape(name), false).trim();
        // Legacy display names/external URLs are not URL paths (a literal '%' is valid in a name).
        if (ENVIRONMENT_KEY.matcher(key).find()) {
            key = decodePath(key);
            rejectOtherEnvironment(key);
        }
        return key;
    }

    public List<String> contentUrls(String html) {
        List<String> urls = new ArrayList<>();
        if (html == null || html.isBlank()) {
            return urls;
        }
        var document = Jsoup.parseBodyFragment(unescape(html));
        for (var element : document.getAllElements()) {
            for (String attribute : List.of("src", "href", "poster", "data", "background", "xlink:href")) {
                if (element.hasAttr(attribute)) {
                    urls.add(element.attr(attribute));
                }
            }
            for (String attribute : List.of("srcset", "imagesrcset")) {
                for (String candidate : element.attr(attribute).split(",")) {
                    String trimmed = candidate.trim();
                    if (!trimmed.isEmpty()) {
                        urls.add(trimmed.split("\\s+", 2)[0]);
                    }
                }
            }
            var css = CSS_URL.matcher(element.attr("style"));
            while (css.find()) {
                urls.add(css.group(2));
            }
        }
        return urls;
    }

    private void rejectOtherEnvironment(String key) {
        properties.validateEnvironment();
        if ((key.startsWith("development/") || key.startsWith("production/"))
                && !key.startsWith(properties.getEnvironment() + "/")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Upload belongs to another environment");
        }
    }

    private String unescape(String value) {
        return UNICODE_ESCAPE.matcher(value).replaceAll(match ->
                java.util.regex.Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(match.group(1), 16))))
                .replace("\\/", "/").replace("\\\"", "\"").replace("\\'", "'");
    }

    private String decodePath(String value) {
        try {
            // '+' is a literal URL path character, not the form-encoding space character.
            return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid upload path encoding");
        }
    }

    private String rawAuthorityHost(String authority) {
        if (authority == null) {
            return null;
        }
        int userInfoEnd = authority.lastIndexOf('@');
        String hostPort = userInfoEnd >= 0 ? authority.substring(userInfoEnd + 1) : authority;
        if (hostPort.startsWith("[")) {
            return null;
        }
        int portStart = hostPort.indexOf(':');
        if (portStart >= 0) {
            hostPort = hostPort.substring(0, portStart);
        }
        if (hostPort.isEmpty()) {
            return null;
        }
        String decoded;
        try {
            decoded = URLDecoder.decode(hostPort.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            return null;
        }
        if (decoded.isEmpty() || decoded.chars().anyMatch(c -> c == '/' || c == '\\' || c == '@'
                || c == ':' || Character.isWhitespace(c) || Character.isISOControl(c))) {
            return null;
        }
        return decoded;
    }

    private String removeDotSegments(String path) {
        var output = new ArrayDeque<String>();
        for (String segment : path.split("/", -1)) {
            if (segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (!output.isEmpty()) {
                    output.removeLast();
                }
            } else {
                output.addLast(segment);
            }
        }
        String joined = String.join("/", output);
        if (path.startsWith("/") && !joined.startsWith("/")) {
            joined = "/" + joined;
        }
        if ((path.endsWith("/.") || path.endsWith("/..")) && !joined.endsWith("/")) {
            joined += "/";
        }
        return joined;
    }

    private int port(URI uri) {
        return uri.getPort() < 0 ? 443 : uri.getPort();
    }
}
