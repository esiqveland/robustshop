package no.bekk.distsys.robustness.dealer;

import org.apache.commons.lang3.StringUtils;
import org.apache.zookeeper.WatchedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/dealer")
public class DealerResource implements ZKListener {
    private static final Logger LOG = LoggerFactory.getLogger(DealerResource.class);
    private static final String DEALER_INSTANCE = "/dealers/dealer_";
    private static final String ROOT_DEALERS = "/dealers";

    private AtomicInteger atomicInteger = new AtomicInteger();
    // Start with enabled to false so we dont start dealing numbers without being leader
    private AtomicBoolean isEnabled = new AtomicBoolean(false);
    private final ZooKeeperService zkService;
    private String myId;
    private String watchedLeader;

    public DealerResource(ZooKeeperService zkService) {
        this.zkService = zkService;
    }

    public synchronized void enable() {
        isEnabled.set(true);
    }

    public synchronized void setEnabled(boolean enabled) {
        if (enabled) {
            LOG.info("I am now leading!");
        }
        isEnabled.set(enabled);
    }

    @Override
    public void Disconnected() {
        LOG.warn("Lost connection, it is over!!");
        setEnabled(false);
        setMyId("");
    }

    @Override
    public synchronized void Connected() {
        LOG.info("Connected to ZK!");
        registerMe(zkService);
        if (!runLeaderElection(zkService)) {
            do {
            } while (!runLeaderElection(zkService));
        }
    }

    @Override
    public synchronized void Notify(WatchedEvent e) {
        switch (e.getType()) {
            case NodeDeleted:
                if (e.getPath().equals(ROOT_DEALERS + "/" + watchedLeader)) {
                    LOG.info("[Notify] The leader before me has died I must find a new Leader!");
                    // the leader before us has died, start an election
                    runLeaderElection(zkService);
                }
                break;
        }
    }

    private synchronized boolean runLeaderElection(ZooKeeperService zkService) {
        List<String> children = zkService.getChildren(ROOT_DEALERS, true);
        Collections.sort(children);

        String leader = children.get(0);
        LOG.info("children={}", children);

        boolean isLeader = leader.equals(myId);

        setEnabled(isLeader);

        // register a watch on the actual leader
        if (!isLeader) {
            String leaderAheadOfMe = children.get(children.indexOf(myId) - 1);

            String otherLeader = ROOT_DEALERS + "/" + leaderAheadOfMe;
            LOG.info("Watching leader: {}", otherLeader);
            if (!zkService.watchNode(otherLeader, true)) {
                // the node was removed while we were electing, run new election
                return false;
            }
            setWatchedLeader(leaderAheadOfMe);
        }
        return true;
    }

    private void registerMe(ZooKeeperService zkService) {
        // make sure parent exists
        try {
            zkService.createNode(ROOT_DEALERS, false, false);
        } catch (Exception e) {
        }
        // register us as a potential leader
        String myPath = zkService.createNode(DEALER_INSTANCE, false, true);
        setMyId(myPath);
    }

    synchronized public void setMyId(@NotNull String myId) {
        this.myId = StringUtils.removeStart(myId, ROOT_DEALERS+"/");
        LOG.info("I am at node={}", this.myId);
    }


    @GET
    @Path("/next")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNextNumber() {
        if (!isEnabled.get()) {
            return Response.status(503).entity("{\"error\": \"I am disabled!\"}").build();
        }
        int number = atomicInteger.getAndIncrement();
        String payload = "{\"next\": {Number}}".replace("{Number}", String.valueOf(number));

        return Response.ok(payload).build();
    }


    public void setWatchedLeader(String watchedLeader) {
        this.watchedLeader = watchedLeader;
    }
}
