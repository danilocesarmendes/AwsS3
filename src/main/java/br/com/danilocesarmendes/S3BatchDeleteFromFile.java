package br.com.danilocesarmendes;

import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class S3BatchDeleteFromFile {

    private static final int BATCH_SIZE = 100;

    public static void main(String[] args) throws IOException {

        boolean dryRun = false;
        String file = null;

        for (String arg : args) {
            if ("--dry-run".equals(arg)) {
                dryRun = true;
            } else if (file == null) {
                file = arg;
            } else {
                throw new IllegalArgumentException("Argumento inesperado: " + arg);
            }
        }

        if (file == null) {
            throw new IllegalArgumentException("Uso: java -jar AwsS3-1.0-SNAPSHOT.jar [--dry-run] <arquivo.txt>");
        }

        List<String> urls = Files.readAllLines(Path.of(file));

        BatchDeleter.Summary summary;
        try (S3Client s3Client = criarClient()) {
            summary = new BatchDeleter(s3Client, BATCH_SIZE, dryRun, System.out, System.err).deleteAll(urls);
        }

        System.out.printf("Resumo: solicitados=%d, deletados=%d, falhas=%d, urls inválidas=%d%s%n",
                summary.requested(), summary.deleted(), summary.failed(), summary.invalid(),
                dryRun ? " (dry-run: nada foi removido)" : "");

        if (summary.hasProblems()) {
            System.exit(1);
        }
    }

    private static S3Client criarClient() {
        return S3Client.builder()
                .region(new DefaultAwsRegionProviderChain().getRegion())
                .build();
    }
}
