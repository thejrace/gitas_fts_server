package sample;

/**
 * Created by Obarey on 17.02.2017.
 */
public class Otobus_Data {

    private String oto, guz, son_orer;

    public Otobus_Data( String oto, String guz, String son_orer ){
        this.oto = oto;
        this.guz = guz;
        this.son_orer = son_orer;
    }


    public String get_oto(){
        return this.oto;
    }

    public String get_guz(){
        return this.guz;
    }

    public String get_son_orer() {
        return this.son_orer;
    }

}
