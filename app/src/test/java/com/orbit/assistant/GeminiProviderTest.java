package com.orbit.assistant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {29, 35})
public final class GeminiProviderTest {
    private Context context;

    @Before public void setUp() {
        context = RuntimeEnvironment.getApplication();
        Prefs.get(context).edit().clear().commit();
        SecureStore.clearGeminiKey(context);
    }

    @Test public void geminiIsRegisteredAndFirstForFreshInstall() {
        assertEquals(Prefs.PROVIDER_GEMINI, Prefs.provider(context));
        assertEquals(Prefs.PROVIDER_GEMINI, AiProviders.active(context).id());
        assertEquals(AiProvider.Status.NEEDS_SETUP,
                AiProviders.byId(Prefs.PROVIDER_GEMINI).status(context));
    }

    @Test public void officialFlashFallbackModelsAreOrderedAndHaveNoUnsupportedFeatures() {
        assertEquals(Arrays.asList(OrbitModelCatalog.GEMINI_38_FLASH,
                        OrbitModelCatalog.GEMINI_36_FLASH, OrbitModelCatalog.GEMINI_35_FLASH,
                        OrbitModelCatalog.GEMINI_35_FLASH_LITE, OrbitModelCatalog.GEMINI_31_FLASH_LITE),
                Arrays.asList(
                        OrbitModelCatalog.modelsFor(Prefs.PROVIDER_GEMINI).get(0).id,
                        OrbitModelCatalog.modelsFor(Prefs.PROVIDER_GEMINI).get(1).id,
                        OrbitModelCatalog.modelsFor(Prefs.PROVIDER_GEMINI).get(2).id,
                        OrbitModelCatalog.modelsFor(Prefs.PROVIDER_GEMINI).get(3).id,
                        OrbitModelCatalog.modelsFor(Prefs.PROVIDER_GEMINI).get(4).id));
        AiCapabilities caps = AiProviders.byId(Prefs.PROVIDER_GEMINI).capabilities();
        assertFalse(caps.streaming);
        assertFalse(caps.deviceActions);
        assertFalse(caps.images);
        assertTrue(caps.needsCredentials);
    }

    @Test public void geminiKeyIsEncryptedIsolatedAndExcludedFromBackup() {
        String secret = "AIza-test-gemini-secret";
        boolean saved = SecureStore.saveGeminiKey(context, secret);
        if (!saved) {
            assertEquals("", SecureStore.loadGeminiKey(context));
            return;
        }
        assertEquals(secret, SecureStore.loadGeminiKey(context));
        for (Map.Entry<String, ?> entry : Prefs.get(context).getAll().entrySet()) {
            assertFalse(entry.getKey(), String.valueOf(entry.getValue()).contains(secret));
        }
        try {
            String backup = Prefs.backupSnapshot(context).toString();
            assertFalse(backup.contains(secret));
            assertFalse(backup.contains("gemini_key"));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        assertEquals("", SecureStore.loadXaiKey(context));
        assertEquals("", SecureStore.loadAnthropicKey(context));
    }

    @Test public void removingGeminiKeyMakesProviderNeedSetup() {
        SecureStore.saveGeminiKey(context, "temporary-key");
        SecureStore.clearGeminiKey(context);
        assertFalse(SecureStore.hasGeminiKey(context));
        assertEquals(AiProvider.Status.NEEDS_SETUP,
                AiProviders.byId(Prefs.PROVIDER_GEMINI).status(context));
    }
}
