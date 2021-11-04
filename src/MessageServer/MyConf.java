package MessageServer;



import java.io.File;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import com.moandjiezana.toml.Toml;

public class MyConf {
	private int					SocketPort				= 0;
	private int					WebSocketPort				= 0;
	private String 				DdeviceMsgUrl="";
	private int					gc_second				= 30;
	private int					send_second				= 0;
	private String				pass				= "";
	private String	path_crt="";
	private String	path_key="";
//	private int		wss_port= 0;
//	private List<String> admin =new ArrayList<String>();
	Toml toml;
	public MyConf(Toml _Toml) {
		toml=_Toml;
		initConfigure();
	}

	public boolean checkListen(int port) {
		boolean isused = true;
		try {
			new ServerSocket(port).close();
			isused = false;
		}
		catch (Exception e) {
		}
		return isused;
	}
	
	protected void initConfigure() {

		if (toml == null) {
			System.out.println("讀取外部設定檔失敗");
			System.exit(1);
		}
		

		try {
			SocketPort = toml.getLong("SocketPort").intValue();
			if (checkListen(SocketPort)) {
				System.out.println("PORT " + SocketPort + "使用中，程式已經中止");
				System.exit(1);
			}
		}catch (NumberFormatException e) {
			System.out.println("SocketPort 設定錯誤");
			System.exit(1);
		}
		try {
			pass = toml.getString("pass");
			//System.out.println("pass "+pass);
		}catch (NumberFormatException e) {
			System.out.println("pass 設定錯誤");
			System.exit(1);
		}
		

		try {
			WebSocketPort = toml.getLong("WebSocketPort").intValue();
			if (checkListen(WebSocketPort)) {
				System.out.println("PORT " + WebSocketPort + "使用中，程式已經中止");
				System.exit(1);
			}
		}catch (NumberFormatException e) {
			System.out.println("WebSocketPort 設定錯誤");
			System.exit(1);
		}
		try {
			DdeviceMsgUrl = toml.getString("DdeviceMsgUrl");
		}catch (Exception e) {
			System.out.println("DdeviceMsgUrl 錯誤");
			System.exit(1);
		}
		try {
			gc_second = toml.getLong("gc_second").intValue();
		}catch (Exception e) {
			System.out.println("gc_second 設定錯誤");
			System.exit(1);
		}
		try {
			send_second = gc_second = toml.getLong("send_second").intValue();
		}catch (Exception e) {
			System.out.println("send_second 設定錯誤");
			System.exit(1);
		}


		try {
			path_crt = toml.getString("wss.crt");
		}catch (Exception e) {
			System.out.println("wss.crt 錯誤");
			//System.exit(1);
		}

		try {
			path_key = toml.getString("wss.key");
		}catch (Exception e) {
			System.out.println("wss.key 錯誤");
			//System.exit(1);
		}
//		try {
//			wss_port = toml.getLong("wss.port").intValue();
//			if (checkListen(wss_port)) {
//				System.out.println("wss.port " + wss_port + "使用中");
//			}
//		}catch (NumberFormatException e) {
//			System.out.println("wss_port 設定錯誤");
//		}



//		try {
//			admin = toml.getList("admin");
//			System.out.println("admin:"+admin.get(0));
//		}catch (Exception e) {
//			System.out.println("admin 設定錯誤");
//			System.exit(1);
//		}
	}

	public int getSocketPort() {
		return SocketPort;
	}

	public int getWebSocketPort() {
		return WebSocketPort;
	}

	public String getDdeviceMsgUrl() {
		return DdeviceMsgUrl;
	}

	public int getGc_second() {
		return gc_second;
	}

	public int getSend_second() {
		return send_second;
	}

	public String getPass() {
		return pass;
	}

	public String getPath_crt() {
		return path_crt;
	}

	public String getPath_key() {
		return path_key;
	}

//	public int getWss_port() {
//		return wss_port;
//	}
	//	public List<String> getAdminList() {
//		return admin;
//	}
}
