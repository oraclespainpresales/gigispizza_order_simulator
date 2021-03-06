package io.helidon.examples.quickstart.mp;

import java.util.Date;
import java.util.Random;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

public class PizzaOrder {
    private static final Logger LOGGER = Logger.getLogger(PizzaOrder.class.getName());
    private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Collections.emptyMap());

    @Inject
    @ConfigProperty(name="minThreads", defaultValue="20")
    protected int minThreads;

    @Inject
    @ConfigProperty(name="maxThreads", defaultValue="20")
    protected int maxThreads;


    public PizzaOrder(int minThreads, int maxThreads) {
        this.minThreads   = minThreads;
        this.maxThreads   = maxThreads;
    }

    private String getDateTimeZFormat(Date dateCal) {
        SimpleDateFormat formatReturn = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        return formatReturn.format(dateCal);
    }

    private String getOrderIdFromDate(Calendar dateCal) {

        StringBuilder strOrderId = new StringBuilder().append(dateCal.get(Calendar.YEAR))
                .append((dateCal.get(Calendar.MONTH) + 1) < 10 ? "0" + (dateCal.get(Calendar.MONTH) + 1) : (dateCal.get(Calendar.MONTH) + 1))
                .append(dateCal.get(Calendar.DATE)        < 10 ? "0" + dateCal.get(Calendar.DATE)        : dateCal.get(Calendar.DATE))
                .append(dateCal.get(Calendar.HOUR_OF_DAY) < 10 ? "0" + dateCal.get(Calendar.HOUR_OF_DAY) : dateCal.get(Calendar.HOUR_OF_DAY))
                .append(dateCal.get(Calendar.MINUTE)      < 10 ? "0" + dateCal.get(Calendar.MINUTE)      : dateCal.get(Calendar.MINUTE))
                .append(dateCal.get(Calendar.SECOND)      < 10 ? "0" + dateCal.get(Calendar.SECOND)      : dateCal.get(Calendar.SECOND))
                .append(genNumber(100, 999));

        return strOrderId.toString();
    }

    private String[] getOrderIdAndDateTime(String dateFormat, String dateIni, int num) {
        String[] strDates = new String[2];
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat formatIni = new SimpleDateFormat(dateFormat);
        Date dateCal = null;

        try {
            dateCal = formatIni.parse(dateIni);
            // set calendar datetime and adds num seconds
            cal.setTime(dateCal);
            LOGGER.info ("SECONDS to ADD " + num);
            cal.add(Calendar.SECOND, num);

            // get DateTime in Z format
            strDates[0] = getDateTimeZFormat(cal.getTime());
            // get orderId from cal Date
            strDates[1] = getOrderIdFromDate(cal);
        } catch (ParseException parseEx) {
            LOGGER.log(Level.SEVERE, "ERROR: ParseException - " + parseEx.getMessage());
        }

        return strDates;
    }

    private int genNumber(int low, int high) {
        Random r = new Random();
        return r.nextInt(high - low) + low;
    }

    private String genPaymentMethod(int typeCard) {
        String cardType = "VISA";
        switch (typeCard) {
            case 0:
                cardType = "AMEX";
                break;
            case 1:
                cardType = "MASTERCARD";
                break;
            case 2:
                cardType = "VISA";
                break;
            default:
                cardType = "CASH";
                break;
        }

        return cardType;
    }

    private String genBasePizza() {
        String[] size = { "Small", "Medium", "Large", "X-Large" };
        String[] base = { "BACON SPINACH ALFREDO", "CHEESE BASIC", "HAWAIIAN CHICKEN", "MEAT LOVER", "PEPPERONI",
                "PREMIUM GARDEN VEGGIE", "SUPREME", "ULTIMATE CHEESE LOVER" };

        return size[genNumber(0, size.length)] + " " + base[genNumber(0, base.length)];
    }

    private String[] genToppings() {
        String[] toppings = { "Tuna", "Onions", "BBQ Sauce", "Tomatos", "Mushrooms" };
        int index = 0;
        int[] selected = { 0, 0, 0 };
        ArrayList<Integer> selectables = new ArrayList<Integer>(5);
        selectables.add(0);
        selectables.add(1);
        selectables.add(2);
        selectables.add(3);
        selectables.add(4);

        // LOGGER.info("INFO:: " + selectables.toString());
        for (int i = 0; i < 3; i++) {
            index = genNumber(0, selectables.size());
            // LOGGER.info("INFO:: " + index);
            selected[i] = selectables.get(index);
            selectables.remove(index);
            // LOGGER.info("INFO:: " + selectables.toString());
        }

        return new String[] { toppings[selected[0]], toppings[selected[1]], toppings[selected[2]] };
    }

    private String[] genLongLat(){        
        String coorLong = "-3." + Integer.toString(genNumber(607759, 811779));
        String coorLat  = "40." + Integer.toString(genNumber(385053, 551042));
        
        return new String[] {coorLong,coorLat};
    }
    
    protected JsonObject createJsonPizzaOrder(String dateFormat, String date, int segsCal, String pizzaStatus) {
        String[] strOrderDate = getOrderIdAndDateTime(dateFormat, date, segsCal);
        String[] strToppings  = genToppings();
        String[] strCoords    = genLongLat();
        int totalPrice        = genNumber(10,20);
        int originalPrice     = totalPrice + genNumber(0, 3);
        JsonObject jsonResp   = null; 

        try{
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
                .add("long", strCoords[0])
                .add("lat", strCoords[1])
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
                .add("orderId",strOrderDate[1])
                .add("status",pizzaStatus)                  
                .build();

            jsonResp = JSON.createObjectBuilder()
                            .add("order",jsonOBJOrderBody)
                            .add("payment",jsonOBJPaymentBody)                            
                            .build();
        }
        catch (Exception ex){
            ex.printStackTrace();
            jsonResp = JSON.createObjectBuilder()
                            .add("error",ex.getMessage())
                            .build();
        }
        
        return jsonResp;
    }

    /**
     * Used in the MICROSERVICE mode to gen a pizza order with the current Date.
     *
     * @return {@link Response}
     */
    protected Response createOrders(int numOrders, String pizzaStatus) throws Exception {
        return createOrders("dd/MM/yyyy HH:mm:ss", "", numOrders, pizzaStatus);
    }

    /**
     * Used in the DATABASE mode to gen a pizza order with the json date-ini param.
     *
     * @return {@link Response}
     */
    protected Response createOrders(String dateFormat, String date, int numOrders, String pizzaStatus) {                
        Response resp = null;
        return resp;
    }
}