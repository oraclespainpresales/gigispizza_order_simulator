package io.helidon.examples.quickstart.mp;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
//import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "microservice-Orchestrator")
@ApplicationScoped
public interface OrderService {
    @GET
    @Path("/version")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject version();

    @GET
    @Path("/getAllOrders")
    @Produces(MediaType.APPLICATION_JSON)    
    public JsonArray getAllOrders();

    @POST
    @Path("/createOrder")        
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public JsonObject createOrder(JsonObject order);
}