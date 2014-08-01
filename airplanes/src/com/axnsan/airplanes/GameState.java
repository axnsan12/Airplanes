package com.axnsan.airplanes;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;


public class GameState {
	public enum GameMode { Hotseat, Practice, None };
	public GameMode activeGameMode = GameMode.None;
	
	public ArrayList<Player> players = new ArrayList<Player>();
	 
	public int currentPlayer = -1;
	public int focusedGrid = -1;
	public int turnNumber = 0;
	public boolean finished = false;
	
	public static void ResetState(String identifier) {
		Preferences pref = Gdx.app.getPreferences(identifier + "_STATE");
		pref.putBoolean("valid", false);
		pref.flush();
	}
	public static boolean isValidState(String identifier) {
		Preferences pref = Gdx.app.getPreferences(identifier + "_STATE");
		return pref.getBoolean("valid", false);
	}
	private String ID;
	public GameState(String identifier) {
		ID = identifier + "_STATE";
		this.ID = identifier + "_STATE";
		Preferences pref = Gdx.app.getPreferences(ID);
		
		
		if (pref.getBoolean("valid", false))
		{
			currentPlayer = pref.getInteger("currentPlayer", currentPlayer);
			focusedGrid = pref.getInteger("focusedGrid", focusedGrid);
			turnNumber = pref.getInteger("turnNumber", turnNumber);
			finished = pref.getBoolean("finished", finished);
			
			String playersString = pref.getString("players", null);
			if (playersString != null)
			{
				String[] players = playersString.split("([\\[\\]])|(\\,[ ])");
				for (int j = 1; j < players.length; ++j) {
					this.players.add(new Player(players[j]));
				}
			}
		}
		else
		{
			dumpState();
		}
		Airplanes.game.players = this.players;
	}
	
	public void dumpState() {
		Preferences pref = Gdx.app.getPreferences(ID);
		pref.putInteger("currentPlayer", currentPlayer);
		pref.putInteger("focusedGrid", focusedGrid);
		pref.putInteger("turnNumber", turnNumber);
		pref.putBoolean("finished", finished);
		pref.putString("players", players.toString());
		
		pref.putBoolean("valid", true);
		pref.flush();
	}
}
