package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.SignatureUseCase;
import com.viller.gespatiens.access.users.domain.Signature;
import com.viller.gespatiens.access.users.domain.SignatureNotFoundException;
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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SignatureController.class)
@Import(SecurityConfig.class)
class SignatureControllerTest {

    private static final String ME = "7f3c2a10-0000-4000-8000-0000000000aa";
    private static final UUID ME_ID = UUID.fromString(ME);
    private static final UUID OTHER = UUID.fromString("7f3c2a10-0000-4000-8000-0000000000bb");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SignatureUseCase signatureUseCase;
    // La seguridad real necesita estos dos beans; con @WithMockUser no se llegan a usar
    @MockitoBean
    private JwtDecoder jwtDecoder;
    @MockitoBean
    private CurrentUserJwtConverter currentUserJwtConverter;

    private static byte[] png() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB), "png", out);
        return out.toByteArray();
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void eachEmployeeUploadsAndSeesTheirOwn() throws Exception {
        byte[] png = png();
        when(signatureUseCase.get(ME_ID)).thenReturn(Signature.upload(ME_ID, png, Instant.now()));

        mockMvc.perform(put("/api/users/me/signature").contentType(MediaType.IMAGE_PNG).content(png))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/me/signature"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(png));
        verify(signatureUseCase).upload(ME_ID, png);
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void withoutSignatureItIsNotFound() throws Exception {
        when(signatureUseCase.get(ME_ID)).thenThrow(new SignatureNotFoundException(ME_ID));

        mockMvc.perform(get("/api/users/me/signature")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void someoneElsesSignatureIsOnlyForAdministration() throws Exception {
        mockMvc.perform(get("/api/users/{id}/signature", OTHER)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/users/{id}/signature", OTHER).contentType(MediaType.IMAGE_PNG).content(png()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(signatureUseCase);
    }

    @Test
    @WithMockUser(username = ME, roles = "ADMINISTRATOR")
    void administrationCanUploadItForSomeone() throws Exception {
        byte[] png = png();

        mockMvc.perform(put("/api/users/{id}/signature", OTHER).contentType(MediaType.IMAGE_PNG).content(png))
                .andExpect(status().isNoContent());
        verify(signatureUseCase).upload(OTHER, png);
    }

    @Test
    @WithMockUser(username = ME, roles = "PSYCHOLOGIST")
    void onlyPngIsAccepted() throws Exception {
        mockMvc.perform(put("/api/users/me/signature").contentType(MediaType.IMAGE_JPEG).content(new byte[]{1}))
                .andExpect(status().isUnsupportedMediaType());
    }
}
