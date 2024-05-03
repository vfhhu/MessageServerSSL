package MessageServer;

import MessageServer.server_lib.ServerSocketN;
import MessageServer.server_lib.StunServer;
import MessageServer.server_lib.wsServer;
import com.moandjiezana.toml.Toml;
import com.threex.lib.Log;
import com.threex.lib.ssl.WsSSL;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.TimeZone;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class MessageServer {
    private static final String			Server_Version	= "MessageServer Version 2021-09-01";
    public static final String			SYSTEM_NAME		= MessageServer.class.getSimpleName();
    private static String				OS				= null;
    // Windows path
    private static final String			WIN_PATH		= "./";
    // Linux path
    private static final String			LINUX_PATH		= "/usr/local/java/MessageServer/";


    private static File					SERVER_PATH		= null;
    private static String				SERVER_IP		= null;
    private MyConf					conf			= null;
    private Toml toml = null;
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        if (args.length > 0) {
            SERVER_PATH = (OS.indexOf("Win") >= 0) ? new File(WIN_PATH) : new File(args[0]);
        }
        try {
            new MessageServer();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    static {
        OS = System.getProperties().getProperty("os.name");
        SERVER_PATH = (OS.indexOf("Win") >= 0) ? new File(WIN_PATH) : new File(LINUX_PATH);
        try {
            SERVER_IP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }
    private static LinkedList<Logger> loggerPool		= new LinkedList<Logger>();
    public MessageServer(){


        String configFile = SERVER_PATH + "/conf/" + SYSTEM_NAME + ".toml";
        toml = new Toml().read(new File(configFile));
        if(toml==null){
            Log.println("conf.toml is error");
            return;
        }
        conf = new MyConf(toml);
        SystemInfo();
        init();
    }
    private void SystemInfo() {
        StringBuffer logBuffer = new StringBuffer();
        String newline = "\n";
        logBuffer.append("----------------------------" + newline);
        logBuffer.append(Server_Version + newline);
        logBuffer.append("Server IP : " + SERVER_IP + newline);
        logBuffer.append("Server Path  : " + SERVER_PATH + newline);
        logBuffer.append("Listen Socket Port  : " + toml.getLong("SocketPort") + newline);
        logBuffer.append("Listen WebSocket Port  : " + toml.getLong("WebSocketPort") + newline);
        logBuffer.append("DdeviceMsgUrl  : " + toml.getString("DdeviceMsgUrl") + newline);

        logBuffer.append("wss.crt  : " + toml.getString("wss.crt") + newline);
        logBuffer.append("wss.key  : " + toml.getString("wss.key") + newline);

        logBuffer.append(newline);
        logBuffer.append("----------------------------" + newline);
        Calendar ca = Calendar.getInstance(TimeZone.getTimeZone("GMT+08:00"));
        String op_time = String.format("%1$tH:%1$tM:%1$tS", ca);
        logBuffer.append("op_time:" + op_time + newline);

        Log.d(logBuffer.toString());

        Logger logger = Logger.getLogger("SystemInfo");
        logger.info(logBuffer.toString());
        for (Handler h : logger.getHandlers()) {
            h.close();
        }
    }
    private void init() {
        Log.d("Initial Server....2");



        GlobalData.setConf(conf);


        InetSocketAddress addrW = new InetSocketAddress(conf.getWebSocketPort());
        final wsServer ws=wsServer.getInstance(addrW);
        if(conf.getPath_crt()!=null && conf.getPath_key()!=null && !conf.getPath_crt().equals("") && !conf.getPath_key().equals("")){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while(true){
                        try {
                            Thread.sleep(86400000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        setSSL(ws);
                    }
                }
            }).start();
            setSSL(ws);
//            SSLContext context = WsSSL.getContext(conf.getPath_key(),conf.getPath_crt(),conf.getPath_ca_bundle(),conf.get_save_combine_path());
//            if(context!=null){
//                ws.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(context));
//            }
        }
        ws.start();


        InetSocketAddress addrN = new InetSocketAddress(conf.getSocketPort());
        ServerSocketN ssn=ServerSocketN.getInstance(addrN);
        ssn.start();

        new Thread(new Runnable(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                long gcTime=conf.getGc_second()*1000;
                while(true){
                    try {
                        Thread.sleep(gcTime);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        }).start();


        Log.d("ClientListen Initial OK!");

        if(conf.getStun_port()>0){
            try {
                //https://github.com/tking/JSTUN
                new StunServer(conf.getStun_port()).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void setSSL(wsServer ws){
        SSLContext context = WsSSL.getContext(conf.getPath_key(),conf.getPath_crt(),conf.getPath_ca_bundle(),conf.get_save_combine_path());
        if(context!=null){
            ws.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(context));
        }
    }
}

