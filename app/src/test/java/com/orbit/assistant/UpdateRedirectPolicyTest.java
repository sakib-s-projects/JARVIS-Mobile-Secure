package com.orbit.assistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** The updater follows only a small, explicit set of HTTPS GitHub hosts. */
public final class UpdateRedirectPolicyTest {

    @Test public void officialGitHubUpdateHostsAreAllowed() {
        assertTrue(OrbitUpdater.isAllowedRedirectUrl("https://api.github.com/repos/example/repo/releases"));
        assertTrue(OrbitUpdater.isAllowedRedirectUrl("https://github.com/example/repo/releases/download/v1/app.apk"));
        assertTrue(OrbitUpdater.isAllowedRedirectUrl("https://release-assets.githubusercontent.com/path/to/asset"));
        assertTrue(OrbitUpdater.isAllowedRedirectUrl("https://objects.githubusercontent.com/path/to/object"));
        assertTrue(OrbitUpdater.isAllowedRedirectUrl("https://github.com:443/example/repo"));
    }

    @Test public void insecureAndLookalikeHostsAreRejected() {
        assertFalse(OrbitUpdater.isAllowedRedirectUrl("http://github.com/example/repo"));
        assertFalse(OrbitUpdater.isAllowedRedirectUrl("https://github.com.attacker.example/file.apk"));
        assertFalse(OrbitUpdater.isAllowedRedirectUrl("https://attacker.github.com/file.apk"));
        assertFalse(OrbitUpdater.isAllowedRedirectUrl("https://attacker.example/file.apk"));
        assertFalse(OrbitUpdater.isAllowedRedirectUrl("https://github.com@attacker.example/file.apk"));
        assertFalse(OrbitUpdater.isAllowedRedirectUrl("https://github.com:8443/example/repo"));
        assertFalse(OrbitUpdater.isAllowedRedirectUrl("not a URL"));
        assertFalse(OrbitUpdater.isAllowedRedirectUrl(""));
    }

    @Test public void redirectChainHasAFixedMaximumOfFive() {
        assertEquals(5, OrbitUpdater.MAX_REDIRECTS);
    }
}
