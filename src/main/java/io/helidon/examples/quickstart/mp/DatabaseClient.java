package io.helidon.examples.quickstart.mp;

import java.io.IOException;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.JsonObject;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

public class DatabaseClient {
    private static final Logger LOGGER = Logger.getLogger(DatabaseClient.class.getName());

    private PoolDataSource poolDataSource;
    private String dbUser     = System.getenv().get("DB_USER");
    private String dbPassword = System.getenv().get("DB_PASSWORD");
    private String dbUrl      = System.getenv().get("DB_URL") + System.getenv().get("DB_SERVICE_NAME") + "?TNS_ADMIN=/function/wallet";
    private String clientCred = "/function/wallet";
    private String keyStorePassword   = "";
    private String truststorePassword = "";

    public DatabaseClient (String dbUrl,                             
                            String dbUser, 
                            String dbPassword, 
                            String clientCred,
                            String keyStorePassword, 
                            String truststorePassword) {
        this.dbUrl      = dbUrl;
        this.clientCred = clientCred;
        this.dbUser     = dbUser;
        this.dbPassword = dbPassword;

        this.keyStorePassword   = keyStorePassword;
        this.truststorePassword = truststorePassword;

        //if an ATP connection then the new ATP connection string is:
        //daseURL?TNS_ADMIN=<client_credentials_url>
        if (!this.clientCred.equals("")) 
            this.dbUrl +="?TNS_ADMIN="+this.clientCred;
    }

    private void getConnectionPool(){        
        LOGGER.info("Setting up pool data source");        
        poolDataSource = PoolDataSourceFactory.getPoolDataSource();
        try {
            poolDataSource.setConnectionFactoryClassName("oracle.jdbc.pool.OracleDataSource");
            poolDataSource.setURL(dbUrl);
            poolDataSource.setUser(dbUser);
            poolDataSource.setPassword(dbPassword);
            poolDataSource.setConnectionPoolName("UCP_POOL");
        }
        catch (SQLException e) {
            System.out.println("Pool data source error!");
            e.printStackTrace();
        }
        System.err.println("Pool data source setup...");
    }

    private PoolDataSource getPoolDataSource(){
        return this.poolDataSource;
    }

