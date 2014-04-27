package edu.berkeley.camerademo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class MapView extends View {
    private ShapeDrawable mDrawable;
    private float pos_x;
    private float pos_y;
    private boolean gotPosition;
    Paint mPaint = new Paint();
    private ArrayList<String> maplines= new ArrayList<String>();

    public MapView(Context context, AttributeSet attrs) {
	    super(context,attrs);
	
	    int x = 10;
	    int y = 10;
	    int width = 600;
	    int height = 100;
	
	    mDrawable = new ShapeDrawable(new OvalShape());
	    mDrawable.getPaint().setColor(0xff74AC23);
	    mDrawable.setBounds(x, y, x + width, y + height);
	    
	    AssetManager am = context.getAssets();
	    
	    InputStream inputStream = null;
        try {
        	inputStream = am.open("cory2.edge");   
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String sCurrentLine;
        try {
			while ((sCurrentLine = reader.readLine()) != null) {
				maplines.add(sCurrentLine);
			}
		} catch (IOException e) {
			Log.d("MAPVIEW","Could not read map file " + e.getMessage());
			return;
		}
	}
  
    public void updatePos(float new_x, float new_y) {
    	this.gotPosition = true;
    	this.pos_x = new_x;
    	this.pos_y = new_y;
    	invalidate(); 	
    }
    
    protected void onDraw(Canvas canvas) {
    	
    	int height = this.getBottom()-this.getTop();
    	canvas.scale(15.0f, 15.0f);
    	//height /=3;
    	canvas.translate(0,50);   // reset where 0,0 is located
    	Log.d("MAPVIEW","Height is: " + height);
    	canvas.scale(1,-1);    // invert

	    mPaint.setARGB(150, 000, 000, 000);
	    mPaint.setAntiAlias(true);
	    //mDrawable.draw(canvas);
	  
	    for (String mapline : maplines) {
	    	String[] coords = mapline.split("\\s+");
	    	canvas.drawLine(Float.valueOf(coords[0])*0.3f+10.0f,
	    					Float.valueOf(coords[1])*0.3f+38.0f,
	    					Float.valueOf(coords[2])*0.3f+10.0f, 
	    					Float.valueOf(coords[3])*0.3f+38.0f, 
	    					mPaint);
	    }
	    this.gotPosition = true;
	    mPaint.setARGB(255, 000, 000, 255);
	    if (this.gotPosition)
	    	canvas.drawCircle(this.pos_x*0.3f+10.0f, this.pos_y*0.3f+38.0f, 0.5f, mPaint);    
	}
}
