package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.ChangeOwnPasswordUseCase;
import com.viller.gespatiens.access.users.application.GetCurrentUserUseCase;
import com.viller.gespatiens.access.users.application.GetUserUseCase;
import com.viller.gespatiens.access.users.application.ListUsersUseCase;
import com.viller.gespatiens.access.users.application.RegisterUserUseCase;
import com.viller.gespatiens.access.users.application.UpdateOwnProfileUseCase;
import com.viller.gespatiens.access.users.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserUseCase getUserUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UpdateOwnProfileUseCase updateOwnProfileUseCase;
    private final ChangeOwnPasswordUseCase changeOwnPasswordUseCase;

    public UserController(RegisterUserUseCase registerUserUseCase,
                          ListUsersUseCase listUsersUseCase,
                          GetUserUseCase getUserUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase,
                          UpdateOwnProfileUseCase updateOwnProfileUseCase,
                          ChangeOwnPasswordUseCase changeOwnPasswordUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.getUserUseCase = getUserUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.updateOwnProfileUseCase = updateOwnProfileUseCase;
        this.changeOwnPasswordUseCase = changeOwnPasswordUseCase;
    }

    @GetMapping
    public List<UserResponse> list() {
        return listUsersUseCase.execute().stream()
                .map(UserResponse::from)
                .toList();
    }

    // El empleado autenticado. "/me" es un literal, así que Spring lo prefiere a "/{id}"
    @GetMapping("/me")
    public UserResponse me(Principal principal) {
        return UserResponse.from(getCurrentUserUseCase.execute(principal.getName()));
    }

    // RF4: cada empleado modifica sus propios datos, salvo el email
    @PutMapping("/me")
    public UserResponse updateMe(Principal principal, @RequestBody UpdateOwnProfileRequest request) {
        User user = updateOwnProfileUseCase.execute(principal.getName(), request.name(), request.phoneNumber());
        return UserResponse.from(user);
    }

    // RF4: y sus credenciales de acceso
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeMyPassword(Principal principal, @RequestBody ChangeOwnPasswordRequest request) {
        changeOwnPasswordUseCase.execute(principal.getName(), request.currentPassword(), request.newPassword());
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable UUID id) {
        return UserResponse.from(getUserUseCase.execute(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@RequestBody RegisterUserRequest request) {
        User user = registerUserUseCase.execute(
                request.name(),
                request.email(),
                request.phoneNumber(),
                request.speciality(),
                request.password());
        return UserResponse.from(user);
    }
}
