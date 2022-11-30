package MessageServer;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.threex.lib.Log;
import com.threex.lib.hash.Hmac;

import MessageServer.server_lib.CustData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class GlobalData {
	private static MyConf conf	= null;
	private static ConcurrentHashMap <String,CustData> CustDataMap=new ConcurrentHashMap<>();
	private static ConcurrentHashMap <String, List<CustData>> GroupDataMap=new ConcurrentHashMap<>();
	private static ConcurrentHashMap <String, List<JSONObject>> GroupMsgMap=new ConcurrentHashMap<>();
	public static MyConf getConf() {
		return conf;
	}
	public static void setConf(MyConf conf) {
		GlobalData.conf = conf;
	}
	public static ConcurrentHashMap<String, CustData> getCustDataMap() {
		return CustDataMap;
	}
	public static ConcurrentHashMap<String, List<CustData>> getGroupDataMap() {
		return GroupDataMap;
	}


	private static ConcurrentHashMap <String, List<String>> blackList=new ConcurrentHashMap<>();
//	public static List blackList = Collections.synchronizedList(new ArrayList());

	public static void onData(CustData custData, JSONObject json,boolean sendGroupFromServer){

		String cmd="";
		String value="";
		try{
			cmd=json.optString("cmd");//get("type");
		}catch(Exception e){
			e.printStackTrace();
			cmd="";
		}
		if(!sendGroupFromServer && cmd.equals("sendGroupFromServer"))return;
		try{
			value=json.optString("value");//get("type");
		}catch(Exception e){
			e.printStackTrace();
			value="";
		}
		if(cmd.equals(""))return;
		cmd=cmd.trim();
		value=value.trim();
		try {
			if(cmd.equals("setInfo")){
				boolean retV=false;
				String info="";
				String no="";
				String token="";
				String time="";
				if(value.length()>0){
					//custData.setInfo(value);
					try{
						info=value;
					}catch(Exception e){
						e.printStackTrace();
					}
					try{
						no=json.optString("no").trim();
					}catch(Exception e){
						e.printStackTrace();
					}

					if(getPass(custData.getGroup()).length()>0){
						try{
							token=json.optString("token").trim();
						}catch(Exception e){
							e.printStackTrace();
						}
						try{
							time=json.optString("time").trim();
						}catch(Exception e){
							e.printStackTrace();
						}
						String sign_server=Hmac.hash_sha256(getPass(custData.getGroup())+time,cmd+info+no);
						if(!token.toLowerCase().equals(sign_server.toLowerCase()) || Math.abs(System.currentTimeMillis()-Long.parseLong(time))>10000){
							Log.println("========error token from ");
							Log.println("========time1 "+System.currentTimeMillis());
							Log.println("========time2 "+time);
							Log.println("======== "+custData.toString());
							Log.println("======== "+json.toString());
//							GlobalData.println(token);
//							GlobalData.println(sign_server);
							return;
						}
					}

					if(no.length()>0){
						retV=true;
						custData.setInfo(info);
						custData.setNo(no);
					}
				}


				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", retV);
				json_ret.put("data", value);
//				custData.send(json_ret.toString());
				send(custData,json_ret);

			}else if(cmd.equals("getNo")){
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", "ok");
				json_ret.put("data", custData.getKey());
//				custData.send(json_ret.toString());
				send(custData,json_ret);
			}else if(cmd.equals("sendNo")){
				JSONObject json_send = new JSONObject();
				json_send.put("cmd", cmd);
				json_send.put("value", value);
				json_send.put("info", custData.getInfo());
				String key=json.optString("no").trim();

				boolean retV=false;
				if(key.length()>0 && CustDataMap.containsKey(key)){
					send(CustDataMap.get(key),json_send);
					retV=true;
				}
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", retV);
				json_ret.put("data", value);
//				custData.send(json_ret.toString());
				send(custData,json_ret);
			}else if(cmd.equals("addGroup")){
				String group=value.trim();
				String retV="ok";
				if(group.length()>0){
					String old_gp=custData.getGroup();
					if(old_gp.length()>0 && !old_gp.equals(group)){
						exitGroup(custData ,old_gp);
					}
					custData.setGroup(group);
					if(!GroupDataMap.containsKey(group))GroupDataMap.put(group, new ArrayList<>());
					if(!GroupDataMap.get(group).contains(custData))GroupDataMap.get(group).add(custData);
				}
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", retV);
				json_ret.put("data", String.join(",", custData.getGroup()));
//				custData.send(json_ret.toString());
				send(custData,json_ret);
			}else if(cmd.equals("removeGroup")){
				String group=value.trim();
				if(group.length()>0){
					exitGroup(custData ,group);
				}
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", "ok");
				json_ret.put("data", custData.getGroup());
				send(custData,json_ret);
			}else if(cmd.equals("getGroupSize")){
				String group=custData.getGroup();
				if(group.trim().length()==0)return;
				int gps=0;
				if(GroupDataMap.containsKey(group))gps=GroupDataMap.get(group).size();
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", "ok");
				json_ret.put("data", gps);
//				custData.send(json_ret.toString());
				send(custData,json_ret);
			}else if(cmd.equals("sendGroup") || cmd.equals("sendGroupFromServer")){
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", value);
				json_ret.put("info", custData.getInfo());
				json_ret.put("no", custData.getNo());
				json_ret.put("time", System.currentTimeMillis()+"");


				String group=custData.getGroup();
				if(group.trim().length()==0)return;
				if(GroupDataMap.containsKey(group) && !isBlack(group,custData.getNo())) {
					json_ret.put("size", GroupDataMap.get(group).size());
					List<CustData> lll=GroupDataMap.get(group);
					for(int i=0;i<lll.size();i++){
						send(lll.get(i),json_ret);
					}
					if(!GroupMsgMap.containsKey(group)){
						GroupMsgMap.put(group,new ArrayList<>());
					}
					GroupMsgMap.get(group).add(json_ret);
					if(GroupMsgMap.get(group).size()>10)GroupMsgMap.get(group).remove(0);
				}
				if(isBlack(group,custData.getNo())) {
					JSONObject json_ret2 = new JSONObject();
					json_ret2.put("cmd", cmd);
					json_ret2.put("value", false);
					json_ret2.put("msg", "account blocked");
					send(custData,json_ret2);
				}
			}else if(cmd.equals("sendGroupPrive")){
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", value);
				json_ret.put("info", custData.getInfo());
				json_ret.put("no", custData.getNo());
				json_ret.put("time", System.currentTimeMillis()+"");

				String group=custData.getGroup();;
				if(group.trim().length()==0)return;
				String no=json.optString("no").trim();
				boolean retV=false;
				if(GroupDataMap.containsKey(group) && no.length()>0  && !isBlack(group,custData.getNo())) {
					json_ret.put("size", GroupDataMap.get(group).size());
					List<CustData> lll=GroupDataMap.get(group);
					for(int i=0;i<lll.size();i++){
						if(lll.get(i).getNo().equals(no)){
							send(lll.get(i),json_ret);
							retV=true;
						}
					}
				}
				JSONObject json_ret2 = new JSONObject();
				json_ret2.put("cmd", cmd);
				json_ret2.put("value", retV);
				if(isBlack(group,custData.getNo()))json_ret2.put("msg", "account blocked");
				send(custData,json_ret2);
			}else if(cmd.equals("getGroupMsg")){
				String group=custData.getGroup();;
				if(group.trim().length()==0)return;
				JSONArray ja=new JSONArray();
				if(GroupMsgMap.containsKey(group) && GroupMsgMap.get(group).size()>0){
					ja=new JSONArray(GroupMsgMap.get(group));
				}
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", true);
				json_ret.put("data", ja);
//				custData.send(json_ret.toString());
				send(custData,json_ret);



//////////////////////// admin cmd
			}else if(cmd.startsWith("Server") || cmd.startsWith("Admin") || cmd.startsWith("Sys")){
				String group=custData.getGroup();;
				if(group.trim().length()==0)return;
				boolean retV=false;
//				String info="";
//				String no="";
				String token="";
				String time="";
				if(value.length()>0){
					//custData.setInfo(value);

					if(getPass(custData.getGroup()).length()>0){
						try{
							token=json.optString("token").trim();
						}catch(Exception e){
							e.printStackTrace();
						}
						try{
							time=json.optString("time").trim();
						}catch(Exception e){
							e.printStackTrace();
						}
						String sign_server=Hmac.hash_sha256(getPass(custData.getGroup())+time,cmd+value);
						if(!token.toLowerCase().equals(sign_server.toLowerCase()) || Math.abs(System.currentTimeMillis()-Long.parseLong(time))>10000){
							Log.println("========error token from ");
							Log.println("========time1 "+System.currentTimeMillis());
							Log.println("========time2 "+time);
							Log.println("======== "+custData.toString());
							Log.println("======== "+json.toString());
//							Log.println(token);
//							Log.println(sign_server);
							return;
						}
					}
					// 驗證完後另外處理
					if(cmd.startsWith("Sys")){
						if(!conf.getSysGroupL().contains(custData.getGroup())
								|| !conf.getSysNoL().contains(custData.getNo()))return;
						onSysCmd(custData, json);
					}else {
						onAdminCmd(custData, json);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

			try {
				JSONObject json_ret2 = new JSONObject();
				json_ret2.put("cmd", cmd);
				json_ret2.put("value", "Exception");
				json_ret2.put("Exception",e.toString());
				send(custData,json_ret2);
			} catch (JSONException jsonException) {
				jsonException.printStackTrace();
			}

		}
	}
	private static void onSysCmd(CustData custData, JSONObject json){

		String cmd="";
		String value="";
		try{
			cmd=json.optString("cmd");//get("type");
		}catch(Exception e){
			e.printStackTrace();
			cmd="";
		}
		try{
			value=json.optString("value");//get("type");
		}catch(Exception e){
			e.printStackTrace();
			value="";
		}
		if(cmd.equals(""))return;
		cmd=cmd.trim();
		value=value.trim();
		try {


			if(cmd.equals("SysGetAllGroupInfo")){
				JSONArray ja=new JSONArray();
				for(Map.Entry<String,List<CustData>> ent:GroupDataMap.entrySet()){
					JSONObject _tmp = new JSONObject();
					_tmp.put("group",ent.getKey());
					_tmp.put("size",ent.getValue().size());
					String pass="";
					if(GroupPassMap.containsKey(ent.getKey()))pass=GroupPassMap.get(ent.getKey());
					_tmp.put("pass",pass);
					ja.put(_tmp);
				}

				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", ja);
				send(custData,json_ret);
			}
			if(cmd.equals("SysAddGroupPass")){
				String group=json.optString("group").trim();//get("type");
				if(group.length()>0 && value.length()>0){
					GroupPassMap.put(group,value);
				}
			}
			if(cmd.equals("SysBroadcast")){
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", value);
				json_ret.put("info", custData.getInfo());
				json_ret.put("no", custData.getNo());
				json_ret.put("time", System.currentTimeMillis()+"");
				String group=json.optString("group").trim();//get("type");

				if(GroupDataMap.containsKey(group)) {

					json_ret.put("size", GroupDataMap.get(group).size());
					List<CustData> lll=GroupDataMap.get(group);
					for(int i=0;i<lll.size();i++){
						send(lll.get(i),json_ret);
//						lll.get(i).send(json_ret.toString());
					}
					if(!GroupMsgMap.containsKey(group)){
						GroupMsgMap.put(group,new ArrayList<>());
					}
					GroupMsgMap.get(group).add(json_ret);
					if(GroupMsgMap.get(group).size()>10)GroupMsgMap.get(group).remove(0);
				}
			}
		}catch (Exception e){
			e.printStackTrace();
			try{
				JSONObject json_ret2 = new JSONObject();
				json_ret2.put("cmd", cmd);
				json_ret2.put("value", "Exception");
				json_ret2.put("Exception",e.toString());
//					custData.send(json_ret2.toString());
				send(custData,json_ret2);
			}catch(Exception ex){

			}

		}
	}
	private static void onAdminCmd(CustData custData, JSONObject json){

		String cmd="";
		String value="";
		try{
			cmd=json.optString("cmd");//get("type");
		}catch(Exception e){
			e.printStackTrace();
			cmd="";
		}
		try{
			value=json.optString("value");//get("type");
		}catch(Exception e){
			e.printStackTrace();
			value="";
		}
		if(cmd.equals(""))return;
		cmd=cmd.trim();
		value=value.trim();
		try {
			if(cmd.equals("AdminAddBlack")){
				String no=json.optString("no").trim();
				addBlack(custData.getGroup(),no);
			}
			if(cmd.equals("AdminDelBlack")){
				String no=json.optString("no").trim();
				delBlack(custData.getGroup(),no);
			}
			if(cmd.equals("AdminGetBlack")){
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", new JSONArray(getBlackList(custData.getGroup())));
				send(custData,json_ret);
			}

			if(cmd.equals("AdminClearMsg")){
				String group=custData.getGroup();;
				String no=json.optString("no").trim();
				if(no.length()>0 && group.length()>0 && GroupMsgMap.containsKey(group) && GroupMsgMap.get(group).size()>0){
					JSONArray ja=new JSONArray(GroupMsgMap.get(group));
					ArrayList<JSONObject> arr=new ArrayList<>();
					boolean isClear=false;
					for(int i=0;i<ja.length();i++){
						JSONObject jo=ja.optJSONObject(i);
						if(jo.has("no") && jo.optString("no").equals(no)){
							isClear=true;
							continue;
						}
						arr.add(jo);
					}
					GroupMsgMap.put(group,arr);



					JSONObject json_ret = new JSONObject();
					json_ret.put("cmd", cmd);
					json_ret.put("value", no);
					json_ret.put("info", custData.getInfo());
					json_ret.put("no", custData.getNo());
					json_ret.put("time", System.currentTimeMillis()+"");
					if(GroupDataMap.containsKey(group)) {
						json_ret.put("size", GroupDataMap.get(group).size());
						List<CustData> lll=GroupDataMap.get(group);
						for(int i=0;i<lll.size();i++){
							send(lll.get(i),json_ret);
						}
					}
				}
			}
			if(cmd.equals("AdminClearSingleMsg")){
				String group=custData.getGroup();;
				String no=json.optString("no").trim();
				String mTime=json.optString("mTime").trim();
				if(no.length()>0 && group.length()>0 && GroupMsgMap.containsKey(group) && GroupMsgMap.get(group).size()>0){
					JSONArray ja=new JSONArray(GroupMsgMap.get(group));
					ArrayList<JSONObject> arr=new ArrayList<>();
					boolean isClear=false;
					for(int i=0;i<ja.length();i++){
						JSONObject jo=ja.optJSONObject(i);
						if(jo.has("no") && jo.has("time") && jo.optString("no").equals(no) && jo.optString("time").equals(mTime)){
							isClear=true;
							continue;
						}
						arr.add(jo);
					}
					GroupMsgMap.put(group,arr);



					JSONObject json_ret = new JSONObject();
					json_ret.put("cmd", cmd);
					json_ret.put("value", no);
					json_ret.put("mTime", mTime);
					json_ret.put("info", custData.getInfo());
					json_ret.put("no", custData.getNo());
					json_ret.put("time", System.currentTimeMillis()+"");
					if(GroupDataMap.containsKey(group)) {
						json_ret.put("size", GroupDataMap.get(group).size());
						List<CustData> lll=GroupDataMap.get(group);
						for(int i=0;i<lll.size();i++){
							send(lll.get(i),json_ret);
						}
					}
				}
			}


//			if(cmd.equals("ServerBroadcast")){
//				JSONObject json_ret = new JSONObject();
//				json_ret.put("cmd", cmd);
//				json_ret.put("value", value);
//				json_ret.put("info", custData.getInfo());
//				json_ret.put("no", custData.getNo());
//				json_ret.put("time", System.currentTimeMillis()+"");
//				String group=json.optString("group").trim();//get("type");
//
//				if(GroupDataMap.containsKey(group)) {
//
//					json_ret.put("size", GroupDataMap.get(group).size());
//					List<CustData> lll=GroupDataMap.get(group);
//					for(int i=0;i<lll.size();i++){
//						send(lll.get(i),json_ret);
////						lll.get(i).send(json_ret.toString());
//					}
//					if(!GroupMsgMap.containsKey(group)){
//						GroupMsgMap.put(group,new ArrayList<>());
//					}
//					GroupMsgMap.get(group).add(json_ret);
//					if(GroupMsgMap.get(group).size()>10)GroupMsgMap.get(group).remove(0);
//				}
//			}
		}catch (Exception e){
			e.printStackTrace();
			try{
				JSONObject json_ret2 = new JSONObject();
				json_ret2.put("cmd", cmd);
				json_ret2.put("value", "Exception");
				json_ret2.put("Exception",e.toString());
//					custData.send(json_ret2.toString());
				send(custData,json_ret2);
			}catch(Exception ex){

			}

		}
	}
	public static void send(CustData custData,JSONObject dataJ){
		try {
			if(!dataJ.has("time"))dataJ.put("time", System.currentTimeMillis()+"");
			custData.send(dataJ.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}
	public static void exitGroup(CustData custData ,String group){
		if(group.length()==0)return;

		JSONObject jobj=new JSONObject();
		try {
			jobj.put("cmd","sendGroupFromServer");
			jobj.put("group",group);
			jobj.put("value",custData.getNo());
			GlobalData.onData(custData ,jobj,true);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		if(GroupDataMap.containsKey(group)){
			List<CustData> ll=GroupDataMap.get(group);
			if(ll.contains(custData)){
				ll.remove(custData);
			}
		}
		if(GroupDataMap.get(group).size()<=0){
			GroupDataMap.remove(group);
			if(GroupMsgMap.containsKey(group))GroupMsgMap.remove(group);
		}
		custData.setGroup("");
	}
	public static CustData CustExit(String key){
		CustData custData=null;
		try{
			if(CustDataMap.containsKey(key))custData=CustDataMap.get(key);
			//if(CustDataMap.containsKey(key))custData=CustDataMap.remove(key);
			if(custData!=null){
//				for(int i=0;i< custData.getGroupA().size();i++){
//					String group=custData.getGroupA().get(i);
//					exitGroup(custData ,group);
//				}
				exitGroup(custData ,custData.getGroup());
			}
			if(CustDataMap.containsKey(key))CustDataMap.remove(key);
		}catch(Exception e){e.printStackTrace();}

		return custData;
	}
	public static boolean isBlack(String group,String no){
		if (group.trim().length()==0 || no.trim().length()==0)return true;
		if(blackList.containsKey(group)){
			if(blackList.get(group).contains(no))return true;
		}
		return false;
	}
	public static void addBlack(String group,String no){
		if(group.trim().length()==0 || no.trim().length()==0)return;
		if(!blackList.containsKey(group))blackList.put(group,new ArrayList<>());
		if(!blackList.get(group).contains(no))blackList.get(group).add(no);

	}
	public static void delBlack(String group,String no){
		if(group.trim().length()==0 || no.trim().length()==0)return;
		if(!blackList.containsKey(group))return;
		if(blackList.get(group).contains(no))blackList.get(group).remove(no);
	}
	public static List<String> getBlackList(String group){
		if(group.trim().length()==0 )return new ArrayList<>();
		if(!blackList.containsKey(group))return new ArrayList<>();
		return blackList.get(group);
	}





	private static ConcurrentHashMap <String, Integer> GroupFlowMap=new ConcurrentHashMap<>();
	private static ConcurrentHashMap <String, String> GroupPassMap=new ConcurrentHashMap<>();
	public static String getPass(String group){
		if(group.trim().length()==0)return conf.getPass();
		if(GroupPassMap.containsKey(group) && !GroupPassMap.get(group).equals("")){
			return GroupPassMap.get(group);
		}
		return conf.getPass();
	}

}
