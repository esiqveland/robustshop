package no.bekk.distsys.robustness.pinger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.net.URI;

public class MyPingerClient implements Pinger {
    private static final Logger LOG = LoggerFactory.getLogger(PingerClient.class);

    private static final String ENDPOINT_PING = "/gamble";
    private final HttpClient client;
    private final String hostname;
    private final ObjectMapper mapper;

    public MyPingerClient(@NotNull String host, @NotNull ObjectMapper mapper) {
        this.client = HttpClientBuilder.create().setMaxConnTotal(256).build();
        this.mapper = mapper;
        this.hostname = StringUtils.stripEnd(host, "/");
        LOG.info("[PingerClient] got host={}", hostname);
    }

    /**
     * Ping() returns Pong or null
     */
    @Override
    public Pong Ping() {
        try {
            URI uri = new URI(hostname + ENDPOINT_PING);
            HttpGet get = new HttpGet(uri);
            get.addHeader("Accept", "application/json");

            HttpResponse resp = client.execute(get);
            return mapper.readValue(resp.getEntity().getContent(), Pong.class);
        } catch (Exception e) {
        }
        return null;
    }
}
