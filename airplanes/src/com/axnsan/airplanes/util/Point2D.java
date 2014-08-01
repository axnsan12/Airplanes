package com.axnsan.airplanes.util;

public class Point2D {
	public int x, y;
	public Point2D() {}
	public Point2D(int x, int y) {
		this.x = x; this.y = y;
	}
	
	@Override
	public String toString()
	{
		return x + ";"  + y + ";";
	}
}
