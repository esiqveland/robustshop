package no.bekk.distsys.robustness;

import io.dropwizard.Application;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import no.bekk.distsys.robustness.dealer.DealerResource;
import no.bekk.distsys.robustness.dealer.MyWatcher;
import no.bekk.distsys.robustness.pinger.*;
import org.apache.http.client.HttpClient;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

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

        DealerResource dealerResource = new DealerResource();

        Watcher watcher = new MyWatcher(dealerResource);
        ZooKeeper zooKeeper = new ZooKeeper(config.getZooKeeper(), (int)TimeUnit.SECONDS.toMillis(15), watcher);


        env.jersey().register(pingResource);
        env.jersey().register(dealerResource);

    }

    @Override
    public String getName() {
        return "robust-app";
    }

}
