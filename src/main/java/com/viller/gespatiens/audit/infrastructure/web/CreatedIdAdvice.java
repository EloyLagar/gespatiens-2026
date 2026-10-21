package com.viller.gespatiens.audit.infrastructure.web;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.RecordComponent;
import java.util.UUID;

// El alta de un residente no lleva su id en la ruta: sale en la respuesta. Este aviso lo copia en la petición
// para que AuditInterceptor sepa de quién era. Vale para cualquier respuesta de un POST que sea un record con
// un componente "id" de tipo UUID; no depende de los DTO de otros módulos
@RestControllerAdvice
public class CreatedIdAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return returnType.getParameterType().isRecord();
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body != null && request.getMethod() == HttpMethod.POST && request instanceof ServletServerHttpRequest servlet) {
            for (RecordComponent component : body.getClass().getRecordComponents()) {
                if (component.getName().equals("id") && component.getType() == UUID.class) {
                    try {
                        servlet.getServletRequest().setAttribute(AuditInterceptor.CREATED_ID,
                                component.getAccessor().invoke(body));
                    } catch (ReflectiveOperationException e) {
                        // Sin el id, la línea del registro queda sin residente; la respuesta sigue igual
                    }
                }
            }
        }
        return body;
    }
}
