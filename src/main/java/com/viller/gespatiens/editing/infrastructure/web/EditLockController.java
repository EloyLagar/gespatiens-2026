package com.viller.gespatiens.editing.infrastructure.web;

import com.viller.gespatiens.editing.application.AcquireEditLockUseCase;
import com.viller.gespatiens.editing.application.EditLockView;
import com.viller.gespatiens.editing.application.ListEditLocksUseCase;
import com.viller.gespatiens.editing.application.ReleaseEditLockUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

// RF8: quién está editando qué. La pantalla pide el bloqueo al empezar a editar, lo renueva mientras tanto
// y lo suelta al guardar, cancelar o salir
@RestController
@RequestMapping("/api/edit-locks")
public class EditLockController {

    private final AcquireEditLockUseCase acquireEditLockUseCase;
    private final ReleaseEditLockUseCase releaseEditLockUseCase;
    private final ListEditLocksUseCase listEditLocksUseCase;

    public EditLockController(AcquireEditLockUseCase acquireEditLockUseCase,
                              ReleaseEditLockUseCase releaseEditLockUseCase,
                              ListEditLocksUseCase listEditLocksUseCase) {
        this.acquireEditLockUseCase = acquireEditLockUseCase;
        this.releaseEditLockUseCase = releaseEditLockUseCase;
        this.listEditLocksUseCase = listEditLocksUseCase;
    }

    // ?prefix=report:<id>: todo lo que se está editando dentro de ese informe
    @GetMapping
    public List<EditLockResponse> list(Principal principal, @RequestParam String prefix) {
        return listEditLocksUseCase.execute(prefix, currentUserId(principal)).stream()
                .map(EditLockResponse::from)
                .toList();
    }

    // Empezar a editar o renovar. 409 si lo está editando otra persona
    @PutMapping("/{resource}")
    public EditLockResponse acquire(Principal principal, @PathVariable String resource) {
        return EditLockResponse.from(acquireEditLockUseCase.execute(resource, currentUserId(principal)));
    }

    @DeleteMapping("/{resource}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(Principal principal, @PathVariable String resource) {
        releaseEditLockUseCase.execute(resource, currentUserId(principal));
    }

    // holderName null: quien lo edita ya no está en la plantilla
    public record EditLockResponse(String resource, UUID holderId, String holderName, Instant since,
                                   Instant expiresAt, boolean mine) {
        static EditLockResponse from(EditLockView view) {
            return new EditLockResponse(view.resource(), view.holderId(), view.holderName(), view.since(),
                    view.expiresAt(), view.mine());
        }
    }

    // CurrentUserJwtConverter pone el id del empleado como "name" de la autenticación
    private static UUID currentUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
