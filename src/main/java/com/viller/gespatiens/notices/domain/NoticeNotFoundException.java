package com.viller.gespatiens.notices.domain;

import java.util.UUID;

public class NoticeNotFoundException extends RuntimeException {

    public NoticeNotFoundException(UUID id) {
        super("No existe ningún aviso con id " + id);
    }
}
