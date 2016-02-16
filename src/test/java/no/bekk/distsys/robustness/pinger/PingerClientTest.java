package no.bekk.distsys.robustness.pinger;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.util.Duration;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PingerClientTest {
    private Pinger pinger;
    private HttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {
        client = buildClient();
    }

    private static final long TIMEOUT_MS = 400;

    private static HttpClient buildClientShitty() {
        return org.apache.http.impl.client.HttpClientBuilder.create()
                .build();
    }

    private static HttpClient buildClient() {
        HttpClientConfiguration config = new HttpClientConfiguration();
        config.setKeepAlive(Duration.minutes(5));
        config.setMaxConnections(16);
        config.setMaxConnectionsPerRoute(16);
        config.setTimeout(Duration.milliseconds(TIMEOUT_MS));

        return new HttpClientBuilder(new MetricRegistry())
                .using(config)
                .build("client");
    }

    @Test
    public void testOKResponse() throws IOException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(RESPONSE_OK).setResponseCode(200));

        server.start();

        HttpUrl baseUrl = server.url("/");

        pinger = new PingerClient(client, baseUrl.toString(), mapper);

        Pong pong = pinger.Ping();

        assertNotNull("pong should not be null", pong);
        assertEquals("pong", pong.getAnswer());
    }

    @Test
    public void testOKResponse_Slow() throws IOException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(RESPONSE_OK).setResponseCode(200).setBodyDelay(600, TimeUnit.MILLISECONDS));

        server.start();

        HttpUrl baseUrl = server.url("/");

        pinger = new PingerClient(client, baseUrl.toString(), mapper);

        long before = System.currentTimeMillis();
        Pong pong = pinger.Ping();
        long after = System.currentTimeMillis();

        assertDuration(after - before, TIMEOUT_MS);

        assertNull("pong should be null when timing out", pong);
    }
    @Test
    public void testOKResponse_thottle_1byte_per_second() throws IOException {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse()
                .setBody(RESPONSE_OK)
                .setResponseCode(200)
                .throttleBody(1, 100, TimeUnit.MILLISECONDS));

        server.start();

        HttpUrl baseUrl = server.url("/");

        pinger = new PingerClient(client, baseUrl.toString(), mapper);


        long before = System.currentTimeMillis();
        Pong pong = pinger.Ping();
        long after = System.currentTimeMillis();

        assertDuration(after - before, TIMEOUT_MS);

        assertNotNull("pong should not be null", pong);
        assertEquals("pong", pong.getAnswer());
    }

    static void assertDuration(long duration_ms, long timeout_ms) {
        assertFalse(String.format("A call should not hang the thread for more than %dms, but was %dms", TIMEOUT_MS, duration_ms),
                duration_ms > (timeout_ms*1.10));
    }

    String RESPONSE_OK = "{\"answer\": \"pong\"}";
}