package no.bekk.distsys.robustness.pinger;

import com.codahale.metrics.annotation.Timed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/answer")
@Produces(MediaType.APPLICATION_JSON)
public class PingResource {

    private final Pinger pinger;

    public PingResource(Pinger pinger) {
        this.pinger = pinger;
    }

    @GET
    @Timed
    public Response getAnswer() {
        Pong answer = pinger.Ping();

        return Response.ok(answer).build();
    }
}
