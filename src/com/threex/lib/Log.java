package com.threex.lib;


import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
    public static void println(String s){
        System.out.println( getDateTime()+" "+s);
    }
    public static void d(String s){
        System.out.println( getDateTime()+" "+s);
    }
    public static String getDateTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }
}
