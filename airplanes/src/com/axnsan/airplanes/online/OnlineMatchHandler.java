package com.axnsan.airplanes.online;

import com.axnsan.airplanes.MatchHandler;
import com.axnsan.airplanes.util.Point2D;

public interface OnlineMatchHandler extends MatchHandler {
	void addPlayer(String username);
	void removePlayer(String username);
	void playerAttackedCell(String username, Point2D cell);
	void beginTurn(int turn);
	void addPlanes(String playerName, String planeLocations);
	int getGameID();
}
