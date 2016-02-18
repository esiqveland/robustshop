package no.bekk.distsys.robustness;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.global.RequestDelaySpec;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.google.common.base.Charsets;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
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
    private static final ObjectMapper mapper = new ObjectMapper();

    @ClassRule
    public static DropwizardAppRule<RobustConfiguration> RULE =
            new DropwizardAppRule<>(PingerApplication.class, ResourceHelpers.resourceFilePath("config-test.yml"));

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(8089);
    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    @Test
    public void test_Answer() throws IOException {
        instanceRule.addRequestProcessingDelay(300);
        instanceRule.addSocketAcceptDelay(new RequestDelaySpec(500));
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
