package com.axnsan.airplanes;

import com.axnsan.airplanes.Plane.Model;
import com.axnsan.airplanes.Plane.Orientation;


public class GridRandomizer extends BaseGrid {
	private Model[] model;
	
	public int tryToFill() {
		model = new Model[4];
		model[0] = Plane.calculateGridModel(Orientation.UP);
		model[1] = Plane.calculateGridModel(Orientation.LEFT);
		model[2] = Plane.calculateGridModel(Orientation.DOWN);
		model[3] = Plane.calculateGridModel(Orientation.RIGHT);
		try_placement: while (true) 
		{
			int x, y, o;
			int attempts = 0;
			double range = size;
			do {
				x = (int) (Math.random() * range);
				y = (int) (Math.random() * range);
				o = (int) (Math.random() * 4);
				
				attempts++;
			} while (addPlane(x, y, model[o]) <= 0 && attempts <= 500);
			if (attempts > 500) {
				for (o = 0;o < 4;++o) {
					for (x = 0;x < size;++x)
						for (y = 0;y < size;++y)
							{
								if (addPlane(x, y, model[o]) > 0) {
									continue try_placement;
								}
							}
				}
				break;
			}
		}
		return planeLocations.size();
	}
	public GridRandomizer(int size) {
		super(size);
	}
		
	public static class thread1 implements Runnable {

	    public void run() {
		    for (int numPlanes = 4;numPlanes <= 41;++numPlanes) {
		    	int avg = 0;
				int samplesize = 10000;
				for (int i = 1;i <= samplesize;++i) {
					avg += new GridRandomizer(numPlanes).tryToFill();
				}
				System.out.println(numPlanes + " " + (double)avg/samplesize);
		    }
	    }
	}
	public static class thread2 implements Runnable {

	    public void run() {
		    for (int numPlanes = 42;numPlanes <= 50;++numPlanes) {
		    	int avg = 0;
				int samplesize = 10000;
				for (int i = 1;i <= samplesize;++i) {
					avg += new GridRandomizer(numPlanes).tryToFill();
				}
				System.out.println(numPlanes + " " + (double)avg/samplesize);
		    }
	    }
	}
	public static void main(String[] args) {
		for (int i = 4;i <= 50;++i) {
			Airplanes game = Airplanes.game;
			game.config.gridSize = i;
			System.out.println(i + " " + Airplanes.game.maxNumPlanes());
		}
		/*new Thread(new thread1()).start();
		new Thread(new thread2()).start();*/
		/*for (int numPlanes = 4;numPlanes <= 50;++numPlanes) {
			//new Thread(new thread(numPlanes)).start();
			int avg = 0;
			int samplesize = 10000;
			for (int i = 1;i <= samplesize;++i) {
				avg += new GridRandomizer(numPlanes).tryToFill();
			}
			System.out.println(numPlanes + ": " + (double)avg/samplesize);
		}*/
	}
}
