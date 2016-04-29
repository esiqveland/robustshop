package no.bekk.distsys.robustness.pinger;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;

public class PingerFluent implements Pinger {
    private static final Logger LOG = LoggerFactory.getLogger(PingerClient.class);

    private static final String ENDPOINT_PING = "/gamble";
    private static final int TIMEOUT_MS = 500;
    private final HttpClient client;
    private final String hostname;
    private final ObjectMapper mapper;

    public PingerFluent(@NotNull HttpClient client, @NotNull String host, @NotNull ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
        this.hostname = StringUtils.stripEnd(host, "/");
    }

    @Override
    public Pong Ping() {
        try {
            InputStream stream = Request.Get(hostname + ENDPOINT_PING)
                    .socketTimeout(TIMEOUT_MS)
                    .connectTimeout(TIMEOUT_MS)
                    .execute()
                    .returnContent()
                    .asStream();
            Pong pong = mapper.readValue(stream, Pong.class);
            return pong;
        } catch (IOException e) {
            LOG.error("[Ping] exception while executing request", e);
        }
        return null;
    }
}
