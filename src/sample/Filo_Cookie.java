package sample;

import java.sql.SQLException;

/**
 * Created by Obarey on 19.02.2017.
 */
public class Filo_Cookie {

    public static String    A_BOLGESI = "A",
            B_BOLGESI = "B",
            C_BOLGESI = "C";

    public static String get( String bolge ){

        java.sql.Connection con = null;
        java.sql.Statement  st = null;
        java.sql.ResultSet  res = null;
        String cookie = "";

        try {
            con = DBC.getInstance().getConnection();
            st = con.createStatement();
            res = st.executeQuery("SELECT * FROM " + GitasDBT.COOKIES);
            res.next();
            cookie = res.getString( "cookie_" + bolge );
        } catch( SQLException e ){
            e.printStackTrace();
        }

        try {
            if( res != null ) res.close();
            if( st != null ) st.close();
            if( con != null ) con.close();
        } catch( SQLException e){
            e.printStackTrace();
        }


        return cookie;
    }

}
