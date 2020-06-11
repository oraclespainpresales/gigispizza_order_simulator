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

import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import org.eclipse.microprofile.rest.client.RestClientBuilder;

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
    private static final Logger LOGGER           = Logger.getLogger(SimulatorResource.class.getName());
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());
    private boolean databaseMode = false;    
    
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
     * Using constructor injection to get a RestClient creation (Interface OrderService.java).
     * By default this gets the value from META-INF/microprofile-config
     *
     * @param mp-rest/url the configured url to microservice orchestrator
     * @param mp-rest/connectTimeout connection timeout 
     * @param mp-rest/responseTimeout response timeout 
     * 
     * Alternative to Inject and configurable dinamically:
     * OrderService os = RestClientBuilder.newBuilder()
     *                              .baseUri(URI.create("https://madrid-gigispizza.wedoteam.io"))
     *                              .build(OrderService.class);
     */
    //@Inject
	//@RestClient
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

    private JsonObject setDataBaseMode (JsonObject dataBaseObj) throws Exception {
        JsonObject entity = null;
        if(!dataBaseObj.containsKey("date-ini")) {
            entity = JSON.createObjectBuilder()
                    .add("error", "No sim-config -> database -> date-ini provided")
                    .build();            
        }
        else if(!dataBaseObj.containsKey("date-format")) {
            entity = JSON.createObjectBuilder()
                    .add("error", "No sim-config -> database -> date-format provided")
                    .build();            
        }
        else if(!dataBaseObj.containsKey(("pizza-status"))) {
            entity = JSON.createObjectBuilder()
                    .add("error", "No sim-config -> database -> pizza-status provided")
                    .build();            
        }
        else if(!dataBaseObj.containsKey("connection-string")) {
            entity = JSON.createObjectBuilder()
                    .add("error", "No sim-config -> database -> connection-string provided")
                    .build();            
        }
        else if(!dataBaseObj.containsKey("database-user")) {
            entity = JSON.createObjectBuilder()
                    .add("error", "No sim-config -> database -> database-user provided")
                    .build();            
        }
        else if(!dataBaseObj.containsKey("database-password")) {
            entity = JSON.createObjectBuilder()
                    .add("error", "No sim-config -> database -> database-password provided")
                    .build();            
        }
        else {
            databaseMode = true;

            LOGGER.info ("DATA-BASE MODE ON");
            LOGGER.info ("DATA-BASE date-format      : " + dataBaseObj.getString("date-format"));
            LOGGER.info ("DATA-BASE date-ini         : " + dataBaseObj.getString("date-ini"));        
            LOGGER.info ("DATA-BASE pizza-status     : " + dataBaseObj.getString("pizza-status"));        
            LOGGER.info ("DATA-BASE connection-string: " + dataBaseObj.getString("connection-string"));
            LOGGER.info ("DATA-BASE database-user    : " + dataBaseObj.getString("database-user"));
            LOGGER.info ("DATA-BASE database-password: " + dataBaseObj.getString("database-password"));
        }

        return entity;
    }

    private JsonObject setMicroserviceMode (JsonObject jsonMsObj) throws Exception {
        JsonObject entity = null;
        if(!jsonMsObj.containsKey("url")) {
            entity = JSON.createObjectBuilder()
                    .add("error", "No sim-config -> microservice -> url provided")
                    .build();            
        }
        else if(!jsonMsObj.containsKey("connection-timeout")) {
            entity = JSON.createObjectBuilder()
                    .add("error", "No sim-config -> microservice -> connection-timeout provided")
                    .build();            
        }
        else if(!jsonMsObj.containsKey("response-timeout")) {
            entity = JSON.createObjectBuilder()
                    .add("error", "No sim-config -> microservice -> response-timeout provided")
                    .build();            
        }
        else{
            databaseMode = false;
            
            LOGGER.info ("MICROSERVICE MODE ON");
            LOGGER.info ("MICROSERVICE url                    : " + jsonMsObj.getString("url"));
            LOGGER.info ("MICROSERVICE connection-timeout (ms): " + jsonMsObj.getInt("connection-timeout"));
            LOGGER.info ("MICROSERVICE response-timeout (ms)  : " + jsonMsObj.getInt("response-timeout"));
        }
        return entity;
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
    @RequestBody(name     = "sim-config",
                 required = true,
                 content  = @Content(mediaType = "application/json",
                 schema   = @Schema(type = SchemaType.STRING, 
                 example  = "{\"sim-config\" : {\"num-orders\": 10,\"pizza-status\":\"ORDERED\"}}")))
    @APIResponses({
            @APIResponse(name = "normal", responseCode = "204", description = "orders creating"),
            @APIResponse(name = "missing 'sim-config'", responseCode = "400", description = "JSON did not contain setting for 'sim-config'")})
    public Response getCreateMessage(JsonObject jsonObject) {
        Response resp = null;
        try {
            if (jsonObject == null) {
                JsonObject entity = JSON.createObjectBuilder()
                        .add("error", "No sim-config provided")
                        .build();       
                resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
            }        
            else if (!jsonObject.containsKey("sim-config")) {
                JsonObject entity = JSON.createObjectBuilder()
                        .add("error", "No sim-config provided")
                        .build();            
                resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
            }            
            else if(!jsonObject.getJsonObject("sim-config").containsKey("num-orders")) {
                JsonObject entity = JSON.createObjectBuilder()
                        .add("error", "No sim-config -> num-orders provided")
                        .build();            
                resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
            }            
            else if(!jsonObject.getJsonObject("sim-config").containsKey("database")
                 && !jsonObject.getJsonObject("sim-config").containsKey("microservice")) {
                JsonObject entity = JSON.createObjectBuilder()
                        .add("error", "No sim-config -> database or microservice connection provided")
                        .build();            
                resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
            }
            else if(jsonObject.getJsonObject("sim-config").containsKey("database")) {
                JsonObject dataBaseObj = jsonObject.getJsonObject("sim-config").getJsonObject("database");
                JsonObject entity      = setDataBaseMode(dataBaseObj);
                if (entity != null)
                    resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
                else
                    resp = createOrders(dataBaseObj.getString("date-format"),
                                        dataBaseObj.getString("date-ini"),
                                        jsonObject.getJsonObject("sim-config").getInt("num-orders"),
                                        dataBaseObj.getString("pizza-status"));
            }
            else if(!databaseMode && jsonObject.getJsonObject("sim-config").containsKey("microservice")) {
                JsonObject jsonMsObj = jsonObject.getJsonObject("sim-config").getJsonObject("microservice");
                JsonObject entity    = setMicroserviceMode(jsonMsObj);
                if (entity != null)
                    resp = Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
                else{
                    msOrchestrator = RestClientBuilder.newBuilder()
                                    .baseUri(URI.create(jsonMsObj.getString("url")))
                                    .connectTimeout(jsonMsObj.getInt("connection-timeout"), TimeUnit.MILLISECONDS)
                                    .readTimeout(jsonMsObj.getInt("response-timeout"), TimeUnit.MILLISECONDS)                                    
                                    .build(OrderService.class);
                    resp = createOrders(jsonObject.getJsonObject("sim-config").getInt("num-orders"));
                }
            }
            
            return resp;
                   
        }
        catch(Exception ex){
            LOGGER.log(Level.SEVERE,ex.getMessage());
            ex.printStackTrace();
            JsonObject entity = JSON.createObjectBuilder()
                        .add("error", "problem with json config")
                        .build();            
            return Response.status(Response.Status.BAD_REQUEST).entity(entity).build();
        }
    }

    private String getDateTimeZFormat(Date dateCal){
        SimpleDateFormat formatReturn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        return formatReturn.format(dateCal);
    }

    private String getOrderIdFromDate(Calendar dateCal){
        StringBuilder strOrderId = new StringBuilder()
            .append(dateCal.get(Calendar.YEAR))
            .append((dateCal.get(Calendar.MONTH) + 1) < 10? "0" + (dateCal.get(Calendar.MONTH) + 1) : (dateCal.get(Calendar.MONTH) + 1))
            .append(dateCal.get(Calendar.DATE)        < 10? "0" + dateCal.get(Calendar.DATE)        : dateCal.get(Calendar.DATE))
            .append(dateCal.get(Calendar.HOUR)        < 10? "0" + dateCal.get(Calendar.HOUR)        : dateCal.get(Calendar.HOUR))
            .append(dateCal.get(Calendar.MINUTE)      < 10? "0" + dateCal.get(Calendar.MINUTE)      : dateCal.get(Calendar.MINUTE))
            .append(dateCal.get(Calendar.SECOND)      < 10? "0" + dateCal.get(Calendar.SECOND)      : dateCal.get(Calendar.SECOND))
            .append(genNumber(100, 999));

        return strOrderId.toString();
    }

    private String[] getOrderIdAndDateTime (String dateFormat, String dateIni, int num) {        
        String [] strDates         = new String[2];
        Calendar cal               = Calendar.getInstance();
        SimpleDateFormat formatIni = new SimpleDateFormat(dateFormat);                
        Date dateCal               = null;

        try {
            dateCal = formatIni.parse(dateIni);
            //set calendar datetime and adds num seconds
            cal.setTime(dateCal);
            cal.add(Calendar.SECOND, num);
            
            //get DateTime in Z format
            strDates[0] = getDateTimeZFormat(cal.getTime());
            //get orderId from cal Date
            strDates[1] = getOrderIdFromDate(cal); 
        }
        catch (ParseException parseEx){
            LOGGER.log(Level.SEVERE,"ERROR: ParseException - " + parseEx.getMessage());
        }

        return strDates;
    }

    private int genNumber(int low, int high){
        Random r = new Random();
        return r.nextInt(high-low) + low;
    }

    private String genPaymentMethod(int typeCard){
        String cardType = "VISA";
        switch (typeCard){
            case 0: cardType  = "AMEX"; break;
            case 1: cardType  = "MASTERCARD"; break;
            case 2: cardType  = "VISA"; break;            
            default: cardType = "CASH"; break;
        }

        return cardType;
    }

    private String genBasePizza(){
        String [] size = {"Small",
                          "Medium",
                          "Large",
                          "X-Large"};
        String [] base = {"BACON SPINACH ALFREDO",
                          "CHEESE BASIC",
                          "HAWAIIAN CHICKEN",
                          "MEAT LOVER",
                          "PEPPERONI",
                          "PREMIUM GARDEN VEGGIE",
                          "SUPPREME",
                          "ULTIMATE CHEESE LOVER"};

        return size[genNumber(0, size.length)] + " " + base[genNumber(0, base.length)];
    }

    private String[] genToppings(){
        String [] toppings = {"Tuna","Onions","BBQ Sauce","Tomatos","Mushrooms"};
        int index          = 0;
        int [] selected    = {0,0,0};
        ArrayList<Integer> selectables = new ArrayList<Integer>(5);
        selectables.add(0);
        selectables.add(1);
        selectables.add(2);
        selectables.add(3);
        selectables.add(4);

        //LOGGER.info("INFO:: " + selectables.toString());
        for (int i=0;i < 3;i++){
            index = genNumber(0, selectables.size());
            //LOGGER.info("INFO:: " + index);
            selected [i] = selectables.get(index);            
            selectables.remove(index);
            //LOGGER.info("INFO:: " + selectables.toString());
        }

        return new String [] {toppings[selected[0]],toppings[selected[1]],toppings[selected[2]]};
    }

    /**
     * Used in the MICROSERVICE mode to gen a pizza order with the current Date.
     *
     * @return {@link Response}
     */
    private Response createOrders(int numOrders) throws Exception {   
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return createOrders("dd/MM/yyyy HH:mm:ss", sdf.format(new Date()), numOrders, "PIZZA ORDERED");
    }

    /**
     * Used in the DATABASE mode to gen a pizza order with the json date-ini param.
     *
     * @return {@link Response}
     */
    private Response createOrders (String dateFormat, String date, int numOrders, String pizzaStatus) {
        Response resp;
        LOGGER.info("DATE-INI: " + date);
        try {
            JsonObject pizzaOrder = createOrder(dateFormat,date,numOrders,pizzaStatus);
            LOGGER.info("PIZZA ORDER: " + pizzaOrder);
            JsonObject entity = null;
            if (databaseMode){
                entity = pizzaOrder;
            }
            else {
                entity = msOrchestrator.createOrder(pizzaOrder);
            }
            //LOGGER.info("orderreturn: " + msOrchestrator.createOrder().toString());
            resp = Response.status(Response.Status.ACCEPTED)
                        .entity(entity)
                        .build();
        }
        catch (Exception ex){
            JsonObject entity = JSON.createObjectBuilder()
                    .add("error", "problem with order creation")
                    .build();   
            LOGGER.log(Level.SEVERE,"ERROR createOrders: " + ex.getMessage());
            ex.printStackTrace();
            resp = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(entity).build();
        }
        return resp;
    }
    
    private JsonObject createOrder(String dateFormat, String date, int numOrders, String pizzaStatus) throws Exception{
        String[] strOrderDate = getOrderIdAndDateTime(dateFormat, date, numOrders);
        String[] strToppings  = genToppings();
        int totalPrice        = genNumber(10,20);
        int originalPrice     = totalPrice + genNumber(0, 2);

        JsonObject jsonOBJPaymentBody =  JSON.createObjectBuilder()
            .add("paymentid", "p" + strOrderDate[1])
            .add("paymentTime", strOrderDate[0])
            .add("orderId", strOrderDate[1])
            .add("paymentMethod", String.valueOf(genPaymentMethod(genNumber(0, 4))))
            .add("serviceSurvey", String.valueOf(genNumber(1, 6)))
            .add("totalPaid", String.valueOf(totalPrice))
            .add("customerId", "sim345")
            .add("originalPrice", String.valueOf(originalPrice))
            .build();        
        JsonObject jsonOBJStreetBody = JSON.createObjectBuilder()            
            .add("name", "SimStreet")
            .add("long", "-3.692763")
            .add("lat", "40.484408")
            .build();
        JsonObject jsonOBJCustomerAddrBody = JSON.createObjectBuilder()            
            .add("street", jsonOBJStreetBody)            
            .add("number", String.valueOf(genNumber(1, 100)))
            .add("door", String.valueOf(genNumber(1, 5)))
            .add("email", "ivan.smith@sim-email.es")
            .add("citycode", String.valueOf(genNumber(28001, 28039)))
            .add("city", "Madrid")            
            .build();
        JsonObject jsonOBJPizzaOrderedBody = JSON.createObjectBuilder()                                    
            .add("baseType", genBasePizza())
            .add("topping1", strToppings[0])
            .add("topping2", strToppings[1])
            .add("topping3", strToppings[2])            
            .build();
        JsonObject jsonOBJCustomerIdBody = JSON.createObjectBuilder()                                    
            .add("telephone", String.valueOf(genNumber(601000000, 678000000)))
            .add("email", "ivan.smith@sim-email.es")                  
            .build();
        JsonObject jsonOBJCustomerId = JSON.createObjectBuilder()                                    
            .add("customerId", jsonOBJCustomerIdBody)                           
            .build();
        JsonObject jsonOBJOrderBody = JSON.createObjectBuilder()
            .add("dateTimeOrderTaken",strOrderDate[0])
            .add("takenByEmployee","sim001")
            .add("customer",jsonOBJCustomerId)
            .add("pizzaOrdered",jsonOBJPizzaOrderedBody)
            .add("totalPrice",totalPrice + "$")
            .add("customerAdress",jsonOBJCustomerAddrBody)                    
            .build();
        
        return JSON.createObjectBuilder()
                   .add("order",jsonOBJOrderBody)
                   .add("payment",jsonOBJPaymentBody)
                   .add("status",pizzaStatus)
                   .build();
    }

}
