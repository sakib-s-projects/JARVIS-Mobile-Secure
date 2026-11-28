package com.orbit.assistant;

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

import javax.crypto.spec.SecretKeySpec;

/** Restore must clear extension ciphertext even if best-effort Keystore key deletion fails. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {29, 35})
public final class OrbitExtensionSecretStoreTest {
    private static final String ALIAS = "orbit_extension_secrets_v1";
    private static final String PREF_FILE = "orbit_extension_secrets";
    private static final String PREF_KEY = "encrypted_entries_v1";

    private Context context;

    @Before public void setUp() {
        TestKeystore.install();
        context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).edit().clear().commit();
        TestKeystore.KEYS.put(ALIAS, new SecretKeySpec(new byte[32], "AES"));
    }

    @After public void tearDown() {
        TestKeystore.uninstall();
    }

    @Test public void credentialCiphertextIsRemovedEvenWhenKeystoreCleanupFails() {
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
                .edit().putString(PREF_KEY, "{\"extension\":{\"token\":\"encrypted\"}}")
                .commit();
        TestKeystore.FAIL_DELETE = true;

        assertTrue(OrbitExtensionSecretStore.clearAll(context));

        assertFalse(context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE).contains(PREF_KEY));
        // The orphaned key contains no credential data and can be cleaned up on a later install.
        assertTrue(TestKeystore.KEYS.containsKey(ALIAS));
    }

    @Test public void nullContextDoesNotClaimCredentialsWereCleared() {
        assertFalse(OrbitExtensionSecretStore.clearAll(null));
    }
}
