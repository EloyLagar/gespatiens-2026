package com.viller.gespatiens.access.users.application;

import com.viller.gespatiens.access.users.domain.EmailAlreadyRegisteredException;
import com.viller.gespatiens.access.users.domain.InvalidPasswordSetupTokenException;
import com.viller.gespatiens.access.users.domain.Speciality;
import com.viller.gespatiens.access.users.domain.User;
import com.viller.gespatiens.access.users.domain.UserNotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Memoria final: el empleado nuevo recibe un correo y crea su propia contraseña. Alta, reenvío y
// creación de la contraseña, juntos porque comparten el enlace
class PasswordSetupUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
    private static final Duration VALIDITY = Duration.ofDays(3);

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryPasswordSetupTokenRepository tokens = new InMemoryPasswordSetupTokenRepository();
    private final RecordingLinkSender sender = new RecordingLinkSender();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private InviteUserUseCase invite() {
        return new InviteUserUseCase(users, new PasswordSetupLinks(tokens, sender, clock, VALIDITY));
    }

    private SendPasswordSetupLinkUseCase resend() {
        return new SendPasswordSetupLinkUseCase(users, new PasswordSetupLinks(tokens, sender, clock, VALIDITY));
    }

    private PasswordSetupUseCase setup() {
        return new PasswordSetupUseCase(tokens, users, hasher, clock);
    }

    private User inviteAna() {
        return invite().execute("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST).user();
    }

    @Test
    void invitedEmployeeHasNoPasswordAndReceivesTheLink() {
        InviteUserUseCase.Invitation invitation =
                invite().execute("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST);

        assertTrue(invitation.emailSent());
        assertFalse(invitation.user().hasPassword());
        assertEquals(1, sender.sent.size());
        assertEquals("ana@vientos.es", sender.sent.getFirst().user().getEmail());
        assertEquals(NOW.plus(VALIDITY), sender.sent.getFirst().expiresAt());
    }

    @Test
    void onlyTheHashOfTheTokenIsStored() {
        inviteAna();

        assertEquals(1, tokens.tokens.size());
        assertFalse(tokens.tokens.containsKey(sender.lastToken()));
    }

    @Test
    void invitationKeepsTheEmployeeWhenTheEmailFails() {
        sender.failing = true;

        InviteUserUseCase.Invitation invitation =
                invite().execute("Ana", "ana@vientos.es", "600111222", Speciality.PSYCHOLOGIST);

        assertFalse(invitation.emailSent());
        assertTrue(users.existsByEmail("ana@vientos.es"));
    }

    @Test
    void invitationRejectsDuplicateEmail() {
        inviteAna();

        assertThrows(EmailAlreadyRegisteredException.class, () ->
                invite().execute("Otra", "ana@vientos.es", "600999999", Speciality.SOCIAL_WORKER));
    }

    @Test
    void linkShowsWhoseAccountItIs() {
        inviteAna();

        assertEquals("Ana", setup().check(sender.lastToken()).getName());
    }

    @Test
    void employeeCreatesThePasswordAndTheLinkStopsWorking() {
        User ana = inviteAna();
        String token = sender.lastToken();

        setup().complete(token, "secreto123");

        assertEquals("hashed:secreto123", users.findById(ana.getId()).orElseThrow().getPasswordHash());
        assertThrows(InvalidPasswordSetupTokenException.class, () -> setup().complete(token, "otraclave1"));
    }

    @Test
    void shortPasswordIsRejectedAndTheLinkStillWorks() {
        inviteAna();
        String token = sender.lastToken();

        assertThrows(IllegalArgumentException.class, () -> setup().complete(token, "corta"));
        setup().complete(token, "secreto123");
    }

    @Test
    void expiredLinkIsRejected() {
        inviteAna();
        String token = sender.lastToken();
        clock = Clock.fixed(NOW.plus(VALIDITY), ZoneOffset.UTC);

        assertThrows(InvalidPasswordSetupTokenException.class, () -> setup().check(token));
    }

    @Test
    void unknownOrMissingTokenIsRejected() {
        assertThrows(InvalidPasswordSetupTokenException.class, () -> setup().check("inventado"));
        assertThrows(InvalidPasswordSetupTokenException.class, () -> setup().check(" "));
    }

    @Test
    void resendingReplacesThePreviousLink() {
        User ana = inviteAna();
        String first = sender.lastToken();

        resend().execute(ana.getId());

        assertThrows(InvalidPasswordSetupTokenException.class, () -> setup().check(first));
        assertEquals("Ana", setup().check(sender.lastToken()).getName());
    }

    @Test
    void employeeWhoForgotThePasswordReplacesItFromTheLink() {
        User luis = users.save(User.create("Luis", "luis@vientos.es", "600333444", Speciality.SOCIAL_EDUCATOR,
                hasher.hash("antigua123")));

        resend().execute(luis.getId());
        setup().complete(sender.lastToken(), "nueva12345");

        assertEquals("hashed:nueva12345", users.findById(luis.getId()).orElseThrow().getPasswordHash());
    }

    @Test
    void resendingToUnknownEmployeeFails() {
        assertThrows(UserNotFoundException.class, () -> resend().execute(UUID.randomUUID()));
    }

    @Test
    void resendingPropagatesEmailFailure() {
        User ana = inviteAna();
        sender.failing = true;

        assertThrows(PasswordSetupDeliveryException.class, () -> resend().execute(ana.getId()));
    }
}
