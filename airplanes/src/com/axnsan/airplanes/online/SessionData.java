package com.axnsan.airplanes.online;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class SessionData {
	public Map<Integer, Game> games = new TreeMap<Integer, Game>(Collections.reverseOrder());
	public boolean needRebuild = false;
	public String username;
	public OnlineMatchHandler currentGame = null;
	public int pendingJoin = -1;
	public Object waitJoin = new Object();
	
	public synchronized Game addGame(Game game) { 
		games.put(game.gameID, game);
		needRebuild = true;
		return game;
	}
	
	public synchronized Game getGame(int gameID) { 
		return games.get(gameID);
	}
}
