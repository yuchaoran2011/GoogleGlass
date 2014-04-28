package edu.berkeley.camerademo;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import pf.floors.Area;
import pf.floors.AreaBuilder;
import pf.particle.ParticlePosition;
import pf.utils.Point2D;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import cz.muni.fi.sandbox.dsp.filters.ContinuousConvolution;
import cz.muni.fi.sandbox.dsp.filters.FrequencyCounter;
import cz.muni.fi.sandbox.dsp.filters.SinXPiWindow;
import cz.muni.fi.sandbox.service.stepdetector.MovingAverageStepDetector;
import cz.muni.fi.sandbox.service.stepdetector.MovingAverageStepDetector.MovingAverageStepDetectorState;

public class MainActivity extends Activity implements SensorEventListener {

	private static final String TAG = "ImgLoc";
	static final String IMAGE_URL = "http://ahvaz.eecs.berkeley.edu:8001";//sofia.eecs.berkeley.edu:8010
	private HttpClient httpclient;
	ClientConnectionManager cm;
	private Camera mCamera;
	private FrameLayout mFrameLayout, mFrameLayout2;
	private CameraPreview mPreview;
	private TextView textview;
	private int numPictureTaken = 0;

	private float[] cameraPose = new float[3];
	float[] orientVals = new float[3];
	float[] gravity = new float[3];
	float[] geomag = new float[3];
	float[] inR = new float[16];
	float[] I = new float[16];
	private SensorManager mSensorManager;
	private Sensor accelerometer, magnetometer;

	File root = new File(Environment.getExternalStorageDirectory()+File.separator+"wifiloc");

	private MovingAverageStepDetector mStepDetector;
	private ContinuousConvolution mCC;
	float mConvolution;
	private FrequencyCounter freqCounter;

	private ParticlePosition mParticleCloud;
	double[] cloudCenter;
	ArrayList<Point2D> imgStepHistory;

	final float pi = (float) Math.PI;
	final float rad2deg = 180/pi; 

	MapView mMapView;

	double movingAverage1 = MovingAverageStepDetector.MA1_WINDOW;
	double movingAverage2 = MovingAverageStepDetector.MA2_WINDOW;

	double lowPowerCutoff = MovingAverageStepDetector.LOW_POWER_CUTOFF_VALUE;
	double highPowerCutoff = MovingAverageStepDetector.HIGH_POWER_CUTOFF_VALUE;

	private int mMASize = 20;

	private AreaBuilder mCory2Builder;
	private Area mCory2;

	private String PATH_FILE_UPLOAD_URL = "http://10.142.34.53:8003/central/path_file";

	Intent intent;

	private WifiScanService wifiService;


	private final Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			Log.d(TAG, "Picture taken!");
			numPictureTaken += 1;
			textview.setText(Integer.toString(numPictureTaken));

			Bitmap tmp;
			try {
				tmp = createBitmap(data, 640, 480, 0);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				return;
			}
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			tmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
			data = stream.toByteArray();
			File root = new File(Environment.getExternalStorageDirectory()+File.separator+"wifiloc");
			File output = new File(root, "img_" + System.currentTimeMillis() + ".jpg");
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

			JSONObject pose, returnParams;
			JSONObject imageQuery;
			HashMap<String, Float> poseMap = new HashMap<String, Float>();
			HashMap<String, Boolean> returnMap = new HashMap<String, Boolean>();
			HashMap<String, Object> paramsMap = new HashMap<String, Object>();

			HashMap<String, Object> imageQueryMap = new HashMap<String, Object>();
			poseMap.put("latitude", 0f);
			poseMap.put("longitude", 0f);
			poseMap.put("altitude", (float)0.0);
			poseMap.put("yaw", cameraPose[0]);
			poseMap.put("pitch", cameraPose[1]);
			poseMap.put("roll", cameraPose[2]);
			poseMap.put("ambiguity_meters", (float)1.0e+12);
			pose = new JSONObject(poseMap);

			returnMap.put("statistics", true);
			returnMap.put("image_data", false);
			returnMap.put("estimated_client_pose", true);
			returnMap.put("pose_visualization_only", false);
			returnParams = new JSONObject(returnMap);

