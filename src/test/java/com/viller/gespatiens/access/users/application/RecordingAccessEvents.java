package com.viller.gespatiens.access.users.application;

import java.util.ArrayList;
import java.util.List;

// Guarda lo que se avisa al registro de accesos, para comprobarlo en los tests
public class RecordingAccessEvents implements AccessEvents {

    public final List<LoginAttempted> logins = new ArrayList<>();
    public final List<PasswordChanged> passwords = new ArrayList<>();

    @Override
    public void loginAttempted(LoginAttempted attempt) {
        logins.add(attempt);
    }

    @Override
    public void passwordChanged(PasswordChanged change) {
        passwords.add(change);
    }
}
