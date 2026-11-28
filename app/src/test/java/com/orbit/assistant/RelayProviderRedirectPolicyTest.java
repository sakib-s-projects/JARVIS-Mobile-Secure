package com.orbit.assistant;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

import java.net.HttpURLConnection;
import java.net.URL;

/** Authenticated relay calls must never forward credentials through an HTTP redirect. */
public final class RelayProviderRedirectPolicyTest {

    @Test public void authenticatedRelayConnectionsDoNotFollowRedirects() throws Exception {
        HttpURLConnection connection =
                (HttpURLConnection) new URL("https://example.com/assistant").openConnection();

        RelayProvider.disableRedirects(connection);

        assertFalse(connection.getInstanceFollowRedirects());
        connection.disconnect();
    }
}
