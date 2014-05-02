package pf.utils;

import java.io.Serializable;


public class Line2D implements Serializable {

	private Point2D start, end;
	
	// precompute intersection expressions
	private double detXY12;
	private double X1minusX2;
	private double Y1minusY2;
	private double minX, minY, maxX, maxY;
	final double rad2deg = 180/Math.PI;
	public Line2D() {
		setCoords(0.0, 0.0, 0.0, 0.0);
	}
	
	public Line2D(double x1, double y1, double x2, double y2) {
		setCoords(x1, y1, x2, y2);
	}
	
	public String toString() {
		return "line(" + start.x + ", " + start.y + ", " + end.x + ", " + end.y + ")";
	}
	
	public void setCoords(double x1, double y1, double x2, double y2) {
		start = new Point2D(x1, y1);
		end = new Point2D(x2, y2);
		
		detXY12 = (x1 * y2 - y1 * x2);
		X1minusX2 = (x1 - x2);
		Y1minusY2 = (y1 - y2);
		minX = Math.min(x1, x2);
		minY = Math.min(y1, y2);
		maxX = Math.max(x1, x2);
		maxY = Math.max(y1, y2);
	}
	
	public double getX1() {
		return start.x;
	}
	public double getX2() {
		return end.x;
	}
	public double getY1() {
		return start.y;
	}
	public double getY2() {
		return end.y;
	}
	

	
	/**
	 * Intersection of two lines.
	 * Formula taken from http://en.wikipedia.org/wiki/Line-line_intersection
	 * 
	 * @param that other line
	 * @return true iff this and that lines intersect.
	 */
	public boolean intersect(Line2D that) {
		return intersect(that, null);
	}
	
	public boolean intersect(Line2D that, Point2D intersection) {
		
		double delimiter = X1minusX2 * that.Y1minusY2 - Y1minusY2 * that.X1minusX2;
		double pX = detXY12 * that.X1minusX2 - X1minusX2 * that.detXY12;
		pX /= delimiter;
		double pY = detXY12 * that.Y1minusY2 - Y1minusY2 * that.detXY12;
		pY /= delimiter;
		
		//System.out.println("intersection at = " + pX + ", " + pY);
		
		if (intersection != null) {
			intersection.x = pX;
			intersection.y = pY;
		}
		if (minX == pX && minY == pY && maxY < that.minY) {
			System.out.println("RAY INTERECT");
			return true;
			}
		if (maxX == pX && maxY == pY && minY < that.minY) {
			System.out.println("RAY INTERECT");
			return true;
			}
		
		return (minX == maxX || minX <= pX && pX <= maxX)
				&& (minY == maxY || minY <= pY && pY <= maxY)
				&& (that.minX == that.maxX || that.minX <= pX && pX <= that.maxX)
				&& (that.minY == that.maxY || that.minY <= pY && pY <= that.maxY);
	}
	

	// Used to compute angle between this line and line only when this line and line intersect.
	// Formula taken from http://www.tpub.com/math2/5.htm
	public double angle(Line2D line) {
		double thisSlope = (end.getY() - start.getY()) / (end.getX() - start.getX());
		double slope = (line.getY2() - line.getY1()) / (line.getX2() - line.getX1());
		return Math.atan((thisSlope - slope) / (1 + thisSlope * slope));
	}
	
	public double heading() {
		return rad2deg*this.angle(new Line2D(0,0,0,100));
	}
	
	public double length() {
		return Math.sqrt(Math.pow(X1minusX2, 2)+ Math.pow(Y1minusY2, 2));
	}
	
	public double slope() {
		return (this.getY2() - this.getY1()) / (this.getX2() - this.getX1());
	}


	public static double angle2(double angleOfFirstLine, Line2D line) {
		double thisSlope = Math.tan(angleOfFirstLine);
		double slope = (line.getY2() - line.getY1()) / (line.getX2() - line.getX1());
		return Math.atan((thisSlope - slope) / (1 + thisSlope * slope));
	}
}
