package com.axnsan.airplanes.online;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.GameConfiguration;
import com.axnsan.airplanes.util.ActionManager;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class LobbyScreen implements Screen {

	private Stage stage;
	private ScrollPane scrollPane;
	
	private final ClientSocket socket;
	private BlockingQueue<ServerResponseMessage> responseQueue = new LinkedBlockingQueue<ServerResponseMessage>();
	private SessionData session = new SessionData();
	private Table table;
	private TextButton backButton, createButton;
	private Thread eventThread;
	
	public LobbyScreen(ClientSocket socket, BlockingQueue<ServerResponseMessage> responseQueue
			, BlockingQueue<Message> eventQueue, String username) {
		this.socket = socket;
		this.responseQueue = responseQueue;
		session.username = username;
		eventThread = new Thread(new EventHandler(eventQueue, session));
		eventThread.start();
		
		if (stage != null) {
			Airplanes.game.input.removeProcessor(stage);
			stage.dispose();
		}
		
		stage = new Stage();
		table = new Table(Airplanes.game.skin);
		table.align(Align.center | Align.top);
		scrollPane = new ScrollPane(table);
		scrollPane.setFadeScrollBars(false);
		stage.addActor(scrollPane);
		
		backButton = new TextButton(StringManager.getString("back"), Airplanes.game.skin);
		backButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				Airplanes.game.back();
		    }
		});
		stage.addActor(backButton);
		
		createButton = new TextButton(StringManager.getString("create"), Airplanes.game.skin);
		createButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				Airplanes.game.config = new GameConfiguration("ONLINE");
				Airplanes.game.setScreen(new OnlineSettingsScreen(LobbyScreen.this.socket, LobbyScreen.this.responseQueue, session));
		    }
		});
		stage.addActor(createButton);
		
		try {
			ActionManager.showProgressDialog(StringManager.getString("waiting_server"));
			socket.sendMessage(new RequestGamesMessage());
			ServerResponseMessage message = responseQueue.poll(Airplanes.TIMEOUT, TimeUnit.SECONDS);
			if (message == null)
				throw new IOException("Request timed out.");
			if (message.responseCode != RESPONSE_CODE.GAME_LISTING)
				throw new IOException("Server error: bad response");
			
			String[] strings = message.metadata.split("\\|", -1);
			String[] lobbyGames = (strings[0].length() > 0)?strings[0].split("\\."):new String[0];
			String[] myGames = (strings[1].length() > 0)?strings[1].split("\\."):new String[0];
			synchronized (session) {
				for (String game : myGames) {
					String[] attr = game.split("\\,");
					session.addGame(new Game(Integer.parseInt(attr[0]),
							Integer.parseInt(attr[1]),
							Integer.parseInt(attr[2]),
							Integer.parseInt(attr[3]),
							Integer.parseInt(attr[4]),
							Boolean.parseBoolean(attr[5]),
							Boolean.parseBoolean(attr[6]),
							Integer.parseInt(attr[7]),
							Integer.parseInt(attr[8]),
							true)).setFinished(Boolean.parseBoolean(attr[9]));
				}
				for (String game : lobbyGames) {
					String[] attr = game.split("\\,");
					if (session.getGame(Integer.parseInt(attr[0])) == null) {
						session.addGame(new Game(Integer.parseInt(attr[0]),
								Integer.parseInt(attr[1]),
								Integer.parseInt(attr[2]),
								Integer.parseInt(attr[3]),
								Integer.parseInt(attr[4]),
								Boolean.parseBoolean(attr[5]),
								Boolean.parseBoolean(attr[6]),
								Integer.parseInt(attr[7]),
								-1,
								false));
					}
				}
			}
		} catch (IOException | InterruptedException e) {
			ActionManager.dismissProgressDialog();
			ActionManager.showLongToast(StringManager.getString("connection_failed"));
			e.printStackTrace();
			Airplanes.game.back();
			return;
		}
		ActionManager.dismissProgressDialog();
	}
	
	@Override
	public synchronized void render(float delta) {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		stage.act(delta);
		stage.draw();
		synchronized (session) {
			if (session.needRebuild)
				rebuildTable();
		}
		
		try {
			Thread.sleep(Math.max(0, (long) (Airplanes.MAX_FRAME_TIME - Gdx.graphics.getRawDeltaTime())));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void resize(int width, int height) {
		rebuildTable();
		stage.setViewport(width, height);
		scrollPane.setSize(width, height - height/10 - 2*height/100);
		scrollPane.setPosition(0, height/10 + 2*height/100);
		TextButtonStyle tbs = Airplanes.game.skin.get(TextButtonStyle.class);
		BitmapFont old = tbs.font;
		tbs.font = FontManager.getFontForHeight(height/17);
		backButton.setBounds(height/100, height/100, width/2.7f, height/10);
		backButton.setStyle(tbs);
		createButton.setBounds(width - height/100 - width/2.7f, height/100, width/2.7f, height/10);
		createButton.setStyle(tbs);
		tbs.font = old;
	}

	@Override
	public void show() {
		synchronized (session) {
			session.currentGame = null;
		}
		Airplanes.game.input.clear();
		Airplanes.game.input.addProcessor(Airplanes.game);
		Gdx.graphics.setContinuousRendering(true);
		Airplanes.game.input.addProcessor(stage);
	}

	public synchronized void rebuildTable() {
		table.clearChildren();
		int h = Gdx.graphics.getHeight();
		Airplanes.game.setTextButtonFont(FontManager.getFontForHeight(h/30));
		Airplanes.game.setLabelFont(FontManager.getFontForHeight(h/30));
		LabelStyle ls = new LabelStyle();
		ls.font = FontManager.getFontForHeight(h/30);
		ls.fontColor = Color.BLACK;
		table.add(new Label("ID  ", ls));
		table.add(new Label("Reveal ", ls));
		table.add(new Label("Grid ", ls));
		table.add(new Label("Planes ", ls));
		table.add(new Label("Players ", ls));
		table.add(new Label("Turn", ls));
		table.row();
		
		synchronized (session) {
			for (Game g : session.games.values()) {
				if (g.joined)
					g.addRowToTable(table, socket, session);
			}
			table.add().height(h/100);
			table.row();
			for (Game g : session.games.values()) {
				if (!g.joined)
					g.addRowToTable(table, socket, session);
			}
			session.needRebuild = false;
		}
	}
	
	@Override
	public void hide() {
		Gdx.graphics.setContinuousRendering(false);
		Airplanes.game.input.removeProcessor(stage);
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		if (stage != null) {
			Airplanes.game.input.removeProcessor(stage);
			stage.dispose();
			stage = null;
		}
		if (socket != null && socket.isConnected()) {
			socket.disconnect();
			try {
				eventThread.join();
			} catch (InterruptedException e) { }
		}
	}
	
	@Override 
	public void finalize() {
		if (socket != null && socket.isConnected()) {
			socket.disconnect();
			try {
				eventThread.join();
			} catch (InterruptedException e) { }
		}
	}
}
