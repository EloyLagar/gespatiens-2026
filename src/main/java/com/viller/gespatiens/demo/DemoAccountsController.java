package com.viller.gespatiens.demo;

import com.viller.gespatiens.access.users.domain.Speciality;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Las cuentas de la demo, una por perfil, para que la pantalla de login ofrezca "Entrar como …" sin tener que
// copiar emails y contraseñas. Solo existe con el perfil demo: en un centro de verdad esta ruta no está y el login
// no enseña nada. La contraseña no es ningún secreto, es la misma que publica el README
@RestController
@RequestMapping("/api/demo")
@Profile("demo")
public class DemoAccountsController {

    public record DemoAccount(String name, String email, Speciality speciality, String password) {
    }

    private static final List<DemoAccount> ACCOUNTS = List.of(
            new DemoAccount("Marta Serra", "marta.serra@vientos.es", Speciality.ADMINISTRATOR, DemoData.PASSWORD),
            new DemoAccount("Ana Beltrán", "ana.beltran@vientos.es", Speciality.SOCIAL_EDUCATOR, DemoData.PASSWORD),
            new DemoAccount("Jorge Vidal", "jorge.vidal@vientos.es", Speciality.PSYCHOLOGIST, DemoData.PASSWORD),
            new DemoAccount("Laura Gil", "laura.gil@vientos.es", Speciality.MEDICAL_TEAM, DemoData.PASSWORD),
            new DemoAccount("Carmen López", "carmen.lopez@vientos.es", Speciality.SOCIAL_WORKER, DemoData.PASSWORD));

    @GetMapping("/accounts")
    public List<DemoAccount> accounts() {
        return ACCOUNTS;
    }
}
