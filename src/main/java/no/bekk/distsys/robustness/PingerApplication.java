package no.bekk.distsys.robustness;

import io.dropwizard.Application;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import no.bekk.distsys.robustness.dealer.DealerResource;
import no.bekk.distsys.robustness.dealer.ZooKeeperService;
import no.bekk.distsys.robustness.pinger.LoggingPinger;
import no.bekk.distsys.robustness.pinger.MyPingerClient;
import no.bekk.distsys.robustness.pinger.PingResource;
import no.bekk.distsys.robustness.pinger.Pinger;
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

        Pinger pinger = new MyPingerClient(pingerHost, env.getObjectMapper());
        PingResource pingResource = new PingResource(new LoggingPinger(pinger));

        ZooKeeperService zooKeeperService = new ZooKeeperService(config.getZooKeeper());
        env.lifecycle().manage(zooKeeperService);

        DealerResource dealerResource = new DealerResource(zooKeeperService);
        zooKeeperService.addListener(dealerResource);

        env.jersey().register(pingResource);
        env.jersey().register(dealerResource);

    }

    @Override
    public String getName() {
        return "robust-app";
    }

}
