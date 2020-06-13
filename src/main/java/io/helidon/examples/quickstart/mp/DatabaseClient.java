package io.helidon.examples.quickstart.mp;

import java.sql.*;
import java.util.logging.Logger;

import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;

public class DatabaseClient {
    private static final Logger LOGGER = Logger.getLogger(DatabaseClient.class.getName());

    private PoolDataSource poolDataSource;
    private String dbUser     = System.getenv().get("DB_USER");
    private String dbPassword = System.getenv().get("DB_PASSWORD");
    private String dbUrl      = System.getenv().get("DB_URL") + System.getenv().get("DB_SERVICE_NAME") + "?TNS_ADMIN=/function/wallet";

    public DatabaseClient (String dbUrl, String dbUser, String dbPassword){
        this.dbUrl      = dbUrl;
        this.dbUser     = dbUser;
        this.dbPassword = dbPassword;

        this.getDiscountPool();
    }

    private void getDiscountPool(){        
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

    public PoolDataSource getPoolDataSource(){
        return this.poolDataSource;
    }
}