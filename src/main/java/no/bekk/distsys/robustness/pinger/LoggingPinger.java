package no.bekk.distsys.robustness.pinger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingPinger implements Pinger {
    private static final Logger LOG = LoggerFactory.getLogger(Pinger.class);

    private final Pinger pinger;

    public LoggingPinger(Pinger pinger) {
        this.pinger = pinger;
    }

    @Override
    public Pong Ping() {
        long before = System.currentTimeMillis();
        try {
//            LOG.debug("[Ping]");
            return pinger.Ping();
        } finally {
            long after = System.currentTimeMillis();
//            LOG.debug("[Ping] took {}ms", after-before);
        }
    }
}
