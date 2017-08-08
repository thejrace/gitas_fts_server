package sample;
import javafx.concurrent.Task;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Obarey on 20.01.2017.
 */
public class Filo_Login_Task extends Task<String> {

    private String login;
    private String sifre;
    private String cookie;

    public Filo_Login_Task( String login, String sifre ){
        this.login = login;
        this.sifre = sifre;
    }

    private void action(){
        System.out.println("Giriş yapılıyor..");
        updateMessage("Giriş yapılıyor...");

        Connection.Response res;

        try{
            res = Jsoup.connect("http://filo5.iett.gov.tr/login.php?sayfa=/_FYS.php&aday=x")
                    .data("login", this.login, "password", this.sifre )
                    .method(Connection.Method.POST)
                    .execute();

            this.cookie = res.cookies().get("PHPSESSID");
        } catch( IOException e ){
            System.out.println("Login hatası, tekrar deneniyor");
            this.action();
        }
    }

    @Override
    protected String call(){

        this.action();

        return cookie;
    }

}



