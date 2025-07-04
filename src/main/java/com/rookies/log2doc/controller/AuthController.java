package com.rookies.log2doc.controller;

import com.rookies.log2doc.dto.request.LoginRequest;
import com.rookies.log2doc.dto.request.TokenRefreshRequest;
import com.rookies.log2doc.dto.response.JwtResponse;
import com.rookies.log2doc.dto.response.MessageResponse;
import com.rookies.log2doc.entity.RefreshToken;
import com.rookies.log2doc.entity.User;
import com.rookies.log2doc.exception.TokenRefreshException;
import com.rookies.log2doc.repository.RoleRepository;
import com.rookies.log2doc.repository.UserRepository;
import com.rookies.log2doc.security.jwt.JwtUtils;
import com.rookies.log2doc.security.services.UserDetailsImpl;
import com.rookies.log2doc.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 인증 관련 REST API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    
    /**
     * 사용자 로그인
     * @param loginRequest 로그인 요청 정보
     * @param request HTTP 요청 객체
     * @return JWT 토큰 정보
     */
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletRequest request) {
        try {
            // 사용자 인증
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // JWT 토큰 생성
            String jwt = jwtUtils.generateJwtToken(authentication);
            
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());
            
            // 기기 정보 및 IP 주소 추출
            String deviceInfo = getDeviceInfo(request);
            String ipAddress = getClientIpAddress(request);
            
            // 리프레시 토큰 생성
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(
                    userDetails.getId(), deviceInfo, ipAddress
            );
            
            // JWT 토큰 만료 시간 계산
            long expiresIn = jwtUtils.getExpirationTimeFromJwtToken(jwt);
            
            log.info("사용자 로그인 성공: {}, IP: {}", loginRequest.getUsername(), ipAddress);
            
            return ResponseEntity.ok(new JwtResponse(
                    jwt,
                    refreshToken.getToken(),
                    userDetails.getId(),
                    userDetails.getUsername(),
                    userDetails.getEmail(),
                    roles,
                    expiresIn
            ));
            
        } catch (Exception e) {
            log.error("로그인 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("로그인에 실패했습니다: " + e.getMessage(), false));
        }
    }

    /**
     * 토큰 갱신
     * @param request 토큰 갱신 요청
     * @return 새로운 JWT 토큰
     */
    @PostMapping("/refreshtoken")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody TokenRefreshRequest request) {
        try {
            String requestRefreshToken = request.getRefreshToken();
            
            return refreshTokenService.findByToken(requestRefreshToken)
                    .map(refreshTokenService::verifyExpiration)
                    .map(RefreshToken::getUser)
                    .map(user -> {
                        String token = jwtUtils.generateTokenFromUsername(user.getUsername());
                        long expiresIn = jwtUtils.getExpirationTimeFromJwtToken(token);
                        
                        log.info("토큰 갱신 성공: {}", user.getUsername());
                        
                        return ResponseEntity.ok(JwtResponse.builder()
                                .token(token)
                                .refreshToken(requestRefreshToken)
                                .expiresIn(expiresIn)
                                .build());
                    })
                    .orElseThrow(() -> new TokenRefreshException(requestRefreshToken, "리프레시 토큰이 데이터베이스에 없습니다."));
            
        } catch (TokenRefreshException e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    /**
     * 로그아웃
     * @param request 토큰 갱신 요청 (리프레시 토큰 포함)
     * @return 로그아웃 결과 메시지
     */
    @PostMapping("/signout")
    public ResponseEntity<?> logoutUser(@Valid @RequestBody TokenRefreshRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            
            RefreshToken token = refreshTokenService.findByToken(refreshToken)
                    .orElseThrow(() -> new TokenRefreshException(refreshToken, "리프레시 토큰을 찾을 수 없습니다."));
            
            refreshTokenService.deleteRefreshToken(token);
            
            log.info("사용자 로그아웃: {}", token.getUser().getUsername());
            
            return ResponseEntity.ok(new MessageResponse("로그아웃되었습니다."));
            
        } catch (TokenRefreshException e) {
            log.error("로그아웃 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse(e.getMessage(), false));
        }
    }
    
    /**
     * 모든 기기에서 로그아웃
     * @return 로그아웃 결과 메시지
     */
    @PostMapping("/signout-all")
    public ResponseEntity<?> logoutAllDevices() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
                
                refreshTokenService.deleteByUser(user);
                
                log.info("모든 기기에서 로그아웃: {}", username);
                
                return ResponseEntity.ok(new MessageResponse("모든 기기에서 로그아웃되었습니다."));
            }
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new MessageResponse("인증되지 않은 사용자입니다.", false));
            
        } catch (Exception e) {
            log.error("모든 기기 로그아웃 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("로그아웃에 실패했습니다: " + e.getMessage(), false));
        }
    }
    
    /**
     * 기기 정보 추출
     * @param request HTTP 요청 객체
     * @return 기기 정보 문자열
     */
    private String getDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent.substring(0, Math.min(userAgent.length(), 500)) : "Unknown";
    }
    
    /**
     * 클라이언트 IP 주소 추출
     * @param request HTTP 요청 객체
     * @return IP 주소
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}