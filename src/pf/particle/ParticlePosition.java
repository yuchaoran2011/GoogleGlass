package pf.particle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import pf.floors.Area;
import pf.floors.AreaLayerModel;
import pf.floors.EmptyArea;
import pf.utils.Line2D;
import pf.utils.Point2D;
import android.content.res.AssetManager;
import android.os.Environment;
import android.util.Log;


public class ParticlePosition {
	private Set<Particle> particles;
	private Area mArea;
	private double[] mCloudAverageState;

	private static final int DEFAULT_PARTICLE_COUNT = 100;
	private static final double DEFAULT_WEIGHT = 1.0;

	private double mPositionSigma = 1.0f;
	private int mNumberOfParticles;

	private ArrayList<Line2D> wallCache;

	private ArrayList<Point2D> wifiHist; 
	private ArrayList<Point2D> wifiDbCoords;
	private long tLastCollision;
	private final double mElimThr = 0.04;
	
	public enum ParticleGenerationMode { GAUSSIAN, UNIFORM }
	private ParticleGenerationMode mParticleGeneration = ParticleGenerationMode.GAUSSIAN;


	public ParticlePosition(double posX, double posY) {
		this(posX, posY, 1.0);
	}
	
	public ParticlePosition(double posX, double posY, Area area) {
		this(posX, posY, 1.0);
		this.setArea(area);
		removeInvalidParticles(posX, posY);
	}

	public ParticlePosition(double x, double y, double sigma) {
		int numberOfParticles = DEFAULT_PARTICLE_COUNT;
		particles = new HashSet<Particle>(numberOfParticles);
		while (numberOfParticles > 0) {
			particles.add(Particle.polarNormalDistr(x, y, 0.5, DEFAULT_WEIGHT));
			numberOfParticles--;
		}
		mArea = new EmptyArea();
		mCloudAverageState = new double[2];
		mCloudAverageState[0] = x;
		mCloudAverageState[1] = y;
		wallCache = new ArrayList<Line2D>();
		wifiHist = new ArrayList<Point2D>();
		wifiDbCoords = new ArrayList<Point2D>();
		tLastCollision = 0;
		
	}


	public void setArea(Area area) { mArea = new EmptyArea(); mArea = area; }
	public Area getArea() { return mArea; }


	public void setPositionEvenlySpread(double posX, double posY, double spreadX, double spreadY, double weight) {
		int number = mNumberOfParticles;
		particles = new HashSet<Particle>(mNumberOfParticles);
		while (number > 0) {
			particles.add(Particle.evenSpread(posX, posY, spreadX, spreadY, weight));
			number--;
		}
		computeCloudAverageState();
	}


	public AreaLayerModel getWallsModel() {
		return mArea.getWallsModel();
	}


	public void onStep(double hdg, double length) {		
		if (length > 0.0) {
			System.out.println("onStep(hdg: " + hdg + ", length: " + length);
			/*for (Point2D pt : wifiHist) {
				System.out.println("HIST: " + pt.toString());
			}*/
			HashSet<Particle> living = new HashSet<Particle>(particles.size());
			for (Particle particle : particles) {
				Particle newParticle = updateParticle(particle, hdg, length);
				if (newParticle.getWeight() > 0.0) {
					living.add(newParticle);
				}
			}
			if (living.size() > mElimThr * particles.size()) {
				computeCloudAverageState();
				removeInvalidParticles(mCloudAverageState[0], mCloudAverageState[1]);
				particles.clear();
				particles.addAll(living);
				System.out.println("No. particles = " + particles.size());
				if (particles.size() < 0.5*DEFAULT_PARTICLE_COUNT) {
					System.out.println("Too few particles! Resampling...");
					resample();
					System.out.println("After resampling: No. particles = " + particles.size());
				}
			}
			else {
				System.out.println("All particles collided with walls and none was left!");
				System.out.println("Generating new particles at most recent valid position! ");
				long currTime = System.currentTimeMillis();
				double sigmaFactor = 1.0;
				boolean regenAtWifi = false;
				if (currTime-tLastCollision <= 1000) {
					sigmaFactor = 6.0;
					System.out.println("CONSECUTIVE COLLISIONS!!!");
				}
				tLastCollision = currTime;
				int numberOfParticles = DEFAULT_PARTICLE_COUNT;
				particles = new HashSet<Particle>(numberOfParticles);
				//Point2D lastWifi = wifiHist.get(wifiHist.size()-1);
				Point2D cloudCenter = new Point2D(mCloudAverageState[0], mCloudAverageState[1]);
				System.out.println("Generating at: " + cloudCenter.toString());
				int closestToCloud = cloudCenter.findClosestPoint(wifiDbCoords);
				//int closestToWifi = lastWifi.findClosestPoint(wifiDbCoords);
				
				//double lastWifi_x = lastWifi.getX();
				//double lastWifi_y = lastWifi.getY();
				//double lastWifi_confidence = wifiHist.get(wifiHist.size()-1).getExtraData();
				while (numberOfParticles > 0) {
					//if (regenAtWifi)
					//	particles.add(Particle.polarNormalDistr(lastWifi_x, lastWifi_y, 2.0, DEFAULT_WEIGHT));
					//else
					particles.add(Particle.polarNormalDistr(mCloudAverageState[0], mCloudAverageState[1], 5.0, DEFAULT_WEIGHT));
					numberOfParticles--;	
				}	
				mNumberOfParticles = DEFAULT_PARTICLE_COUNT;
				computeCloudAverageState();
				removeInvalidParticles(mCloudAverageState[0], mCloudAverageState[1]);
			}	
		}
	}


