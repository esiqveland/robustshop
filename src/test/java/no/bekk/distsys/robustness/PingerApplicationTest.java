package no.bekk.distsys.robustness;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.global.RequestDelaySpec;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.base.Charsets;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import no.bekk.distsys.robustness.wiremock.DistributedResopnseTimeTransformer;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertEquals;

public class PingerApplicationTest {
    /** set to longer than SO_TIMEOUT (milliseconds) */
    private static final int LONGER_THAN_SOCKET_TIMEOUT = 500*3;


    @ClassRule
    public static DropwizardAppRule<RobustConfiguration> RULE =
            new DropwizardAppRule<>(PingerApplication.class, ResourceHelpers.resourceFilePath("config-test.yml"));

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(new WireMockConfiguration().port(8089).extensions(DistributedResopnseTimeTransformer.class));
    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    @Before
    public void setup() {
        instanceRule.resetMappings();
        instanceRule.resetRequests();
        instanceRule.resetScenarios();
    }

    @Test
    public void test_socket_timeout() throws IOException {
        WireMock.reset();
        instanceRule.resetMappings();
        instanceRule.resetRequests();
        instanceRule.resetScenarios();

        instanceRule.addRequestProcessingDelay(LONGER_THAN_SOCKET_TIMEOUT);
        instanceRule.addSocketAcceptDelay(new RequestDelaySpec(LONGER_THAN_SOCKET_TIMEOUT));

        instanceRule.stubFor(get(urlEqualTo("/gamble"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(
                        aResponse()
                                .withBody(EXPECTED)
                                .withHeader("Content-Type", "application/json")
                                .withStatus(200)
                                .withFixedDelay(200)
                                .withTransformers(DistributedResopnseTimeTransformer.NAME)
                )
        );

        String url = String.format("http://localhost:%d/answer", RULE.getLocalPort());

        HttpClient client = HttpClientBuilder.create().build();

        HttpGet req = new HttpGet(url);
        req.addHeader("Accept", "application/json");
        HttpResponse res = client.execute(req);

        assertEquals("Response status should be 200 OK", HttpStatus.SC_OK, res.getStatusLine().getStatusCode());

        String body = IOUtils.toString(res.getEntity().getContent(), Charsets.UTF_8);

        assertEquals(EXPECTED, body);

    }

    @Test
    public void test_Answer() throws IOException {
        instanceRule.addRequestProcessingDelay(LONGER_THAN_SOCKET_TIMEOUT);

        instanceRule.stubFor(get(urlEqualTo("/gamble"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(
                        aResponse()
                                .withBody(EXPECTED)
                                .withHeader("Content-Type", "application/json")
                                .withStatus(200)
                                .withFixedDelay(100)
                )
        );

        String url = String.format("http://localhost:%d/answer", RULE.getLocalPort());

        HttpClient client = HttpClientBuilder.create().build();

        HttpGet req = new HttpGet(url);
        req.addHeader("Accept", "application/json");
        HttpResponse res = client.execute(req);

        assertEquals("Response status should be 200 OK", HttpStatus.SC_OK, res.getStatusLine().getStatusCode());

        String body = IOUtils.toString(res.getEntity().getContent(), Charsets.UTF_8);

        assertEquals(EXPECTED, body);
    }

    private static final String EXPECTED = "{\"answer\":\"pong\"}";
}