    private Connection getConnectionThin() throws SQLException, IOException {        
        LOGGER.info("SQLDB_URL:      " + dbUrl);        
        LOGGER.info("SQLDB_USERNAME: " + dbUser);
        LOGGER.info("SQLDB_PASSWORD: " + "********");
        LOGGER.info("SQLDB_KEYSTOREPASSWORD  : " + (keyStorePassword.equals("")? "False" : "true"));
        LOGGER.info("SQLDB_TRUSTSTOREPASSWORD: " + (truststorePassword.equals("")? "False" : "true"));

        System.setProperty("oracle.jdbc.driver.OracleDriver", "true");
        System.setProperty("oracle.jdbc.fanEnabled", "false");
        System.setProperty("oracle.net.ssl_version", "1.2");       
        System.setProperty("javax.net.ssl.keyStore", clientCred + "/keystore.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
        System.setProperty("javax.net.ssl.trustStore", clientCred + "/truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", truststorePassword);
        System.setProperty("oracle.net.tns_admin", clientCred);
        
        DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
                
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    public String executeInsertOrder(JsonObject pizzaOrder) { 
        String dbresult = "";  
        Connection conn = null;      
        try {
            conn = getConnectionThin();
            if (conn!= null){
                // Insert some data                             
                StringBuffer insertSQL = new StringBuffer("INSERT INTO MICROSERVICE.PIZZAORDER (")
                                                .append("ID").append(",")
                                                .append("DATA").append(",")
                                                .append("TIMESTAMP").append(")")
                                                .append(" VALUES (?,?,"+System.currentTimeMillis() +")");
    
                // logging values passed:
                LOGGER.info(insertSQL.toString());
                LOGGER.info("parameter 1 OrderID: "  + pizzaOrder.getString("orderId"));
                LOGGER.info("parameter 2 Data:    "  + pizzaOrder.toString());
                               
                PreparedStatement pstat = conn.prepareStatement(insertSQL.toString());
    
                pstat.setString(1,pizzaOrder.getString("orderId"));
                pstat.setString(2,pizzaOrder.toString());
    
                if (pstat.executeUpdate() > 0){
                    dbresult = "PizzaOrder with orderId["+pizzaOrder.getString("orderId")+"] inserted OK!";
                }
                else {
                    LOGGER.log(Level.SEVERE,"ERROR IN DB INSERT PizzaOrder with orderId["+pizzaOrder.getString("orderId")+"] result <= 0");
                    dbresult = "ERROR IN DB INSERT PizzaOrder with orderId["+pizzaOrder.getString("orderId")+"] result <= 0";
                }
                conn.close();
            }
            else {
                LOGGER.log(Level.SEVERE,"ERROR ["+pizzaOrder.getString("orderId")+"] Connection null!");
                dbresult = "ERROR ["+pizzaOrder.getString("orderId")+"] Connection null!";
            }
        }
        catch (Exception ex){
            try{
                if (conn != null)
                    conn.close();
            }
            catch(SQLException sqlex){
                LOGGER.log(Level.SEVERE,"ERROR close connection on Order ["+pizzaOrder.getString("orderId")+"] " + ex.getMessage());    
            }
            
            ex.printStackTrace();
            LOGGER.log(Level.SEVERE,"ERROR ["+pizzaOrder.getString("orderId")+"] " + ex.getMessage());
            dbresult = "ERROR ["+pizzaOrder.getString("orderId")+"] " + ex.getMessage();
        }
        finally{
            try{
                if (conn!=null)
                    conn.close();
            }
            catch(SQLException sqlex){
                LOGGER.log(Level.SEVERE,"ERROR close connection on Order ["+pizzaOrder.getString("orderId")+"] " + sqlex.getMessage());    
            }
        }        
        return dbresult;
    }

    public String executeInsertPayment(JsonObject jsonPayment) { 
        String dbresult = "";
        Connection conn = null;
        try {
            conn = getConnectionThin();
            if (conn!=null) {                
                StringBuffer insertSQL = new StringBuffer("INSERT INTO MICROSERVICE.PAYMENTS (")
                                                .append("PAYMENTCODE").append(",")
                                                .append("ORDERID").append(",")
                                                .append("PAYMENTTIME").append(",")
                                                .append("PAYMENTMETHOD").append(",")
                                                //.append("SERVICESURVEY").append(",")
                                                .append("ORIGINALPRICE").append(",")
                                                .append("TOTALPAID").append(",")
                                                .append("CUSTOMERID").append(")")
                                                .append(" VALUES (PAYMENT_SEQ.nextval,")
                                                .append("?,TO_TIMESTAMP(?,'YYYY-MM-DD\"T\"HH24:MI:SS.ff3\"Z\"'),?,?,?,?)");
    
                // logging values passed:
                LOGGER.info(insertSQL.toString());
                LOGGER.info("parameter 1 orderId      : " + jsonPayment.getString("orderId"));
                LOGGER.info("parameter 2 paymentTime  : " + jsonPayment.getString("paymentTime"));
                LOGGER.info("parameter 3 paymentMethod: " + jsonPayment.getString("paymentMethod"));
                LOGGER.info("parameter 4 originalPrice: " + jsonPayment.getString("originalPrice"));
                //System.out.println("parameter 5 servSurvey: " + servSurvey);
                LOGGER.info("parameter 5 totalPaid    : " + jsonPayment.getString("totalPaid"));
                LOGGER.info("parameter 6 customerId   : " + jsonPayment.getString("customerId"));
    
                PreparedStatement pstat = conn.prepareStatement(insertSQL.toString());
    
                pstat.setString(1,jsonPayment.getString("orderId"));
                pstat.setString(2,jsonPayment.getString("paymentTime"));
                pstat.setString(3,jsonPayment.getString("paymentMethod"));
                pstat.setFloat (4,Float.parseFloat(jsonPayment.getString("originalPrice")));
                //pstat.setInt   (5,Integer.parseInt(servSurvey));
                pstat.setFloat (5,Float.parseFloat(jsonPayment.getString("totalPaid")));
                pstat.setString(6,jsonPayment.getString("customerId"));
    
                if (pstat.executeUpdate() > 0){
                    dbresult = "Payment for orderId["+jsonPayment.getString("orderId")+"] inserted OK!";
                }
                else {
                    LOGGER.log(Level.SEVERE,"ERROR IN DB INSERT orderId["+jsonPayment.getString("orderId")+"] result <= 0");
                    dbresult = "ERROR IN DB INSERT orderId["+jsonPayment.getString("orderId")+"] result <= 0";
                }
                conn.close();
            }
            else {
                LOGGER.log(Level.SEVERE,"ERROR ["+jsonPayment.getString("orderId")+"] Connection null!");
                dbresult = "ERROR ["+jsonPayment.getString("orderId")+"] Connection null!";
            }
        }
        catch (Exception ex){
            try{
                if (conn!=null)
                    conn.close();
            }
            catch(SQLException sqlex){
                LOGGER.log(Level.SEVERE,"ERROR close connection on Order ["+jsonPayment.getString("orderId")+"] " + ex.getMessage());    
            }
            ex.printStackTrace();
            LOGGER.log(Level.SEVERE,"ERROR ["+jsonPayment.getString("orderId")+"] " + ex.getMessage());
            dbresult = "ERROR ["+jsonPayment.getString("orderId")+"] " + ex.getMessage();
        }
        finally{
            try{
                if (conn!=null)
                    conn.close();
            }
            catch(SQLException sqlex){
                LOGGER.log(Level.SEVERE,"ERROR close connection on Order ["+jsonPayment.getString("orderId")+"] " + sqlex.getMessage());    
            }
        }
        return dbresult;
    }

    public String executeUpdateIngredients(JsonObject jsonPizzaOrder) { 
        String dbresult = "";
        Connection conn = null;
        try {
            conn = getConnectionThin();
            if (conn!=null) {                
                StringBuffer updateSQL = new StringBuffer("UPDATE MICROSERVICE.TOPPING_STORAGE SET consumed = consumed + 1 WHERE topping in (?,?,?) ");
    
                // logging values passed:
                LOGGER.info(updateSQL.toString());
                LOGGER.info("parameter 1 topping 1 : " + jsonPizzaOrder.getJsonObject("pizzaOrdered").getString("topping1"));
                LOGGER.info("parameter 2 topping 2 : " + jsonPizzaOrder.getJsonObject("pizzaOrdered").getString("topping2"));
                LOGGER.info("parameter 3 topping 3 : " + jsonPizzaOrder.getJsonObject("pizzaOrdered").getString("topping3"));
    
                PreparedStatement pstat = conn.prepareStatement(updateSQL.toString());
    
                pstat.setString(1,jsonPizzaOrder.getJsonObject("pizzaOrdered").getString("topping1"));
                pstat.setString(2,jsonPizzaOrder.getJsonObject("pizzaOrdered").getString("topping2"));
                pstat.setString(3,jsonPizzaOrder.getJsonObject("pizzaOrdered").getString("topping3"));
    
                if (pstat.executeUpdate() > 0){
                    dbresult = "Payment for orderId["+jsonPizzaOrder.getString("orderId")+"] inserted OK!";
                }
                else {
                    LOGGER.log(Level.SEVERE,"ERROR IN DB INSERT orderId["+jsonPizzaOrder.getString("orderId")+"] result <= 0");
                    dbresult = "ERROR IN DB INSERT orderId["+jsonPizzaOrder.getString("orderId")+"] result <= 0";
                }
                conn.close();
            }
            else {
                LOGGER.log(Level.SEVERE,"ERROR ["+jsonPizzaOrder.getString("orderId")+"] Connection null!");
                dbresult = "ERROR ["+jsonPizzaOrder.getString("orderId")+"] Connection null!";
            }
        }
        catch (Exception ex){
            try{
                if (conn!=null)
                    conn.close();
            }
            catch(SQLException sqlex){
                LOGGER.log(Level.SEVERE,"ERROR close connection on Order ["+jsonPizzaOrder.getString("orderId")+"] " + ex.getMessage());    
            }
            ex.printStackTrace();
            LOGGER.log(Level.SEVERE,"ERROR ["+jsonPizzaOrder.getString("orderId")+"] " + ex.getMessage());
            dbresult = "ERROR ["+jsonPizzaOrder.getString("orderId")+"] " + ex.getMessage();
        }
        finally{
            try{
                if (conn!=null)
                    conn.close();
            }
            catch(SQLException sqlex){
                LOGGER.log(Level.SEVERE,"ERROR close connection on Order ["+jsonPizzaOrder.getString("orderId")+"] " + sqlex.getMessage());    
            }
        }
        return dbresult;
    }
}