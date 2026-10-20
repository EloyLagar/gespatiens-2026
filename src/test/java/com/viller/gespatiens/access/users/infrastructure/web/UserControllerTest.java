package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.ChangeOwnPasswordUseCase;
import com.viller.gespatiens.access.users.application.GetCurrentUserUseCase;
import com.viller.gespatiens.access.users.application.GetUserUseCase;
import com.viller.gespatiens.access.users.application.ListUsersUseCase;
import com.viller.gespatiens.access.users.application.RegisterUserUseCase;
import com.viller.gespatiens.access.users.application.UpdateOwnProfileUseCase;
import com.viller.gespatiens.access.users.domain.EmailAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.InvalidCurrentPasswordException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Levanta solo la capa web + seguridad, con el caso de uso simulado (no necesita PostgreSQL)
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    private static final String VALID_BODY = """
            {"name":"Ana","email":"ana@vientos.es","phoneNumber":"600111222",
             "speciality":"PSYCHOLOGIST","password":"secreto123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @MockitoBean
    private ListUsersUseCase listUsersUseCase;

    @MockitoBean
    private GetUserUseCase getUserUseCase;

    @MockitoBean
    private GetCurrentUserUseCase getCurrentUserUseCase;

    @MockitoBean
    private UpdateOwnProfileUseCase updateOwnProfileUseCase;

    @MockitoBean
    private ChangeOwnPasswordUseCase changeOwnPasswordUseCase;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    void rejectsAnonymousRequest() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "SOCIAL_EDUCATOR")
    void rejectsNonAdministrator() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administratorRegistersUserWithoutExposingPassword() throws Exception {
        User user = User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash");
        when(registerUserUseCase.execute(any(), any(), any(), any(), any())).thenReturn(user);

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.speciality").value("PSYCHOLOGIST"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void duplicateEmailReturnsConflict() throws Exception {
        when(registerUserUseCase.execute(any(), any(), any(), any(), any()))
                .thenThrow(new EmailAlreadyRegisteredException("ana@vientos.es"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void invalidDataReturnsBadRequest() throws Exception {
        when(registerUserUseCase.execute(any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Email inválido"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Email inválido"));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void unknownSpecialityReturnsBadRequest() throws Exception {
        String body = VALID_BODY.replace("PSYCHOLOGIST", "ASTRONAUT");

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    private static User ana() {
        return User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash");
    }

    @Test
    @WithMockUser(roles = "SOCIAL_EDUCATOR")
    void anyEmployeeListsUsersWithoutPasswords() throws Exception {
        when(listUsersUseCase.execute()).thenReturn(List.of(ana()));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("ana@vientos.es"))
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(username = "ana@vientos.es")
    void meReturnsAuthenticatedUser() throws Exception {
        when(getCurrentUserUseCase.execute("ana@vientos.es")).thenReturn(ana());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@vientos.es"))
                .andExpect(jsonPath("$.speciality").value("PSYCHOLOGIST"));
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void unknownUserReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(getUserUseCase.execute(id)).thenThrow(new UserNotFoundException(id));

        mockMvc.perform(get("/api/users/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "ana@vientos.es", roles = "PSYCHOLOGIST")
    void employeeUpdatesOwnProfile() throws Exception {
        when(updateOwnProfileUseCase.execute("ana@vientos.es", "Ana María", "600999888")).thenReturn(ana());

        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana María\",\"phoneNumber\":\"600999888\"}"))
                .andExpect(status().isOk());
        verify(updateOwnProfileUseCase).execute("ana@vientos.es", "Ana María", "600999888");
    }

    @Test
    @WithMockUser(username = "ana@vientos.es", roles = "PSYCHOLOGIST")
    void employeeChangesOwnPassword() throws Exception {
        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"antigua123\",\"newPassword\":\"nueva12345\"}"))
                .andExpect(status().isNoContent());
        verify(changeOwnPasswordUseCase).execute("ana@vientos.es", "antigua123", "nueva12345");
    }

    @Test
    @WithMockUser(username = "ana@vientos.es")
    void wrongCurrentPasswordReturnsBadRequest() throws Exception {
        doThrow(new InvalidCurrentPasswordException())
                .when(changeOwnPasswordUseCase).execute(any(), any(), any());

        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"mal\",\"newPassword\":\"nueva12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La contraseña actual no es correcta"));
    }
}
