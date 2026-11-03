package br.com.danilocesarmendes;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Extrai bucket e chave de URLs S3 nos formatos:
 * <ul>
 *   <li>{@code s3://bucket/key}</li>
 *   <li>path-style: {@code https://s3.<region>.amazonaws.com/bucket/key}</li>
 *   <li>virtual-hosted: {@code https://bucket.s3.amazonaws.com/key}</li>
 * </ul>
 */
final class S3UrlParser {

    private S3UrlParser() {
    }

    static S3ObjectInfo parse(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL S3 vazia");
        }
        String value = url.trim();

        if (value.startsWith("s3://")) {
            String withoutPrefix = value.substring(5);
            int idx = withoutPrefix.indexOf('/');
            if (idx <= 0 || idx == withoutPrefix.length() - 1) {
                throw invalid(url);
            }
            return new S3ObjectInfo(withoutPrefix.substring(0, idx), withoutPrefix.substring(idx + 1));
        }

        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL S3 inválida: " + url, e);
        }

        String host = uri.getHost();
        String path = uri.getPath();
        if (host == null || path == null || path.length() <= 1) {
            throw invalid(url);
        }

        // Path-style: s3.<region>.amazonaws.com/bucket/key
        if (host.startsWith("s3.")) {
            String[] parts = path.split("/", 3);
            if (parts.length < 3 || parts[1].isEmpty() || parts[2].isEmpty()) {
                throw invalid(url);
            }
            return new S3ObjectInfo(parts[1], parts[2]);
        }

        // Virtual-hosted-style: bucket.s3.amazonaws.com/key
        String bucket = host.split("\\.")[0];
        return new S3ObjectInfo(bucket, path.substring(1));
    }

    private static IllegalArgumentException invalid(String url) {
        return new IllegalArgumentException("URL S3 inválida: " + url);
    }
}
