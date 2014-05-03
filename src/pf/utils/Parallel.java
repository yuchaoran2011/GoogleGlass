package pf.utils;

import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import pf.particle.Particle;

public class Parallel {
	public static HashSet<Particle> parGenParticles(int tot, double x, double y, double weight)
			throws InterruptedException, ExecutionException {

		int threads = Runtime.getRuntime().availableProcessors();
		//Log.d("Parallel", "# Threads: " + threads+"");
		final ExecutorService service = Executors.newFixedThreadPool(threads);

		final double w = weight;
		final double x_pos = x;
		final double y_pos = y;

		final HashSet<Particle> outputs = new HashSet<Particle>(); 

		final HashSet<Future<Void>> futures = new HashSet<Future<Void>>();
		final int workLoad = (int) Math.floor(tot/threads);
		final int tail = tot - workLoad * threads;

		for (int i=0; i<threads; ++i) {
			Callable<Void> callable = new Callable<Void>() {
				public Void call() { 
					for (int j=0; j<workLoad; ++j) {
						outputs.add(Particle.polarNormalDistr(x_pos, y_pos, 0.5, w));
					}
					return null;
				}			
			};
			futures.add(service.submit(callable));
		}

		if (tail != 0) {
			Callable<Void> callable = new Callable<Void>() {
				public Void call() {
					for (int j=0; j<tail; ++j) {
						outputs.add(Particle.polarNormalDistr(x_pos, y_pos, 0.5, w));
					}
					return null;
				}
			};
			futures.add(service.submit(callable));
		}

		service.shutdown();
		
		for (Future<Void> f: futures) {
			while (!f.isDone()) {
				// Keep looping until current future is done.
			}
		}

		return outputs;
	}

	/*
	static HashSet<Particle> parPropParticles(int tot, double x, double y) {

	}

	static HashSet<Particle> parWeightParticles(int tot, double x, double y) {

	}*/
}
