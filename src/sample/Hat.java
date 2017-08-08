package sample;

import java.sql.*;

/**
 * Created by Jeppe on 14.03.2017.
 */
public class Hat {

    private String hat_kod, aciklama;
    private double iett_km, gitas_km;
    private boolean ok = true;
    public Hat( String hat_kod ){
        this.hat_kod = hat_kod;

        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement st = con.prepareStatement("SELECT * FROM " + GitasDBT.HATLAR + " WHERE hat = ?");
            st.setString(1, hat_kod );
            ResultSet res = st.executeQuery();
            if( res.next() ){
                aciklama = res.getString("aciklama");
                iett_km = res.getDouble("iett_uzunluk");
                gitas_km = res.getDouble("uzunluk");
            } else {
                ok = false;
            }
            res.close();
            st.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }

    }
    public Hat(){

    }

    public boolean is_ok(){
        return this.ok;
    }

    public String get_aciklama(){
        return this.aciklama;
    }

    public double get_iett_km(){
        return this.iett_km;
    }

    public double get_gitas_km(){
        return this.gitas_km;
    }


}