			paramsMap.put("method", "client_query");
			paramsMap.put("user", "test");
			paramsMap.put("database", "0815_db");
			paramsMap.put("deadline_seconds", 60.0);
			paramsMap.put("disable_gpu", false);
			paramsMap.put("perfmode", "fast");
			paramsMap.put("pose", pose);
			paramsMap.put("return", returnParams);

			imageQueryMap.put("params", paramsMap);
			imageQuery = new JSONObject(paramsMap);
			Context ctx = getBaseContext();

			LocRequest lr = new LocRequest(ctx, httpclient, IMAGE_URL, imageQuery, "image", data);
			lr.start();

			Toast.makeText(ctx, "Sent request to server", Toast.LENGTH_SHORT).show();

			restartCamera();
		}
	};

	private Bitmap createBitmap(byte[] imageData, int maxWidth, int maxHeight,
			int rotationDegrees) throws FileNotFoundException {

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 2;
		options.inDensity = 240;
		int imageWidth = 0;
		int imageHeight = 0;

		Bitmap image = BitmapFactory.decodeByteArray(imageData, 0,
				imageData.length, options);

		imageWidth = image.getWidth();
		imageHeight = image.getHeight();

		double imageAspect = (double) imageWidth / imageHeight;
		double desiredAspect = (double) maxWidth / maxHeight;
		double scaleFactor;

		if (imageAspect < desiredAspect) {
			scaleFactor = (double) maxHeight / imageHeight;
		} else {
			scaleFactor = (double) maxWidth / imageWidth;
		}

		float scaleWidth = ((float) scaleFactor) * imageWidth;
		float scaleHeight = ((float) scaleFactor) * imageHeight;

		Bitmap scaledBitmap = Bitmap.createScaledBitmap(image,
				(int) scaleWidth, (int) scaleHeight, true);
		image = scaledBitmap;

		if (rotationDegrees != 0) {
			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			Bitmap rotatedBMP = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap .getWidth(), scaledBitmap .getHeight(), matrix, true);

			image = rotatedBMP;
		}
		return image;
	}



	public void processAccelerometerEvent(SensorEvent event) {                
		mConvolution = (float) (mCC.process(event.values[2]));
		mStepDetector.onSensorChanged(event);
		displayStepDetectorState(mStepDetector);
	}


	void displayStepDetectorState(MovingAverageStepDetector detector) {
		MovingAverageStepDetectorState s = detector.getState();
		boolean stepDetected = s.states[0];
		boolean signalPowerOutOfRange = s.states[1];

		// The offset is now hard coded
		float offsetDeg = 0.0f;

		if (stepDetected) {

			if (!signalPowerOutOfRange && detector.stepLength >= 0) {

				if (mParticleCloud != null) {
					float azimuth = orientVals[0]*rad2deg+12.387f-offsetDeg;

					double currHeading = (double)azimuth;// - 45;

					long currTime = System.currentTimeMillis();
					if (wifiService != null) {
						wifiService.addStep(new Step(currHeading, detector.stepLength, currTime));
					}
					this.mParticleCloud.onStep(currHeading, detector.stepLength);

					String partCenter = mParticleCloud.getCenter();
					String coords[] = partCenter.split("\\s+");
					float new_x = Float.parseFloat(coords[0]);
					float new_y = Float.parseFloat(coords[1]);
					imgStepHistory.add(new Point2D(new_x,new_y));
					mMapView.updatePos(new_x, new_y);

					long tstamp = System.currentTimeMillis();
					writeToFile("path.txt", "s" + " " + new_x + " " + new_y + " " + tstamp + " " + 0 + " " + 0 + " " + "\n");
					cloudCenter[0] = (double)new_x;
					cloudCenter[1] = (double)new_y;
					if (wifiService != null)
						wifiService.setCloudPosition(new Point2D(cloudCenter[0], cloudCenter[1]));
				}

			}
		}
	}


	// The following method is required by the SensorEventListener interface;
	public void onAccuracyChanged(Sensor sensor, int accuracy) {    
	}

	// The following method is required by the SensorEventListener interface;
	// Hook this event to process updates;
	public void onSensorChanged(SensorEvent event) {
		int type = event.sensor.getType();

		switch (type) {  
		case Sensor.TYPE_ACCELEROMETER:
			synchronized(this) {
				gravity = event.values.clone();
				cameraPose = gravity;
				processAccelerometerEvent(event);
				freqCounter.push(event.timestamp);
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			geomag = event.values.clone();
			break;
		}

		// If gravity and geomag have values then find rotation matrix
		if (gravity != null && geomag != null){

			// checks that the rotation matrix is found
			boolean success = SensorManager.getRotationMatrix(inR, I, gravity, geomag);
			if (success){
				SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X, SensorManager.AXIS_Z, inR);
				SensorManager.getOrientation(inR, orientVals);
			}
		} 
	}





	private BroadcastReceiver uiUpdated_img= new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("Picture", "Received image response!");

			JSONObject loc_json = null;//= new JSONObject();
			try {
				loc_json = new JSONObject(intent.getExtras().getString("json_response"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//if (!intent.getExtras().getString("json_response").isEmpty())
			if (loc_json != null) {
				try {
					if (loc_json.has("error")) {
						Toast.makeText(context, loc_json.getString("error"), Toast.LENGTH_SHORT).show();
						return;
					}

					float new_x = Float.valueOf(loc_json.getString("local_x"));
					float new_y = Float.valueOf(loc_json.getString("local_y"));
					float retr_confidence = Float.valueOf(loc_json.getString("retrieval_confidence"));
					Log.d("Picture", new_x + " " + new_y + " " + retr_confidence);

					float img_x = new_x;
					float img_y = new_y;

					String imgResult = "IMG Coordinates: " + img_x + " " + img_y
							+ "\n Confidence: " +loc_json.getString("overall_confidence");
					Toast.makeText(context, imgResult, Toast.LENGTH_SHORT).show();

					if (mParticleCloud != null) {

						if (imgStepHistory.size() > 0) {
							//Point2D lastCoord = imgStepHistory.get(imgStepHistory.size()-1);
							Point2D firstCoord = imgStepHistory.get(0);			

							Point2D imgPosition = mParticleCloud.getShiftedCoord(new_x, new_y, 
									firstCoord.getX(), firstCoord.getY());
							img_x = (float)imgPosition.getX();
							img_y = (float)imgPosition.getY();

							imgStepHistory.clear();
						}

						mParticleCloud.onRssImageUpdate(1.0-Double.parseDouble(loc_json.getString("retrieval_confidence")), img_x, img_y, (double)retr_confidence, "i");
						String partCenter = mParticleCloud.getCenter();
						String coords[] = partCenter.split("\\s+");
						new_x = Float.parseFloat(coords[0]);
						new_y = Float.parseFloat(coords[1]);
						mMapView.updatePos(new_x, new_y);
					}
					long tstamp = System.currentTimeMillis();
					writeToFile("path.txt", "i " + img_x + " " + img_y + " " + tstamp + " "  + 0 + " " + 0 + "\n");

				} catch (JSONException e) {
					Toast.makeText(context, "No coordinates received", Toast.LENGTH_SHORT).show();
				}
			}
			else {
				Toast.makeText(context, "onReceive Image: Did not receive JSON", Toast.LENGTH_SHORT).show();
			}
		}
	};

	private BroadcastReceiver uiUpdated= new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			Log.d("WiFi", "Received WiFi response!");
			JSONObject loc_json = new JSONObject();
			try {
				loc_json = new JSONObject(intent.getExtras().getString("json_response"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (!intent.getExtras().getString("json_response").isEmpty() )
				try {
					long t_received = intent.getExtras().getLong("t_received");
					long t_init = intent.getExtras().getLong("t_init");
					long t_sent = intent.getExtras().getLong("t_sent");

					double confidence = Double.parseDouble(loc_json.getString("confidence"));

					String loc[] = loc_json.getString("location").split("\\s");
					float new_x = Float.valueOf(loc[0]);
					float new_y = Float.valueOf(loc[1]);
					Log.d(TAG, new_x + " " + new_y);

					float old_x = new_x;
					float old_y = new_y;

					if (mParticleCloud != null) {
						String partCenter = mParticleCloud.getCenter();
						String[] coords;
						ArrayList<Step> stepHistory = new ArrayList<Step>();
						if (wifiService != null)
							stepHistory = wifiService.getStepHistory(t_init, t_received);
						if (stepHistory.size() > 0) {
							for (Step s : stepHistory) {
								new_x += s.distance*Math.sin(Math.toRadians(s.hdg));
								new_y += s.distance*Math.cos(Math.toRadians(s.hdg));
							}
							new_x = (old_x+new_x)/2;
							new_y = (old_y+new_y)/2;
							Point2D newCoord = mParticleCloud.getValidPoint(new Point2D(new_x,new_y), new Point2D(old_x,old_y));
							new_x = (float)newCoord.getX();
							new_y = (float)newCoord.getY();
						}

						writeToFile("path.txt", "w " + new_x + " "  + new_y + " " + t_init + " " + t_sent + " " + t_received +"\n");
						mParticleCloud.onRssImageUpdate(5.0, new_x, new_y, confidence,"w");
						partCenter = mParticleCloud.getCenter();
						coords = partCenter.split("\\s+");
						new_x = Float.parseFloat(coords[0]);
						new_y = Float.parseFloat(coords[1]);
						mMapView.updatePos(new_x, new_y);

					} else {				
						long tstamp = System.currentTimeMillis();
						writeToFile("path.txt", "w " + new_x + " "  + new_y + " " +  tstamp + " " + 0 + " " + 0 + "\n");
						String wifiResult = "Coordinates: " + new_x + " " + new_y
								+ "\nConfidence: " +loc_json.getString("confidence");
						Toast.makeText(context, wifiResult, Toast.LENGTH_SHORT).show();
					}		
				} catch (JSONException e) {
					e.printStackTrace();
				}
		}
	};


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		textview = (TextView)findViewById(R.id.textView2);

		HttpParams params = new BasicHttpParams();
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 8010));
		cm = new ThreadSafeClientConnManager(params, registry);
		httpclient = new DefaultHttpClient(cm, params);

		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		mStepDetector = new MovingAverageStepDetector(movingAverage1, movingAverage2, lowPowerCutoff, highPowerCutoff);

		mCC = new ContinuousConvolution(new SinXPiWindow(mMASize));
		freqCounter = new FrequencyCounter(20);
		cloudCenter = new double[2];        
		File directory = new File(Environment.getExternalStorageDirectory()+File.separator+"wifiloc");
		if (directory.exists())
			deleteRecursive(directory);
		directory.mkdirs();
	}

	void deleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				deleteRecursive(child);

		fileOrDirectory.delete();
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onResume() {
		super.onResume();
		restartCamera();
		restartMapView();

		//registerReceiver(uiUpdated, new IntentFilter("LOCATION_UPDATED"));
		//registerReceiver(uiUpdated_img, new IntentFilter("IMG_LOCATION_UPDATED"));
		registerReceiver(uiUpdated, new IntentFilter("LOCATION_UPDATED"), null, new Handler());
		registerReceiver(uiUpdated_img, new IntentFilter("IMG_LOCATION_UPDATED"), null, new Handler());

		mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);

		this.mCory2Builder = new AreaBuilder();        
		AssetManager am = getAssets();
		this.mCory2Builder.readSimpleTextWalls(am, "cory2.edge");
		mCory2 = mCory2Builder.create();
		this.mParticleCloud = new ParticlePosition(0,0, mCory2); 
		mParticleCloud.readCoords(am, "wifi_coords.dat");
		imgStepHistory = new ArrayList<Point2D>();

		startScan();
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder binder) {
			WifiScanService.MyBinder b = (WifiScanService.MyBinder) binder;
			wifiService = b.getService();
			Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
		}

		public void onServiceDisconnected(ComponentName className) {
			wifiService = null;
		}
	};

	@Override
	public void onDestroy(){
		super.onDestroy();

		Intent intent = new Intent(this, WifiScanService.class);
		stopService(intent);
		unbindService(mConnection);
	}

	@Override
	protected void onPause() {
		super.onPause();

		postPathFileToServer("path.txt");

		stopCamera();
		stopMapView();
		unregisterReceiver(uiUpdated_img);
		unregisterReceiver(uiUpdated);

		//String partCenter = mParticleCloud.getCenter();
		//String coords[] = partCenter.split("\\s+");
		//float new_x = Float.parseFloat(coords[0]);
		//float new_y = Float.parseFloat(coords[1]);
		//long tstamp = System.currentTimeMillis();

		Intent intent = new Intent(this, WifiScanService.class);
		stopService(intent);
		PendingIntent pintent = PendingIntent.getService(this, 0, intent, 0);
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		alarm.cancel(pintent);

		mSensorManager.unregisterListener(this, accelerometer);
		mSensorManager.unregisterListener(this, magnetometer);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
			Log.d(TAG, "onKeyDown: DPAD_CENTER");
			mCamera.takePicture(null, null, mPictureCallback);
			return false;
		default:
			return super.onKeyDown(keyCode, event);
		}
	}

	private void restartCamera() {
		try {
			stopCamera();
			mCamera = Camera.open();
			Camera.Parameters params = mCamera.getParameters();
			params.setPreviewFpsRange(30000, 30000);
			params.setPictureSize(640,480);
			params.setPreviewSize(128, 96);
			mCamera.setParameters(params);
			mPreview = new CameraPreview(this, mCamera);
			mFrameLayout = (FrameLayout) findViewById(R.id.frameLayout);
			mFrameLayout.addView(mPreview);
			mPreview.setKeepScreenOn(true);
			mCamera.startPreview();
		} catch (Exception e) {
			Log.e(TAG, "failed to open Camera");
			e.printStackTrace();
		}
	}

	private void stopCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mFrameLayout.removeView(mPreview);
			mCamera.release();
			mCamera = null;
		}
	}

	private void restartMapView() {
		mMapView = new MapView(this.getApplicationContext());
		mFrameLayout2 = (FrameLayout) findViewById(R.id.frameLayout2);
		mFrameLayout2.addView(mMapView);
		mMapView.setKeepScreenOn(true);
	}

	private void stopMapView() {
		mFrameLayout2.removeView(mMapView);
	}

	public void startScan() {
		intent = new Intent(this, WifiScanService.class);

		Bundle b = new Bundle();
		b.putInt("floor_id", 1);

		intent.replaceExtras(b);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		PendingIntent pintent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		Calendar cal = Calendar.getInstance();
		AlarmManager alarm = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
		// Start every 5 seconds
		alarm.setRepeating(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 5*1000, pintent); 	
	}

	private void writeToFile(String fname, String data)  {

		Log.d("Save", Environment.getExternalStorageState());

		File file = new File(root, fname);

		FileWriter filewriter;
		try {
			filewriter = new FileWriter(file,true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			Log.d("IMGRES","Could not create file " + e1.getMessage());
			return;
		}

		BufferedWriter out = new BufferedWriter(filewriter);
		try {
			out.write(data);
			out.close();
		} catch (IOException e) {
			Log.d("IMGRES","Could not write to file " + e.getMessage());
		}
	}

	private class PostToServerTask extends AsyncTask<Context, Void, Void> {
		private String url_str;
		private JSONObject path;

		public PostToServerTask(String url, File file) {
			this.url_str = url;
			JSONObject pathJSON = new JSONObject();
			FileInputStream fis = null;
			 
			String fileContents = null;
			try {
				fis = new FileInputStream(file); 
				StringWriter writer = new StringWriter();
				IOUtils.copy(fis, writer, "UTF-8");
				fileContents  = writer.toString();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (fis != null)
						fis.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}	
			
			try {
				pathJSON.put("path", fileContents);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			this.path = pathJSON;
		}

		protected Void doInBackground(Context... c) {	
			byte[] data = path.toString().getBytes();
			try {
				URL url = new URL(url_str);
				HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

				try {
					urlConnection.setReadTimeout(30000);
					urlConnection.setConnectTimeout(30000);

					urlConnection.setDoInput(true);
					urlConnection.setDoOutput(true);
					urlConnection.setFixedLengthStreamingMode(data.length);

					urlConnection.setRequestProperty("content-type","application/json; charset=utf-8");
					urlConnection.setRequestProperty("Accept", "application/json");
					urlConnection.setRequestMethod("POST");

					urlConnection.connect();
					OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

					out.write(data);
					Log.d("DATA", path.toString());
					out.flush();
					out.close();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finally {
					urlConnection.disconnect();
				}			
			}
			catch (MalformedURLException e){ }
			catch (IOException e) {Log.d("URL_EXCEPTION","FAILURE! "+ e.getMessage()); }		
			return null;
		}
	}


	private void postPathFileToServer(String fname) {
		File file = new File(root, fname);

		new PostToServerTask(PATH_FILE_UPLOAD_URL, file).execute(getApplicationContext());
	}
}
