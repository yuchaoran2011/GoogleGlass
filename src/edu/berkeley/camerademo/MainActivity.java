package edu.berkeley.camerademo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private static final String TAG = "ImgLoc";
	static final String IMAGE_URL = "http://sofia.eecs.berkeley.edu:8010";
	private HttpClient httpclient;
	ClientConnectionManager cm;
	private Camera mCamera;
	private FrameLayout mFrameLayout;
	private CameraPreview mPreview;
	private TextView textview;
	private int numPictureTaken = 0;

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
			poseMap.put("yaw", null);
			poseMap.put("pitch", null);
			poseMap.put("roll", null);
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

		//if (imageWidth > maxWidth || imageHeight > maxHeight) {

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
		//}

		if (rotationDegrees != 0) {
			Matrix matrix = new Matrix();
			matrix.postRotate(90);
			Bitmap rotatedBMP = Bitmap.createBitmap(scaledBitmap , 0, 0, scaledBitmap .getWidth(), scaledBitmap .getHeight(), matrix, true);

			image = rotatedBMP;
		}

		return image;
	}

	private BroadcastReceiver uiUpdated_img= new BroadcastReceiver() {
		/*public byte[] readImgFile(String path) {
			File file = new File(path);
			int size = (int) file.length();
			byte[] bytes = new byte[size];
			try {
				BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
				buf.read(bytes, 0, bytes.length);
				buf.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return bytes;
		}*/

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d("Picture", "Received image response!");

			JSONObject loc_json = new JSONObject();
			try {
				loc_json = new JSONObject(intent.getExtras().getString("json_response"));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
			if (!intent.getExtras().getString("json_response").isEmpty() )
				try {
					//float poseConfidence = Float.parseFloat(loc_json.getString("pose_confidence"));

					float new_x = Float.valueOf(loc_json.getString("local_x"));
					float new_y = Float.valueOf(loc_json.getString("local_y"));
					float retr_confidence = Float.valueOf(loc_json.getString("retrieval_confidence"));
					Log.d("Picture", new_x + " " + new_y + " " + retr_confidence);

					float img_x = new_x;
					float img_y = new_y;

					String imgResult = "IMG Coordinates: " + img_x + " " + img_y
							+ "\n Confidence: " +loc_json.getString("overall_confidence");
					Toast.makeText(context, imgResult, Toast.LENGTH_SHORT).show();

				} catch (JSONException e) {
					Toast.makeText(context, "No coordinates received", Toast.LENGTH_SHORT).show();
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
		registerReceiver(uiUpdated_img, new IntentFilter("IMG_LOCATION_UPDATED"));
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopCamera();
		unregisterReceiver(uiUpdated_img);
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

}
