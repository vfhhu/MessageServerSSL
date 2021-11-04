package MessageServer;

import MessageServer.server_socket.ServerSocketN;
import MessageServer.websocket.wsServer;
import com.moandjiezana.toml.Toml;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class MessageServer {
    private static final String			Server_Version	= "MessageServer Version 2021-09-01";
    // �t�ΦW��
    public static final String			SYSTEM_NAME		= MessageServer.class.getSimpleName();
    // �@�~�t��
    private static String				OS				= null;
    // Windows���w
    private static final String			WIN_PATH		= "./";
    // Linux���|
    private static final String			LINUX_PATH		= "/usr/local/java/MessageServer/";

    // �t�θ��|
    private static File					SERVER_PATH		= null;
    // �t�Υ���IP
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
            GlobalData.println("conf.toml is error");
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

        System.out.println(logBuffer);

        Logger logger = Logger.getLogger("SystemInfo");
        logger.info(logBuffer.toString());
        for (Handler h : logger.getHandlers()) {
            h.close();
        }
    }
    private void init() {
        System.out.println();
        System.out.println("Initial Server....");
        System.out.println();



        GlobalData.setConf(conf);


        InetSocketAddress addrW = new InetSocketAddress(conf.getWebSocketPort());
        wsServer ws=wsServer.getInstance(addrW);
        if(!conf.getPath_crt().equals("") && !conf.getPath_key().equals("")){
            SSLContext context = GlobalData.getContext(conf.getPath_crt(),conf.getPath_key());
            if(context!=null){
                ws.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(context));
            }
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


        System.out.println("ClientListen Initial OK!");

    }
}
