package com.axnsan.airplanes.util;

public interface Detector {
	public boolean waiting();
	
	/*Only to be called after a positive check to waiting()*/
	public void elapsed(float delta);
}

