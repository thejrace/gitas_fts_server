package sample;

import org.apache.commons.dbcp2.BasicDataSource;
import java.sql.*;

/**
 * Created by Obarey on 05.02.2017.
 */


public class DBC {

    private static DBC     instance;
    private BasicDataSource ds;
    // vargonen old
    /*private String dbAddress = "jdbc:mysql://localhost:3306/";
    private String userPass = "?user=root&password=kuw@GmQ3!bZLB*j3O";
    private String dbName = "gitas_filo_takip";
    private String userName = "root";
    private String password = "kuw@GmQ3!bZLB*j3O";*/


    /*private String dbAddress = "jdbc:mysql://localhost:3306/";
    private String userPass = "?user=root&password=mEP3isJVWqYPL";
    private String dbName = "db3_gitas_filo_takip";
    private String userName = "root";
    private String password = "mEP3isJVWqYPL";*/

    // LOCAL 01.05.2018
    private String dbAddress = "jdbc:mysql://localhost:3306/";
    private String userPass = "?user=root&password=";
    private String dbName = "ahmet";
    private String userName = "ahmet";
    private String password = "KHLHjklh654";


    private DBC(){

        ds = new BasicDataSource();
        ds.setDriverClassName("com.mysql.jdbc.Driver");
        ds.setUsername(userName);
        ds.setPassword(password);
        ds.setUrl(dbAddress + dbName + "?useSSL=false");

        ds.setMinIdle(15);
        ds.setMaxIdle(30);
        ds.setMaxOpenPreparedStatements(150);
        ds.setMaxTotal(150);

        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while( true ){
                    System.out.println("[ IDLE DB: " + ds.getNumIdle() + "] - [ ACTIVE DB CON: " +  ds.getNumActive() + " ] " );
                    try {
                        Thread.sleep(10000);
                    } catch( InterruptedException e ){
                        e.printStackTrace();
                    }
                }
            }
        });
        th.setDaemon(true);
        th.start();
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
