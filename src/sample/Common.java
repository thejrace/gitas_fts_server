package sample;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.apache.commons.codec.digest.DigestUtils;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Obarey on 05.02.2017.
 */
public class Common {

    public static String mac_hash(){
        try {
            String command = "ipconfig /all";
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader inn = new BufferedReader(new InputStreamReader(p.getInputStream()));
            Pattern pattern = Pattern.compile(".*Physical Addres.*: (.*)");
            StringBuilder sb = new StringBuilder();
            while (true) {
                String line = inn.readLine();
                if (line == null)
                    break;
                Matcher mm = pattern.matcher(line);
                if (mm.matches()) {
                    //System.out.println(mm.group(1));
                    sb.append(mm.group(1));
                }
            }
            return DigestUtils.sha256Hex(sb.toString());
        } catch( IOException e ){
            e.printStackTrace();
            return null;
        }
    }

    public static int month_day_count( int month_int, int year_int ){
        YearMonth year_month_object = YearMonth.of(year_int, month_int);
        return year_month_object.lengthOfMonth();
    }

    public static int month_int(){
        java.util.Date date= new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.MONTH) + 1; // ocak = 0. index
    }

    public static int year_int(){
        java.util.Date date= new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }

    public static boolean dt_gecmis( String d1, String d2 ){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date t1 = sdf.parse(d1);
            Date t2 = sdf.parse(d2);
            if( t1.compareTo(t2) == 0 || t1.compareTo(t2) == 1 ){
                return true;
            } else {
                return false;
            }
        } catch(ParseException e ){
            e.printStackTrace();
        }
        return false;
    }

    public static String rev_datetime( String dt ){
        String date = dt.substring(0, 10);
        String[] exp = date.split("-");
        return exp[2]+"-"+exp[1]+"-"+exp[0]+ " " + dt.substring(11);
    }

    public static String rev_date( String dt ){
        String[] exp = dt.split("-");
        return  exp[2]+"-"+exp[1]+"-"+exp[0];
    }

    public static String iys_to_date( String iys_tarih ){
        String[] exp = iys_tarih.split("\\.");
        return "20"+exp[2]+"-"+exp[1]+"-"+exp[0];
    }

    public static int result_count( ResultSet res ){
        try {
            res.last();
            int count = res.getRow();
            res.beforeFirst();
            return count;
        } catch( SQLException e ){
            e.printStackTrace();
        }
        return 0;
    }

    public static boolean is_numeric( String val ){
        return val.matches("\\d+");
    }

    public static String hat_kod_sef( String hat_kodu ){
        if( hat_kodu.indexOf("Ç") > 0 ) hat_kodu = hat_kodu.replace("Ç", "C.");
        if( hat_kodu.indexOf("Ş") > 0 ) hat_kodu = hat_kodu.replace("Ş", "S.");
        if( hat_kodu.indexOf("Ü") > 0 ) hat_kodu = hat_kodu.replace("Ü", "U.");
        if( hat_kodu.indexOf("Ö") > 0 ) hat_kodu = hat_kodu.replace("Ö", "O.");
        if( hat_kodu.indexOf("İ") > 0 ) hat_kodu = hat_kodu.replace("İ", "I.");
        return hat_kodu;
    }

    public static void exception_db_kayit( String hata ){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("INSERT INTO " + GitasDBT.SUNUCU_APP_HATA_KAYITLARI + " ( etiket, hata, tarih, durum ) VALUES ( ?, ?, ?, ? )");
            pst.setString(1, "SUNUCU EXCEPTION" );
            pst.setString(2, hata );
            pst.setString(3, Common.get_current_datetime_db() );
            pst.setInt(4, 1 );
            pst.executeUpdate();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
    }

    public static void orer_degisiklik_log_kaydet( String oto, String tarih, String tip, int aktif_versiyon, int son_versiyon, int rows_size ){
        try {
            Connection con = DBC.getInstance().getConnection();
            PreparedStatement pst = con.prepareStatement("INSERT INTO " + GitasDBT.FILO_ORER_DEGISIKLIK_LOG + " ( oto, tarih, tip, aktif_versiyon, son_versiyon, rows_size, timestamp ) VALUES ( ?, ?, ?, ?, ?, ?, ? )");
            pst.setString(1, oto );
            pst.setString(2, tarih );
            pst.setString(3, tip );
            pst.setInt(4, aktif_versiyon );
            pst.setInt(5, son_versiyon );
            pst.setInt(6, rows_size );
            pst.setString(7, Common.get_current_datetime_db() );
            pst.executeUpdate();
            pst.close();
            con.close();
        } catch( SQLException e ){
            e.printStackTrace();
        }
    }

    public static String regex_trim( String str ){
        return str.replaceAll("\u00A0", "");
    }

    public static Map<String, Integer> get_screen_res(){
        Map<String, Integer> out = new HashMap<>();
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();
        out.put("W", width );
        out.put("H", height );
        return out;
    }

    public static long get_unix() {
        return (System.currentTimeMillis() / 1000L) - 3600;
    }

    public static String get_current_datetime(){
        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }
    public static String get_current_datetime_db(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String get_current_date(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String get_current_hmin(){
        DateFormat dateFormat = new SimpleDateFormat("HH:mm");
        Date date = new Date();
        return dateFormat.format(date);

    }

    public static String get_yesterday_date() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return dateFormat.format(cal.getTime());
    }

    static class Delta { double x, y; }
    public static void make_draggable(Node node) {
        final Delta dragDelta = new Delta();
        node.setOnMousePressed(me -> {
            dragDelta.x = me.getX();
            dragDelta.y = me.getY();
        });
        node.setOnMouseDragged(me -> {
            node.setLayoutX(node.getLayoutX() + me.getX() - dragDelta.x);
            node.setLayoutY(node.getLayoutY() + me.getY() - dragDelta.y);
        });
    }

    public static void make_stage_draggable(final Stage stage, final Node byNode) {
        final Delta dragDelta = new Delta();
        byNode.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                // record a delta distance for the drag and drop operation.
                dragDelta.x = stage.getX() - mouseEvent.getScreenX();
                dragDelta.y = stage.getY() - mouseEvent.getScreenY();
            }
        });
        byNode.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override public void handle(MouseEvent mouseEvent) {
                stage.setX(mouseEvent.getScreenX() + dragDelta.x);
                stage.setY(mouseEvent.getScreenY() + dragDelta.y);
            }
        });

    }

}
