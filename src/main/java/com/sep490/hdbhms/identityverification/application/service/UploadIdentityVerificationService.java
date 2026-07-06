package com.sep490.hdbhms.identityverification.application.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.ByQuadrantReader;
import com.sep490.hdbhms.identityandaccess.domain.valueObjects.Gender;
import com.sep490.hdbhms.identityverification.application.port.in.command.UploadIdentityVerificationCommand;
import com.sep490.hdbhms.identityverification.application.port.in.usecase.UploadIdentityVerificationUseCase;
import com.sep490.hdbhms.identityverification.application.port.out.CccdOcrExtractionPort;
import com.sep490.hdbhms.identityverification.domain.model.CccdExtractedIdentity;
import com.sep490.hdbhms.identityverification.infrastructure.web.dto.response.IdentityVerificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadIdentityVerificationService implements UploadIdentityVerificationUseCase {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.ofPattern("ddMMyyyy");
    private static final DateTimeFormatter SLASH_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DEBUG_DUMP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int QR_CROP_TARGET_MIN_SIDE = 900;
    private static final int QR_FULL_IMAGE_TARGET_MIN_SIDE = 1400;
    private static final int QR_IMAGE_MAX_SIDE = 2800;
    private static final double MAX_QR_SCALE_FACTOR = 3.5;
    private static final Map<DecodeHintType, Object> QR_DECODE_HINTS = Map.of(
            DecodeHintType.TRY_HARDER, Boolean.TRUE,
            DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE),
            DecodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name()
    );
    private static final float[] SHARPEN_KERNEL = {
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
    };
    private static final List<CropRatio> CCCD_QR_REGION_RATIOS = List.of(
            new CropRatio(0.42, 0.00, 0.58, 1.00),
            new CropRatio(0.48, 0.00, 0.52, 0.65),
            new CropRatio(0.55, 0.00, 0.45, 0.55),
            new CropRatio(0.60, 0.00, 0.40, 0.45),
            new CropRatio(0.48, 0.12, 0.52, 0.62)
    );

    private final ObjectProvider<CccdOcrExtractionPort> cccdOcrExtractionPortProvider;

    @Value("${app.identity-verification.debug.dump-candidates:false}")
    private boolean dumpQrCandidates;

    @Value("${app.identity-verification.debug.dump-directory:.debug/identity-verification}")
    private String qrCandidateDumpDirectory;

    @Override
    public IdentityVerificationResponse execute(UploadIdentityVerificationCommand command) {
        validateImageFile(command.cccdImage());

        CccdExtractedIdentity qrData = readCccdQr(command.cccdImage())
                .orElse(null);
        if (qrData != null) {
            log.debug("CCCD extraction completed by QR.");
            return IdentityVerificationResponse.builder()
                    .success(true)
                    .code("CCCD_QR_SCANNED")
                    .message("Đã đọc mã QR CCCD.")
                    .qrExtracted(true)
                    .ocrExtracted(false)
                    .extractionMethod("QR")
                    .rawQrPayload(qrData.rawPayload())
                    .extractedIdentity(toResponse(qrData))
                    .build();
        }

        CccdExtractedIdentity ocrData = cccdOcrExtractionPortProvider
                .getIfAvailable(() -> cccdImage -> Optional.empty())
                .extract(command.cccdImage())
                .orElse(null);
        if (ocrData == null) {
            log.debug("CCCD extraction failed: QR did not decode and OCR fallback returned empty.");
            return IdentityVerificationResponse.builder()
                    .success(false)
                    .code("CCCD_EXTRACTION_FAILED")
                    .message("Không thể đọc mã QR CCCD từ ảnh, OCR fallback chưa trích xuất được dữ liệu.")
                    .qrExtracted(false)
                    .ocrExtracted(false)
                    .build();
        }

        log.debug("CCCD extraction completed by OCR fallback.");
        return IdentityVerificationResponse.builder()
                .success(true)
                .code("CCCD_OCR_EXTRACTED")
                .message("Không đọc được mã QR CCCD, đã trích xuất bằng OCR fallback.")
                .qrExtracted(false)
                .ocrExtracted(true)
                .extractionMethod("OCR")
                .extractedIdentity(toResponse(ocrData))
                .build();
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Vui lòng upload ảnh CCCD."
            );
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Ảnh quá lớn, vui lòng chọn ảnh khác.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Định dạng ảnh không hợp lệ.");
        }
    }

    private Optional<CccdExtractedIdentity> readCccdQr(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(file.getInputStream());
            if (image == null) {
                log.debug("CCCD QR extraction skipped: uploaded image could not be decoded.");
                return Optional.empty();
            }
            log.debug("CCCD QR extraction started: originalWidth={}, originalHeight={}.", image.getWidth(), image.getHeight());
            List<QrDecodeCandidate> candidates = buildQrDecodeCandidates(image);
            log.debug("CCCD QR extraction candidate count={}.", candidates.size());
            dumpQrDebugCandidates(image, candidates);

            for (int index = 0; index < candidates.size(); index++) {
                QrDecodeCandidate candidate = candidates.get(index);
                log.debug(
                        "Trying CCCD QR candidate {}/{}: label={}, width={}, height={}.",
                        index + 1,
                        candidates.size(),
                        candidate.label(),
                        candidate.image().getWidth(),
                        candidate.image().getHeight()
                );
                Optional<CccdExtractedIdentity> parsedQr = decodeQrText(candidate)
                        .flatMap(this::parseCccdQr);
                if (parsedQr.isPresent()) {
                    log.debug("CCCD QR extraction succeeded with candidate {}: label={}.", index + 1, candidate.label());
                    return parsedQr;
                }
            }
            log.debug("CCCD QR extraction failed after {} candidates.", candidates.size());
            return Optional.empty();
        } catch (IOException | RuntimeException ex) {
            log.debug("CCCD QR extraction failed due to image processing error: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private List<QrDecodeCandidate> buildQrDecodeCandidates(BufferedImage image) {
        List<QrDecodeCandidate> candidates = new ArrayList<>();
        for (OrientedImage orientedImage : buildOrientationCandidates(image)) {
            addPreprocessedVariants(
                    candidates,
                    orientedImage.image(),
                    QR_FULL_IMAGE_TARGET_MIN_SIDE,
                    orientedImage.label() + ":full"
            );

            for (CropRatio ratio : CCCD_QR_REGION_RATIOS) {
                cropImage(orientedImage.image(), ratio)
                        .ifPresent(crop -> addPreprocessedVariants(
                                candidates,
                                crop,
                                QR_CROP_TARGET_MIN_SIDE,
                                orientedImage.label() + ":crop(" + ratio + ")"
                        ));
            }
        }

        return candidates;
    }

    private void dumpQrDebugCandidates(BufferedImage originalImage, List<QrDecodeCandidate> candidates) {
        if (!dumpQrCandidates) {
            return;
        }

        Path sessionDirectory = Paths.get(qrCandidateDumpDirectory)
                .resolve("cccd-qr-" + LocalDateTime.now().format(DEBUG_DUMP_TIMESTAMP));
        try {
            Files.createDirectories(sessionDirectory);
            ImageIO.write(originalImage, "png", sessionDirectory.resolve("000_original.png").toFile());
            for (int index = 0; index < candidates.size(); index++) {
                QrDecodeCandidate candidate = candidates.get(index);
                String fileName = String.format(
                        "%03d_%s.png",
                        index + 1,
                        sanitizeDebugFileName(candidate.label())
                );
                ImageIO.write(candidate.image(), "png", sessionDirectory.resolve(fileName).toFile());
            }
            log.debug("CCCD QR debug candidates dumped to {}.", sessionDirectory.toAbsolutePath());
        } catch (IOException | RuntimeException ex) {
            log.warn("Could not dump CCCD QR debug candidates: {}", ex.getMessage());
        }
    }

    private String sanitizeDebugFileName(String value) {
        String sanitized = String.valueOf(value)
                .replaceAll("[^a-zA-Z0-9._=-]+", "_")
                .replaceAll("_+", "_");
        return sanitized.length() > 120 ? sanitized.substring(0, 120) : sanitized;
    }

    private List<OrientedImage> buildOrientationCandidates(BufferedImage image) {
        return List.of(
                new OrientedImage("rotate=0", image),
                new OrientedImage("rotate=90", rotateImage(image, 90)),
                new OrientedImage("rotate=180", rotateImage(image, 180)),
                new OrientedImage("rotate=270", rotateImage(image, 270))
        );
    }

    private BufferedImage rotateImage(BufferedImage image, int degrees) {
        if (degrees == 0) {
            return image;
        }

        double radians = Math.toRadians(degrees);
        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();
        boolean swapsDimensions = degrees == 90 || degrees == 270;
        int rotatedWidth = swapsDimensions ? originalHeight : originalWidth;
        int rotatedHeight = swapsDimensions ? originalWidth : originalHeight;
        BufferedImage rotatedImage = new BufferedImage(rotatedWidth, rotatedHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = rotatedImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            AffineTransform transform = new AffineTransform();
            transform.translate(rotatedWidth / 2.0, rotatedHeight / 2.0);
            transform.rotate(radians);
            transform.translate(-originalWidth / 2.0, -originalHeight / 2.0);
            graphics.drawImage(image, transform, null);
        } finally {
            graphics.dispose();
        }
        return rotatedImage;
    }

    private void addPreprocessedVariants(
            List<QrDecodeCandidate> candidates,
            BufferedImage image,
            int targetMinSide,
            String baseLabel
    ) {
        BufferedImage normalizedImage = normalizeQrImageSize(image, targetMinSide);
        BufferedImage grayscaleImage = toGrayscale(normalizedImage);
        BufferedImage contrastImage = stretchContrast(grayscaleImage);
        BufferedImage sharpenedImage = sharpen(contrastImage);

        candidates.add(new QrDecodeCandidate(baseLabel + ":normalized", normalizedImage));
        candidates.add(new QrDecodeCandidate(baseLabel + ":grayscale", grayscaleImage));
        candidates.add(new QrDecodeCandidate(baseLabel + ":contrast", contrastImage));
        candidates.add(new QrDecodeCandidate(baseLabel + ":sharpen", sharpenedImage));
        candidates.add(new QrDecodeCandidate(baseLabel + ":contrast_otsu", thresholdImage(contrastImage)));
        candidates.add(new QrDecodeCandidate(baseLabel + ":sharpen_otsu", thresholdImage(sharpenedImage)));
    }

    private Optional<BufferedImage> cropImage(BufferedImage image, CropRatio ratio) {
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        int x = clamp((int) Math.round(imageWidth * ratio.x()), 0, imageWidth - 1);
        int y = clamp((int) Math.round(imageHeight * ratio.y()), 0, imageHeight - 1);
        int width = clamp((int) Math.round(imageWidth * ratio.width()), 1, imageWidth - x);
        int height = clamp((int) Math.round(imageHeight * ratio.height()), 1, imageHeight - y);

        if (width < 80 || height < 80) {
            return Optional.empty();
        }
        return Optional.of(image.getSubimage(x, y, width, height));
    }

    private BufferedImage normalizeQrImageSize(BufferedImage image, int targetMinSide) {
        int maxSide = Math.max(image.getWidth(), image.getHeight());
        int minSide = Math.min(image.getWidth(), image.getHeight());
        if (minSide >= targetMinSide && maxSide <= QR_IMAGE_MAX_SIDE) {
            return image;
        }

        double scaleFactor = minSide < targetMinSide
                ? Math.min(MAX_QR_SCALE_FACTOR, (double) targetMinSide / minSide)
                : 1.0;
        if (maxSide * scaleFactor > QR_IMAGE_MAX_SIDE) {
            scaleFactor = (double) QR_IMAGE_MAX_SIDE / maxSide;
        }
        if (Math.abs(scaleFactor - 1.0) < 0.01) {
            return image;
        }

        int scaledWidth = Math.max(1, (int) Math.round(image.getWidth() * scaleFactor));
        int scaledHeight = Math.max(1, (int) Math.round(image.getHeight() * scaleFactor));
        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = scaledImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
        } finally {
            graphics.dispose();
        }
        return scaledImage;
    }

    private BufferedImage toGrayscale(BufferedImage image) {
        BufferedImage grayscaleImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D graphics = grayscaleImage.createGraphics();
        try {
            graphics.drawImage(image, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return grayscaleImage;
    }

    private BufferedImage stretchContrast(BufferedImage grayscaleImage) {
        WritableRaster sourceRaster = grayscaleImage.getRaster();
        int width = grayscaleImage.getWidth();
        int height = grayscaleImage.getHeight();
        int[] histogram = new int[256];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = sourceRaster.getSample(x, y, 0);
                histogram[value]++;
            }
        }

        int totalPixels = width * height;
        int low = percentileFromHistogram(histogram, (int) Math.round(totalPixels * 0.02));
        int high = percentileFromHistogram(histogram, (int) Math.round(totalPixels * 0.98));

        if (high - low < 24) {
            return grayscaleImage;
        }

        BufferedImage contrastImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster targetRaster = contrastImage.getRaster();
        double scale = 255.0 / (high - low);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = sourceRaster.getSample(x, y, 0);
                int stretchedValue = clamp((int) Math.round((value - low) * scale), 0, 255);
                targetRaster.setSample(x, y, 0, stretchedValue);
            }
        }
        return contrastImage;
    }

    private int percentileFromHistogram(int[] histogram, int targetCount) {
        int cumulativeCount = 0;
        for (int value = 0; value < histogram.length; value++) {
            cumulativeCount += histogram[value];
            if (cumulativeCount >= targetCount) {
                return value;
            }
        }
        return histogram.length - 1;
    }

    private BufferedImage sharpen(BufferedImage image) {
        ConvolveOp sharpenOperator = new ConvolveOp(
                new Kernel(3, 3, SHARPEN_KERNEL),
                ConvolveOp.EDGE_NO_OP,
                null
        );
        BufferedImage sharpenedImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        sharpenOperator.filter(image, sharpenedImage);
        return sharpenedImage;
    }

    private BufferedImage thresholdImage(BufferedImage grayscaleImage) {
        WritableRaster sourceRaster = grayscaleImage.getRaster();
        int width = grayscaleImage.getWidth();
        int height = grayscaleImage.getHeight();
        int[] histogram = new int[256];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                histogram[sourceRaster.getSample(x, y, 0)]++;
            }
        }

        int threshold = otsuThreshold(histogram, width * height);
        BufferedImage thresholdedImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster targetRaster = thresholdedImage.getRaster();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int value = sourceRaster.getSample(x, y, 0) < threshold ? 0 : 255;
                targetRaster.setSample(x, y, 0, value);
            }
        }
        return thresholdedImage;
    }

    private int otsuThreshold(int[] histogram, int totalPixels) {
        long totalIntensity = 0;
        for (int value = 0; value < histogram.length; value++) {
            totalIntensity += (long) value * histogram[value];
        }

        long backgroundWeight = 0;
        long backgroundIntensity = 0;
        double maxVariance = -1;
        int threshold = 127;
        for (int value = 0; value < histogram.length; value++) {
            backgroundWeight += histogram[value];
            if (backgroundWeight == 0) {
                continue;
            }

            long foregroundWeight = totalPixels - backgroundWeight;
            if (foregroundWeight == 0) {
                break;
            }

            backgroundIntensity += (long) value * histogram[value];
            double backgroundMean = backgroundIntensity / (double) backgroundWeight;
            double foregroundMean = (totalIntensity - backgroundIntensity) / (double) foregroundWeight;
            double betweenClassVariance = backgroundWeight * (double) foregroundWeight
                    * Math.pow(backgroundMean - foregroundMean, 2);
            if (betweenClassVariance > maxVariance) {
                maxVariance = betweenClassVariance;
                threshold = value;
            }
        }
        return threshold;
    }

    private Optional<String> decodeQrText(QrDecodeCandidate candidate) {
        Optional<DecodeAttemptResult> hybridResult = decodeQrText(candidate.image(), true);
        if (hybridResult.isPresent()) {
            log.debug(
                    "CCCD QR candidate decoded by HybridBinarizer/{}: label={}.",
                    hybridResult.get().reader(),
                    candidate.label()
            );
            return Optional.of(hybridResult.get().text());
        }

        Optional<DecodeAttemptResult> globalResult = decodeQrText(candidate.image(), false);
        if (globalResult.isPresent()) {
            log.debug(
                    "CCCD QR candidate decoded by GlobalHistogramBinarizer/{}: label={}.",
                    globalResult.get().reader(),
                    candidate.label()
            );
            return Optional.of(globalResult.get().text());
        }
        return Optional.empty();
    }

    private Optional<DecodeAttemptResult> decodeQrText(BufferedImage image, boolean useHybridBinarizer) {
        Optional<String> directResult = decodeQrText(image, useHybridBinarizer, false);
        if (directResult.isPresent()) {
            return directResult.map(text -> new DecodeAttemptResult("direct", text));
        }

        Optional<String> quadrantResult = decodeQrText(image, useHybridBinarizer, true);
        return quadrantResult.map(text -> new DecodeAttemptResult("quadrant", text));
    }

    private Optional<String> decodeQrText(BufferedImage image, boolean useHybridBinarizer, boolean useQuadrantReader) {
        try {
            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(useHybridBinarizer
                    ? new HybridBinarizer(source)
                    : new GlobalHistogramBinarizer(source)
            );
            Reader reader = useQuadrantReader
                    ? new ByQuadrantReader(new MultiFormatReader())
                    : new MultiFormatReader();
            Result result = reader.decode(bitmap, QR_DECODE_HINTS);
            return Optional.ofNullable(result.getText());
        } catch (ReaderException | RuntimeException ex) {
            return Optional.empty();
        }
    }

    private Optional<CccdExtractedIdentity> parseCccdQr(String payload) {
        if (!isNotBlank(payload)) {
            return Optional.empty();
        }
        String normalizedPayload = payload.trim();
        String[] parts = normalizedPayload.split("\\|", -1);
        if (parts.length < 6) {
            return Optional.empty();
        }

        String idNumber = blankToNull(parts[0]);
        if (!looksLikeIdNumber(idNumber)) {
            return Optional.empty();
        }

        boolean hasOldId = parts.length >= 7 && looksLikeIdNumber(parts[1]);
        String oldIdNumber = hasOldId ? part(parts, 1) : null;
        int offset = hasOldId ? 1 : 0;
        String fullName = part(parts, 1 + offset);
        LocalDate dob = parseDate(part(parts, 2 + offset));
        Gender gender = parseGender(part(parts, 3 + offset));
        String address = part(parts, 4 + offset);
        LocalDate issuedDate = parseDate(part(parts, 5 + offset));

        return Optional.of(new CccdExtractedIdentity(
                normalizedPayload,
                idNumber,
                oldIdNumber,
                fullName,
                dob,
                gender,
                address,
                issuedDate
        ));
    }

    private boolean looksLikeIdNumber(String value) {
        String normalized = blankToNull(value);
        return normalized != null && normalized.matches("\\d{9,12}");
    }

    private LocalDate parseDate(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        List<DateTimeFormatter> formatters = normalized.contains("/")
                ? List.of(SLASH_DATE, COMPACT_DATE)
                : List.of(COMPACT_DATE, SLASH_DATE);
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private Gender parseGender(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.equals("nam") || lower.equals("male") || lower.equals("m")) {
            return Gender.MALE;
        }
        if (lower.equals("nữ") || lower.equals("nu") || lower.equals("female") || lower.equals("f")) {
            return Gender.FEMALE;
        }
        return Gender.OTHER;
    }

    private IdentityVerificationResponse.ExtractedIdentity toResponse(CccdExtractedIdentity data) {
        if (data == null) {
            return null;
        }
        return IdentityVerificationResponse.ExtractedIdentity.builder()
                .idNumber(data.idNumber())
                .fullName(data.fullName())
                .dob(data.dob())
                .gender(data.gender() == null ? null : data.gender().name())
                .address(data.address())
                .issuedDate(data.issuedDate())
                .oldIdNumber(data.oldIdNumber())
                .build();
    }

    private String part(String[] parts, int index) {
        return index >= 0 && index < parts.length ? blankToNull(parts[index]) : null;
    }

    private String blankToNull(String value) {
        return isNotBlank(value) ? value.trim() : null;
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private record OrientedImage(String label, BufferedImage image) {
    }

    private record QrDecodeCandidate(String label, BufferedImage image) {
    }

    private record DecodeAttemptResult(String reader, String text) {
    }

    private record CropRatio(double x, double y, double width, double height) {
    }

}