	private void removeInvalidParticles(double x, double y) {
		ArrayList<Particle> bad = new ArrayList<Particle>();
		Collection<Line2D> walls = mArea.getWallsModel().getWalls();
		for (Particle p : particles) {
			Line2D l1 = new Line2D(15.0, 15.0, p.getX(), p.getY());
			int nIntersect = 0;
			
			for (Line2D l2 : walls) 
				if (l2.intersect(l1))  {
					nIntersect++;
					}
					
			if (nIntersect%2 == 0) {
				bad.add(p);
				mNumberOfParticles--;
				}	
		}
		
		System.out.println(particles.size() + " start particles remain.");
		particles.removeAll(bad);
		System.out.println(bad.size() + " new particles were removed because they are in invalid regions!");
		System.out.println(particles.size() + " valid particles remain.");
		computeCloudAverageState();
	}



	private Particle updateParticle(Particle particle, double hdg, double length) {
		Random ran = new Random();

		// Gaussian noise: http://www.javamex.com/tutorials/random_numbers/gaussian_distribution_2.shtml
		double deltaX = length * Math.sin(Math.toRadians(hdg)); //+ ran.nextGaussian() * 0.4;
		double deltaY = length * Math.cos(Math.toRadians(hdg)); //+ ran.nextGaussian() * 0.4;
		double oldX=particle.getX(), oldY=particle.getY();
		Line2D trajectory = new Line2D(oldX, oldY, oldX + deltaX, oldY + deltaY);
		//System.out.println("PARTICLE POS " + oldX + " " + oldY + " | " + deltaX + " " + deltaY);
		if (mArea != null) {
			// wall collision
			for (Line2D wall: wallCache) {
				if (trajectory.intersect(wall)) {
					mNumberOfParticles--;	
					return particle.copy(0.0);   // Return a dead particle of weight 0
				}
			} 
			Collection<Line2D> walls = mArea.getWallsModel().getWalls();
			for (Line2D wall: walls) {
				if (wall.intersect(trajectory)) {
					mNumberOfParticles--;
					if (wallCache.size() == 10) {
						wallCache.set((new Random()).nextInt(10), wall);
					}
					else {
						wallCache.add(wall);
					}
					return particle.copy(0);   // Return a dead particle of weight 0
				}
			} 
		}
		return new Particle(oldX+deltaX, oldY+deltaY, particle.getWeight());
	}

	public Point2D getShiftedCoord(double pt_x, double pt_y, double oldCloud_x, double oldCloud_y) {
		computeCloudAverageState();
		double x_coeff, y_coeff;
		
		x_coeff = 1.0;
		y_coeff = 1.0;
		Line2D trajectory = new Line2D(oldCloud_x,oldCloud_y, mCloudAverageState[0], mCloudAverageState[1]);
		if (trajectory.getX1() > trajectory.getX2()) {
			x_coeff = -1.0;
			y_coeff = -1.0;
		}
		
		double trajSlope = trajectory.slope();
		double trajLength = trajectory.length();
		double trajCos = 1/Math.sqrt(1+trajSlope*trajSlope);
		double trajSin = trajSlope/Math.sqrt(1+trajSlope*trajSlope);
		double new_x = pt_x + x_coeff*trajCos*trajLength;
		double new_y = pt_y + y_coeff*trajSin*trajLength;
		Point2D newPt = new Point2D(new_x, new_y);
		int closestPt = newPt.findClosestPoint(wifiDbCoords);
		writeToFile("shifting.txt", pt_x + " " + pt_y + " " + oldCloud_x + " " + oldCloud_y + " " + mCloudAverageState[0] + " " + mCloudAverageState[1] +  " -> " + wifiDbCoords.get(closestPt) + "\n");
		return wifiDbCoords.get(closestPt);
		//Point2D validPoint = getValidPoint(new Point2D(pt_x, pt_y),
		//									new Point2D(new_x, new_y));
		//Log.d("VALID POINT: ",  validPoint.toString() + "; " + pt_x + " " + pt_y + " " + new_x + " " + new_y);
		//return validPoint;
		}

