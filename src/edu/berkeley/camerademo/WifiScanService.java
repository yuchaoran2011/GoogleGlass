package edu.berkeley.camerademo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import pf.utils.Point2D;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

class ScanComparable implements Comparator<ScanResult> {

	@Override
	public int compare(ScanResult s1, ScanResult s2) {
		return (s1.level>s2.level ? -1 : (s1.level==s2.level ? 0 : 1));
	}
}

public class WifiScanService extends Service {
	private final IBinder mBinder = new MyBinder();
	WifiManager wifi;
	Integer floor = 1;
	String cutoff_freq = "3000";
	Integer requestId = 1;
	static final String WIFI_URL = "http://sofia.eecs.berkeley.edu:8003/wifi/submit_fingerprint";
	Point2D wifiPos;
	ArrayList<Point2D> cloudPos;
	ArrayList<Step> stepHistory;
	SparseArray<long[]> wifiRequests;
	public void writeToFile(String data)  {
		File root = new File(Environment.getExternalStorageDirectory()+File.separator+"wifiloc");

		File file = new File(root, "wifiscan.txt");
		FileWriter filewriter;
		try {
			filewriter = new FileWriter(file,true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Log.d("WIFIRES","Could not create file " + e1.getMessage());
			return;
		}

		BufferedWriter out = new BufferedWriter(filewriter);
		try {
			out.write(data);
			out.close();
		} catch (IOException e)
		{
			Log.d("WIFIRES","Could not write to file " + e.getMessage());
		}
	}

	private final BroadcastReceiver receiverWifi = new BroadcastReceiver(){


		@Override
		public void onReceive(Context context, Intent intent) {
			List<ScanResult> scanResults = wifi.getScanResults();

			if(scanResults == null || scanResults.isEmpty()) {
				Log.d("WIFI_RECEIVER","No APs detected :(");
				Toast.makeText(context, "No APs detected :(", Toast.LENGTH_SHORT).show();
			} else {
				Collections.sort(scanResults, new ScanComparable());
				HashMap<String,Integer> reqParams = new HashMap<String,Integer>();
				String logResult = "";
				long tstamp = System.currentTimeMillis();
				for (ScanResult scan : scanResults) {
					reqParams.put(scan.BSSID.toString(), scan.level);
					logResult += tstamp + " " + scan.BSSID.toString() + " " + scan.level + " "+  scan.frequency + " " + scan.SSID + "\n";
				}
				writeToFile(logResult);
				reqParams.put("cluster_id", floor);
				reqParams.put("request_id", requestId);
				JSONObject queryCore = new JSONObject(reqParams);
				HashMap<String, JSONObject> postedData = new HashMap<String, JSONObject>();
				postedData.put("fingerprint_data", queryCore);

				JSONObject query = new JSONObject(postedData);

				QueryTask qr;

				if (wifiRequests.get(requestId) == null)
					Log.d("REQMIS", "ALERT: " + requestId + " " + wifiRequests.size());
				else {
					long[] ts = wifiRequests.get(requestId);
					ts[1] = System.currentTimeMillis();
					wifiRequests.put(requestId, ts);
					qr = new QueryTask(WIFI_URL, query, "wifi", wifiRequests);
					qr.execute(context);

					requestId++;
				}
			}
		}   
	};

	@Override 
	public void onCreate() {
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.net.wifi.SCAN_RESULTS");
		registerReceiver(receiverWifi, filter);
		cloudPos = new ArrayList<Point2D>();
		stepHistory = new ArrayList<Step>();
		wifiRequests = new SparseArray<long[]>();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		unregisterReceiver(receiverWifi);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("WIFISCAN","service started!");
		Bundle b = intent.getExtras();
		floor = b.getInt("floor_id");
		cutoff_freq = b.getString("cutoff_freq");

		long[] ts = new long[2];
		ts[0] = System.currentTimeMillis();
		wifiRequests.put(requestId, ts);
		wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		if (!wifi.startScan()) {
			Log.d("SCANNING_FAILURE","Wifi is turned off!");
		}
		return Service.START_NOT_STICKY;
	}

	public void setCloudPosition(Point2D newPos) {
		cloudPos.add(newPos);
	}
	public void addStep(Step s) {
		stepHistory.add(s);
	}

	public ArrayList<Step> getStepHistory(long ts_from, long ts_to) {
		ArrayList<Step> stepHist = new ArrayList<Step>();

		for (Step s: stepHistory) {
			if (s.tstamp > ts_to)
				break;
			if (s.tstamp >= ts_from)
				stepHist.add(s);
		}
		Log.d("STEPHIST", "STEPS: "+ stepHist.size());
		return stepHist;
	}

	@Override
	public IBinder onBind(Intent intent) {
		//TODO for communication return IBinder implementation
		return mBinder;
	}

	public class MyBinder extends Binder {
		WifiScanService getService() {
			return WifiScanService.this;
		}
	}
}
