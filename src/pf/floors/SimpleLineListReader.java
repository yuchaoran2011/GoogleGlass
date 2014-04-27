package pf.floors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;

import pf.utils.Line2D;
import android.content.res.AssetManager;

public class SimpleLineListReader {

	ArrayList<Line2D> mLines;
	
	public ArrayList<Line2D> readWalls(AssetManager assetManager, String filename) {

        InputStream inputStream = null;
        try {
        	inputStream = assetManager.open(filename);   
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		
		mLines = new ArrayList<Line2D>();

		try {	
			Pattern pattern = Pattern.compile("[ ]");
			String line = null;	
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			while ((line = reader.readLine()) != null) {
				String[] coords = pattern.split(line);
				if (coords.length == 4) {
					double x1 = Double.valueOf(coords[0]);
					double y1 = Double.valueOf(coords[1]);
					double x2 = Double.valueOf(coords[2]);
					double y2 = Double.valueOf(coords[3]);
					mLines.add(new Line2D(x1, y1, x2, y2));
				}
			}
			
			inputStream.close();

		} catch (IOException ex) {
			ex.printStackTrace();
			
			try {
				if (inputStream != null)
					inputStream.close();
			} catch(IOException ex2) {
				ex2.printStackTrace();
			}
		}
		//Log.d(TAG, "SimpleLineListReader found " + mLines.size() + " lines");
		return mLines;
	}
}
