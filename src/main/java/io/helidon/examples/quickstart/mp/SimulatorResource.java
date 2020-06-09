/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.examples.quickstart.mp;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;

/**
 * A simple JAX-RS resource to greet you. Examples:
 *
 * Get default greeting message:
 * curl -X GET http://localhost:8080/greet
 *
 * Get greeting message for Joe:
 * curl -X GET http://localhost:8080/greet/Joe
 *
 * Change greeting
 * curl -X PUT -H "Content-Type: application/json" -d '{"greeting" : "Howdy"}' http://localhost:8080/greet/greeting
 *
 * The message is returned as a JSON object.
 */
@Path("/simulator")
@RequestScoped
public class SimulatorResource {

    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    /**
     * The greeting message provider.
     */
    private final SimulatorProvider simulatorProvider;

    /**
     * Using constructor injection to get a configuration property.
     * By default this gets the value from META-INF/microprofile-config
     *
     * @param greetingConfig the configured greeting message
     */
    @Inject
    public SimulatorResource(SimulatorProvider simulatorConfig) {
        this.simulatorProvider = simulatorConfig;
    }

    /**
     * Return a wordly greeting message.
     *
     * @return {@link JsonObject}
     */
    @SuppressWarnings("checkstyle:designforextension")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getDefaultMessage() {
        return createResponse("World");
    }

    /**
     * Return a greeting message using the name that was provided.
     *
     * @param name the name to greet
     * @return {@link JsonObject}
     */
    @SuppressWarnings("checkstyle:designforextension")
    @Path("/{name}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getMessage(@PathParam("name") String name) {
        return createResponse(name);
    }

    /**
     * Set the greeting to use in future messages.
     *
     * @param jsonObject JSON containing the new greeting
     * @return {@link Response}
     */
    @SuppressWarnings("checkstyle:designforextension")
    @Path("/saludos")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequestBody(name = "greeting",
            required = true,
            content = @Content(mediaType = "application/json",
                    schema = @Schema(type = SchemaType.STRING, example = "{\"greeting\" : \"Holas\"}")))
    @APIResponses({
            @APIResponse(name = "normal", responseCode = "204", description = "Greeting updated"),
            @APIResponse(name = "missing 'greeting'", responseCode = "400",
                    description = "JSON did not contain setting for 'greeting'")})
    public Response updateGreeting(JsonObject jsonObject) {

        if (!jsonObject.containsKey("greeting")) {
            JsonObject entity = JSON.createObjectBuilder()
                    .add("error", "No greeting provided")
                    .build();
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }

        String newGreeting = jsonObject.getString("greeting");

        simulatorProvider.setMessage(newGreeting);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private JsonObject createResponse(String who) {
        String msg = String.format("%s %s!", simulatorProvider.getMessage(), who);

        return JSON.createObjectBuilder()
                .add("message", msg)
                .build();
    }

    /**
     * Return a wordly greeting message.
     *
     * @return {@link JsonObject}
     */
    @SuppressWarnings("checkstyle:designforextension")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public JsonObject getCreateMessage() {
        return createOrder();
    }

    private String getDateTimeZFormat(){
        Calendar cal            = Calendar.getInstance();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        String dateForId        = format.format(cal.getTime());
        return dateForId;
    }

    private JsonObject createOrder() {
        JsonObject jsonOBJPaymentBody =  JSON.createObjectBuilder()
            .add("paymentid", "Test")
            .add("paymentTime", "Test")
            .add("orderId", "Test")
            .add("paymentMethod", "Test")
            .add("serviceSurvey", "Test")
            .add("totalPaid", "Test")
            .add("customerId", "Test")
            .add("originalPrice", "Test")
            .build();        
        JsonObject jsonOBJStreetBody = JSON.createObjectBuilder()            
            .add("name", "Test")
            .add("long", "Test")
            .add("lat", "Test")
            .build();
        JsonObject jsonOBJCustomerAddrBody = JSON.createObjectBuilder()            
            .add("street", jsonOBJStreetBody)            
            .add("number", "Test")
            .add("door", "Test")
            .add("email", "Test")
            .add("citycode", "Test")
            .add("city", "Test")            
            .build();
        JsonObject jsonOBJPizzaOrderedBody = JSON.createObjectBuilder()                                    
            .add("baseType", "Test")
            .add("topping1", "Test")
            .add("topping2", "Test")
            .add("topping3", "Test")            
            .build();
        JsonObject jsonOBJCustomerIdBody = JSON.createObjectBuilder()                                    
            .add("telephone", "Test")
            .add("email", "Test")                  
            .build();
        JsonObject jsonOBJCustomerId = JSON.createObjectBuilder()                                    
            .add("customerId", jsonOBJCustomerIdBody)                           
            .build();

        JsonObject jsonOBJOrderBody = JSON.createObjectBuilder()
            .add("dateTimeOrderTaken",getDateTimeZFormat())
            .add("takenByEmployee","")
            .add("customer",jsonOBJCustomerId)
            .add("pizzaOrdered",jsonOBJPizzaOrderedBody)
            .add("totalPrice","")
            .add("customerAdress",jsonOBJCustomerAddrBody)
            .add("payment",jsonOBJPaymentBody)
            .add("status","ORDERED")
            .build();

        return JSON.createObjectBuilder().add("order",jsonOBJOrderBody).build();
    }

}
