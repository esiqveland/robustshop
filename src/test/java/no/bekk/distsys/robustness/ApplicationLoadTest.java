package no.bekk.distsys.robustness;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import com.tomakehurst.crashlab.CrashLab;
import com.tomakehurst.crashlab.HttpSteps;
import com.tomakehurst.crashlab.TimeInterval;
import com.tomakehurst.crashlab.metrics.AppMetrics;
import com.tomakehurst.crashlab.metrics.HttpJsonAppMetricsSource;
import com.tomakehurst.crashlab.saboteur.Saboteur;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import no.bekk.distsys.robustness.wiremock.DistributedResopnseTimeTransformer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.tomakehurst.crashlab.Rate.rate;
import static com.tomakehurst.crashlab.TimeInterval.interval;
import static com.tomakehurst.crashlab.TimeInterval.period;
import static org.junit.Assert.assertTrue;

public class ApplicationLoadTest {

    @ClassRule
    public static DropwizardAppRule<RobustConfiguration> RULE =
            new DropwizardAppRule<>(PingerApplication.class, ResourceHelpers.resourceFilePath("config-test.yml"));

    @ClassRule
    public static WireMockClassRule wireMockRule = new WireMockClassRule(new WireMockConfiguration().port(8089).extensions(DistributedResopnseTimeTransformer.class));
    @Rule
    public WireMockClassRule instanceRule = wireMockRule;

    Saboteur textSnippetServiceSaboteur = Saboteur.defineClient("text-snippet-web-service", 8080, "192.168.2.12");

    /** private to access portnumber from RULE */
    private HttpJsonAppMetricsSource metricsSource = new HttpJsonAppMetricsSource(
            String.format("http://localhost:%d/metrics", RULE.getAdminPort())
    );
    static CrashLab crashLab = new CrashLab();

    @Before
    public void init() {
//        textSnippetServiceSaboteur.reset();
    }

    @Test
    public void latency_less_than_200ms_with_no_faults() {
        instanceRule.stubFor(responseOk());

        String GET_ANSWER = String.format("http://localhost:%d/answer", RULE.getLocalPort());

        crashLab.run(period(10, TimeUnit.SECONDS), rate(500).per(TimeUnit.SECONDS), new HttpSteps("10 seconds moderate load") {
            public ListenableFuture<Response> run(AsyncHttpClient http, AsyncCompletionHandler<Response> completionHandler) throws IOException {
                return http.prepareGet(GET_ANSWER).execute(completionHandler);
            }
        });

        AppMetrics appMetrics = metricsSource.fetch();
        TimeInterval p95 = appMetrics.timer("no.bekk.distsys.robustness.pinger.PingResource.getAnswer").percentile95();
        assertTrue("Expected 95th percentile latency to be less than 500 milliseconds. Was actually " + p95.timeIn(TimeUnit.MILLISECONDS) + "ms",
                p95.lessThan(interval(500, TimeUnit.MILLISECONDS)));
    }

    private MappingBuilder responseOk() {
        return get(urlEqualTo("/gamble"))
                .withHeader("Accept", equalTo("application/json"))
                .willReturn(
                        aResponse()
                            .withBody(EXPECTED)
                            .withHeader("Content-Type", "application/json")
                            .withStatus(200)
                            .withFixedDelay(200)
                            .withTransformers(DistributedResopnseTimeTransformer.NAME)
                );
    }
    private static final String EXPECTED = "{\"answer\":\"pong\"}";

}
