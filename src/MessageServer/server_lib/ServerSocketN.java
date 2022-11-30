package MessageServer.server_lib;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.channels.SocketChannel;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.threex.lib.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.threex.lib.connect.Http;
import com.threex.lib.connect.OnSocketServerListener;
import com.threex.lib.connect.SocketServerNio;

import MessageServer.GlobalData;

public class ServerSocketN extends SocketServerNio{
	private static ServerSocketN server;
	SimpleDateFormat sdFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	long send_second=0;

    private static ConcurrentHashMap <String,Long> sendMapper;
    //private static ConcurrentHashMap <SocketChannel,String> logonMapper;
    private static Thread t;
    private boolean isThreadSendRun=false;
	
	public ServerSocketN( InetSocketAddress address ) {
		super(address);
		this.setListener(l );
		send_second=GlobalData.getConf().getSend_second()*1000;
		
		sendMapper=new ConcurrentHashMap<>();
		//logonMapper=new ConcurrentHashMap<>();
		
		if(t!=null){
			isThreadSendRun=false;
			t.interrupt();
		}
		t=new Thread(new Runnable(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				isThreadSendRun=true;
				while(isThreadSendRun){
					long nowTime=System.currentTimeMillis();
					JSONArray ja=new JSONArray();
					for(Map.Entry<String,Long> entry : sendMapper.entrySet()) {
					    String key = entry.getKey();
					    Long value = entry.getValue();
					    ja.put(key);
//					    if(value<(nowTime-1000)){
//					    	ja.put(key);
//					    }
					}
					sendMapper.clear();

					try {
						if(ja.length()>0){
							try {
								server.postToUrl(URLEncoder.encode(ja.toString(),"UTF-8"));
							} catch (UnsupportedEncodingException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
//							for(int i=0;i<ja.length();i++){
//								sendMapper.remove(ja.optString(i));
//							}
						}
						
						Thread.sleep(1000);
						
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}});
		t.start();
	}
	public static ServerSocketN getInstance(InetSocketAddress address){
		if(server==null){
			server=new ServerSocketN(address);
		}
		return server;
	}
	public static ServerSocketN getInstance(){
		return server;
	}
	private OnSocketServerListener l=new OnSocketServerListener(){

		@Override
		public void onStart() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onStop() {
			// TODO Auto-generated method stub
			server.start();
		}

		@Override
		public void onSend(String src, SocketChannel client) {
			// TODO Auto-generated method stub
			Log.d("onSend: " + src);
		}

		@Override
		public void onSend(byte[] src, SocketChannel client) {
			// TODO Auto-generated method stub
			Log.d("onSendbyte : " +  byteToHexString(src));
		}

		@Override
		public void onData(String src, SocketChannel client, byte[] data) {
			
			// TODO Auto-generated method stub
			String SrcArr[]=new String[]{src};
			if(src.indexOf("\r\n")>0){
//				Log.d("onData new line: rn");
				SrcArr=src.split("\\r\\n");
			}	else if(src.indexOf("\n")>0){
//				Log.d("onData new line: n");
				SrcArr=src.split("\\n");			
			}
			for(String s:SrcArr){
				server.onData(s, client);
			}
		}

		@Override
		public void onConnected(SocketChannel client) {
			// TODO Auto-generated method stub
			if(client==null)return;
			try{
				Log.d( client.socket().getRemoteSocketAddress() + " entered the room!" );
				int key=client.hashCode()+client.socket().getRemoteSocketAddress().hashCode();
				GlobalData.getCustDataMap().put("client"+key, new CustData("client"+key,client));
			}catch(Exception e){
				//e.printStackTrace();
			}
		}

		@Override
		public void onClosed(SocketChannel client) {
			// TODO Auto-generated method stub
			if(client==null)return;
			try{
				Log.d( client.socket().getRemoteSocketAddress() + " leave the room!" );
				int key=client.hashCode()+client.socket().getRemoteSocketAddress().hashCode();
				GlobalData.CustExit("client"+key);
//				GlobalData.getCustDataMap().remove("client"+key);
			}catch(Exception e){
				//e.printStackTrace();
			}
		}

		@Override
		public void onError(String err, SocketChannel client) {
			// TODO Auto-generated method stub
			
		}
		
	} ;
	
	public void onData(String src, SocketChannel client){
		if(client==null)return;
//		Log.d("onData: " + src.trim());
		try{
			int key_cust=client.hashCode()+client.socket().getRemoteSocketAddress().hashCode();
			CustData custData=GlobalData.getCustDataMap().get("client"+key_cust);
			if(custData==null)return;
			if (src == null) return;
			JSONObject json = null;
			try {
				json = new JSONObject(src);
			} catch (JSONException e) {
				//e.printStackTrace();
			}
			if (json == null) return;
			GlobalData.onData(custData ,json,false);
		}catch (Exception e){e.printStackTrace();}


	}
	
	public static byte[] hexToBytes(String hexString) {
        hexString=hexString.trim().replace(" ","");
        char[] hex = hexString.toCharArray();
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            int value = (high << 4) | low;
            if (value > 127)
                value -= 256;
            rawData[i] = (byte) value;
        }
        return rawData;
    }
    public String byteToHexString(byte []data) {
        String str = "";
        for (int i = 0; i < data.length; i++) {
            str += String.format("%02X", data[i]) + " ";
        }
        return str.trim();
    }
    public int byteToInt(byte[] b){
        int MASK = 0xFF;
        int result = 0;
        result = b[0] & MASK;
        result = result + ((b[1] & MASK) << 8);
        result = result + ((b[2] & MASK) << 16);
        result = result + ((b[3] & MASK) << 24);
        return result;
    }
    public int byteToInt2(byte[] b){
        int MASK = 0xFF;
        int result = 0;
        result = b[0] & MASK;
        result = result + ((b[1] & MASK) << 8);
        return result;
    }
    public void postToUrl(String para){
    	if(GlobalData.getConf()!=null){
    		GlobalData.getConf().getDdeviceMsgUrl();
    		String url=GlobalData.getConf().getDdeviceMsgUrl();
    		
    		try {
				//Http.Post(url, para);
				Http.Get(url+"?data="+ para);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        
    	}
    }
}
