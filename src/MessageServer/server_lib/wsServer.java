package MessageServer.server_lib;



import java.net.InetSocketAddress;

import com.threex.lib.Log;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

import MessageServer.GlobalData;
//import org.java_websocket.drafts.Draft_6455;






public class wsServer extends WebSocketServer {
	private static wsServer ws;
	private long memberID=0;
	private int connectCnt=0;
//	private static ConcurrentHashMap<String,TreeMap<Integer,CustData>> GroupMap=new ConcurrentHashMap<String,TreeMap<Integer,CustData>>();
//	private static ConcurrentHashMap<Integer,CustData> CustDataMap=new ConcurrentHashMap<Integer,CustData>();
	public wsServer( InetSocketAddress address ) {
		super( address );
		
		//CustDataMap=new HashMap<Integer,CustData>();
	}
//	public wsServer(int port, Draft_6455 draft) {
//		super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
//	}
	public static wsServer getInstance(InetSocketAddress address){
		if(ws==null){
			ws=new wsServer(address);
		}
		return ws;
	}
	public static wsServer getInstance(){
		return ws;
	}
	
	@Override
	public void onOpen( WebSocket conn, ClientHandshake handshake ) {
		//this.sendToAll( "new connection: " + handshake.getResourceDescriptor() );
		//memberID++;	
		if(conn==null)return;
		try{
			Log.println( conn.getRemoteSocketAddress().getAddress().getHostAddress() + " entered the room!" );
			int key=conn.hashCode()+conn.getRemoteSocketAddress().hashCode();
			GlobalData.getCustDataMap().put("conn"+key, new CustData("conn"+key,conn));
		}catch(Exception e){
			//e.printStackTrace();
		}
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
		//this.sendToAll( conn + " has left the room!" );
		
		if( conn != null ) {
			try{
				Log.println( conn + " has left the room!" );
				int key=conn.hashCode()+conn.getRemoteSocketAddress().hashCode();
				//CustData cd=GlobalData.getCustDataMap().remove("conn"+key);
				CustData cd=GlobalData.CustExit("conn"+key);

				if(cd!=null)cd.stop();
				this.releaseBuffers(conn);
				//conn.close(0);
			}catch(Exception e){
				//e.printStackTrace();
			}
		}
		
	}
	@Override
	public void onError( WebSocket conn, Exception ex ) {
		//ex.printStackTrace();
		if( conn != null ) {
			try{
				ex.printStackTrace();
				//GlobalData.println("onError my:"+GlobalData.getCustDataMap().size()+",ws:"+connections().size()+",cnt"+(connectCnt++)+",addr:"+conn.getRemoteSocketAddress());
			}catch(Exception e){e.printStackTrace();}
			// some errors like port binding failed may not be assignable to a specific websocket
		}
	}

	@Override
	public void onStart() {
		Log.println( "onStart" );
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		//this.sendToAll( message );
		if( conn == null ) return;
		Log.println( conn + " onMessage: " + message );
		try{
			int key_cust=conn.hashCode()+conn.getRemoteSocketAddress().hashCode();
			CustData custData=GlobalData.getCustDataMap().get("conn"+key_cust);
			if(custData==null)return;
			if (message == null)return;
			JSONObject json = null;
			try{
				json = new JSONObject(message);
			}catch(Exception e){
				e.printStackTrace();
			}
			if(json==null)return;

			GlobalData.onData(custData ,json);
		}catch(Exception e){e.printStackTrace();}
	}	
	
	
	


}
