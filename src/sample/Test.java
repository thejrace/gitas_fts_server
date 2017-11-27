package sample;

import java.sql.SQLException;
import java.util.ArrayList;

public class Test {


    public void init(){
        ArrayList<String> otobusler = new ArrayList<>();
        otobusler.add("A-1636");
        otobusler.add("A-1721");
        otobusler.add("B-1740");
        otobusler.add("B-1886");
        otobusler.add("C-1892");


        Rotasyon_Task rt = new Rotasyon_Task( otobusler );
        rt.yap();
    }

    /*public static void main(String[] args){



    }*/

}
