package edu.berkeley.camerademo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONException;
import org.json.JSONObject;

import pf.utils.Point2D;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.util.SparseArray;

public class QueryTask extends AsyncTask<Context, Void, Void> 
{
	private String url_str;
	private JSONObject json;
	private String qryType;
	private byte[] imgData;
	public String result;
	private Point2D oldLoc;
	SparseArray<long[]> wifiRequests;

	public QueryTask(String url, JSONObject json, String qryType) {
		this.url_str = url;
		this.json = json;
		this.qryType = qryType;

	}

	public QueryTask(String url, JSONObject json, String qryType, byte[] imgData) {
		this(url,json,qryType);
		this.imgData = imgData;

	}

	public QueryTask(String url, JSONObject json, String qryType, Point2D oldLoc) {
		this(url,json,qryType);
		this.oldLoc = oldLoc;
	}

	public QueryTask(String url, JSONObject json, String qryType, SparseArray<long[]> wifiRequests) {
		this(url,json,qryType);
		this.wifiRequests = wifiRequests;
	}


	protected Void doInBackground(Context... c) {
		byte[] data = json.toString().getBytes();
		//Log.d("IMG", "sending: " + json.toString());
		String boundary = "-------------------------";
		MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, boundary, null);
		try {

			URL url = new URL(url_str);


			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			try {
				urlConnection.setReadTimeout( 20000 /*milliseconds*/ );
				urlConnection.setConnectTimeout( 15000 /* milliseconds */ );

				urlConnection.setDoInput(true);
				urlConnection.setDoOutput(true);
				//urlConnection.setFixedLengthStreamingMode(data.length);
				if (this.qryType.equals("image")) {
					//urlConnection.setRequestProperty("Content-Type","multipart/form-data");
					urlConnection.setRequestMethod("POST");
					urlConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
					//urlConnection.setRequestProperty("Accept", "application/json");
					multipartEntity.addPart("data", new ByteArrayBody(this.imgData,"loc.jpg"));
					multipartEntity.addPart("params",new StringBody(json.toString()));
				}
				else {
					urlConnection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
					urlConnection.setRequestProperty("Accept", "application/json");
					urlConnection.setRequestMethod("POST");
				}

				urlConnection.connect();
				OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

				if (this.qryType.equals("image")) {
					Log.d("Picture", "Sending image...");
					try {
						multipartEntity.writeTo(out);
						out.flush();
					} catch (IOException e)
					{
						e.printStackTrace();
					}

				}
				else {
					out.write(data);
					out.flush();
				}


				InputStream in = new BufferedInputStream(urlConnection.getInputStream());
				this.result = readStream(in);
				//Log.d("RES", "Received response: " + this.result);
				long t_received = System.currentTimeMillis();
				Intent i;
				if (this.qryType.equals("wifi")) {
					i = new Intent("LOCATION_UPDATED");
				} else {
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
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}

				i.putExtra("json_response",this.result);
				if (this.oldLoc != null) {
					double[] oldCoords = new double[2];
					oldCoords[0] = oldLoc.getX();
					oldCoords[1] = oldLoc.getY();
					i.putExtra("oldPosition", oldCoords);
				}
				if (wifiRequests != null) {
					JSONObject loc_json = new JSONObject();
					try {
						Integer request_id = 0;
						loc_json = new JSONObject(this.result);
						if (loc_json.has("request_id")) {
							request_id = loc_json.getInt("request_id");
							long[] tstamps = wifiRequests.get(request_id); 

							i.putExtra("t_init", tstamps[0]);
							i.putExtra("t_sent", tstamps[1]);
							i.putExtra("t_received", t_received);
						}
					} catch (JSONException e) {e.printStackTrace();}
				}
				c[0].sendBroadcast(i);

				in.close();
				out.close();
			}
			finally {
				urlConnection.disconnect();
			}
		}
		catch (MalformedURLException e){ }
		catch (IOException e) {Log.d("URLEXCEPTION","FAILURE!"+ e.getMessage()); }

		return null;
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
