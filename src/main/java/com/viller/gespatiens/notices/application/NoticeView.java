package com.viller.gespatiens.notices.application;

import com.viller.gespatiens.notices.domain.Author;
import com.viller.gespatiens.notices.domain.Notice;

import java.util.Optional;

// Un aviso tal como lo ve un empleado concreto: con su autor resuelto, si es suyo (la memoria pide
// diferenciar los propios de los ajenos) y si puede modificarlo o eliminarlo
public record NoticeView(Notice notice, Optional<Author> author, boolean mine, boolean manageable) {
}
