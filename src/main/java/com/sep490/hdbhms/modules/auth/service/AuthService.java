package com.sep490.hdbhms.modules.auth.service;

import com.sep490.hdbhms.common.AuditService;
import com.sep490.hdbhms.common.exception.ApiException;
import com.sep490.hdbhms.modules.auth.dto.ChangePasswordRequest;
import com.sep490.hdbhms.modules.auth.dto.ChangePasswordResponse;
import com.sep490.hdbhms.modules.auth.dto.LoginRequest;
import com.sep490.hdbhms.modules.auth.dto.LoginResponse;
import com.sep490.hdbhms.modules.auth.dto.OnboardingStateResponse;
import com.sep490.hdbhms.modules.tenant.repository.TenantMembershipRepository;
import com.sep490.hdbhms.modules.user.entity.User;
import com.sep490.hdbhms.modules.user.entity.UserStatus;
import com.sep490.hdbhms.modules.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String INVALID_LOGIN_MESSAGE = "Sai thông tin đăng nhập";

    private final UserRepository userRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OnboardingService onboardingService;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            TenantMembershipRepository tenantMembershipRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OnboardingService onboardingService,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.tenantMembershipRepository = tenantMembershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.onboardingService = onboardingService;
        this.auditService = auditService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String phoneOrEmail = request.phoneOrEmail().trim();
        User user = userRepository.findByPhoneOrEmail(phoneOrEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, INVALID_LOGIN_MESSAGE));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, INVALID_LOGIN_MESSAGE);
        }

        validateLoginStatus(user.getStatus());

        List<LoginResponse.TenantInfo> tenants = tenantMembershipRepository.findActiveLoginTenants(user.getId())
                .stream()
                .map(item -> new LoginResponse.TenantInfo(
                        item.getTenantId(),
                        item.getTenantName(),
                        item.getRole(),
                        item.getPropertyId()
                ))
                .toList();

        user.setLastLoginAt(LocalDateTime.now());

        String accessToken = jwtService.createAccessToken(user, tenants);
        String refreshToken = jwtService.createRefreshToken(user);
        OnboardingStateResponse onboarding = onboardingService.resolve(user);
        auditService.record(user.getId(), "LOGIN_SUCCESS", "USER", user.getId());

        return new LoginResponse(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpiresInSeconds(),
                new LoginResponse.UserInfo(
                        user.getId(),
                        resolveFullName(user),
                        user.getPhone(),
                        user.getEmail(),
                        user.getStatus().name(),
                        user.isMustChangePassword(),
                        onboarding.identityCompleted()
                ),
                tenants,
                onboarding
        );
    }

    @Transactional
    public ChangePasswordResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ"));

        validateNewPassword(request);

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới không được trùng mật khẩu cũ");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        auditService.record(user.getId(), "PASSWORD_CHANGED", "USER", user.getId());

        return new ChangePasswordResponse(
                "Đổi mật khẩu thành công",
                onboardingService.resolve(user)
        );
    }

    public OnboardingStateResponse getOnboarding(Long userId) {
        User user = userRepository.findById(userId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ"));
        return onboardingService.resolve(user);
    }

    private void validateLoginStatus(UserStatus status) {
        if (status == UserStatus.ACTIVE) {
            return;
        }
        if (status == UserStatus.PENDING_APPROVAL || status == UserStatus.PENDING_CONTRACT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đang chờ duyệt");
        }
        if (status == UserStatus.DISABLED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị vô hiệu hóa");
        }
        if (status == UserStatus.REJECTED) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản đã bị từ chối");
        }
        throw new ApiException(HttpStatus.FORBIDDEN, "Tài khoản không được phép đăng nhập");
    }

    private String resolveFullName(User user) {
        return onboardingService.resolveFullName(user);
    }

    private void validateNewPassword(ChangePasswordRequest request) {
        if (request.newPassword().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải có ít nhất 8 ký tự");
        }
        if (!request.newPassword().matches(".*[A-Za-z].*") || !request.newPassword().matches(".*\\d.*")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Mật khẩu mới phải có chữ và số");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Xác nhận mật khẩu không khớp");
        }
    }
}
