package no.bekk.distsys.robustness.dealer;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ZooKeeperService {

    private final ZooKeeper zooKeeper;

    public ZooKeeperService(final String url, final ProcessNodeWatcher processNodeWatcher) throws IOException {
        zooKeeper = new ZooKeeper(url, (int) TimeUnit.SECONDS.toMillis(5), processNodeWatcher);
    }

    public String createNode(final String node, final boolean watch, final boolean ephimeral) {
        String createdNodePath = null;
        try {

            final Stat nodeStat = zooKeeper.exists(node, watch);

            if (nodeStat == null) {
                createdNodePath = zooKeeper.create(node, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, (ephimeral ? CreateMode.EPHEMERAL_SEQUENTIAL : CreateMode.PERSISTENT));
            } else {
                createdNodePath = node;
            }
        } catch (KeeperException | InterruptedException e) {
            throw new IllegalStateException(e);
        }

        return createdNodePath;
    }

    public boolean watchNode(final String node, final boolean watch) {
        boolean watched = false;
        try {
            final Stat nodeStat = zooKeeper.exists(node, watch);

            if (nodeStat != null) {
                watched = true;
            }

        } catch (KeeperException | InterruptedException e) {
            throw new IllegalStateException(e);
        }

        return watched;
    }

    public List<String> getChildren(final String node, final boolean watch) {
        List<String> childNodes = null;

        try {
            childNodes = zooKeeper.getChildren(node, watch);
        } catch (KeeperException | InterruptedException e) {
            throw new IllegalStateException(e);
        }

        return childNodes;
    }
}