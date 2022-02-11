package MessageServer.server_lib;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;

public class CustData {
	private String key="";
	private boolean isSendInfo=false;
	private WebSocket conn;
	private SocketChannel client;
	private String group="";
	private List<String> groupA;
	private List<String> roomA;
	private String info="";
	private String no="";
	//private ArrayBlockingQueue<String> arr=new ArrayBlockingQueue<String>(10240); 
//	private LinkedList<String> arr=new LinkedList<String>(); 
	//private Thread t;
	private boolean isRun=false;
	public CustData(String key,WebSocket conn){
		this.key=key;
		this.conn=conn;
		isRun=true;
		groupA=Collections.synchronizedList(new ArrayList());
		roomA=Collections.synchronizedList(new ArrayList());
	}
	public CustData(String key,SocketChannel client){
		this.key=key;
		this.client=client;
		isRun=true;
		groupA=Collections.synchronizedList(new ArrayList());
		roomA=Collections.synchronizedList(new ArrayList());
	}
	public String getKey() {
		return key;
	}
	
	
	public boolean send(String s){
		if(no.trim().length()==0)return false;
//		arr.offer(s);
//		Log.d("************* send:"+isRun+","+conn.isOpen());
		boolean issend=false;
		if(conn!=null && isRun && conn.isOpen() && !conn.isClosed()){
			conn.send(s);
			issend=true;
		}
		if(client!=null && isRun && client.isOpen() && client.isConnected()) {
			try {
				ServerSocketN.getInstance().send(client,s+"\n");
				issend=true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return issend;
		//conn.isOpen()
	}
	public void stop() {
		// TODO Auto-generated method stub
		isRun=false;		
//		if(t!=null)t.interrupt();
//		arr.clear();
//		arr=null;
	}
	public WebSocket getConn() {
		return conn;
	}
	public void setConn(WebSocket conn) {
		this.conn = conn;
	}
//	public List<String> getGroupA() {
//		return groupA;
//	}
	public String getGroup() {
		return group;
	}
	public void setGroup(String _gp) {
		group=_gp;
		info="";
		no="";
	}
	public List<String> getRoomA() {
		return roomA;
	}

	public JSONObject getInfoA() {
		JSONObject jo=new JSONObject();
		try {
			jo=new JSONObject(info);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jo;
	}
	public String getInfo() {
		return info;
	}
	public void setInfo(String _info) {
		info=_info;
	}

	public String getNo() {
		return no;
	}

	public void setNo(String no) {
		this.no = no;
	}
	public String toString(){
		String str="";
		if(conn!=null)str=conn.getRemoteSocketAddress().toString();
		if(client!=null)str=client.socket().getRemoteSocketAddress().toString();
		return str;
	}
}
