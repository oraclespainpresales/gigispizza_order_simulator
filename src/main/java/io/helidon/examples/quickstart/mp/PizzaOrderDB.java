package io.helidon.examples.quickstart.mp;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
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
import javax.ws.rs.core.Response;


public class PizzaOrderDB extends PizzaOrder {
    private static final Logger LOGGER = Logger.getLogger(PizzaOrderDB.class.getName());
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    private String dbUser             = "";
    private String dbPassword         = "";
    private String dbUrl              = "";
    private String clientCred         = ""; 
    private String keystorePassword   = ""; 
    private String truststorePassword = ""; 

    private int seconds = 1;

    public PizzaOrderDB(int minThreads, int maxThreads, 
                      String dbUrl, String dbUser, String dbPass, 
                      String clientCred,
                      String keystorePassword,
                      String truststorePassword){

        super(minThreads, maxThreads);

        this.truststorePassword = truststorePassword;        
        this.keystorePassword   = keystorePassword;
        
        this.clientCred = clientCred;        
        this.dbUrl      = dbUrl;          
        this.dbUser     = dbUser;
        this.dbPassword = dbPass;       
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
            LOGGER.info("DATE-INI: " + date);
            orders = createOrdersWithDataBase(dateFormat,date,numOrders,pizzaStatus);
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

    private JsonArrayBuilder createOrdersWithDataBase (String dateFormat, String date, int numOrders, String pizzaStatus) throws Exception {
        JsonArrayBuilder orders = Json.createArrayBuilder();
        //SimpleDateFormat sdf    = new SimpleDateFormat(dateFormat);

        LOGGER.info("ThreadPoolCreation: minThreads["+minThreads+"] | maxThreads["+maxThreads+"]" );
        ExecutorService executorService = new ThreadPoolExecutor(minThreads, maxThreads, 0L, TimeUnit.MICROSECONDS, new LinkedBlockingQueue<Runnable>()); 
        
        //Create Pizza Order Database Task.
        Callable<String> callableTaskDb = () -> {
            String orderId = null;            
            try {                                                
                JsonObject jsonPizzaOrder = createJsonPizzaOrder(dateFormat, date, seconds++, pizzaStatus);
                //LOGGER.info("PIZZA ORDER ["+Thread.currentThread().getId()+"]: " + pizzaOrder);
                JsonObject pizzaPayment = jsonPizzaOrder.getJsonObject("payment");
                JsonObject pizzaOrder   = jsonPizzaOrder.getJsonObject("order");
                orderId = pizzaOrder.getString("orderId");
                LOGGER.info("PIZZA Payment ["+Thread.currentThread().getId()+"]: " + pizzaPayment.toString());                                
                LOGGER.info("PIZZA Order   ["+Thread.currentThread().getId()+"]: " + pizzaOrder.toString());

                DatabaseClient dbClient = new DatabaseClient(dbUrl,dbUser,dbPassword,clientCred,keystorePassword,truststorePassword);
                LOGGER.info("dbClient Created created");
                LOGGER.info(dbClient.executeInsertOrder(pizzaOrder));
                LOGGER.info(dbClient.executeInsertPayment(pizzaPayment));
                LOGGER.info(dbClient.executeUpdateIngredients(pizzaOrder));
            }
            catch (Exception ex){
                ex.printStackTrace();
                LOGGER.log(Level.SEVERE, "ERROR Task " + ex.getMessage());                
                orderId = "false";
            }

            return orderId.toString();
        };
                
        List<Callable<String>> callableTasksDb = new ArrayList<>();
        for (int task=0;task<numOrders;task++){
            callableTasksDb.add(callableTaskDb);
        }
        LocalDateTime dIni = LocalDateTime.now();
        LOGGER.info("Tasks Start! at " + dIni);
        List<Future<String>> futureList = executorService.invokeAll(callableTasksDb); 
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