package pf.floors;

import java.util.ArrayList;
import java.util.Collection;

import pf.utils.Line2D;
import android.content.res.AssetManager;

/**
 * AreaBuilder class. Responsible for incremental construction area parts and
 * creation of a new Area instance.
 */
public class AreaBuilder {

	private Area area;
	private Collection<Line2D> mWalls;
	private Collection<Line2D> mStairs;

	/**
	 * AreaBuilder constructor
	 * 
	 */
	public AreaBuilder() {
		area = null;
		mWalls = new ArrayList<Line2D>();
	}

	/**
	 * Creates a new instance of the AreaBuilder
	 * 
	 * @return new instance of the AreaBuilder
	 */
	public static AreaBuilder builder() {

		return new AreaBuilder();
	}

	/**
	 * Creates an instance of the constructed area.
	 * 
	 * @return an instance of the created area.
	 */
	public Area create() {
		area = new Area();
		if (mWalls != null)
			area.setWalls(mWalls);
		// area.optimize();
		Area retval = area;
		area = null;
		return retval;
	}


	/**
	 * Translates and scales imported wall lines according to given
	 * parameters.
	 * 
	 * @param originX
	 *            x coordinate of the new origin.
	 * @param originY
	 *            y coordinate of the new origin.
	 * @param scaleX
	 *            x axis scale factor.
	 * @param scaleY
	 *            y axis scale factor.
	 * @return this AreaBuilder instance.
	 */
	public AreaBuilder scale(double originX, double originY, double scaleX,
			double scaleY) {
		scaleLines(mStairs, originX, originY, scaleX, scaleY);
		scaleLines(mWalls, originX, originY, scaleX, scaleY);
		return this;
	}

	/**
	 * Generic method to scale a collection of lines according to given
	 * parameters. Lines are scaled in place. The input set is used for output.
	 * 
	 * @param lines
	 *            the set of lines to be scaled.
	 * @param originX
	 * @param originY
	 * @param scaleX
	 * @param scaleY
	 */
	private void scaleLines(Collection<Line2D> lines, double originX,
			double originY, double scaleX, double scaleY) {
		ArrayList<Line2D> otherLines = new ArrayList<Line2D>();
		for (Line2D wall : lines) {
			double x1 = scaleX * ((double) wall.getX1() + originX);
			double y1 = scaleY * ((double) wall.getY1() + originY);
			double x2 = scaleX * ((double) wall.getX2() + originX);
			double y2 = scaleY * ((double) wall.getY2() + originY);
			otherLines.add(new Line2D(x1, y1, x2, y2));
		}
		lines.clear();
		lines.addAll(otherLines);
	}


	/**
	 * Imports wall lines from the file in simple text format. Lines coordinates
	 * are white-space separated, lines are by separated newlines. I.e. 4
	 * coordinates per line.
	 * 
	 * @param fileName
	 *            the name of the file to be read.
	 * @return this AreaBuilder instance.
	 */
	public AreaBuilder readSimpleTextWalls(AssetManager am, String fileName) {
		SimpleLineListReader reader = new SimpleLineListReader();
		mWalls = reader.readWalls(am, fileName);
		return this;
	}
}
