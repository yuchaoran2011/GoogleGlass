package edu.berkeley.camerademo;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;

public class LocRequest extends Thread {
	private String url_str;
	private JSONObject json;
	private String qryType;
	private byte[] imgData;
	public String result;
	//private Point2D oldLoc;
	private HttpClient httpclient;
	HashMap<Integer,long[]> wifiRequests;
	Context c;

	public LocRequest(HttpClient httpclient, String url, JSONObject json, String qryType) {
		this.url_str = url;
		this.json = json;
		this.qryType = qryType;
		this.httpclient = httpclient;

	}

	public LocRequest(Context c, HttpClient httpclient, String url, JSONObject json, String qryType, byte[] imgData) {
		this(httpclient, url,json,qryType);
		this.imgData = imgData;
		this.c = c;

	}

	public LocRequest(Context c, HttpClient httpclient, String url, JSONObject json, String qryType, HashMap<Integer,long[]> wifiRequests) {
		this(httpclient, url,json,qryType);
		this.wifiRequests = wifiRequests;
		this.c = c;
	}

	public void run() {
		//byte[] data = json.toString().getBytes();
		String boundary = "-------------------------";
		MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, boundary, null);
		//AndroidHttpClient httpClient = AndroidHttpClient.newInstance("Android");

		try {

			HttpPost httppost = new HttpPost(url_str);

			if (this.qryType.equals("image")) {
				multipartEntity.addPart("data", new ByteArrayBody(this.imgData,"loc.jpg"));
				multipartEntity.addPart("params",new StringBody(json.toString()));
				httppost.setEntity(multipartEntity);
			}
			else { // wifi request
				StringEntity se = new StringEntity(json.toString());

				//sets the post request as the resulting string
				httppost.setEntity(se);
				//sets a request header so the page receving the request
				//will know what to do with it
				httppost.setHeader("Accept", "application/json");
				httppost.setHeader("Content-type", "application/json");
			}

			long t_sent = System.currentTimeMillis(); 
			HttpResponse response = httpclient.execute(httppost);
			StatusLine statusLine = response.getStatusLine();
			int statusCode = statusLine.getStatusCode();
			//httpClient.close();
			if (statusCode == 200) {
				HttpEntity entity = response.getEntity();
				InputStream content = entity.getContent();

				this.result = readStream(content);

				long t_received = System.currentTimeMillis();
				Intent i;
				if (this.qryType.equals("wifi")) { // wifi query
					i = new Intent("LOCATION_UPDATED");
				} else { // image query
					i = new Intent("IMG_LOCATION_UPDATED");
					JSONObject loc_json = new JSONObject();
					try {
						loc_json = new JSONObject(this.result);
						if (loc_json.has("image_data")) {
							String imgData = loc_json.getString("image_data");
							byte[] decodedString = Base64.decode(imgData, Base64.DEFAULT);
							String filePath = writeImgToFile(decodedString);
							i.putExtra("imgPath", filePath);
							loc_json.remove("image_data");
						}
						this.result = loc_json.toString();
						i.putExtra("t_sent", t_sent);
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				i.putExtra("json_response",this.result);
				/*
				     if (this.oldLoc != null) {
				    	 double[] oldCoords = ne
				    	 w double[2];
				    	 oldCoords[0] = oldLoc.getX();
				    	 oldCoords[1] = oldLoc.getY();
				    	 i.putExtra("oldPosition", oldCoords);
				     	}
				 */
				if (wifiRequests != null) {
					JSONObject loc_json = new JSONObject();
					try {
						loc_json = new JSONObject(this.result);
						Integer request_id = loc_json.getInt("request_id");
						long[] tstamps = wifiRequests.get(request_id); 

						i.putExtra("t_init", tstamps[0]);
						i.putExtra("t_scanend", tstamps[1]);
						i.putExtra("t_sent", t_sent);
						i.putExtra("t_received", t_received);
					} catch (JSONException e) {e.printStackTrace();}
				}
				c.sendBroadcast(i);
				//content.close();
			} // statusCode 200
		}
		catch (MalformedURLException e){ }
		catch (IOException e) {Log.d("URLEXCEPTION","FAILURE "+ e.getMessage()); }
	}


	private String readStream(InputStream is) {
		try {
			ByteArrayOutputStream bo = new ByteArrayOutputStream();
			int i = is.read();
			while(i != -1) {
				bo.write(i);
				i = is.read();
			}
			//Log.d("RESPONSE DATA", bo.toString());
			return bo.toString();
		} catch (IOException e) {

			Log.d("readStreamException", "Read Stream failed!!");
			return "";

		}
	}

	public String writeImgToFile(byte[] data) {
		File root = new File(Environment.getExternalStorageDirectory()+File.separator+"wifiloc");
		File output = new File(root, "imgresp_" + System.currentTimeMillis() + ".jpg");
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(output);
			fos.write(data);
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return output.getPath();
	}    	    
}
