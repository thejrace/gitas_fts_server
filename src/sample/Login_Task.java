package sample;


import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class Login_Task {
    private Map<String, String> cookies = new HashMap<>();
    public void yap(){
        System.out.println("A Filo Giriş yapılıyor..");
        request("A", "dk_oasa", "oas145");
        System.out.println("B Filo Giriş yapılıyor..");
        request("B", "dk_oasb", "oas125");
        System.out.println("C Filo Giriş yapılıyor..");
        request("C", "dk_oasc", "oas165");

    }

    private void request( String bolge, String ka, String sifre ){
        try{
            org.jsoup.Connection.Response res = Jsoup.connect("http://filo5.iett.gov.tr/login.php?sayfa=/_FYS.php&aday=x")
                    .data("login", ka, "password", sifre )
                    .method(org.jsoup.Connection.Method.POST)
                    .execute();

            this.cookies.put(bolge, res.cookies().get("PHPSESSID") );
        } catch( IOException e ){
            System.out.println(bolge+ " Login hatası, tekrar deneniyor");
            request( bolge, ka, sifre );
        }
    }

    public Map<String, String> get_cookies(){
        return cookies;
    }
}