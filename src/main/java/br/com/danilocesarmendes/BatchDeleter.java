package br.com.danilocesarmendes;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Remove objetos do S3 em lotes, agrupando as chaves por bucket.
 * Cada requisição {@code DeleteObjects} contém chaves de um único bucket.
 */
final class BatchDeleter {

    /** Limite do S3 para uma requisição DeleteObjects. */
    static final int MAX_BATCH_SIZE = 1000;

    record Summary(int requested, int deleted, int failed, int invalid) {
        boolean hasProblems() {
            return failed > 0 || invalid > 0;
        }
    }

    private static final class Counters {
        int requested;
        int deleted;
        int failed;
        int invalid;
    }

    private final S3Client s3;
    private final int batchSize;
    private final boolean dryRun;
    private final PrintStream out;
    private final PrintStream err;

    BatchDeleter(S3Client s3, int batchSize, boolean dryRun, PrintStream out, PrintStream err) {
        if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("batchSize deve estar entre 1 e " + MAX_BATCH_SIZE);
        }
        this.s3 = s3;
        this.batchSize = batchSize;
        this.dryRun = dryRun;
        this.out = out;
        this.err = err;
    }

    Summary deleteAll(List<String> urls) {
        Counters counters = new Counters();
        Map<String, List<ObjectIdentifier>> pending = new LinkedHashMap<>();

        for (String url : urls) {
            if (url == null || url.isBlank()) {
                continue;
            }

            S3ObjectInfo info;
            try {
                info = S3UrlParser.parse(url);
            } catch (IllegalArgumentException e) {
                counters.invalid++;
                err.println("URL ignorada: " + url + " (" + e.getMessage() + ")");
                continue;
            }

            counters.requested++;
            List<ObjectIdentifier> batch = pending.computeIfAbsent(info.bucket(), b -> new ArrayList<>());
            batch.add(ObjectIdentifier.builder().key(info.key()).build());

            if (batch.size() >= batchSize) {
                flush(info.bucket(), batch, counters);
                batch.clear();
            }
        }

        pending.forEach((bucket, batch) -> {
            if (!batch.isEmpty()) {
                flush(bucket, batch, counters);
            }
        });

        return new Summary(counters.requested, counters.deleted, counters.failed, counters.invalid);
    }

    private void flush(String bucket, List<ObjectIdentifier> batch, Counters counters) {
        if (dryRun) {
            batch.forEach(o -> out.println("[dry-run] s3://" + bucket + "/" + o.key()));
            return;
        }

        try {
            DeleteObjectsResponse response = s3.deleteObjects(
                    DeleteObjectsRequest.builder()
                            .bucket(bucket)
                            .delete(Delete.builder().objects(List.copyOf(batch)).build())
                            .build());

            response.deleted().forEach(d -> out.println("Deletado: s3://" + bucket + "/" + d.key()));
            response.errors().forEach(e -> err.println(
                    "Falha: s3://" + bucket + "/" + e.key() + " | " + e.code() + " | " + e.message()));

            counters.deleted += response.deleted().size();
            counters.failed += response.errors().size();
        } catch (NoSuchBucketException e) {
            counters.failed += batch.size();
            err.println("Bucket não existe ou sem acesso: " + bucket);
        } catch (S3Exception e) {
            counters.failed += batch.size();
            err.println("Erro S3 no bucket " + bucket + " | status " + e.statusCode() + " | " + e.getMessage());
        }
    }
}
