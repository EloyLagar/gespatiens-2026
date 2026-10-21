package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.InvalidCredentialsException;
import com.viller.gespatiens.access.users.application.LoginBlockedException;
import com.viller.gespatiens.access.users.application.PasswordSetupDeliveryException;
import com.viller.gespatiens.access.users.domain.EmailAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.InvalidCurrentPasswordException;
import com.viller.gespatiens.access.users.domain.InvalidPasswordSetupTokenException;
import com.viller.gespatiens.access.users.domain.LastAdministratorException;
import com.viller.gespatiens.access.users.domain.PhoneNumberAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.SignatureNotFoundException;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Traduce las excepciones de dominio de usuarios a respuestas HTTP (formato estándar ProblemDetail, RFC 9457).
// Los errores genéricos (400) están en GlobalExceptionHandler
@RestControllerAdvice
public class UserExceptionHandler {

    @ExceptionHandler({EmailAlreadyRegisteredException.class, PhoneNumberAlreadyRegisteredException.class,
            LastAdministratorException.class})
    public ProblemDetail handleDuplicate(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    public ProblemDetail handleInvalidCurrentPassword(InvalidCurrentPasswordException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // 429: el email está bloqueado por demasiados intentos fallidos. retryAfter dice hasta cuándo
    @ExceptionHandler(LoginBlockedException.class)
    public ProblemDetail handleLoginBlocked(LoginBlockedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
        problem.setProperty("retryAfter", ex.getUntil());
        return problem;
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({UserNotFoundException.class, SignatureNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    // 410: el enlace existió pero ya no sirve (usado, sustituido o caducado). Tampoco se distingue de uno
    // inventado, para no dar pistas
    @ExceptionHandler(InvalidPasswordSetupTokenException.class)
    public ProblemDetail handleInvalidSetupToken(InvalidPasswordSetupTokenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.GONE, ex.getMessage());
    }

    // 502: el fallo es del servidor de correo, no de la petición
    @ExceptionHandler(PasswordSetupDeliveryException.class)
    public ProblemDetail handleDelivery(PasswordSetupDeliveryException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY, ex.getMessage());
    }
}
