package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.ChangeOwnLanguageUseCase;
import com.viller.gespatiens.access.users.application.ChangeOwnPasswordUseCase;
import com.viller.gespatiens.access.users.application.ChangeOwnThemeUseCase;
import com.viller.gespatiens.access.users.application.DeleteUserUseCase;
import com.viller.gespatiens.access.users.application.EditUserUseCase;
import com.viller.gespatiens.access.users.application.GetUserUseCase;
import com.viller.gespatiens.access.users.application.ListUsersUseCase;
import com.viller.gespatiens.access.users.application.InviteUserUseCase;
import com.viller.gespatiens.access.users.application.MarkOwnTutorialSeenUseCase;
import com.viller.gespatiens.access.users.application.PasswordSetupDeliveryException;
import com.viller.gespatiens.access.users.application.ResetUserPasswordUseCase;
import com.viller.gespatiens.access.users.application.SendPasswordSetupLinkUseCase;
import com.viller.gespatiens.access.users.application.UpdateOwnProfileUseCase;
import com.viller.gespatiens.access.users.domain.EmailAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.InvalidCurrentPasswordException;
import com.viller.gespatiens.access.users.domain.LastAdministratorException;
import com.viller.gespatiens.access.users.domain.Language;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.Theme;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Levanta solo la capa web + seguridad, con el caso de uso simulado (no necesita PostgreSQL)
@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    // Con JWT, el "username" de la autenticación es el id del empleado
    private static final String ME = "7f3c2a10-0000-4000-8000-0000000000aa";
    private static final UUID ME_ID = UUID.fromString(ME);

    private static final String VALID_BODY = """
            {"name":"Ana","email":"ana@vientos.es","phoneNumber":"600111222",
             "speciality":"PSYCHOLOGIST"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InviteUserUseCase inviteUserUseCase;

    @MockitoBean
    private ListUsersUseCase listUsersUseCase;

    @MockitoBean
    private GetUserUseCase getUserUseCase;

    @MockitoBean
    private UpdateOwnProfileUseCase updateOwnProfileUseCase;

    @MockitoBean
    private ChangeOwnPasswordUseCase changeOwnPasswordUseCase;

    @MockitoBean
    private EditUserUseCase editUserUseCase;

    @MockitoBean
    private ResetUserPasswordUseCase resetUserPasswordUseCase;

    @MockitoBean
    private DeleteUserUseCase deleteUserUseCase;

    @MockitoBean
    private ChangeOwnLanguageUseCase changeOwnLanguageUseCase;

    @MockitoBean
    private MarkOwnTutorialSeenUseCase markOwnTutorialSeenUseCase;

    @MockitoBean
    private ChangeOwnThemeUseCase changeOwnThemeUseCase;

    @MockitoBean
    private SendPasswordSetupLinkUseCase sendPasswordSetupLinkUseCase;

    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

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
    void administratorInvitesUserWhoReceivesTheEmail() throws Exception {
        User user = User.invite("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST);
        when(inviteUserUseCase.execute("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST))
                .thenReturn(new InviteUserUseCase.Invitation(user, true));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailSent").value(true))
                .andExpect(jsonPath("$.user.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.user.speciality").value("PSYCHOLOGIST"))
                .andExpect(jsonPath("$.user.passwordPending").value(true))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void invitationSaysWhenTheEmailCouldNotBeSent() throws Exception {
        User user = User.invite("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST);
        when(inviteUserUseCase.execute(any(), any(), any(), any()))
                .thenReturn(new InviteUserUseCase.Invitation(user, false));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.emailSent").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administratorResendsThePasswordSetupEmail() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(post("/api/users/{id}/password-setup", id))
                .andExpect(status().isNoContent());
        verify(sendPasswordSetupLinkUseCase).execute(id);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void failedEmailReturnsBadGateway() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new PasswordSetupDeliveryException("ana@vientos.es", new RuntimeException("SMTP caído")))
                .when(sendPasswordSetupLinkUseCase).execute(id);

        mockMvc.perform(post("/api/users/{id}/password-setup", id))
                .andExpect(status().isBadGateway());
    }

    @Test
    @WithMockUser(roles = "SOCIAL_EDUCATOR")
    void onlyAdministrationResendsThePasswordSetupEmail() throws Exception {
        mockMvc.perform(post("/api/users/{id}/password-setup", UUID.randomUUID()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(sendPasswordSetupLinkUseCase);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void duplicateEmailReturnsConflict() throws Exception {
        when(inviteUserUseCase.execute(any(), any(), any(), any()))
                .thenThrow(new EmailAlreadyRegisteredException("ana@vientos.es"));

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void invalidDataReturnsBadRequest() throws Exception {
        when(inviteUserUseCase.execute(any(), any(), any(), any()))
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
    @WithMockUser(username = ME)
    void meReturnsAuthenticatedUser() throws Exception {
        when(getUserUseCase.execute(ME_ID)).thenReturn(ana());

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
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void employeeUpdatesOwnProfile() throws Exception {
        when(updateOwnProfileUseCase.execute(ME_ID, "Ana María", "600999888")).thenReturn(ana());

        mockMvc.perform(put("/api/users/me").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana María\",\"phoneNumber\":\"600999888\"}"))
                .andExpect(status().isOk());
        verify(updateOwnProfileUseCase).execute(ME_ID, "Ana María", "600999888");
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void employeeChangesOwnPassword() throws Exception {
        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"antigua123\",\"newPassword\":\"nueva12345\"}"))
                .andExpect(status().isNoContent());
        verify(changeOwnPasswordUseCase).execute(ME_ID, "antigua123", "nueva12345");
    }

    @Test
    @WithMockUser(username = ME)
    void wrongCurrentPasswordReturnsBadRequest() throws Exception {
        doThrow(new InvalidCurrentPasswordException())
                .when(changeOwnPasswordUseCase).execute(any(), any(), any());

        mockMvc.perform(put("/api/users/me/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"mal\",\"newPassword\":\"nueva12345\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La contraseña actual no es correcta"));
    }

    private static final String EDIT_BODY = """
            {"name":"Ana","email":"ana@vientos.es","phoneNumber":"600111222","speciality":"SOCIAL_WORKER"}
            """;

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administratorEditsAnyProfile() throws Exception {
        UUID id = UUID.randomUUID();
        when(editUserUseCase.execute(id, "Ana", "ana@vientos.es", "600111222", Speciality.SOCIAL_WORKER))
                .thenReturn(ana());

        mockMvc.perform(put("/api/users/{id}", id).contentType(MediaType.APPLICATION_JSON).content(EDIT_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "PSYCHOLOGIST")
    void nonAdministratorCannotEditOthersResetPasswordsOrDelete() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/users/{id}", id).contentType(MediaType.APPLICATION_JSON).content(EDIT_BODY))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/{id}/password", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"provisional1\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isForbidden());
        verifyNoInteractions(editUserUseCase, resetUserPasswordUseCase, deleteUserUseCase);
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void administratorResetsPassword() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(put("/api/users/{id}/password", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"provisional1\"}"))
                .andExpect(status().isNoContent());
        verify(resetUserPasswordUseCase).execute(id, "provisional1");
    }

    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    void deletingLastAdministratorReturnsConflict() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new LastAdministratorException()).when(deleteUserUseCase).execute(id);

        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isConflict());
    }

    // RNF9: cada empleado guarda su idioma en el perfil
    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void eachEmployeeSavesTheirLanguage() throws Exception {
        User user = User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash");
        user.changeLanguage(Language.EN);
        when(changeOwnLanguageUseCase.execute(ME_ID, Language.EN)).thenReturn(user);

        mockMvc.perform(put("/api/users/me/language").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"language\":\"EN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("EN"));
    }

    // El tema se guarda en el perfil, como el idioma
    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void eachEmployeeSavesTheirTheme() throws Exception {
        User user = User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash");
        user.changeTheme(Theme.DARK);
        when(changeOwnThemeUseCase.execute(ME_ID, Theme.DARK)).thenReturn(user);

        mockMvc.perform(put("/api/users/me/theme").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"theme\":\"DARK\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.theme").value("DARK"));
    }

    // El tutorial de bienvenida se marca como visto en el perfil: no vuelve a salir en otro equipo
    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void eachEmployeeMarksTheirTutorialAsSeen() throws Exception {
        User user = User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash");
        user.markTutorialSeen();
        when(markOwnTutorialSeenUseCase.execute(ME_ID)).thenReturn(user);

        mockMvc.perform(put("/api/users/me/tutorial"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorialSeen").value(true));
    }

    @Test
    void markingTheTutorialNeedsASession() throws Exception {
        mockMvc.perform(put("/api/users/me/tutorial"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(markOwnTutorialSeenUseCase);
    }
}
