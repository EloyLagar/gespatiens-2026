package com.viller.gespatiens.notices.infrastructure.web;

import com.viller.gespatiens.access.users.infrastructure.security.CurrentUserJwtConverter;
import com.viller.gespatiens.access.users.infrastructure.security.SecurityConfig;
import com.viller.gespatiens.notices.application.DeleteNoticeUseCase;
import com.viller.gespatiens.notices.application.EditNoticeUseCase;
import com.viller.gespatiens.notices.application.ListNoticesUseCase;
import com.viller.gespatiens.notices.application.NoticeView;
import com.viller.gespatiens.notices.application.PublishNoticeUseCase;
import com.viller.gespatiens.notices.domain.Author;
import com.viller.gespatiens.notices.domain.Notice;
import com.viller.gespatiens.notices.domain.NoticeNotManageableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoticeController.class)
@Import(SecurityConfig.class)
class NoticeControllerTest {

    private static final String ME = "7f3c2a10-0000-4000-8000-0000000000aa";
    private static final UUID ME_ID = UUID.fromString(ME);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ListNoticesUseCase listNoticesUseCase;
    @MockitoBean
    private PublishNoticeUseCase publishNoticeUseCase;
    @MockitoBean
    private EditNoticeUseCase editNoticeUseCase;
    @MockitoBean
    private DeleteNoticeUseCase deleteNoticeUseCase;

    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    private static NoticeView adminNotice() {
        Author admin = new Author(UUID.randomUUID(), "Administrador", "ADMINISTRATOR", true);
        Notice notice = Notice.reconstitute(UUID.randomUUID(), admin.id(), "Simulacro de incendio el jueves",
                Instant.parse("2026-10-20T08:00:00Z"), null);
        return new NoticeView(notice, Optional.of(admin), false, false);
    }

    @Test
    void anonymousCannotReadNotices() throws Exception {
        mockMvc.perform(get("/api/notices")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = ME, roles = "SOCIAL_EDUCATOR")
    void listsNoticesWithAuthorAndWhetherTheyAreMine() throws Exception {
        when(listNoticesUseCase.execute(ME_ID)).thenReturn(List.of(adminNotice()));

        mockMvc.perform(get("/api/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Simulacro de incendio el jueves"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-20T08:00:00Z"))
                .andExpect(jsonPath("$[0].author.administrator").value(true))
                .andExpect(jsonPath("$[0].mine").value(false))
                .andExpect(jsonPath("$[0].manageable").value(false));
    }

    @Test
    @WithMockUser(username = ME, roles = "MEDICAL_TEAM")
    void anyEmployeePublishes() throws Exception {
        when(publishNoticeUseCase.execute(ME_ID, "Revisión médica a las 11")).thenReturn(adminNotice());

        mockMvc.perform(post("/api/notices").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Revisión médica a las 11\"}"))
                .andExpect(status().isCreated());
        verify(publishNoticeUseCase).execute(ME_ID, "Revisión médica a las 11");
    }

    @Test
    @WithMockUser(username = ME)
    void deletingSomeoneElsesNoticeIsForbidden() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new NoticeNotManageableException()).when(deleteNoticeUseCase).execute(id, ME_ID);

        mockMvc.perform(delete("/api/notices/{id}", id))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value(
                        "Solo el autor del aviso o la administración pueden modificarlo o eliminarlo"));
    }

    @Test
    @WithMockUser(username = ME)
    void emptyNoticeIsBadRequest() throws Exception {
        when(publishNoticeUseCase.execute(ME_ID, " "))
                .thenThrow(new IllegalArgumentException("El aviso no puede estar vacío"));

        mockMvc.perform(post("/api/notices").contentType(MediaType.APPLICATION_JSON).content("{\"text\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El aviso no puede estar vacío"));
    }
}
