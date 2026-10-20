package com.viller.gespatiens.notices.domain;

public class NoticeNotManageableException extends RuntimeException {

    public NoticeNotManageableException() {
        super("Solo el autor del aviso o la administración pueden modificarlo o eliminarlo");
    }
}
