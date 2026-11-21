package com.orbit.assistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;

/** Relay tokens must migrate to Keystore encryption and never fall back to plaintext. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 29)
public final class RelayTokenSecurityTest {
    private static final String TOKEN = "relay-token-migration-test-secret";
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        Prefs.get(context).edit().clear().commit();
        TestKeystore.install();
    }

    @After public void tearDown() {
        TestKeystore.uninstall();
        Prefs.get(context).edit().clear().commit();
    }

    @Test public void legacyPlaintextTokenMigratesToEncryptedStorage() {
        Prefs.get(context).edit().putString(Prefs.BACKEND_TOKEN, TOKEN).commit();

        assertEquals(TOKEN, SecureStore.loadRelayToken(context));
        assertFalse(Prefs.get(context).contains(Prefs.BACKEND_TOKEN));
        assertTrue(Prefs.get(context).contains("backend_token_enc"));
        assertTrue(Prefs.get(context).contains("backend_token_iv"));
        for (Map.Entry<String, ?> entry : Prefs.get(context).getAll().entrySet()) {
            assertFalse("plaintext token remains in " + entry.getKey(),
                    String.valueOf(entry.getValue()).contains(TOKEN));
        }
        assertEquals(TOKEN, SecureStore.loadRelayToken(context));
    }

    @Test public void encryptedTokenCanBeSavedAndCleared() {
        assertTrue(SecureStore.saveRelayToken(context, TOKEN));
        assertEquals(TOKEN, SecureStore.loadRelayToken(context));
        assertTrue(SecureStore.saveRelayToken(context, ""));
        assertEquals("", SecureStore.loadRelayToken(context));
        assertFalse(Prefs.get(context).contains("backend_token_enc"));
        assertFalse(Prefs.get(context).contains("backend_token_iv"));
    }

    @Test public void saveSettingsDoesNotPersistPlaintextWhenKeystoreUnavailable() {
        TestKeystore.uninstall();
        String error = Prefs.saveRelaySettings(context, "https://relay.example", TOKEN);

        assertFalse(error.isEmpty());
        assertFalse(Prefs.get(context).contains(Prefs.BACKEND_TOKEN));
        assertFalse(Prefs.get(context).getString(Prefs.BACKEND_URL, "").contains("relay.example"));
        assertEquals("", SecureStore.loadRelayToken(context));
    }
}
