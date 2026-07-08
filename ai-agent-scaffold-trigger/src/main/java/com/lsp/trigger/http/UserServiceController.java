package com.lsp.trigger.http;

import com.lsp.api.response.Response;
import com.lsp.types.enums.ResponseCode;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/v1/user/")
@CrossOrigin(origins = "*")
public class UserServiceController {

    @PostMapping("register")
    public Response<UserInfoResponse> register(@RequestBody RegisterRequest request) {
        log.info("user register username:{}", request.getUsername());

        UserInfoResponse response = UserInfoResponse.builder()
                .userId("mock-user-id")
                .username(request.getUsername())
                .nickname(request.getNickname())
                .status("NORMAL")
                .createTime(LocalDateTime.now())
                .build();

        return success(response);
    }

    @PostMapping("login")
    public Response<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("user login username:{}", request.getUsername());

        LoginResponse response = LoginResponse.builder()
                .userId("mock-user-id")
                .username(request.getUsername())
                .accessToken("mock-access-token")
                .expireSeconds(7200L)
                .build();

        return success(response);
    }

    @GetMapping("query_user_info/{userId}")
    public Response<UserInfoResponse> queryUserInfo(@PathVariable("userId") String userId) {
        log.info("query user info userId:{}", userId);

        UserInfoResponse response = UserInfoResponse.builder()
                .userId(userId)
                .username("mock-user")
                .nickname("DrawIO User")
                .status("NORMAL")
                .createTime(LocalDateTime.now())
                .build();

        return success(response);
    }

    @GetMapping("query_credit_account/{userId}")
    public Response<CreditAccountResponse> queryCreditAccount(@PathVariable("userId") String userId) {
        log.info("query credit account userId:{}", userId);

        CreditAccountResponse response = CreditAccountResponse.builder()
                .userId(userId)
                .availableCredits(BigDecimal.ZERO)
                .frozenCredits(BigDecimal.ZERO)
                .totalGrantedCredits(BigDecimal.ZERO)
                .totalUsedCredits(BigDecimal.ZERO)
                .status("NORMAL")
                .build();

        return success(response);
    }

    private <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String nickname;
        private String mobile;
        private String email;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    @Builder
    public static class LoginResponse {
        private String userId;
        private String username;
        private String accessToken;
        private Long expireSeconds;
    }

    @Data
    @Builder
    public static class UserInfoResponse {
        private String userId;
        private String username;
        private String nickname;
        private String status;
        private LocalDateTime createTime;
    }

    @Data
    @Builder
    public static class CreditAccountResponse {
        private String userId;
        private BigDecimal availableCredits;
        private BigDecimal frozenCredits;
        private BigDecimal totalGrantedCredits;
        private BigDecimal totalUsedCredits;
        private String status;
    }

}
