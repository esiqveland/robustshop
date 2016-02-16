package no.bekk.distsys.robustness.pinger;

/**
 * Pinger pings a very difficult service and tries to get it to answer pong.
 */
public interface Pinger {
    Pong Ping();
}
