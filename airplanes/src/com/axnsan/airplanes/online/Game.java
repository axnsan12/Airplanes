package com.axnsan.airplanes.online;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.GameConfiguration;
import com.axnsan.airplanes.GameConfiguration.KillMode;
import com.axnsan.airplanes.util.ActionManager;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class Game {
	public GameConfiguration config;
	
	public int gameID;
	public boolean joined = false;
	private int turnNumber;
	private int currentPlayers;
	@SuppressWarnings("unused") private int timeout;
	private boolean finished = false;
	
	public Game(int gameID, int timeout, int numPlayers, int gridSize, int numPlanes, boolean headshots,
			boolean reveal, int currentPlayers, int turn, boolean joined) {
		this.gameID = gameID;
		this.timeout = timeout;
		this.config = new GameConfiguration();
		this.config.numPlayers = numPlayers;
		this.config.gridSize = gridSize;
		this.config.numPlanes = numPlanes;
		this.config.killMode = (headshots)?KillMode.Headshot:KillMode.Full;
		this.config.revealDeadPlanes = reveal;
		this.currentPlayers = currentPlayers;
		this.turnNumber = turn;
		this.joined = joined;
		playerLabel = new Label(Integer.toString(currentPlayers) + "/" + Integer.toString(config.numPlayers), Airplanes.game.skin);
		turnLabel = new Label((turnNumber>=0)?Integer.toString(turnNumber):"-", Airplanes.game.skin);
		but = new TextButton(joined?">":"Join", Airplanes.game.skin);
	}
	
	private Label turnLabel, playerLabel;
	private TextButton but;
	public void addRowToTable(Table table, final ClientSocket socket, final SessionData session) {
		if (joined == false && (currentPlayers >= config.numPlayers || turnNumber >= 0))
			return;
		
		table.add(Integer.toString(gameID));
		table.add(config.revealDeadPlanes?"yes":"no");
		table.add(Integer.toString(config.gridSize));
		table.add(Integer.toString(config.numPlanes));
		playerLabel = new Label(Integer.toString(currentPlayers) + "/" + Integer.toString(config.numPlayers), Airplanes.game.skin);
		table.add(playerLabel);
		turnLabel = new Label((turnNumber>=0)?Integer.toString(turnNumber):"-", Airplanes.game.skin);
		table.add(turnLabel);
		but = new TextButton(joined?">":"Join", Airplanes.game.skin);
		but.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				try {
					ActionManager.showProgressDialog(StringManager.getString("waiting_server"));
					synchronized (session) {
						if (joined == false && currentPlayers >= config.numPlayers) {
							ActionManager.dismissProgressDialog();
							return;
						}
						if (joined == true && currentPlayers >= config.numPlayers) {
							ActionManager.dismissProgressDialog();
							Airplanes.game.setScreen((Screen) (session.currentGame = new OnlineGameScreen(socket, session, gameID)));
							return;
						}
						socket.sendMessage(new GameJoinMessage(gameID, ""));
						ServerResponseMessage response = socket.responseQueue.poll(Airplanes.TIMEOUT, TimeUnit.SECONDS);
						if (response == null)
							throw new IOException("Request timed out");
						
						switch (response.responseCode) {
						case RESPONSE_CODE.BAD_GAME_ID:
							break;
						case RESPONSE_CODE.GAME_FULL:
							break;
						case RESPONSE_CODE.WRONG_GAME_PASSWORD:
							break;
						case RESPONSE_CODE.RESPONSE_OK:
							session.currentGame = new OnlineGameScreen(socket, session, gameID);
							joined = true;
							if (response.metadata.length() > 0) {
								String[] p = response.metadata.split("\\,");
								for (String s : p)
									session.currentGame.addPlayer(s);
							}
							Airplanes.game.setScreen((Screen) session.currentGame);
						}
					}
				}
				catch (IOException | InterruptedException e) {
					ActionManager.dismissProgressDialog();
					ActionManager.showLongToast(StringManager.getString("connection_failed"));
					e.printStackTrace();
					Airplanes.game.back();
				}
				ActionManager.dismissProgressDialog();
		    }
		});
		table.add(but).width(Gdx.graphics.getWidth()/9).height(Gdx.graphics.getHeight()/30 + 16).pad(2).padLeft(6);
		table.row();
	}

	public int getCurrentPlayers() {
		return currentPlayers;
	}

	public void setCurrentPlayers(int currentPlayers) {
		this.currentPlayers = currentPlayers;
		String tmp = Integer.toString(currentPlayers) + "/" + Integer.toString(config.numPlayers);
		playerLabel.setText(tmp);
	}
	
	public void addPlayer() {
		if (currentPlayers >= config.numPlayers)
			throw new RuntimeException("Adding player to full game");
		setCurrentPlayers(currentPlayers + 1);
	}
	
	public void removePlayer() {
		if (turnNumber <= 0)
			setCurrentPlayers(currentPlayers - 1);
	}
	
	public void join() {
		joined = true;
		but.setText(">");
	}
	
	public void leave() {
		joined = false;
		but.setText("Join");
	}

	public int getTurnNumber() { return turnNumber; }
	
	public void setTurn(int turn) { this.turnNumber = turn; }
	
	public boolean isFull() { return currentPlayers >= config.numPlayers; }
	
	public boolean isJoined() { return joined; }
	
	public void setFinished(boolean finished) { this.finished = finished; }
	
	public boolean isFinished() { return finished; }
}
