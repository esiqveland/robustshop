package no.bekk.distsys.robustness;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.dropwizard.client.HttpClientConfiguration;

import javax.validation.constraints.NotNull;

public class RobustConfiguration extends Configuration {

    @JsonProperty
    @NotNull
    private String pingerHost;

    @JsonProperty
    private HttpClientConfiguration httpClient;

    public String getPingerHost() {
        return pingerHost;
    }

    public HttpClientConfiguration getHttpClient() {
        return httpClient;
    }
}
