package pf.utils;

import java.io.Serializable;
import java.util.ArrayList;

public class Point2D implements Serializable {
	
	double x, y, extraData;
	// private int precision = 1000000;
	
	public Point2D (Point2D other) {
		this.x = other.x;
		this.y = other.y;
	}
	
	public Point2D minus (Point2D other) {
		this.x -= other.x;
		this.y -= other.y;
		return this;
	}
	
	public double dot (Point2D other) {		
		return this.x * other.x + this.y * other.y;
	}
	
	public Point2D (double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public Point2D (double x, double y, double extraData) {
		this.x = x;
		this.y = y;
		this.extraData = extraData;
	}
	
	public void set(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	public double getX() {
		return this.x;
	}
	
	public double getY() {
		return this.y;
	}
	
	public double getExtraData() {
		return this.extraData;
		}
	
	public boolean contains(Point2D other) {
		return contains(other.x, other.y);
	}
	
	public boolean contains(double x, double y) {
		if (this.x != 0) {
			double alpha = x / this.x;
			//System.out.println("alpha = " + alpha);
			return alpha >= 0 && alpha <= 1 && this.y * alpha == y;
		} else if (this.y != 0) {
			double alpha = y / this.y;
			//System.out.println("alpha = " + alpha);
			return alpha >= 0 && alpha <= 1 && this.x * alpha == x;
		}
		return x == 0 && y == 0;
	}
	
	@Override
    public boolean equals(Object obj) {
    	if (obj instanceof Point2D) {
    		Point2D that = (Point2D)obj;
    		return this.x == that.x && this.y == that.y;
    		/* return (int)(precision * this.x) == (int)(precision * that.x)
    				&& (int)(precision * this.y) == (int)(precision * that.y); */
    	}
    	return false;
    }
	
	@Override
	public int hashCode() {
		return (int)x + 13063 * (int)y;
	}
	
	@Override
	public String toString() {
		return x + " " + y;
	}
	
	public double distanceTo(Point2D p2) {
		return Math.sqrt(Math.pow(this.getX()-p2.getX(),2) + Math.pow(this.getY()-p2.getY(),2)); 

	}

	public int findClosestPoint(ArrayList<Point2D> pts) {
		int i;
		double minDist = 999999;
		int closestIdx = 0;
		for (i = 0; i < pts.size(); i++) {
			if (this.distanceTo(pts.get(i)) < minDist) {
				closestIdx = i;
				minDist = this.distanceTo(pts.get(i));
			}
		}
		return closestIdx;
		
	
	}
}
