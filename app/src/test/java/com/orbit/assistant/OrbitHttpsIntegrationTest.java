package com.orbit.assistant;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Dns;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.junit.Test;
import java.net.InetAddress;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

/**
 * Real loopback socket + TLS integration tests. DNS override is test-only and deliberately
 * bypasses OrbitPublicDns' public-address gate so the local TLS fixture can be reached.
 * Production DNS gate is covered by OrbitPublicDnsTest.
 */
public class OrbitHttpsIntegrationTest {
    private static Dns loopbackDns() {
        return hostname -> Collections.singletonList(InetAddress.getByName("127.0.0.1"));
    }

    private static HandshakeCertificates certificates() {
        HeldCertificate certificate = new HeldCertificate.Builder()
                .addSubjectAlternativeName("localhost")
                .addSubjectAlternativeName("127.0.0.1")
                .build();
        return new HandshakeCertificates.Builder().heldCertificate(certificate).build();
    }

    private static OkHttpClient client(boolean followRedirects, HandshakeCertificates certificates) {
        javax.net.ssl.X509TrustManager trustManager = certificates.trustManager();
        return new OkHttpClient.Builder()
                .dns(loopbackDns())
                .sslSocketFactory(certificates.sslSocketFactory(), trustManager)
                .proxy(java.net.Proxy.NO_PROXY)
                .followRedirects(followRedirects)
                .followSslRedirects(false)
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();
    }

    @Test public void pinnedDnsSocketConnectAndValidTlsHostnamePass() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            HandshakeCertificates certificates = certificates();
            server.useHttps(certificates.sslSocketFactory(), false);
            server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));
            server.start();
            String url = server.url("/socket-check").toString().replace("127.0.0.1", "localhost");
            Request request = new Request.Builder().url(url).build();
            try (Response response = client(false, certificates).newCall(request).execute()) {
                assertEquals(200, response.code());
                assertEquals("ok", response.body().string());
            }
            RecordedRequest recorded = server.takeRequest(2, TimeUnit.SECONDS);
            assertNotNull("request should reach TLS server over socket", recorded);
            assertEquals("/socket-check", recorded.getPath());
        }
    }

    @Test public void invalidTlsHostnameIsRejected() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            HandshakeCertificates certificates = certificates();
            server.useHttps(certificates.sslSocketFactory(), false);
            server.enqueue(new MockResponse().setResponseCode(200).setBody("should-not-be-read"));
            server.start();
            String url = server.url("/tls-check").toString()
                    .replace("localhost", "wrong-host.invalid")
                    .replace("127.0.0.1", "wrong-host.invalid");
            try {
                client(false, certificates).newCall(new Request.Builder().url(url).build()).execute().close();
                fail("TLS hostname mismatch must fail");
            } catch (javax.net.ssl.SSLPeerUnverifiedException expected) {
                // OkHttp default hostname verification is active.
            }
            assertNull("TLS request must not reach application handler",
                    server.takeRequest(500, TimeUnit.MILLISECONDS));
        }
    }

    @Test public void redirectsRemainUnfollowedWhenConfigured() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            HandshakeCertificates certificates = certificates();
            server.useHttps(certificates.sslSocketFactory(), false);
            server.enqueue(new MockResponse().setResponseCode(302).setHeader("Location", "/private"));
            server.enqueue(new MockResponse().setResponseCode(200).setBody("redirect-followed"));
            server.start();
            String url = server.url("/redirect").toString().replace("127.0.0.1", "localhost");
            try (Response response = client(false, certificates)
                    .newCall(new Request.Builder().url(url).build()).execute()) {
                assertEquals(302, response.code());
            }
            assertEquals(1, server.getRequestCount());
        }
    }
}
