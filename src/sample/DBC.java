package sample;

import org.apache.commons.dbcp2.BasicDataSource;
import java.sql.*;

/**
 * Created by Obarey on 05.02.2017.
 */


/*
*     private String dbAddress = "jdbc:mysql://localhost:3306/";
    private String userPass = "?user=root&password=kuw@GmQ3!bZLB*j3O";
    private String dbName = "gitas";
    private String userName = "root";
    private String password = "kuw@GmQ3!bZLB*j3O";
*
* */


public class DBC {

    private static DBC     instance;
    private BasicDataSource ds;
    private String dbAddress = "jdbc:mysql://localhost:3306/";
    private String userPass = "?user=root&password=kuw@GmQ3!bZLB*j3O";
    private String dbName = "gitas_filo_takip";
    private String userName = "root";
    private String password = "kuw@GmQ3!bZLB*j3O";



    private DBC(){

        ds = new BasicDataSource();
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setUsername(userName);
        ds.setPassword(password);
        ds.setUrl(dbAddress + dbName + "?useSSL=false");

        // the settings below are optional -- dbcp can work with defaults
        ds.setMinIdle(5);
        ds.setMaxIdle(20);
        ds.setMaxOpenPreparedStatements(100);
        ds.setMaxTotal(50);

    }


    public static DBC getInstance(){
        if (instance == null) {
            instance = new DBC();
            return instance;
        } else {
            return instance;
        }
    }

    public Connection getConnection() throws SQLException {
        //System.out.println("IDLE DB: " + ds.getNumIdle() );
        //System.out.println("ACTIVE DB CON: " +  ds.getNumActive() );
        return this.ds.getConnection();
    }




}
