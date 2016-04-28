package no.bekk.distsys.robustness.dealer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Path("/dealer")
public class DealerResource {

    private AtomicInteger atomicInteger = new AtomicInteger();
    // Start with enabled to false so we dont start dealing numbers without being leader
    private AtomicBoolean isEnabled = new AtomicBoolean(false);

    public synchronized void enable() {
        isEnabled.set(true);
    }
    public synchronized void setEnabled(boolean enabled) {
        isEnabled.set(enabled);
    }

    @GET
    @Path("/next")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNextNumber() {
        if (!isEnabled.get()) {
            return Response.status(503).entity("{\"error\": \"I am disabled!\"}").build();
        }
        int number = atomicInteger.getAndIncrement();
        String payload = "{\"next\": {Number}}".replace(String.valueOf(number), "{Number}");

        return Response.ok(payload).build();
    }

}
