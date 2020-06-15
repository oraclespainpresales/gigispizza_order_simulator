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
 * 
 * Created by: Iván Sampedro Postigo -> ivan.sampedro@oracle.com
 */

package io.helidon.examples.quickstart.mp;

import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue.ValueType;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

/**
 * A simple JAX-RS resource to greet you. Examples:
 *
 * Get default greeting message: curl -X GET http://localhost:8080/greet
 *
 * Get greeting message for Joe: curl -X GET http://localhost:8080/greet/Joe
 *
 * Change greeting curl -X PUT -H "Content-Type: application/json" -d
 * '{"greeting" : "Howdy"}' http://localhost:8080/greet/greeting
 *
 * The message is returned as a JSON object.
 */
@Path("/simulator")
@RequestScoped
public class SimulatorResource {
    private static final Logger LOGGER           = Logger.getLogger(SimulatorResource.class.getName());
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());
    private boolean databaseMode                 = false;

    @Inject
    @ConfigProperty(name="minThreads", defaultValue="20")
    private int minThreads;

    @Inject
    @ConfigProperty(name="maxThreads", defaultValue="20")
    private int maxThreads;

    /**
     * The greeting message provider.
     */
    private final SimulatorProvider simulatorProvider;

    /**
     * Using constructor injection to get a configuration property. By default this
     * gets the value from META-INF/microprofile-config
     *
     * @param greetingConfig the configured greeting message
     */
    @Inject
    public SimulatorResource(SimulatorProvider simulatorConfig) {
        this.simulatorProvider = simulatorConfig;
    }

    /**
     * Using constructor injection to get a RestClient creation (Interface
     * OrderService.java). By default this gets the value from
     * META-INF/microprofile-config
     *
     * @param mp-rest/url             the configured url to microservice
     *                                orchestrator
     * @param mp-rest/connectTimeout  connection timeout
     * @param mp-rest/responseTimeout response timeout
     * 
     *                                Alternative to Inject and configurable
     *                                dinamically: OrderService os =
     *                                RestClientBuilder.newBuilder()
     *                                .baseUri(URI.create("https://madrid-gigispizza.wedoteam.io"))
     *                                .build(OrderService.class);
     */
    // @Inject
    // @RestClient
    private OrderService msOrchestrator;

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
    @RequestBody(name = "greeting", required = true, content = @Content(mediaType = "application/json", schema = @Schema(type = SchemaType.STRING, example = "{\"greeting\" : \"Holas\"}")))
    @APIResponses({ @APIResponse(name = "normal", responseCode = "204", description = "Greeting updated"),
            @APIResponse(name = "missing 'greeting'", responseCode = "400", description = "JSON did not contain setting for 'greeting'") })
    public Response updateGreeting(JsonObject jsonObject) {

        if (!jsonObject.containsKey("greeting")) {
            JsonObject entity = JSON.createObjectBuilder().add("error", "No greeting provided").build();
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }

        String newGreeting = jsonObject.getString("greeting");

        simulatorProvider.setMessage(newGreeting);
        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private JsonObject createResponse(String who) {
        String msg = String.format("%s %s!", simulatorProvider.getMessage(), who);

        return JSON.createObjectBuilder().add("message", msg).build();
    }

    private JsonObject setDataBaseMode(JsonObject dataBaseObj) throws Exception {
        JsonObject entity = null;
        if (!dataBaseObj.containsKey("date-ini")) {
            entity = JSON.createObjectBuilder().add("error", "No sim-config -> database -> date-ini provided").build();
        } else if (!dataBaseObj.containsKey("date-format")) {
            entity = JSON.createObjectBuilder().add("error", "No sim-config -> database -> date-format provided")
                    .build();
        } else if (!dataBaseObj.containsKey("connection-string")) {
            entity = JSON.createObjectBuilder().add("error", "No sim-config -> database -> connection-string provided")
                    .build();
        } else if (!dataBaseObj.containsKey("client-credentials")) {
            entity = JSON.createObjectBuilder().add("error", "No sim-config -> database -> client-credentials provided")
                    .build();
        } else if (!dataBaseObj.containsKey("keystore-password")) {
            entity = JSON.createObjectBuilder().add("error", "No sim-config -> database -> keystore-password provided")
                    .build();
        } else if (!dataBaseObj.containsKey("truststore-password")) {
            entity = JSON.createObjectBuilder().add("error", "No sim-config -> database -> truststore-password provided")
                    .build();
        } else if (!dataBaseObj.containsKey("user")) {
            entity = JSON.createObjectBuilder().add("error", "No sim-config -> database -> user provided")
                    .build();
        } else if (!dataBaseObj.containsKey("password")) {
            entity = JSON.createObjectBuilder().add("error", "No sim-config -> database -> password provided")
                    .build();
        } else {
            databaseMode = true;

            LOGGER.info("DATA-BASE MODE ON");
            LOGGER.info("DATA-BASE date-format        : " + dataBaseObj.getString("date-format"));
            LOGGER.info("DATA-BASE date-ini           : " + dataBaseObj.getString("date-ini"));
            LOGGER.info("DATA-BASE connection-string  : " + dataBaseObj.getString("connection-string"));
            LOGGER.info("DATA-BASE client-credentials : " + dataBaseObj.getString("client-credentials"));
            LOGGER.info("DATA-BASE keystore-password  : " + dataBaseObj.getString("keystore-password"));
            LOGGER.info("DATA-BASE truststore-password: " + dataBaseObj.getString("truststore-password"));
            LOGGER.info("DATA-BASE user               : " + dataBaseObj.getString("user"));
            LOGGER.info("DATA-BASE password           : " + dataBaseObj.getString("password"));
        }

        return entity;
    }

    private JsonObject setMicroserviceMode(JsonObject jsonMsObj) throws Exception {
        JsonObject entity = null;
        if (!jsonMsObj.containsKey("url")) {
            entity = JSON.createObjectBuilder().add("error", "No sim-config -> microservice -> url provided").build();
        } else if (!jsonMsObj.containsKey("connection-timeout")) {
            entity = JSON.createObjectBuilder()
                    .add("error", "No sim-config -> microservice -> connection-timeout provided").build();
        } else if (!jsonMsObj.containsKey("response-timeout")) {
            entity = JSON.createObjectBuilder()
                    .add("error", "No sim-config -> microservice -> response-timeout provided").build();
        } else {
            databaseMode = false;

            LOGGER.info("MICROSERVICE MODE ON");
            LOGGER.info("MICROSERVICE url                    : " + jsonMsObj.getString("url"));
            LOGGER.info("MICROSERVICE connection-timeout (ms): " + jsonMsObj.getInt("connection-timeout"));
            LOGGER.info("MICROSERVICE response-timeout (ms)  : " + jsonMsObj.getInt("response-timeout"));
        }
        return entity;
    }

    private Response verifyThreadsJsonProperties(JsonObject jsonObject, String threadField){
        Response resp = null;
        if (jsonObject.containsKey((threadField))) {   
            if (jsonObject.get(threadField).getValueType() == ValueType.STRING){
                if (threadField.equals("min-threads"))
                    minThreads = Integer.parseInt(jsonObject.getString(threadField));
                else
                    maxThreads = Integer.parseInt(jsonObject.getString(threadField));
            }            
            else if (jsonObject.get(threadField).getValueType() == ValueType.NUMBER){
                if (threadField.equals("min-threads"))
                    minThreads = jsonObject.getInt(threadField);
                else
                    maxThreads = jsonObject.getInt(threadField);
            }
            else {
                JsonObject entity = JSON.createObjectBuilder()
                    .add("error", threadField + " -> type missmatch").build();
                resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
            }
        }
        return resp;
    }

    /**
     * Return a wordly greeting message.
     *
     * @return {@link Response}
     */
    @SuppressWarnings("checkstyle:designforextension")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RequestBody(name = "sim-config", 
        required = true, 
        content = @Content(mediaType = "application/json", 
        schema = @Schema(type = SchemaType.STRING, example = "{\"sim-config\" : {\"num-orders\": 10,\"pizza-status\":\"ORDERED\"}}")))
    @APIResponses({ @APIResponse(name = "normal", responseCode = "204", description = "orders creating"),
    @APIResponse(name = "missing 'sim-config'", responseCode = "400", description = "JSON did not contain setting for 'sim-config'") })
    public Response getCreateMessage(JsonObject jsonObject) {
        Response resp  = null;
        try {
            if (jsonObject == null) {
                JsonObject entity = JSON.createObjectBuilder().add("error", "No sim-config provided").build();
                resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
            } else if (!jsonObject.containsKey("sim-config")) {
                JsonObject entity = JSON.createObjectBuilder().add("error", "No sim-config provided").build();
                resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
            } else if (!jsonObject.getJsonObject("sim-config").containsKey("num-orders")) {
                JsonObject entity = JSON.createObjectBuilder().add("error", "No sim-config -> num-orders provided")
                        .build();
                resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
            } else if (!jsonObject.getJsonObject("sim-config").containsKey(("pizza-status"))) {
                JsonObject entity = JSON.createObjectBuilder().add("error", "No sim-config -> pizza-status provided")
                        .build();
                resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();            
            } else if (!jsonObject.getJsonObject("sim-config").containsKey("database")
                    && !jsonObject.getJsonObject("sim-config").containsKey("microservice")) {
                JsonObject entity = JSON.createObjectBuilder()
                        .add("error", "No sim-config -> database or microservice connection provided").build();
                resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();                
            } else if (jsonObject.getJsonObject("sim-config").containsKey("database")) {
                JsonObject dataBaseObj = jsonObject.getJsonObject("sim-config").getJsonObject("database");
                JsonObject entity      = setDataBaseMode(dataBaseObj);
                if (entity != null)
                    resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
                else{
                    if((resp = verifyThreadsJsonProperties(jsonObject.getJsonObject("sim-config"), "min-threads")) == null){
                        if((resp = verifyThreadsJsonProperties(jsonObject.getJsonObject("sim-config"), "max-threads")) == null){
                            PizzaOrder pizzaOrder = new PizzaOrder(minThreads, maxThreads, true,
                                                                    dataBaseObj.getString("connection-string"),
                                                                    dataBaseObj.getString("user"),
                                                                    dataBaseObj.getString("password"),
                                                                    dataBaseObj.getString("client-credentials"),
                                                                    dataBaseObj.getString("keystore-password"),
                                                                    dataBaseObj.getString("truststore-password"));
                            resp = pizzaOrder.createOrders(dataBaseObj.getString("date-format"), 
                                                           dataBaseObj.getString("date-ini"),
                                                           jsonObject.getJsonObject("sim-config").getInt("num-orders"),
                                                           jsonObject.getJsonObject("sim-config").getString("pizza-status"));
                        }
                    }                    
                }
            } else if (!databaseMode && jsonObject.getJsonObject("sim-config").containsKey("microservice")) {
                JsonObject jsonMsObj = jsonObject.getJsonObject("sim-config").getJsonObject("microservice");
                JsonObject entity    = setMicroserviceMode(jsonMsObj);
                if (entity != null)
                    resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
                else {
                    if((resp = verifyThreadsJsonProperties(jsonObject.getJsonObject("sim-config"), "min-threads")) == null){
                        if((resp = verifyThreadsJsonProperties(jsonObject.getJsonObject("sim-config"), "max-threads")) == null){
                            
                            PizzaOrder pizzaOrder = new PizzaOrder(minThreads, maxThreads, jsonMsObj.getString("url"), 
                                                                                           jsonMsObj.getInt("connection-timeout"),
                                                                                           jsonMsObj.getInt("response-timeout"));
                            resp = pizzaOrder.createOrders(jsonObject.getJsonObject("sim-config").getInt("num-orders"),
                                                           jsonObject.getJsonObject("sim-config").getString("pizza-status"));
                        }
                    } 
                }
            }

            return resp;
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
            ex.printStackTrace();
            JsonObject entity = JSON.createObjectBuilder().add("error", "problem with json config")
                                                          .add("error-mess",ex.getMessage()).build();
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }
    }
}
