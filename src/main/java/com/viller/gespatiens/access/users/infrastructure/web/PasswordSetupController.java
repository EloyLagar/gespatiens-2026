package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.PasswordSetupUseCase;
import com.viller.gespatiens.access.users.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// La pantalla /crear-contrasena del enlace del correo. Sin token de acceso: el del enlace hace de credencial
@RestController
@RequestMapping("/api/auth/password-setup")
public class PasswordSetupController {

    private final PasswordSetupUseCase passwordSetupUseCase;
    private final LoginOriginResolver originResolver;

    public PasswordSetupController(PasswordSetupUseCase passwordSetupUseCase, LoginOriginResolver originResolver) {
        this.passwordSetupUseCase = passwordSetupUseCase;
        this.originResolver = originResolver;
    }

    // Lo mínimo para saludarle y rellenar su email en el login: nada del resto del perfil
    @GetMapping("/{token}")
    public SetupAccount check(@PathVariable String token) {
        return SetupAccount.from(passwordSetupUseCase.check(token));
    }

    @PostMapping("/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@PathVariable String token, @RequestBody ResetPasswordRequest request,
                         HttpServletRequest http) {
        passwordSetupUseCase.complete(token, request.newPassword(), originResolver.resolve(http));
    }

    public record SetupAccount(String name, String email) {
        static SetupAccount from(User user) {
            return new SetupAccount(user.getName(), user.getEmail());
        }
    }
}
