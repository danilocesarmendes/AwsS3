package br.com.danilocesarmendes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Error;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchDeleterTest {

    @Mock
    S3Client s3;

    private ByteArrayOutputStream outBuffer;
    private ByteArrayOutputStream errBuffer;
    private PrintStream out;
    private PrintStream err;

    @BeforeEach
    void setUp() {
        outBuffer = new ByteArrayOutputStream();
        errBuffer = new ByteArrayOutputStream();
        out = new PrintStream(outBuffer, true);
        err = new PrintStream(errBuffer, true);
        // Por padrão o S3 responde sucesso para todas as chaves da requisição.
        lenient().when(s3.deleteObjects(any(DeleteObjectsRequest.class))).thenAnswer(invocation -> {
            DeleteObjectsRequest request = invocation.getArgument(0);
            List<DeletedObject> deleted = request.delete().objects().stream()
                    .map(o -> DeletedObject.builder().key(o.key()).build())
                    .toList();
            return DeleteObjectsResponse.builder().deleted(deleted).build();
        });
    }

    private BatchDeleter deleter(int batchSize, boolean dryRun) {
        return new BatchDeleter(s3, batchSize, dryRun, out, err);
    }

    private static List<String> keys(DeleteObjectsRequest request) {
        return request.delete().objects().stream().map(ObjectIdentifier::key).toList();
    }

    @Test
    void neverMixesBucketsInTheSameRequest() {
        BatchDeleter.Summary summary = deleter(100, false).deleteAll(List.of(
                "s3://bucket-a/a1", "s3://bucket-b/b1", "s3://bucket-a/a2", "s3://bucket-b/b2"));

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3, times(2)).deleteObjects(captor.capture());

        for (DeleteObjectsRequest request : captor.getAllValues()) {
            if ("bucket-a".equals(request.bucket())) {
                assertEquals(List.of("a1", "a2"), keys(request));
            } else {
                assertEquals("bucket-b", request.bucket());
                assertEquals(List.of("b1", "b2"), keys(request));
            }
        }
        assertEquals(new BatchDeleter.Summary(4, 4, 0, 0), summary);
    }

    @Test
    void flushesWhenBatchSizeIsReached() {
        BatchDeleter.Summary summary = deleter(2, false).deleteAll(List.of(
                "s3://b/1", "s3://b/2", "s3://b/3", "s3://b/4", "s3://b/5"));

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3, times(3)).deleteObjects(captor.capture());

        assertEquals(List.of("1", "2"), keys(captor.getAllValues().get(0)));
        assertEquals(List.of("3", "4"), keys(captor.getAllValues().get(1)));
        assertEquals(List.of("5"), keys(captor.getAllValues().get(2)));
        assertEquals(5, summary.deleted());
    }

    @Test
    void doesNotSendDuplicatedKeys() {
        deleter(100, false).deleteAll(List.of("s3://b/x", "s3://b/y"));

        ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
        verify(s3).deleteObjects(captor.capture());
        assertEquals(List.of("x", "y"), keys(captor.getValue()));
    }

    @Test
    void dryRunDoesNotCallS3AndPrintsWhatWouldBeDeleted() {
        BatchDeleter.Summary summary = deleter(100, true).deleteAll(List.of("s3://b/1", "s3://c/2"));

        verifyNoInteractions(s3);
        String output = outBuffer.toString();
        assertTrue(output.contains("[dry-run] s3://b/1"));
        assertTrue(output.contains("[dry-run] s3://c/2"));
        assertEquals(new BatchDeleter.Summary(2, 0, 0, 0), summary);
    }

    @Test
    void ignoresBlankLinesAndCountsInvalidUrls() {
        BatchDeleter.Summary summary = deleter(100, false).deleteAll(List.of(
                "", "   ", "s3://b/ok", "isto-nao-e-uma-url", "s3://semchave"));

        assertEquals(new BatchDeleter.Summary(1, 1, 0, 2), summary);
        assertTrue(summary.hasProblems());
        assertTrue(errBuffer.toString().contains("isto-nao-e-uma-url"));
    }

    @Test
    void countsPerKeyErrorsReturnedByS3() {
        when(s3.deleteObjects(any(DeleteObjectsRequest.class))).thenReturn(
                DeleteObjectsResponse.builder()
                        .deleted(DeletedObject.builder().key("1").build())
                        .errors(S3Error.builder().key("2").code("AccessDenied").message("negado").build())
                        .build());

        BatchDeleter.Summary summary = deleter(100, false).deleteAll(List.of("s3://b/1", "s3://b/2"));

        assertEquals(new BatchDeleter.Summary(2, 1, 1, 0), summary);
        assertTrue(errBuffer.toString().contains("AccessDenied"));
    }

    @Test
    void s3ExceptionFailsTheBatchButProcessingContinues() {
        when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenThrow(S3Exception.builder().message("erro interno").statusCode(500).build())
                .thenAnswer(invocation -> {
                    DeleteObjectsRequest request = invocation.getArgument(0);
                    return DeleteObjectsResponse.builder()
                            .deleted(DeletedObject.builder().key(request.delete().objects().get(0).key()).build())
                            .build();
                });

        BatchDeleter.Summary summary = deleter(1, false).deleteAll(List.of("s3://b/1", "s3://b/2"));

        assertEquals(new BatchDeleter.Summary(2, 1, 1, 0), summary);
        assertTrue(summary.hasProblems());
    }

    @Test
    void missingBucketCountsAsFailure() {
        when(s3.deleteObjects(any(DeleteObjectsRequest.class)))
                .thenThrow(NoSuchBucketException.builder().message("sem bucket").statusCode(404).build());

        BatchDeleter.Summary summary = deleter(100, false).deleteAll(List.of("s3://inexistente/1"));

        assertEquals(new BatchDeleter.Summary(1, 0, 1, 0), summary);
        assertTrue(errBuffer.toString().contains("inexistente"));
    }

    @Test
    void successfulRunHasNoProblems() {
        assertFalse(deleter(100, false).deleteAll(List.of("s3://b/1")).hasProblems());
    }

    @Test
    void rejectsInvalidBatchSize() {
        assertThrows(IllegalArgumentException.class, () -> deleter(0, false));
        assertThrows(IllegalArgumentException.class, () -> deleter(BatchDeleter.MAX_BATCH_SIZE + 1, false));
    }
}
