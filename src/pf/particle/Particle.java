package pf.particle;

import java.util.Arrays;

import pf.distr.NormalDistribution;


public class Particle implements Cloneable {

	private double x, y;
	private double weight; 

	
	public Particle(double posX, double posY, double weight) {
		x = posX;
		y = posY;
		this.weight = weight;
	}



	// polar version
	public static Particle polarNormalDistr(double meanX, double meanY,
			double sigma, double weight) {

		double angle = 2 * Math.PI * Math.random();
		double distance = sigma * NormalDistribution.inverse(Math.random());
		
		double x = meanX + (distance * Math.cos(angle));
		double y = meanY + (distance * Math.sin(angle));

		return new Particle(x, y, weight);
	}



	public static Particle evenSpread(double meanX, double meanY, double sizeX, double sizeY, double weight) {	
		double x = meanX + ((Math.random() - 0.5) *  sizeX);
		double y = meanY + ((Math.random() - 0.5) * sizeY);	
		return new Particle(x, y, weight);
	}


	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public String toString() {
		return "particle: x: " + x + " y: " + y + " weight: " + weight;
	}

	// Used to construct a nearly dead particle (a particle that just crossed a wall).
	public Particle copy(double weight) {
		return new Particle(x, y, weight);
	}
}
