package sample;

import java.sql.*;

/**
 * Created by Obarey on 04.02.2017.
 */
public class Surucu_Data {
    private String telefon, isim, sicil_no, id;
    private boolean kontrol = true;

    public Surucu_Data( String id, String isim, String sicil_no, String telefon ){
        this.isim = isim;
        this.sicil_no = sicil_no;
        this.telefon = telefon;
        this.id = id;
    }

    // orer request task yeni sofor icin
    public Surucu_Data(  String isim, String sicil_no, String telefon ){
        this.isim = isim;
        this.sicil_no = sicil_no;
        this.telefon = telefon;
    }

    public Surucu_Data(){

    }

    public Surucu_Data( String sicil_no ){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("SELECT * FROM " + GitasDBT.SURUCULER + " WHERE sicil_no = ?");
            //System.out.println("SELECT * FROM " + GitasDBT.SURUCULER + " WHERE sicil_no = ? ---> " + sicil_no );
            pst.setString(1, sicil_no);
            ResultSet res = pst.executeQuery();
            if( res.next() ){
                this.sicil_no = sicil_no;
                this.isim = res.getString("isim");
                this.telefon = res.getString("telefon");
            } else {
                //System.out.println(sicil_no + " Sürücü yok tabloda");
                kontrol = false;
            }
            res.close();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
    }

    public void ekle( String sicil_no, String isim, String telefon ){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("INSERT INTO " + GitasDBT.SURUCULER + " ( sicil_no, isim, telefon ) VALUES ( ?, ?, ? )");
            pst.setString(1, sicil_no);
            pst.setString(2, isim);
            pst.setString(3, telefon);
            pst.executeUpdate();

            this.isim = isim;
            this.sicil_no = sicil_no;
            this.telefon = telefon;

            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
    }

    public boolean kontrol(){
        return kontrol;
    }

    public String get_isim(){
        return this.isim;
    }

    public String get_telefon(){
        return this.telefon;
    }

    public String get_sicil_no(){
        return this.sicil_no;
    }

    public String get_id(){
        return this.id;
    }

    public String kisalt(){
        return "";
    }
}
