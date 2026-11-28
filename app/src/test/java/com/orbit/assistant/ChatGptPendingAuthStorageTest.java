package com.orbit.assistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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

/** The resumable ChatGPT device-code attempt must remain encrypted and outside backups. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {29, 35})
public final class ChatGptPendingAuthStorageTest {
    private Context context;

    @Before public void setUp() {
        TestKeystore.install();
        context = RuntimeEnvironment.getApplication();
        Prefs.get(context).edit().clear().commit();
        SecureStore.clearPendingChatGptLogin(context);
        SecureStore.clearChatGpt(context);
    }

    @After public void tearDown() {
        SecureStore.clearPendingChatGptLogin(context);
        SecureStore.clearChatGpt(context);
        TestKeystore.uninstall();
    }

    @Test public void pendingDeviceCodeIsEncryptedAndExcludedFromBackups() throws Exception {
        String deviceAuthId = "device-auth-sensitive-123";
        String userCode = "ABCD-EFGH";
        long expiresAt = System.currentTimeMillis() + 5 * 60 * 1000L;

        assertTrue(SecureStore.savePendingChatGptLogin(
                context, deviceAuthId, userCode, 5L, expiresAt));

        SecureStore.PendingChatGptLogin loaded = SecureStore.loadPendingChatGptLogin(context);
        assertTrue(loaded != null);
        assertEquals(deviceAuthId, loaded.deviceAuthId);
        assertEquals(userCode, loaded.userCode);
        assertEquals(5L, loaded.intervalSeconds);
        assertEquals(expiresAt, loaded.expiresAtMs);

        for (Map.Entry<String, ?> entry : Prefs.get(context).getAll().entrySet()) {
            String stored = String.valueOf(entry.getValue());
            assertFalse(entry.getKey(), stored.contains(deviceAuthId));
            assertFalse(entry.getKey(), stored.contains(userCode));
        }
        String backup = Prefs.backupSnapshot(context).toString();
        assertFalse(backup.contains(deviceAuthId));
        assertFalse(backup.contains(userCode));
        assertFalse(backup.contains("chatgpt_pending_auth"));

        SecureStore.clearPendingChatGptLogin(context);
        assertNull(SecureStore.loadPendingChatGptLogin(context));
    }

    @Test public void expiredPendingDeviceCodeIsRejectedAndCleared() {
        assertTrue(SecureStore.savePendingChatGptLogin(
                context, "expired-device-auth", "EXPIRED-CODE", 5L,
                System.currentTimeMillis() - 1L));

        assertNull(SecureStore.loadPendingChatGptLogin(context));
        assertFalse(Prefs.get(context).contains("chatgpt_pending_auth_enc"));
        assertFalse(Prefs.get(context).contains("chatgpt_pending_auth_iv"));
    }
}
