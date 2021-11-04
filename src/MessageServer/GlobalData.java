package MessageServer;

import java.io.*;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.threex.lib.hash.Hmac;

import MessageServer.websocket.CustData;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.bind.DatatypeConverter;


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

	public static List blackList = Collections.synchronizedList(new ArrayList());

	public static void onData(CustData custData, JSONObject json){
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

					if(conf.getPass().length()>0){
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
						String sign_server=Hmac.hash_sha256(conf.getPass()+time,cmd+info+no);
						if(!token.toLowerCase().equals(sign_server.toLowerCase()) || Math.abs(System.currentTimeMillis()-Long.parseLong(time))>10000){
							GlobalData.println("========error token from ");
							GlobalData.println("========time1 "+System.currentTimeMillis());
							GlobalData.println("========time2 "+time);
							GlobalData.println("======== "+custData.toString());
							GlobalData.println("======== "+json.toString());
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
				String data[]=new String[]{value};
				if(value.indexOf(",")>0){
					data=value.split(",");
				}
				for(int i=0;i<data.length && i<10 && custData.getGroupA().size()<=10;i++){
					data[i]=data[i].trim();
					if(data[i].length()<=0)continue;
					custData.getGroupA().add(data[i]);
					if(!GroupDataMap.containsKey(data[i]))GroupDataMap.put(data[i], new ArrayList<>());
					if(!GroupDataMap.get(data[i]).contains(custData))GroupDataMap.get(data[i]).add(custData);
				}
				String retV="ok";
				if(data.length>10 || custData.getGroupA().size()>10){
					retV="Too many group";
				}
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", retV);
				json_ret.put("data", String.join(",", custData.getGroupA()));
//				custData.send(json_ret.toString());
				send(custData,json_ret);
			}else if(cmd.equals("removeGroup")){
				String data[]=new String[]{value};
				if(value.indexOf(",")>0){
					data=value.split(",");
				}
				for(int i=0;i<data.length;i++){
					data[i]=data[i].trim();
					if(data[i].length()<=0)continue;
					custData.getGroupA().remove(data[i]);
					exitGroup(custData ,data[i]);
				}
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", "ok");
				json_ret.put("data", String.join(",", custData.getGroupA()));
//				custData.send(json_ret.toString());
				send(custData,json_ret);
			}else if(cmd.equals("getGroupSize")){
				String group=json.optString("group").trim();
				int gps=0;
				if(GroupDataMap.containsKey(group))gps=GroupDataMap.get(group).size();
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", "ok");
				json_ret.put("data", gps);
//				custData.send(json_ret.toString());
				send(custData,json_ret);
			}else if(cmd.equals("sendGroup")){
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", value);
				json_ret.put("info", custData.getInfo());
				json_ret.put("no", custData.getNo());
				json_ret.put("time", System.currentTimeMillis()+"");


				String group=json.optString("group").trim();
				if(GroupDataMap.containsKey(group) && !blackList.contains(custData.getNo())) {
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
				if(blackList.contains(custData.getNo())) {
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

				String group=json.optString("group").trim();
				String no=json.optString("no").trim();
				boolean retV=false;
				if(GroupDataMap.containsKey(group) && no.length()>0  && !blackList.contains(custData.getNo())) {
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
				if(blackList.contains(custData.getNo()))json_ret2.put("msg", "account blocked");
				send(custData,json_ret2);
			}else if(cmd.equals("getGroupMsg")){
				String group=json.optString("group");
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
			}else if(cmd.startsWith("Server") || cmd.startsWith("Admin")){
				boolean retV=false;
//				String info="";
//				String no="";
				String token="";
				String time="";
				if(value.length()>0){
					//custData.setInfo(value);

					if(conf.getPass().length()>0){
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
						String sign_server=Hmac.hash_sha256(conf.getPass()+time,cmd+value);
						if(!token.toLowerCase().equals(sign_server.toLowerCase()) || Math.abs(System.currentTimeMillis()-Long.parseLong(time))>10000){
							GlobalData.println("========error token from ");
							GlobalData.println("========time1 "+System.currentTimeMillis());
							GlobalData.println("========time2 "+time);
							GlobalData.println("======== "+custData.toString());
							GlobalData.println("======== "+json.toString());
//							GlobalData.println(token);
//							GlobalData.println(sign_server);
							return;
						}
					}
					// 驗證完後另外處理
					onAdminCmd(custData, json);
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
				if (no.length()>0 && !blackList.contains(no)) {
					blackList.add(no);
				}
			}
			if(cmd.equals("AdminDelBlack")){
				String no=json.optString("no").trim();
				if (no.length()>0 && blackList.contains(no)) {
					blackList.remove(no);
				}
			}
			if(cmd.equals("AdminGetBlack")){
				JSONObject json_ret = new JSONObject();
				json_ret.put("cmd", cmd);
				json_ret.put("value", new JSONArray(blackList));
				send(custData,json_ret);

			}
			if(cmd.equals("AdminClearMsg")){
				String group=json.optString("group").trim();
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
				String group=json.optString("group").trim();
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
			if(cmd.equals("ServerBroadcast")){
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
	public static void send(CustData custData,JSONObject dataJ){
		try {
			if(!dataJ.has("time"))dataJ.put("time", System.currentTimeMillis()+"");
			custData.send(dataJ.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}
	public static void exitGroup(CustData custData ,String group){


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
	}
	public static CustData CustExit(String key){
		CustData custData=null;
		try{
			if(CustDataMap.containsKey(key))custData=CustDataMap.remove(key);
			if(custData!=null){
				for(int i=0;i< custData.getGroupA().size();i++){
					String group=custData.getGroupA().get(i);
					exitGroup(custData ,group);
				}
			}
		}catch(Exception e){e.printStackTrace();}

		return custData;
	}


	public static void println(String s){
		System.out.println( GlobalData.getDateTime()+" "+s);
	}
	public static String getDateTime(){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date());
	}
	public static SSLContext getContext(String path_crt,String path_key) {
		SSLContext context;
		String password = "";
		String pathname = "pem";
		try {
			X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream (path_crt));
			Certificate[] certA = parseDERFromPEMtoArr(getBytes(new File(path_crt)),"-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
//			X509Certificate cert = generateCertificateFromDER(certBytes);

			context = SSLContext.getInstance("TLS");//
			byte[] keyBytes = parseDERFromPEM(getBytes(new File(path_key)),"-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----");
//			byte[] keyBytes = getBytes(new File(path_key));//
			RSAPrivateKey key = generatePrivateKeyFromDER(keyBytes);





			KeyStore keystore = KeyStore.getInstance("JKS");
			keystore.load(null);
//			keystore.setCertificateEntry("cert-alias", cert);
//			keystore.setKeyEntry("key-alias", key, password.toCharArray(), new Certificate[]{cert});
			keystore.setCertificateEntry("cert-alias", cert);
			keystore.setKeyEntry("key-alias", key, password.toCharArray(), certA);

			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(keystore, password.toCharArray());

			KeyManager[] km = kmf.getKeyManagers();

			context.init(km, null, null);
		} catch (Exception e) {
			context = null;
			e.printStackTrace();
		}
		return context;
	}
	private static Certificate[] parseDERFromPEMtoArr(byte[] pem, String beginDelimiter, String endDelimiter) {
		String data = new String(pem);
		String[] tokens = data.split(beginDelimiter);
		Certificate[] ret=new Certificate[tokens.length-1];
		for(int i=1;i<tokens.length;i++){
			String _tmp=tokens[i].split(endDelimiter)[0];

			try {
				ret[i-1]=generateCertificateFromDER(DatatypeConverter.parseBase64Binary(_tmp));
			} catch (CertificateException e) {
				e.printStackTrace();
			}
		}

		return ret;
	}
	private static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
		String data = new String(pem);
		String[] tokens = data.split(beginDelimiter);
		tokens = tokens[1].split(endDelimiter);
		return DatatypeConverter.parseBase64Binary(tokens[0]);
	}

	private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes)
//			throws InvalidKeySpecException, NoSuchAlgorithmException
	throws IOException, GeneralSecurityException
	{

		DerParser parser = new DerParser(keyBytes);

		Asn1Object sequence = parser.read();
		if (sequence.getType() != DerParser.SEQUENCE)
			throw new IOException("Invalid DER: not a sequence"); //$NON-NLS-1$

		// Parse inside the sequence
		parser = sequence.getParser();

		parser.read(); // Skip version
		BigInteger modulus = parser.read().getInteger();
		BigInteger publicExp = parser.read().getInteger();
		BigInteger privateExp = parser.read().getInteger();
		BigInteger prime1 = parser.read().getInteger();
		BigInteger prime2 = parser.read().getInteger();
		BigInteger exp1 = parser.read().getInteger();
		BigInteger exp2 = parser.read().getInteger();
		BigInteger crtCoef = parser.read().getInteger();

		RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(
				modulus, publicExp, privateExp, prime1, prime2,
				exp1, exp2, crtCoef);

		KeyFactory factory = KeyFactory.getInstance("RSA");
		return (RSAPrivateKey) factory.generatePrivate(keySpec);




//		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
//
//		KeyFactory factory = KeyFactory.getInstance("RSA");
//
//		return (RSAPrivateKey) factory.generatePrivate(spec);
	}
	static class DerParser {

		// Classes
		public final static int UNIVERSAL = 0x00;
		public final static int APPLICATION = 0x40;
		public final static int CONTEXT = 0x80;
		public final static int PRIVATE = 0xC0;

		// Constructed Flag
		public final static int CONSTRUCTED = 0x20;

		// Tag and data types
		public final static int ANY = 0x00;
		public final static int BOOLEAN = 0x01;
		public final static int INTEGER = 0x02;
		public final static int BIT_STRING = 0x03;
		public final static int OCTET_STRING = 0x04;
		public final static int NULL = 0x05;
		public final static int OBJECT_IDENTIFIER = 0x06;
		public final static int REAL = 0x09;
		public final static int ENUMERATED = 0x0a;
		public final static int RELATIVE_OID = 0x0d;

		public final static int SEQUENCE = 0x10;
		public final static int SET = 0x11;

		public final static int NUMERIC_STRING = 0x12;
		public final static int PRINTABLE_STRING = 0x13;
		public final static int T61_STRING = 0x14;
		public final static int VIDEOTEX_STRING = 0x15;
		public final static int IA5_STRING = 0x16;
		public final static int GRAPHIC_STRING = 0x19;
		public final static int ISO646_STRING = 0x1A;
		public final static int GENERAL_STRING = 0x1B;

		public final static int UTF8_STRING = 0x0C;
		public final static int UNIVERSAL_STRING = 0x1C;
		public final static int BMP_STRING = 0x1E;

		public final static int UTC_TIME = 0x17;
		public final static int GENERALIZED_TIME = 0x18;

		protected InputStream in;

		/**
		 * Create a new DER decoder from an input stream.
		 *
		 * @param in
		 *            The DER encoded stream
		 */
		public DerParser(InputStream in) throws IOException {
			this.in = in;
		}

		/**
		 * Create a new DER decoder from a byte array.
		 *
		 * @param The
		 *            encoded bytes
		 * @throws IOException
		 */
		public DerParser(byte[] bytes) throws IOException {
			this(new ByteArrayInputStream(bytes));
		}

		/**
		 * Read next object. If it's constructed, the value holds
		 * encoded content and it should be parsed by a new
		 * parser from <code>Asn1Object.getParser</code>.
		 *
		 * @return A object
		 * @throws IOException
		 */
		public Asn1Object read() throws IOException {
			int tag = in.read();

			if (tag == -1)
				throw new IOException("Invalid DER: stream too short, missing tag"); //$NON-NLS-1$

			int length = getLength();

			byte[] value = new byte[length];
			int n = in.read(value);
			if (n < length)
				throw new IOException("Invalid DER: stream too short, missing value"); //$NON-NLS-1$

			Asn1Object o = new Asn1Object(tag, length, value);

			return o;
		}

		/**
		 * Decode the length of the field. Can only support length
		 * encoding up to 4 octets.
		 *
		 * <p/>In BER/DER encoding, length can be encoded in 2 forms,
		 * <ul>
		 * <li>Short form. One octet. Bit 8 has value "0" and bits 7-1
		 * give the length.
		 * <li>Long form. Two to 127 octets (only 4 is supported here).
		 * Bit 8 of first octet has value "1" and bits 7-1 give the
		 * number of additional length octets. Second and following
		 * octets give the length, base 256, most significant digit first.
		 * </ul>
		 * @return The length as integer
		 * @throws IOException
		 */
		private int getLength() throws IOException {

			int i = in.read();
			if (i == -1)
				throw new IOException("Invalid DER: length missing"); //$NON-NLS-1$

			// A single byte short length
			if ((i & ~0x7F) == 0)
				return i;

			int num = i & 0x7F;

			// We can't handle length longer than 4 bytes
			if ( i >= 0xFF || num > 4)
				throw new IOException("Invalid DER: length field too big (" //$NON-NLS-1$
						+ i + ")"); //$NON-NLS-1$

			byte[] bytes = new byte[num];
			int n = in.read(bytes);
			if (n < num)
				throw new IOException("Invalid DER: length too short"); //$NON-NLS-1$

			return new BigInteger(1, bytes).intValue();
		}

	}
	static class Asn1Object {

		protected final int type;
		protected final int length;
		protected final byte[] value;
		protected final int tag;

		/**
		 * Construct a ASN.1 TLV. The TLV could be either a
		 * constructed or primitive entity.
		 *
		 * <p/>The first byte in DER encoding is made of following fields,
		 * <pre>
		 *-------------------------------------------------
		 *|Bit 8|Bit 7|Bit 6|Bit 5|Bit 4|Bit 3|Bit 2|Bit 1|
		 *-------------------------------------------------
		 *|  Class    | CF  |     +      Type             |
		 *-------------------------------------------------
		 * </pre>
		 * <ul>
		 * <li>Class: Universal, Application, Context or Private
		 * <li>CF: Constructed flag. If 1, the field is constructed.
		 * <li>Type: This is actually called tag in ASN.1. It
		 * indicates data type (Integer, String) or a construct
		 * (sequence, choice, set).
		 * </ul>
		 *
		 * @param tag Tag or Identifier
		 * @param length Length of the field
		 * @param value Encoded octet string for the field.
		 */
		public Asn1Object(int tag, int length, byte[] value) {
			this.tag = tag;
			this.type = tag & 0x1F;
			this.length = length;
			this.value = value;
		}

		public int getType() {
			return type;
		}

		public int getLength() {
			return length;
		}

		public byte[] getValue() {
			return value;
		}

		public boolean isConstructed() {
			return  (tag & DerParser.CONSTRUCTED) == DerParser.CONSTRUCTED;
		}

		/**
		 * For constructed field, return a parser for its content.
		 *
		 * @return A parser for the construct.
		 * @throws IOException
		 */
		public DerParser getParser() throws IOException {
			if (!isConstructed())
				throw new IOException("Invalid DER: can't parse primitive entity"); //$NON-NLS-1$

			return new DerParser(value);
		}

		/**
		 * Get the value as integer
		 *
		 * @return BigInteger
		 * @throws IOException
		 */
		public BigInteger getInteger() throws IOException {
			if (type != DerParser.INTEGER)
				throw new IOException("Invalid DER: object is not integer"); //$NON-NLS-1$

			return new BigInteger(value);
		}

		/**
		 * Get value as string. Most strings are treated
		 * as Latin-1.
		 *
		 * @return Java string
		 * @throws IOException
		 */
		public String getString() throws IOException {

			String encoding;

			switch (type) {

				// Not all are Latin-1 but it's the closest thing
				case DerParser.NUMERIC_STRING:
				case DerParser.PRINTABLE_STRING:
				case DerParser.VIDEOTEX_STRING:
				case DerParser.IA5_STRING:
				case DerParser.GRAPHIC_STRING:
				case DerParser.ISO646_STRING:
				case DerParser.GENERAL_STRING:
					encoding = "ISO-8859-1"; //$NON-NLS-1$
					break;

				case DerParser.BMP_STRING:
					encoding = "UTF-16BE"; //$NON-NLS-1$
					break;

				case DerParser.UTF8_STRING:
					encoding = "UTF-8"; //$NON-NLS-1$
					break;

				case DerParser.UNIVERSAL_STRING:
					throw new IOException("Invalid DER: can't handle UCS-4 string"); //$NON-NLS-1$

				default:
					throw new IOException("Invalid DER: object is not a string"); //$NON-NLS-1$
			}

			return new String(value, encoding);
		}
	}




	private static X509Certificate generateCertificateFromDER(byte[] certBytes)
			throws CertificateException {
		CertificateFactory factory = CertificateFactory.getInstance("X.509");

		return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
	}

	private static byte[] getBytes(File file) {
		byte[] bytesArray = new byte[(int) file.length()];

		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			fis.read(bytesArray); //read file into bytes[]
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bytesArray;
	}
}
