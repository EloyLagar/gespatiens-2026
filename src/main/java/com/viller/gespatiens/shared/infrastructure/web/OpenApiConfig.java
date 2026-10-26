package com.viller.gespatiens.shared.infrastructure.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// La portada de /swagger-ui.html. springdoc saca las rutas de los controladores; aquí va lo que no se puede
// deducir del código: qué es la API y cómo se entra (el token JWT de POST /api/auth/login en "Authorize")
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "token";

    @Bean
    public OpenAPI gespatiensOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("gespatiens API")
                        .version("1.0")
                        .description("""
                                API REST de gespatiens, la gestión de una comunidad terapéutica: residentes, diario, \
                                valoraciones, intervenciones, informes, turnos y avisos.

                                Para probarla: `POST /api/auth/login` con una cuenta de la demo (por ejemplo \
                                `marta.serra@vientos.es` / `demo1234`), copia el `accessToken` de la respuesta y \
                                pégalo en **Authorize**. Los permisos son los de la aplicación: cada perfil ve y \
                                cambia lo mismo que en las pantallas. Los errores siguen el formato ProblemDetail \
                                (RFC 9457).""")
                        .license(new License().name("MIT")
                                .url("https://github.com/EloyLagar/gespatiens-2026/blob/master/LICENSE")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
