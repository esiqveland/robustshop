package no.bekk.distsys.robustness;

import io.dropwizard.Application;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import no.bekk.distsys.robustness.pinger.LoggingPinger;
import no.bekk.distsys.robustness.pinger.PingResource;
import no.bekk.distsys.robustness.pinger.Pinger;
import no.bekk.distsys.robustness.pinger.PingerClient;
import org.apache.http.client.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingerApplication extends Application<RobustConfiguration> {
    private static final Logger LOG = LoggerFactory.getLogger(PingerApplication.class);

    public static void main(String[] args) throws Exception {
        new PingerApplication().run(args);
    }

    @Override
    public void initialize(Bootstrap<RobustConfiguration> bootstrap) {
        super.initialize(bootstrap);
    }

    @Override
    public void run(RobustConfiguration config, Environment env) throws Exception {
        LOG.info("Booting in ENVIRONMENT={}", System.getenv("ENVIRONMENT"));
        HttpClient client = new HttpClientBuilder(env).using(config.getHttpClient()).build("pinger-client");
        String pingerHost = config.getPingerHost();

        Pinger pinger = new PingerClient(client, pingerHost, env.getObjectMapper());
        PingResource pingResource = new PingResource(new LoggingPinger(pinger));

        env.jersey().register(pingResource);

    }

    @Override
    public String getName() {
        return "robust-app";
    }

}
