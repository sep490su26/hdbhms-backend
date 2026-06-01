//package com.sep490.hdbhms.file.infrastructure.adapter;
//
//import com.sep490.hdbhms.file.application.port.in.usecase.UploadFileUseCase;
//import com.sep490.hdbhms.file.infrastructure.web.dto.response.FileResponse;
//import lombok.AccessLevel;
//import lombok.RequiredArgsConstructor;
//import lombok.experimental.FieldDefaults;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//import org.springframework.web.multipart.MultipartFile;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
//public class UserProfileUploadAvatarAdapter implements UploadAvatarPort {
//    UploadFileUseCase uploadFileUseCase;
//
//    @Override
//    public FileResponse uploadAvatar(MultipartFile file) {
////        var uploaderUuid = AuthUtils.getCurrentAuthenticationUuid();
////        if (StringUtils.isEmpty(uploaderUuid)) {
////            throw new AppException(ApiErrorCode.UNAUTHENTICATED);
////        }
////        try {
////            return uploadFileUseCase.execute(new UploadFileCommand(uploaderUuid, file));
////        } catch (AppException e) {
////            log.error(e.getMessage());
////            return null;
////        }
//        return null;
//    }
//}
