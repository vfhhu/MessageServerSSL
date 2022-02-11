package MessageServer;



import java.io.File;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import com.moandjiezana.toml.Toml;
import com.threex.lib.Log;

public class MyConf {
	private int					SocketPort				= 0;
	private int					WebSocketPort				= 0;
	private String 				DdeviceMsgUrl="";
	private int					gc_second				= 30;
	private int					send_second				= 0;
	private String				pass				= "";
	private String	path_crt="";
	private String	path_ca_bundle="";
	private String	save_combine_path="";
	private String	path_key="";

	private List<String>	sys_no=null;
	private List<String>	sys_group=null;
	private int	limit=0;


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
			Log.println("讀取外部設定檔失敗");
			System.exit(1);
		}
		

		try {
			SocketPort = toml.getLong("SocketPort").intValue();
			if (checkListen(SocketPort)) {
				Log.d("PORT " + SocketPort + "使用中，程式已經中止");
				System.exit(1);
			}
		}catch (NumberFormatException e) {
			Log.d("SocketPort 設定錯誤");
			System.exit(1);
		}
		try {
			pass = toml.getString("pass");
			//Log.d("pass "+pass);
		}catch (NumberFormatException e) {
			Log.d("pass 設定錯誤");
			System.exit(1);
		}
		

		try {
			WebSocketPort = toml.getLong("WebSocketPort").intValue();
			if (checkListen(WebSocketPort)) {
				Log.d("PORT " + WebSocketPort + "使用中，程式已經中止");
				System.exit(1);
			}
		}catch (NumberFormatException e) {
			Log.d("WebSocketPort 設定錯誤");
			System.exit(1);
		}
		try {
			DdeviceMsgUrl = toml.getString("DdeviceMsgUrl");
		}catch (Exception e) {
			Log.d("DdeviceMsgUrl 錯誤");
			System.exit(1);
		}
		try {
			gc_second = toml.getLong("gc_second").intValue();
		}catch (Exception e) {
			Log.d("gc_second 設定錯誤");
			System.exit(1);
		}
		try {
			send_second = gc_second = toml.getLong("send_second").intValue();
		}catch (Exception e) {
			Log.d("send_second 設定錯誤");
			System.exit(1);
		}


		try {
			path_crt = toml.getString("wss.crt");
		}catch (Exception e) {
			Log.d("wss.crt 錯誤");
			path_crt="";
			//System.exit(1);
		}
		try {
			path_ca_bundle = toml.getString("wss.ca_bundle");
		}catch (Exception e) {
			Log.d("wss.ca_bundle 錯誤");
			path_ca_bundle="";
			//System.exit(1);
		}
		try {
			save_combine_path = toml.getString("wss.save_combine_path");
		}catch (Exception e) {
			Log.d("wss.save_combine_path 錯誤");
			save_combine_path="";
			//System.exit(1);
		}


		try {
			path_key = toml.getString("wss.key");
		}catch (Exception e) {
			Log.d("wss.key 錯誤");
			path_key="";
			//System.exit(1);
		}


		try {
			sys_no = toml.getList("sys.no");
		}catch (Exception e) {
			Log.d("sys.no 錯誤");
			sys_no=new ArrayList<>();
			//System.exit(1);
		}
		try {
			sys_group = toml.getList("sys.group");
		}catch (Exception e) {
			Log.d("sys.group 錯誤");
			sys_group=new ArrayList<>();
			//System.exit(1);
		}


		try {
			limit = toml.getLong("limit").intValue();

		}catch (NumberFormatException e) {
			Log.d("limit 設定錯誤");
			limit=0;
		}



//		try {
//			wss_port = toml.getLong("wss.port").intValue();
//			if (checkListen(wss_port)) {
//				Log.d("wss.port " + wss_port + "使用中");
//			}
//		}catch (NumberFormatException e) {
//			Log.d("wss_port 設定錯誤");
//		}



//		try {
//			admin = toml.getList("admin");
//			Log.d("admin:"+admin.get(0));
//		}catch (Exception e) {
//			Log.d("admin 設定錯誤");
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

	public String getPath_ca_bundle() {
		return path_ca_bundle;
	}
	public String get_save_combine_path() {
		return save_combine_path;
	}

	public List<String> getSysNoL() {
		if(sys_no==null)return new ArrayList<>();
		return sys_no;
	}

	public List<String> getSysGroupL() {
		if(sys_group==null)return new ArrayList<>();
		return sys_group;
	}

	public int getLimit() {
		return limit;
	}

	//	public int getWss_port() {
//		return wss_port;
//	}
	//	public List<String> getAdminList() {
//		return admin;
//	}
}
