package com.advancedswiftly.pawfit.auth.application;

import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.LoginRequest;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.LoginResponse;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.LogoutRequest;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.RefreshResponse;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.SignupRequest;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.SignupResponse;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.TermAgreementRequest;
import com.advancedswiftly.pawfit.auth.api.dto.AuthDtos.UserSummary;
import com.advancedswiftly.pawfit.auth.domain.Term;
import com.advancedswiftly.pawfit.auth.domain.User;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import com.advancedswiftly.pawfit.auth.domain.UserTermAgreement;
import com.advancedswiftly.pawfit.auth.infrastructure.TermRepository;
import com.advancedswiftly.pawfit.auth.infrastructure.UserRepository;
import com.advancedswiftly.pawfit.auth.infrastructure.UserTermAgreementRepository;
import com.advancedswiftly.pawfit.global.error.BusinessException;
import com.advancedswiftly.pawfit.global.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final UserTermAgreementRepository agreementRepository;
    private final PhoneVerificationService phoneVerificationService;
    private final TokenService tokenService;
    private final PhoneNumberNormalizer phoneNumberNormalizer;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock = Clock.systemUTC();

    public AuthService(
            UserRepository userRepository,
            TermRepository termRepository,
            UserTermAgreementRepository agreementRepository,
            PhoneVerificationService phoneVerificationService,
            TokenService tokenService,
            PhoneNumberNormalizer phoneNumberNormalizer,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.termRepository = termRepository;
        this.agreementRepository = agreementRepository;
        this.phoneVerificationService = phoneVerificationService;
        this.tokenService = tokenService;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request, UserRole role) {
        if (role != UserRole.GUARDIAN && role != UserRole.BUSINESS) {
            throw new BusinessException(ErrorCode.INVALID_ROLE);
        }
        String phoneNumber = phoneVerificationService.consumeSignupToken(
                request.phoneVerificationToken());
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        AgreementSelection selection = validateAgreements(role, request.termAgreements());
        User user = User.register(
                phoneNumber,
                passwordEncoder.encode(request.password()),
                request.name().trim(),
                role);
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.DUPLICATE_PHONE_NUMBER);
        }

        Instant now = clock.instant();
        agreementRepository.saveAll(selection.terms().stream()
                .map(term -> UserTermAgreement.record(
                        user, term, selection.agreedByTermId().get(term.getId()), now))
                .toList());
        TokenService.IssuedTokens tokens = tokenService.issue(user, null);
        return new SignupResponse(
                user.getId(), role, tokens.accessToken(), tokens.refreshToken());
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String phoneNumber = phoneNumberNormalizer.normalize(request.phoneNumber());
        User user = userRepository.findByPhoneNumber(phoneNumber)
                .filter(User::isActive)
                .filter(value -> passwordEncoder.matches(request.password(), value.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        TokenService.IssuedTokens tokens = tokenService.issue(user, request.deviceToken());
        return new LoginResponse(
                new UserSummary(user.getId(), user.getRole(), user.getName()),
                tokens.accessToken(),
                tokens.refreshToken());
    }

    @Transactional
    public RefreshResponse refresh(String refreshToken) {
        TokenService.IssuedTokens tokens = tokenService.rotate(refreshToken);
        return new RefreshResponse(tokens.accessToken(), tokens.refreshToken());
    }

    @Transactional
    public void logout(Long userId, LogoutRequest request) {
        tokenService.revoke(userId, request.refreshToken(), request.deviceToken());
    }

    private AgreementSelection validateAgreements(
            UserRole role, List<TermAgreementRequest> agreements) {
        Map<Long, Boolean> agreedByTermId = new HashMap<>();
        agreements.forEach(agreement -> agreedByTermId.put(agreement.termId(), agreement.agreed()));
        if (agreedByTermId.size() != agreements.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "중복된 약관이 포함되어 있습니다.");
        }

        List<Term> activeTerms = termRepository.findAllByRoleAndActiveTrueOrderById(role);
        Set<Long> activeTermIds = activeTerms.stream().map(Term::getId).collect(Collectors.toSet());
        if (!activeTermIds.containsAll(agreedByTermId.keySet())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "가입 역할에 맞지 않는 약관이 포함되어 있습니다.");
        }
        boolean requiredTermsAgreed = activeTerms.stream()
                .filter(Term::isRequired)
                .allMatch(term -> Boolean.TRUE.equals(agreedByTermId.get(term.getId())));
        if (!requiredTermsAgreed) {
            throw new BusinessException(ErrorCode.REQUIRED_TERM_NOT_AGREED);
        }

        List<Term> selectedTerms = activeTerms.stream()
                .filter(term -> agreedByTermId.containsKey(term.getId()))
                .toList();
        return new AgreementSelection(selectedTerms, agreedByTermId);
    }

    private record AgreementSelection(List<Term> terms, Map<Long, Boolean> agreedByTermId) {
    }
}
