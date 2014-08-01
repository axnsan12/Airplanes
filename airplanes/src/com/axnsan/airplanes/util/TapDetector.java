package com.axnsan.airplanes.util;

public class TapDetector implements Detector {
	/**Maximum slop(distance between down location and up location), in pixels**/
	public static final int SLOP = 20;
	/**Max delay between touch down and touch up, in seconds**/
	public static final float DELAY = 0.2f;
	
	private float delta = 0.f;
	private int x, y;
	private boolean waitingForUp = false;
	private TapListener listener = null;
	
	public TapDetector(TapListener lis) {
		listener = lis;
	}
	
	public void down(int x, int y) {
		waitingForUp = true;
		this.x = x; this.y = y;
		
		/* We set the initial duration to -1, and only set it to 0 and begin counting in the next frame.
    	 * This necessary to ensure we don't count the time before this frame.
    	 * We must do this because rendering only happens when waiting for input,
    	 * and as such frame delta can be very high if the last input was long ago.*/
		delta = -1.f;
	}
	
	public void up(int x, int y) {
		if (waitingForUp) {
			if (delta <= DELAY && Math.abs(this.x - x) < SLOP && Math.abs(this.y - y) < SLOP) {
				listener.tap(this.x, this.y);
			}
			waitingForUp = false;
		}
	}
	public boolean waiting() {
		return waitingForUp;
	}
	
	public void elapsed(float delta) {
		if (this.delta < 0.f) /*This step is necessary to ensure we don't count the time before the first tap*/
			this.delta = 0.f; 
		else {
			this.delta += delta;
			if (this.delta > DELAY) /*If we passed the max delay, stop waiting*/
				waitingForUp = false;
		}
	}
}
