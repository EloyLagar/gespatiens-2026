package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.GetCurrentUserUseCase;
import com.viller.gespatiens.access.users.application.GetUserUseCase;
import com.viller.gespatiens.access.users.application.ListUsersUseCase;
import com.viller.gespatiens.access.users.application.RegisterUserUseCase;
import com.viller.gespatiens.access.users.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    public UserController(RegisterUserUseCase registerUserUseCase,
                          ListUsersUseCase listUsersUseCase,
                          GetUserUseCase getUserUseCase,
                          GetCurrentUserUseCase getCurrentUserUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.listUsersUseCase = listUsersUseCase;
        this.getUserUseCase = getUserUseCase;
        this.getCurrentUserUseCase = getCurrentUserUseCase;
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
