package com.viller.gespatiens.access.users.infrastructure.security;

import com.viller.gespatiens.access.users.application.AccessTokenIssuer;
import com.viller.gespatiens.access.users.application.ChangeOwnLanguageUseCase;
import com.viller.gespatiens.access.users.application.ChangeOwnPasswordUseCase;
import com.viller.gespatiens.access.users.application.DeleteUserUseCase;
import com.viller.gespatiens.access.users.application.EditUserUseCase;
import com.viller.gespatiens.access.users.application.GetUserUseCase;
import com.viller.gespatiens.access.users.application.ListUsersUseCase;
import com.viller.gespatiens.access.users.application.LoginUseCase;
import com.viller.gespatiens.access.users.application.InviteUserUseCase;
import com.viller.gespatiens.access.users.application.ResetUserPasswordUseCase;
import com.viller.gespatiens.access.users.application.SendPasswordSetupLinkUseCase;
import com.viller.gespatiens.access.users.application.UpdateOwnProfileUseCase;
import com.viller.gespatiens.access.users.domain.Language;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserRepository;
import com.viller.gespatiens.access.users.infrastructure.web.AuthController;
import com.viller.gespatiens.access.users.infrastructure.web.UserController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// La cadena de seguridad real: tokens emitidos de verdad por JwtAccessTokenIssuer y validados
// por el JwtDecoder y el CurrentUserJwtConverter de la aplicación. Solo se simulan la BD y los casos de uso
@WebMvcTest({UserController.class, AuthController.class})
@Import({SecurityConfig.class, JwtConfig.class, JwtAccessTokenIssuer.class, CurrentUserJwtConverter.class})
@TestPropertySource(properties = {
        "gespatiens.jwt.secret=un-secreto-de-pruebas-de-al-menos-32-caracteres",
        "gespatiens.jwt.expiration=PT1H"})
class JwtSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccessTokenIssuer accessTokenIssuer;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private LoginUseCase loginUseCase;
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
    private SendPasswordSetupLinkUseCase sendPasswordSetupLinkUseCase;

    private final User ana = User.create("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST, "hash");

    private String bearer(User user) {
        return "Bearer " + accessTokenIssuer.issueFor(user).value();
    }

    @Test
    void validTokenAuthenticatesTheEmployee() throws Exception {
        when(userRepository.findById(ana.getId())).thenReturn(Optional.of(ana));
        when(listUsersUseCase.execute()).thenReturn(List.of(ana));

        mockMvc.perform(get("/api/users").header("Authorization", bearer(ana)))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithoutTokenIsRejectedWithoutBrowserLoginPopup() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                // "Bearer", no "Basic": con Basic el navegador abriría su propia ventana de login
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")));
    }

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String tampered = bearer(ana) + "x";

        mockMvc.perform(get("/api/users").header("Authorization", tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenOfDeletedEmployeeStopsWorkingImmediately() throws Exception {
        when(userRepository.findById(ana.getId())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users").header("Authorization", bearer(ana)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleComesFromTheDatabaseNotFromTheToken() throws Exception {
        // El token se emitió cuando Ana era administradora...
        User anaAdmin = User.reconstitute(ana.getId(), "Ana", "ana@vientos.es", "600111222",
                Speciality.ADMINISTRATOR, "hash", Language.ES);
        String token = bearer(anaAdmin);
        // ...pero la administración le ha quitado el rol: ya no puede dar de alta empleados
        when(userRepository.findById(ana.getId())).thenReturn(Optional.of(ana));

        mockMvc.perform(post("/api/users").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginIsPublic() throws Exception {
        when(loginUseCase.execute(any(), any()))
                .thenReturn(new LoginUseCase.Session(ana, accessTokenIssuer.issueFor(ana)));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@vientos.es\",\"password\":\"secreto123\"}"))
                .andExpect(status().isOk());
    }
}
