package io.helidon.examples.quickstart.mp;

import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

public class PizzaOrderMS extends PizzaOrder{
    private static final Logger LOGGER = Logger.getLogger(PizzaOrderMS.class.getName());
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    private OrderService msOrchestrator = null;

    public PizzaOrderMS(int minThreads, int maxThreads, String baseURL, int connTimeout, int respTimeout){
        super (minThreads, maxThreads);

        //Create RestClient object to send rest commands to microservice orchestrator:
        LOGGER.info("Creating RestClient. base URL: " + baseURL);

        //To avoid problem if we change to DB connection.
        System.clearProperty("javax.net.ssl.keyStore");
        System.clearProperty("javax.net.ssl.keyStorePassword");
        System.clearProperty("javax.net.ssl.trustStore");
        System.clearProperty("javax.net.ssl.trustStorePassword");

        this.msOrchestrator = RestClientBuilder.newBuilder().baseUri(URI.create(baseURL))
                                            .connectTimeout(connTimeout, TimeUnit.MILLISECONDS)
                                            .readTimeout(respTimeout, TimeUnit.MILLISECONDS)
                                            .build(OrderService.class);
        LOGGER.info(this.msOrchestrator.version().toString());
        LOGGER.info("RestClient Created.");         
    }

    /**
     * Used in the MICROSERVICE mode to gen a pizza order with the current Date.
     *
     * @return {@link Response}
     */
    public Response createOrders(int numOrders, String pizzaStatus) throws Exception {
        return createOrders("dd/MM/yyyy HH:mm:ss", "", numOrders, pizzaStatus);
    }

    /**
     * Used in the DATABASE mode to gen a pizza order with the json date-ini param.
     *
     * @return {@link Response}
     */
    public Response createOrders(String dateFormat, String date, int numOrders, String pizzaStatus) {
        Response resp;
        JsonArrayBuilder orders = Json.createArrayBuilder();
        //SimpleDateFormat sdf    = new SimpleDateFormat(dateFormat);
        try {
            orders = createOrdersWithMicroservices(dateFormat,numOrders,pizzaStatus);
            //LOGGER.info("orderreturn: " + msOrchestrator.createOrder().toString());
            resp = Response.status(Response.Status.ACCEPTED)
                        .entity(Json.createObjectBuilder().add("orders",orders).build())
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

    private JsonArrayBuilder createOrdersWithMicroservices(String dateFormat, int numOrders, String pizzaStatus) throws Exception {
        JsonArrayBuilder orders = Json.createArrayBuilder();
        SimpleDateFormat sdf    = new SimpleDateFormat(dateFormat);

        LOGGER.info("ThreadPoolCreation: minThreads["+minThreads+"] | maxThreads["+maxThreads+"]" );
        ExecutorService executorService = new ThreadPoolExecutor(minThreads, maxThreads, 0L, TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>()); 

        //Create Pizza Order Task.
        Callable<String> callableTaskMicroservice = () -> {
            JsonValue orderId = null;
            try {                                                
                JsonObject pizzaOrder = createJsonPizzaOrder(dateFormat, sdf.format(new Date()), 0, pizzaStatus);
                LOGGER.info("PIZZA ORDER ["+Thread.currentThread().getId()+"]: Pizza Order to Create: " + pizzaOrder);
                if (msOrchestrator == null) 
                    LOGGER.info("PIZZA ORDER ["+Thread.currentThread().getId()+"]: msOrchestrator null");
                JsonObject pizzaOrderResp = msOrchestrator.createOrder(pizzaOrder);
                LOGGER.info("PIZZA ORDER ["+Thread.currentThread().getId()+"]: Pizza Created" + pizzaOrderResp);
                orderId = pizzaOrderResp.getJsonObject("resJSONDB").getValue("/orderId");
                //orders.add(pizzaOrder);
                //orders.add(pizzaOrderResp);
                JsonObject updateStatus = JSON.createObjectBuilder()
                                            .add("orderId",orderId)
                                            .add("status",pizzaStatus)
                                            .build();
                
                LOGGER.info("PIZZA RESP ["+Thread.currentThread().getId()+"]: " + updateStatus);
                //orders.add(msOrchestrator.changeStatus(updateStatus));
                LOGGER.info("PIZZA RESP ["+Thread.currentThread().getId()+"]: " + msOrchestrator.changeStatus(updateStatus));
                //JsonObject respStatus = msOrchestrator.changeStatus(updateStatus);
            }
            catch (Exception ex){
                ex.printStackTrace();
                LOGGER.log(Level.SEVERE, "ERROR Task " + ex.getMessage());                
                orderId = JsonValue.FALSE;          
            }

            return orderId.toString();
        };
        
        
        List<Callable<String>> callableTasks = new ArrayList<>();
        for (int task=0;task<numOrders;task++){
            callableTasks.add(callableTaskMicroservice);
        }
        LocalDateTime dIni = LocalDateTime.now();
        LOGGER.info("Task Start! at " + dIni);
        List<Future<String>> futureList = executorService.invokeAll(callableTasks); 
        //orders.add(futureList.get(0).get());
        executorService.shutdown();   
        //executorService.awaitTermination();
        for (int task=0;task<numOrders;task++){
            orders.add(JSON.createObjectBuilder().add("order",task).add("orderId",futureList.get(task).get()));
        }
        
        LocalDateTime dEnd = LocalDateTime.now();
        LOGGER.info("Task Ended! at " + dEnd);
        Duration duration = Duration.between(dEnd, dIni);
        long diffMin = Math.abs(duration.toMinutes());            
        long diffSec = Math.abs(duration.toSeconds()) - (diffMin*60); 
        LOGGER.info("Time Taken! -- " + diffMin + " minutes " + diffSec + " seconds");
        
        return orders;
    }
}