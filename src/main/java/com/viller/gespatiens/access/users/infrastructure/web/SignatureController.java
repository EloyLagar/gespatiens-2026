package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.SignatureUseCase;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

// La firma escaneada (PNG) que se imprime en los informes. El cuerpo de la petición es la imagen tal cual,
// sin formulario: Content-Type image/png
@RestController
@RequestMapping("/api/users")
public class SignatureController {

    private final SignatureUseCase signatureUseCase;

    public SignatureController(SignatureUseCase signatureUseCase) {
        this.signatureUseCase = signatureUseCase;
    }

    @GetMapping(value = "/me/signature", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> mine(Principal principal) {
        return png(signatureUseCase.get(currentUserId(principal)).getPng());
    }

    @PutMapping(value = "/me/signature", consumes = MediaType.IMAGE_PNG_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void uploadMine(Principal principal, @RequestBody byte[] png) {
        signatureUseCase.upload(currentUserId(principal), png);
    }

    @DeleteMapping("/me/signature")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMine(Principal principal) {
        signatureUseCase.remove(currentUserId(principal));
    }

    // La administración puede ver, subir y quitar la de cualquiera (ver SecurityConfig)
    @GetMapping(value = "/{id}/signature", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> of(@PathVariable UUID id) {
        return png(signatureUseCase.get(id).getPng());
    }

    @PutMapping(value = "/{id}/signature", consumes = MediaType.IMAGE_PNG_VALUE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void upload(@PathVariable UUID id, @RequestBody byte[] png) {
        signatureUseCase.upload(id, png);
    }

    @DeleteMapping("/{id}/signature")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID id) {
        signatureUseCase.remove(id);
    }

    // no-store: es una firma, no debe quedarse en la caché del navegador de un equipo compartido
    private static ResponseEntity<byte[]> png(byte[] image) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore()).body(image);
    }

    // CurrentUserJwtConverter pone el id del empleado como "name" de la autenticación
    private static UUID currentUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
