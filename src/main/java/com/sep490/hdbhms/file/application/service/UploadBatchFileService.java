package com.sep490.hdbhms.file.application.service;

import com.sep490.hdbhms.file.application.port.in.command.UploadBatchFileCommand;
import com.sep490.hdbhms.file.application.port.in.command.UploadFileCommand;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadBatchFileUseCase;
import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
import com.sep490.hdbhms.file.domain.value_objects.FileCategory;
import com.sep490.hdbhms.file.infrastructure.config.FileProperties;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.BatchFileResponse;
import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileMetadataResponse;
import com.sep490.hdbhms.file.infrastructure.web.mapper.FileMetadataWebMapper;
import com.sep490.hdbhms.shared.exception.AppException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;


@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UploadBatchFileService implements UploadBatchFileUseCase {
    FileProperties fileProperties;
    UploadFileUseCase uploadFileUseCase;
    FileMetadataWebMapper fileMetadataWebMapper;

    @Override
    public BatchFileResponse execute(UploadBatchFileCommand query) {
        Long ownerId = query.userId();
        List<MultipartFile> multipartFiles = query.files();

        if (multipartFiles == null || multipartFiles.isEmpty()) {
            return BatchFileResponse.builder()
                    .totalFiles(0)
                    .successfulUploads(0)
                    .failedUploads(0)
                    .message("Chưa cung cấp tệp nào")
                    .build();
        }

        int maxConcurrent = fileProperties.getStorage().getMaxBatchSize();
        Semaphore semaphore = new Semaphore(maxConcurrent);
        List<CompletableFuture<FileMetadataResponse>> futures = new ArrayList<>();
        var batchInterrupted = new AtomicBoolean(false);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (MultipartFile file : multipartFiles) {
                if (batchInterrupted.get()) {
                    // If interrupted, don't submit new tasks
                    futures.add(CompletableFuture.completedFuture(
                            fileMetadataWebMapper.toFailedResponse(file, "Bỏ qua tải tệp do lô tải lên bị gián đoạn")));
                    continue;
                }

                CompletableFuture<FileMetadataResponse> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        semaphore.acquire();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return fileMetadataWebMapper.toFailedResponse(file, "Tải tệp bị gián đoạn trong khi chờ tài nguyên");
                    }
                    try {
                        return uploadWithRetries(file, ownerId, query.category(), query.isSensitive());
                    } finally {
                        semaphore.release();
                    }
                }, executor);

                futures.add(future);
            }

            // Wait for all tasks to complete or timeout
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                        .get(fileProperties.getStorage().getRetry().getTimeOut(), TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                log.error("Batch upload timed out");
                futures.forEach(f -> f.cancel(true));
                // Build response with completed ones only
                List<FileMetadataResponse> completedResponses = futures.stream()
                        .filter(CompletableFuture::isDone)
                        .map(CompletableFuture::join)
                        .toList();
                return BatchFileResponse.builder()
                        .totalFiles(multipartFiles.size())
                        .successfulUploads((int) completedResponses.stream().filter(FileMetadataResponse::isUploaded).count())
                        .failedUploads((int) completedResponses.stream().filter(r -> !r.isUploaded()).count())
                        .fileMetadataResponse(completedResponses)
                        .message("Lô tải tệp đã hết thời gian chờ. Một số tệp có thể chưa được xử lý.")
                        .build();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Batch upload was interrupted", e);
                futures.forEach(f -> f.cancel(true));
                return BatchFileResponse.builder()
                        .totalFiles(multipartFiles.size())
                        .successfulUploads(0)
                        .failedUploads(multipartFiles.size())
                        .message("Lô tải tệp đã bị gián đoạn.")
                        .build();
            } catch (ExecutionException e) {
                log.error("Batch upload failed", e);
                // Continue – some futures might have completed successfully
            }
        }

        // All futures completed successfully
        List<FileMetadataResponse> responses = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        long successCount = responses.stream().filter(FileMetadataResponse::isUploaded).count();
        return BatchFileResponse.builder()
                .totalFiles(multipartFiles.size())
                .successfulUploads((int) successCount)
                .failedUploads(multipartFiles.size() - (int) successCount)
                .fileMetadataResponse(responses)
                .message("Tải tệp hoàn tất.")
                .build();
    }

    private FileMetadataResponse uploadWithRetries(MultipartFile multipartFile, Long ownerUserId, FileCategory fileCategory, boolean isSensitive) {
        int attempts = 0;
        long retryDelayMs = fileProperties.getStorage().getRetry().getInitialDelay();
        final int maxAttempts = fileProperties.getStorage().getRetry().getMaxAttempts();

        while (attempts < maxAttempts) {
            if (Thread.currentThread().isInterrupted()) {
                return fileMetadataWebMapper.toFailedResponse(multipartFile, "Tải tệp bị gián đoạn trước lần thử " + attempts);
            }
            try {
                return fileMetadataWebMapper.toSuccessResponse(
                        uploadFileUseCase.execute(
                                new UploadFileCommand(
                                        ownerUserId,
                                        multipartFile,
                                        fileCategory,
                                        isSensitive)
                        )
                );
            } catch (AppException | IOException e) {
                attempts++;
                if (Thread.currentThread().isInterrupted()) {
                    return fileMetadataWebMapper.toFailedResponse(multipartFile, "Tải tệp bị gián đoạn trước lần thử " + attempts);
                }
                if (attempts >= maxAttempts) {
                    log.warn("Failed to upload file {} after {} attempts: {}",
                            multipartFile.getOriginalFilename(), maxAttempts, e.getMessage());
                    return fileMetadataWebMapper.toFailedResponse(multipartFile, "Tải tệp thất bại sau " + maxAttempts + " lần thử: " + e.getMessage());
                }
                log.info("Retrying file {} in {} ms (attempt {}/{})",
                        multipartFile.getOriginalFilename(), retryDelayMs, attempts, maxAttempts);
                try {
                    Thread.sleep(retryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return fileMetadataWebMapper.toFailedResponse(multipartFile, "Tải tệp bị gián đoạn trong thời gian chờ thử lại");
                }
                retryDelayMs *= 2;
            }
        }
        return fileMetadataWebMapper.toFailedResponse(multipartFile, "Tải tệp thất bại sau các lần thử lại");
    }
}
