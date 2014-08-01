package com.axnsan.airplanes.screens;


public class HotseatSettingsScreen extends PracticeSettingsScreen {
	@Override
	protected void proceed()
	{
		game.setScreen(new HotseatPlayersScreen());
	}
}
