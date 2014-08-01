package com.axnsan.airplanes;

import com.axnsan.airplanes.util.Point2D;

public interface MatchHandler {
	public void playerWasAttacked(int playerID, Point2D cell);
	public void playerWasAttacked(int playerID, int gridX, int gridY);
	public void cellSelected(Point2D cell);
	public void cellSelected(int gridX, int gridY);
	public void playerDied(int playerID);
}
