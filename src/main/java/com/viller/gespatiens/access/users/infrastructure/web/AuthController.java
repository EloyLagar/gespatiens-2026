package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.LoginUseCase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LoginOriginResolver originResolver;

    public AuthController(LoginUseCase loginUseCase, LoginOriginResolver originResolver) {
        this.loginUseCase = loginUseCase;
        this.originResolver = originResolver;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request, HttpServletRequest http) {
        return LoginResponse.from(loginUseCase.execute(request.email(), request.password(),
                originResolver.resolve(http)));
    }
}
