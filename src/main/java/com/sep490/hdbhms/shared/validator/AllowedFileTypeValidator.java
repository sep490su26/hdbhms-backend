package com.sep490.hdbhms.shared.validator;

import com.sep490.hdbhms.shared.utils.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class AllowedFileTypeValidator implements ConstraintValidator<AllowedFileType, MultipartFile> {
    private Set<String> allowedMimeTypes;
    private Set<String> allowedExtensions;
    private final Tika tika = new Tika();

    @Override
    public void initialize(AllowedFileType constraintAnnotation) {
        allowedMimeTypes = Arrays.stream(constraintAnnotation.allowedMimeTypes())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        allowedExtensions = Arrays.stream(constraintAnnotation.allowedExtensions())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true;
        }
        var mimeType = file.getContentType();
        if (mimeType != null && !allowedMimeTypes.contains(mimeType)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Unsupported MIME type: " + mimeType)
                    .addConstraintViolation();
            return false;
        }
        var originalFilename = file.getOriginalFilename();
        if (!StringUtils.isEmpty(originalFilename) && originalFilename.contains(".")) {
            var extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            if (!allowedExtensions.contains(extension)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Unsupported file extension: " + extension)
                        .addConstraintViolation();
                return false;
            }
        } else {
            if (!allowedExtensions.isEmpty()) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("File has no extension")
                        .addConstraintViolation();
                return false;
            }
        }
        try (var is = file.getInputStream()) {
            var detectedType = tika.detect(is);
            if (!allowedMimeTypes.contains(detectedType.toLowerCase())) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate("Unsupported MIME type in file content: " + detectedType)
                        .addConstraintViolation();
                return false;
            }
        } catch (IOException e) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Could not read the file content")
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}
