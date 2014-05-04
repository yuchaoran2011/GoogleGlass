package pf.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import pf.particle.Particle;
import android.util.Log;

public class Parallel {
	public static ArrayList<Particle> parGenParticles(int tot, double x, double y, double weight)
			throws InterruptedException, ExecutionException {

		//int threads = Runtime.getRuntime().availableProcessors();
		int threads = 1;
		//Log.d("Parallel", "# Threads: " + threads+"");
		final ExecutorService service = Executors.newFixedThreadPool(threads);

		final double w = weight;
		final double x_pos = x;
		final double y_pos = y;

		final ArrayList<Particle> output = new ArrayList<Particle>(tot); 

		final HashSet<Future<ArrayList<Particle>>> futures = new HashSet<Future<ArrayList<Particle>>>(threads);
		final int workLoad = (int) Math.floor(tot/threads); // Assume number of threads divides total workload.

		/*
		final class GenParticles extends Thread {

			int workLoad;

			GenParticles(int workLoad) {
			       this.workLoad = workLoad;
			}

		    public void run() {
		    	for (int j=0; j<workLoad; ++j) {
					output.add(Particle.polarNormalDistr(x_pos, y_pos, 0.5, w));
				}
		    }
		}

		ArrayList<GenParticles> list = new ArrayList<GenParticles>();
		for (int i=0; i<threads; ++i) {
			list.add(new GenParticles(workLoad));
			list.get(i).start();
		}
		for (int i=0; i<threads; ++i) {
			list.get(i).join();
		}*/

		
		//long s1 = System.currentTimeMillis();
		for (int i=0; i<threads; ++i) {
			Callable<ArrayList<Particle>> callable = new Callable<ArrayList<Particle>>() {
				public ArrayList<Particle> call() { 
					long start = System.currentTimeMillis();
					ArrayList<Particle> result = new ArrayList<Particle>(workLoad);
					for (int i=0; i<workLoad; ++i) {
						result.add(Particle.polarNormalDistr(x_pos, y_pos, 0.5, w));
					}
					long dur = System.currentTimeMillis() - start;
					Log.d("Parallel", "Thread duration: " + dur);
					return result;
				}			
			};
			futures.add(service.submit(callable));
		}

		service.shutdown();
		//long dur1 = System.currentTimeMillis() - s1;
		//Log.d("Parallel", "Init duration: " + Long.toString(dur1));

		//long start = System.currentTimeMillis();
		for (Future<ArrayList<Particle>> future : futures) {
			output.addAll(future.get());
		}
		//long dur = System.currentTimeMillis() - start;
		//Log.d("Parallel", "Clean-up duration: " + Long.toString(dur));

		return output;
	}

	/*
	static HashSet<Particle> parPropParticles(int tot, double x, double y) {

	}

	static HashSet<Particle> parWeightParticles(int tot, double x, double y) {

	}*/
}
