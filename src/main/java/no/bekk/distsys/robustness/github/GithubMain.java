package no.bekk.distsys.robustness.github;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Client;
import feign.Feign;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import org.slf4j.MDC;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class GithubMain {

    private static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String hostname = "https://api.github.com";
        Client client = new OkHttpClient(new com.squareup.okhttp.OkHttpClient());
        GithubFeign github = buildClient(client, hostname, mapper);

        List<GithubFeign.Contributor> contributors = github.contributors("netflix", "feign");
        for (GithubFeign.Contributor contributor : contributors) {
            System.out.println(contributor.login + " (" + contributor.contributions + ")");
        }

    }

    public static GithubFeign buildClient(Client client, String host, ObjectMapper mapper) {
        Request.Options opts = new Request.Options(1000, 1000);

        return Feign.builder()
                .client(client)
                .options(opts)
                .requestInterceptor((req) -> req.header("Correlation-ID", MDC.get("Correlation-ID")))
                .logLevel(Logger.Level.FULL)
                .retryer(new Retryer.Default(100L, TimeUnit.SECONDS.toMillis(1L), 3))
                .logger(new Slf4jLogger(GithubFeign.class))
                .encoder(new JacksonEncoder(mapper))
                .decoder(new JacksonDecoder(mapper))
                .target(GithubFeign.class, host);
    }

}
