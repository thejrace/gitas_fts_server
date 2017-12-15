package sample;

import java.util.ArrayList;
import java.util.Map;

public class Otobus_Rotasyon {

    private String oto, yil_ay;
    private ArrayList<Hat_Tabela_Data> aylik_plan = new ArrayList<>();

    public Otobus_Rotasyon( ){

    }

    public void plan_ekle( Hat_Tabela_Data plan ){
        aylik_plan.add(plan);
    }

    public ArrayList<Hat_Tabela_Data> get_aylik_plan(){
        return aylik_plan;
    }

}
class Hat_Tabela_Data {
    private String hat, tabela;
    private int gun;
    public Hat_Tabela_Data( int _gun, String _hat, String _tabela ){
        hat = _hat;
        gun = _gun;
        tabela = _tabela;
    }
    public String get_tabela(){
        return tabela;
    }
    public String get_hat(){
        return hat;
    }
    public String get_gun(){
        String st = String.valueOf(gun);
        if( st.length() == 1 ) return "0"+st;
        return st;
    }
    @Override
    public String toString(){
        return get_hat() +"  TABELA: " + get_tabela() +"  GÜN: " + get_gun();
    }
}


/*
*  [OTO] : {
*      [G1] : {
*           [HAT] : [11ÜS],
*           [TABELA] : [3]
*      }
*  }
*
*
*
*
* */