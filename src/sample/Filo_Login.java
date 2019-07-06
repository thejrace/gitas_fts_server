package sample;

import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;

public class Filo_Login {


    private Refresh_Listener listener;
    public void action(){
        Thread thread = new Thread( () -> {
            org.jsoup.Connection.Response res;
            try {
                res = Jsoup.connect("http://192.168.2.177/filotakip/get_cookie?key=nJAHJjksd13")
                        .method(Connection.Method.POST)
                        .timeout(0)
                        .execute();

                Filo_Download.filo5_cookie = res.parse().text();
                listener.on_refresh();
            } catch( IOException e) {
                e.printStackTrace();
            }


        });
        thread.setDaemon(true);
        thread.start();

    }
    public void addListener( Refresh_Listener listener ){
        this.listener = listener;
    }

}
