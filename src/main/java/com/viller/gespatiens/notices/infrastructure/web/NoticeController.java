package com.viller.gespatiens.notices.infrastructure.web;

import com.viller.gespatiens.notices.application.DeleteNoticeUseCase;
import com.viller.gespatiens.notices.application.EditNoticeUseCase;
import com.viller.gespatiens.notices.application.ListNoticesUseCase;
import com.viller.gespatiens.notices.application.PublishNoticeUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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

// RF5. Todos los endpoints son para cualquier empleado autenticado: quién puede modificar o eliminar
// cada aviso (su autor o la administración) lo decide el dominio, no la configuración de seguridad
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final ListNoticesUseCase listNoticesUseCase;
    private final PublishNoticeUseCase publishNoticeUseCase;
    private final EditNoticeUseCase editNoticeUseCase;
    private final DeleteNoticeUseCase deleteNoticeUseCase;

    public NoticeController(ListNoticesUseCase listNoticesUseCase,
                            PublishNoticeUseCase publishNoticeUseCase,
                            EditNoticeUseCase editNoticeUseCase,
                            DeleteNoticeUseCase deleteNoticeUseCase) {
        this.listNoticesUseCase = listNoticesUseCase;
        this.publishNoticeUseCase = publishNoticeUseCase;
        this.editNoticeUseCase = editNoticeUseCase;
        this.deleteNoticeUseCase = deleteNoticeUseCase;
    }

    @GetMapping
    public List<NoticeResponse> list(Principal principal) {
        return listNoticesUseCase.execute(currentUserId(principal)).stream()
                .map(NoticeResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoticeResponse publish(Principal principal, @RequestBody NoticeRequest request) {
        return NoticeResponse.from(publishNoticeUseCase.execute(currentUserId(principal), request.text()));
    }

    @PutMapping("/{id}")
    public NoticeResponse edit(Principal principal, @PathVariable UUID id, @RequestBody NoticeRequest request) {
        return NoticeResponse.from(editNoticeUseCase.execute(id, currentUserId(principal), request.text()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Principal principal, @PathVariable UUID id) {
        deleteNoticeUseCase.execute(id, currentUserId(principal));
    }

    // CurrentUserJwtConverter pone el id del empleado como "name" de la autenticación
    private static UUID currentUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
}
