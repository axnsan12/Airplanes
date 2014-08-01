package com.axnsan.airplanes;

import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class GameConfiguration {
	public enum KillMode { Headshot, Full };
	public KillMode killMode = KillMode.Headshot;
	public boolean revealDeadPlanes = true;
	public int numPlanes = 3;
	public int gridSize = 10;
	public int numPlayers = 1;
	
	public static final int maxPlayers = 8;
	public static final int maxNameLength = 15;
	public String[] playerNames = new String[maxPlayers];
	
	private void initDefaultNames()
	{
		playerNames = new String[maxPlayers];
		for (int i = 1;i <= maxPlayers;++i)
			playerNames[i-1] = StringManager.getString("player_%d").replace("%d", Integer.toString(i));
		
	}
	public static void ResetConfig(String identifier) {
		Preferences pref = Gdx.app.getPreferences(identifier + "_CONFIG");
		pref.putBoolean("valid", false);
		pref.flush();
	}
	
	public GameConfiguration() { }
	private String ID;
	public GameConfiguration(String identifier)
	{
		this.ID = identifier + "_CONFIG";
		Preferences pref = Gdx.app.getPreferences(ID);
		initDefaultNames();
		
		
		if (pref.getBoolean("valid", false))
		{
			numPlanes = pref.getInteger("numPlanes", numPlanes);
			revealDeadPlanes = pref.getBoolean("revealDeadPlanes", revealDeadPlanes);
			gridSize = pref.getInteger("gridSize", gridSize);
			numPlayers = pref.getInteger("numPlayers", numPlayers);

			String nameString = pref.getString("playerNames", null);
			if (nameString != null)
			{
				String[] names = nameString.split("([\\[\\]])|(\\,[ ])");
				for (int i = 0, j = 0;i < maxPlayers && j < names.length;++j)
				{
					if (names[j].length() > 0)
					{
						if (names[j].length() <= maxNameLength)
							playerNames[i++] = names[j];
						else playerNames[i++] = names[j].substring(0, maxNameLength);
					}
				}
			}
			killMode = KillMode.valueOf(pref.getString("killMode", killMode.toString()));
		}
		else
		{
			dumpConfig();
		}
	}
	public void dumpConfig()
	{
		if (ID == null)
			throw new UnsupportedOperationException();
		
		Preferences pref = Gdx.app.getPreferences(ID);
		pref.putString("killMode", killMode.toString());
		pref.putBoolean("revealDeadPlanes", revealDeadPlanes);
		pref.putInteger("numPlanes", numPlanes);
		pref.putInteger("gridSize", gridSize);
		pref.putInteger("numPlayers", numPlayers);
		pref.putString("playerNames", java.util.Arrays.toString(playerNames));
		pref.putBoolean("valid", true);
		pref.flush();
	}
}