	public void onRssImageUpdate(double sigma, double x, double y, double confidence, String type) {
		//System.out.println("onRssImageUpdate()");
		HashSet<Particle> living = new HashSet<Particle>();
		wifiHist.add(new Point2D(x,y,confidence));
		if (particles.isEmpty()) {
			System.out.println("Particles don't exist! Regenerating particles based on WiFi location");

			int numberOfParticles = DEFAULT_PARTICLE_COUNT;
			particles = new HashSet<Particle>(numberOfParticles);
			while (numberOfParticles > 0) {
				particles.add(Particle.polarNormalDistr(x, y, sigma, DEFAULT_WEIGHT));
				numberOfParticles--;	
				}	
			mNumberOfParticles = DEFAULT_PARTICLE_COUNT;
		}
		else {
			//writeToFile("particles.txt", "WIFI: " + x + " " + y + "\n");
			for (Particle particle : particles) {
				Particle newParticle = particle.copy(particle.getWeight());

				double result = (particle.getX()-x)*(particle.getX()-x)+(particle.getY()-y)*(particle.getY()-y);
				double firstPart = 1.0/(Math.sqrt(2.0*Math.PI) * sigma);
				double secondPart = Math.exp(-result/(2.0 * sigma * sigma));
				double finalResult = firstPart * secondPart;

				newParticle.setWeight(particle.getWeight()*finalResult);
				//writeToFile("particles.txt","pw " + newParticle.getX() + " " + newParticle.getY() + " " + newParticle.getWeight() + "\n");
				if (newParticle.getWeight() >= 0.03) {
					living.add(newParticle);
				}
			}
			//writeToFile("particles.txt", "LIVING PARTICLES " + living.size()+  "\n");
			if (living.size() > mElimThr * particles.size()) {
			
				particles.clear();
				particles.addAll(living);
				System.out.println(particles.size());
				System.out.println("WiFi/Image update finished! Resampling...");
				resample();
				System.out.println("After resampling: No. particles = " + particles.size());
			}
			else {
				System.out.println("Too many particles were eliminated! Regenerating particles at WiFi location!");
				//writeToFile("path.txt","Too many particles were eliminated! Regenerating particles at WiFi location! " + x + " " + y);
				particles.clear();
				int numberOfParticles = DEFAULT_PARTICLE_COUNT;
				particles = new HashSet<Particle>(numberOfParticles);
				
				if (type.equals("i")) {
					Point2D newCoord = getValidPoint(new Point2D(x,y), new Point2D(mCloudAverageState[0],mCloudAverageState[1]));
					x = newCoord.getX();
					y = newCoord.getY();
				}
				
				while (numberOfParticles > 0) {
					particles.add(Particle.polarNormalDistr(x, y, 5.0, DEFAULT_WEIGHT));
					numberOfParticles--;	
				}	
				mNumberOfParticles = DEFAULT_PARTICLE_COUNT;
				
				removeInvalidParticles(x,y);
				
			}
		}	
		computeCloudAverageState();
	}


	public Point2D getValidPoint(Point2D newCoord, Point2D oldCoord) {
		//Collection<Line2D> walls = mArea.getWallsModel().getWalls();
		Line2D trajectory = new Line2D(newCoord.getX(), newCoord.getY(),
										oldCoord.getX(), oldCoord.getY());
		while (!isInterior(newCoord)) {
			newCoord.set((newCoord.getX()+oldCoord.getX())/2, (newCoord.getY()+oldCoord.getY())/2);
			trajectory.setCoords(newCoord.getX(), newCoord.getY(),
					oldCoord.getX(), oldCoord.getY());
			}
		/*outer:
		for (Line2D wall : walls) {
			while (wall.intersect(trajectory)) {
				newCoord.set((newCoord.getX()+oldCoord.getX())/2, (newCoord.getY()+oldCoord.getY())/2);
				trajectory.setCoords(newCoord.getX(), newCoord.getY(),
						oldCoord.getX(), oldCoord.getY());
			}
			continue outer;
		}*/
		return newCoord;
	}

