package no.bekk.distsys.robustness.dealer;

import org.apache.zookeeper.WatchedEvent;

public interface ZKListener {

    void Disconnected();
    void Connected();
    void Notify(WatchedEvent e);

}
