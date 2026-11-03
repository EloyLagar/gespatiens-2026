package com.viller.gespatiens.access.users.infrastructure.web;

import com.viller.gespatiens.access.users.application.LoginOrigin;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LoginOriginResolverTest {

    private static MockHttpServletRequest request(String ip, String userAgent) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(ip);
        if (userAgent != null) {
            request.addHeader("User-Agent", userAgent);
        }
        return request;
    }

    @Test
    void keepsTheIpAndTheBrowserInTheCenter() {
        LoginOrigin origin = new LoginOriginResolver(false).resolve(request("192.168.1.37", "Firefox"));

        assertEquals(new LoginOrigin("192.168.1.37", "Firefox"), origin);
    }

    // La demo publicada: se ve de qué red viene, no quién es el visitante
    @Test
    void inThePublicDemoTheIpLosesItsLastPart() {
        LoginOriginResolver demo = new LoginOriginResolver(true);

        assertEquals("83.45.120.0", demo.resolve(request("83.45.120.201", null)).ipAddress());
        assertEquals("2a01:4f8:c0c:0:0:0:0:0", demo.resolve(request("2a01:4f8:c0c:1234::1", null)).ipAddress());
    }

    @Test
    void aVeryLongBrowserIsCut() {
        LoginOrigin origin = new LoginOriginResolver(false).resolve(request("10.0.0.1", "x".repeat(1000)));

        assertEquals(LoginOriginResolver.MAX_USER_AGENT, origin.userAgent().length());
    }

    @Test
    void withoutIpThereIsNothingToAnonymize() {
        assertNull(LoginOriginResolver.anonymize(null));
        assertNull(LoginOriginResolver.anonymize(""));
    }
}
