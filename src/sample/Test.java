package sample;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;

public class Test {

    private ArrayList<String> hatlar = new ArrayList<>();
    public void init(){

        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {

                ArrayList<String> otobusler = new ArrayList<>();
                try {
                    Connection con = DBC.getInstance().getConnection();
                    PreparedStatement pst = con.prepareStatement("SELECT kod FROM " + GitasDBT.OTOBUSLER + " WHERE durum = ?");
                    pst.setInt(1, 1);
                    ResultSet res = pst.executeQuery();
                    while( res.next() ) otobusler.add( res.getString("kod"));
                    res.close();
                    pst.close();
                    con.close();
                } catch (SQLException e ){
                    e.printStackTrace();
                }

                Rotasyon_Task rt = new Rotasyon_Task( otobusler );
                rt.yap();

                LocalDate ld_temp;
                for (Map.Entry<String, Otobus_Rotasyon> entry : rt.get_data().entrySet()) {
                    //System.out.println( entry.getKey() );
                    Otobus otobus = new Otobus(entry.getKey());
                    ArrayList<String> temp_orer_liste;
                    for( Hat_Tabela_Data htdata : entry.getValue().get_aylik_plan() ){
                        //System.out.println( htdata.toString() );
                        ld_temp = LocalDate.parse("2017-12-"+htdata.get_gun());
                        Rotasyon_Orer_Task rot = new Rotasyon_Orer_Task(Common.convert_url_hex(htdata.get_hat()), "2017-12-"+htdata.get_gun());
                        rot.yap();

                        System.out.println(entry.getKey() + " 2017-12-"+htdata.get_gun() + " rotasyon: ");
                        if( ld_temp.getDayOfWeek() != DayOfWeek.SATURDAY && ld_temp.getDayOfWeek() != DayOfWeek.SUNDAY ){
                            temp_orer_liste = rot.get_data().get(DayOfWeek.FRIDAY).get(Integer.valueOf(htdata.get_tabela())-1);
                        } else {
                            temp_orer_liste = rot.get_data().get(ld_temp.getDayOfWeek()).get(Integer.valueOf(htdata.get_tabela())-1);
                        }
                        otobus.rotasyon_ekle( "2017-12-"+htdata.get_gun(), htdata.get_hat(), temp_orer_liste );
                    }
                }
            }
        });
        th.setDaemon(true);
        th.start();




    }

    public static void main(String[] args){

        Rotasyon_Orer_Task rot = new Rotasyon_Orer_Task("11ÜS", "2017-11-11");
        rot.yap();

    }

}
