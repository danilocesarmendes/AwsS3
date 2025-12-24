package br.com.danilocesarmendes;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class S3BatchDeleteFromFile {

    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) throws IOException {

        if (args.length == 0) {
            throw new IllegalArgumentException("Informe o caminho do arquivo TXT");
        }

        Path filePath = Path.of(args[0]);

        List<String> urls = Files.readAllLines(filePath);

        try (S3Client s3Client = criarClient()) {

            List<ObjectIdentifier> batch = new ArrayList<>();
            String currentBucket = null;

            for (String url : urls) {
                if (url.isBlank()) continue;

                try {
                    S3ObjectInfo info = extrairBucketEKey(url);

                    // Valida se existe
                    batch.add(
                            ObjectIdentifier.builder()
                                    .key(info.key())
                                    .build()
                    );

                    // Se mudar de bucket, força flush
                    if (currentBucket != null && !currentBucket.equals(info.bucket())) {
                        executarDelete(s3Client, currentBucket, batch);
                        batch.clear();
                    }

                    currentBucket = info.bucket();
                    batch.add(
                            ObjectIdentifier.builder()
                                    .key(info.key())
                                    .build()
                    );

                    if (batch.size() == BATCH_SIZE) {
                        executarDelete(s3Client, currentBucket, batch);
                        batch.clear();
                    }

                } catch (Exception e) {
                    System.err.println("❌ Erro processando URL: " + url);
                    e.printStackTrace();
                }
            }

            // Deleta o que sobrar
            if (!batch.isEmpty()) {
                executarDelete(s3Client, currentBucket, batch);
            }
        }
    }

    private static S3Client criarClient() {
        return S3Client.builder()
                .region(new DefaultAwsRegionProviderChain().getRegion())
                .build();
    }

    private static void executarDelete(
            S3Client s3Client,
            String bucket,
            List<ObjectIdentifier> objects
    ) {

        if (objects.isEmpty()) return;

        try {
            DeleteObjectsResponse response = s3Client.deleteObjects(
                    DeleteObjectsRequest.builder()
                            .bucket(bucket)
                            .delete(Delete.builder().objects(objects).build())
                            .build()
            );

            response.deleted().forEach(d ->
                    System.out.println("✅ Deletado: s3://" + bucket + "/" + d.key())
            );

            response.errors().forEach(e ->
                    System.err.println("❌ Falha: s3://" + bucket + "/" + e.key() +
                            " | " + e.code() + " | " + e.message())
            );

        } catch (NoSuchBucketException e) {
            System.err.println("🚫 Bucket não existe ou sem acesso: " + bucket);
        } catch (S3Exception e) {
            System.err.println("❌ Erro S3 no bucket " + bucket +
                    " | Status: " + e.statusCode() +
                    " | " + e.awsErrorDetails().errorCode());
        }
    }


    private static S3ObjectInfo extrairBucketEKey(String url) throws Exception {

        // s3://bucket/key
        if (url.startsWith("s3://")) {
            String semPrefixo = url.substring(5);
            int idx = semPrefixo.indexOf('/');
            if (idx < 0) {
                throw new IllegalArgumentException("URL S3 inválida: " + url);
            }
            return new S3ObjectInfo(
                    semPrefixo.substring(0, idx),
                    semPrefixo.substring(idx + 1)
            );
        }

        URI uri = new URI(url);
        String host = uri.getHost();
        String path = uri.getPath();

        // Path-style: s3.<region>.amazonaws.com/bucket/key
        if (host.startsWith("s3.")) {
            String[] parts = path.split("/", 3);
            if (parts.length < 3) {
                throw new IllegalArgumentException("URL S3 inválida: " + url);
            }
            return new S3ObjectInfo(
                    parts[1], // bucket
                    parts[2]  // key
            );
        }

        // Virtual-hosted–style: bucket.s3.amazonaws.com/key
        String bucket = host.split("\\.")[0];
        String key = path.startsWith("/") ? path.substring(1) : path;

        return new S3ObjectInfo(bucket, key);
    }


    private record S3ObjectInfo(String bucket, String key) {}
}

