package br.com.danilocesarmendes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class S3UrlParserTest {

    @Test
    void parsesS3Scheme() {
        assertEquals(new S3ObjectInfo("meu-bucket", "pasta/arquivo.txt"),
                S3UrlParser.parse("s3://meu-bucket/pasta/arquivo.txt"));
    }

    @Test
    void parsesPathStyleUrl() {
        assertEquals(new S3ObjectInfo("meu-bucket", "pasta/arquivo.txt"),
                S3UrlParser.parse("https://s3.us-east-1.amazonaws.com/meu-bucket/pasta/arquivo.txt"));
    }

    @Test
    void parsesVirtualHostedStyleUrl() {
        assertEquals(new S3ObjectInfo("meu-bucket", "pasta/arquivo.txt"),
                S3UrlParser.parse("https://meu-bucket.s3.amazonaws.com/pasta/arquivo.txt"));
    }

    @Test
    void decodesPercentEncodedKey() {
        assertEquals(new S3ObjectInfo("meu-bucket", "pasta/meu arquivo.txt"),
                S3UrlParser.parse("https://meu-bucket.s3.amazonaws.com/pasta/meu%20arquivo.txt"));
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertEquals(new S3ObjectInfo("b", "k"), S3UrlParser.parse("  s3://b/k  "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "s3://",
            "s3://bucket",
            "s3://bucket/",
            "s3:///chave",
            "https://s3.amazonaws.com/so-o-bucket",
            "https://s3.amazonaws.com/bucket/",
            "https://meu-bucket.s3.amazonaws.com/",
            "texto sem formato de url",
            "/apenas/um/caminho"
    })
    void rejectsInvalidUrls(String url) {
        assertThrows(IllegalArgumentException.class, () -> S3UrlParser.parse(url));
    }
}