	public boolean isInterior(Point2D pt) {
		int nIntersect = 0;
		Collection<Line2D> walls = mArea.getWallsModel().getWalls();
		Line2D ray = new Line2D(15.0, 15.0, pt.getX(), pt.getY());;
		for (Line2D wall : walls) {
			if (wall.intersect(ray))
				nIntersect++;
		}
		
		return (nIntersect%2 > 0);
	}
	private void computeCloudAverageState() {
		double[] prevCloudAverageState = new double[2];
		for (int i = 0; i < 2; i++) {
			prevCloudAverageState[i] = mCloudAverageState[i];
		}
		Collection<Line2D> walls = mArea.getWallsModel().getWalls();
		mCloudAverageState[0] = mCloudAverageState[1] = 0.0;
		int totalWeight = 0;
		for (Particle particle : particles) {
			mCloudAverageState[0] += particle.getX() * particle.getWeight();
			mCloudAverageState[1] += particle.getY() * particle.getWeight();
			totalWeight += particle.getWeight();
		}
		for (int i = 0; i < 2; i++) {
			mCloudAverageState[i] /= totalWeight;
		}
		Point2D newCoord = getValidPoint(new Point2D(mCloudAverageState[0],mCloudAverageState[1]), new Point2D(prevCloudAverageState[0], 
				prevCloudAverageState[1]));
		mCloudAverageState[0] = newCoord.getX();
		mCloudAverageState[1] = newCoord.getY();
		/*Line2D centerTrajectory = new Line2D(prevCloudAverageState[0], 
											prevCloudAverageState[1],
											mCloudAverageState[0],
											mCloudAverageState[1]);
		//System.out.println("CENTER TRAJECTORY: " + centerTrajectory.toString());
		for (Line2D wall : walls) {
			if (wall.intersect(centerTrajectory)) {
				mCloudAverageState = prevCloudAverageState;
				break;
			}
		}*/

		System.out.println("Avg x: " + mCloudAverageState[0] + " Avg y: " + mCloudAverageState[1] + "\n\n");
	}




	/**
	 * Resample particles. A particle is selected at a frequency proportional to its weight.
	 * Newly generated particles all have DEFAULT_WEIGHT.
	 */
	private void resample() {

		ArrayList<Particle> temp = new ArrayList<Particle>();
		ArrayList<Double> freq = new ArrayList<Double>();
		temp.addAll(particles);
		System.out.println("particles size: "+particles.size());
		particles.clear();
		mNumberOfParticles = 0;

		double sum = 0;
		for (Particle p: temp) {
			sum += p.getWeight();
		}
		double cumulativeFreq = 0;
		for (int i=0; i<temp.size(); ++i) {
			cumulativeFreq += temp.get(i).getWeight() / sum;
			freq.add(new Double(cumulativeFreq));
		}

		Random generator = new Random();
		double r = generator.nextDouble();

		for (int i=0; i<DEFAULT_PARTICLE_COUNT; ++i) {
			for (int j=0; j<temp.size(); ++j) {
				if (r < freq.get(j)) {
					particles.add(temp.get(j).copy(DEFAULT_WEIGHT));
					mNumberOfParticles++;
					break;
				}
			}
			r = generator.nextDouble();
		}
	}


	public Collection<Particle> getParticles() { 
		return particles; 
	}


	public String getCenter() {
		computeCloudAverageState();
		return mCloudAverageState[0] + " " + mCloudAverageState[1];
	}

	
	public double getPrecision() {
		double sdX = 0.0;
		double sdY = 0.0;
		for (Particle particle: particles) {
			sdX += (particle.getX() - mCloudAverageState[0]) * (particle.getX() - mCloudAverageState[0]);
			sdY += (particle.getY() - mCloudAverageState[1]) * (particle.getY() - mCloudAverageState[1]);
		}
		sdX = Math.sqrt(sdX / particles.size());
		sdY = Math.sqrt(sdY / particles.size());
		return Math.max(sdX, sdY);
	}
	
	public void readCoords(AssetManager am, String filename) {

	    ArrayList<String> maplines= new ArrayList<String>();

	    InputStream inputStream = null;
        try {
        	inputStream = am.open(filename);   
        } catch (IOException e) {
            e.printStackTrace();
        }

	    
    	BufferedReader fis = new BufferedReader(new InputStreamReader(inputStream));
    	String sCurrentLine;
    	try {
			while ((sCurrentLine = fis.readLine()) != null) {
				String[] tmp;
				tmp = sCurrentLine.split("\\s+");
				wifiDbCoords.add(new Point2D(Double.parseDouble(tmp[0]),
											Double.parseDouble(tmp[1])));
			}
			fis.close();
			} catch (IOException e) {
				Log.d("PPOS","Could not read coords file " + e.getMessage());
				return;
			}

		}
	
	public void writeToFile(String fname, String data)  {
		File root = new File(Environment.getExternalStorageDirectory()+File.separator+"wifiloc");
	   
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
			} catch (IOException e)
			{
			Log.d("IMGRES","Could not write to file " + e.getMessage());
			}
	}
}
