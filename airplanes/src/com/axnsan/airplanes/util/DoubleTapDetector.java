package com.axnsan.airplanes.util;

public class DoubleTapDetector implements Detector {
	/**Maximum slop(distance between taps) in pixels**/
	public static final int SLOP = 30;
	/**Max delay between taps in seconds**/
	public static final float DELAY = 0.5f;
	
	private float delta = 0.f;
	private int x, y;
	private boolean waitingFor2ndTap = false;
	private DoubleTapListener listener = null;
	
	public DoubleTapDetector(DoubleTapListener lis) {
		listener = lis;
	}
	
	public void tap(int x, int y) {
		if (waitingFor2ndTap) {
			if (delta <= DELAY && Math.abs(this.x - x) < SLOP && Math.abs(this.y - y) < SLOP) {
				listener.doubleTap(this.x, this.y);
			}
			waitingFor2ndTap = false;
		}
		else {
			waitingFor2ndTap = true;
			this.x = x; this.y = y;
			
			/* We set the initial duration to -1, and only set it to 0 and begin counting in the next frame.
        	 * This necessary to ensure we don't count the time before this frame.
        	 * We must do this because rendering only happens when waiting for input,
        	 * and as such frame delta can be very high if the last input was long ago.*/
			delta = -1.f;
		}
	}
	
	public boolean waiting() {
		return waitingFor2ndTap;
	}
	
	public void elapsed(float delta) {
		if (this.delta < 0.f) 
			this.delta = 0.f; 
		else {
			this.delta += delta;
			if (this.delta > DELAY) /*If we passed the max delay, stop waiting*/
				waitingFor2ndTap = false;
		}
	}
	
};
