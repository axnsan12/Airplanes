package com.axnsan.airplanes.online;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.Plane;
import com.axnsan.airplanes.Player;
import com.axnsan.airplanes.PlayingGrid;
import com.axnsan.airplanes.Player.State;
import com.axnsan.airplanes.screens.PlacementScreen;
import com.axnsan.airplanes.util.ActionManager;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.Point2D;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class OnlineGameScreen implements Screen, OnlineMatchHandler {
	private Stage stage;
	private ChatView chat;
	private Game game;
	private TextButton backButton, leaveButton, nextGridButton;
	private Player player;
	private ArrayList<Player> players = new ArrayList<Player>();
	private PlayingGrid shownGrid = null;
	private SpriteBatch batch;
	private ClientSocket socket;
	
	private Player getPlayerByName(String name) {
		for (Player p : players)
			if (p.name.equals(name))
				return p;
		return null;
	}
	
	public OnlineGameScreen(final ClientSocket socket, final SessionData session, final int gameID) {
		this.socket = socket;
		stage = new Stage();
		chat = new ChatView(stage);
		game = session.getGame(gameID);
		Airplanes.game.config = game.config;
		if (game.getTurnNumber() >= 0) {
			try {
				ActionManager.showProgressDialog(StringManager.getString("waiting_server"));
				socket.sendMessage(new GetGameStateMessage(gameID));
				ServerResponseMessage msg = socket.responseQueue.poll(Airplanes.TIMEOUT, TimeUnit.SECONDS);
				if (msg == null)
					throw new IOException("Request timed out.");
				
				System.out.println("Game state: " + msg.metadata);
				String[] players = msg.metadata.split("([\\[\\]])|(\\,[ ])");
				for (int j = 1; j < players.length; ++j) {
					Player p = new Player(players[j]);
					game.config.playerNames[j-1]= p.name;
					p.grid = new PlayingGrid(0, 1, game.config.gridSize, this, j-1);
					if (game.isFinished())
						p.grid.showPlanes();
					else p.grid.hidePlanes();
					p.grid.hide();
					p.grid.addPlanes(p.planes);
					this.players.add(p);
				}
				player = getPlayerByName(session.username);
				if (player.moves.size() < game.getTurnNumber())
					for (Player p : this.players)
						p.grid.setTargetable(player.state != Player.State.DEAD && !game.isFinished());
				player.grid.setTargetable(false);
				player.grid.setLabel(StringManager.getString("my_grid"));
				player.grid.showPlanes();
				{
					int turn = 0;
					boolean done = false;
					while (!done) {
						done = true;
						for (int i = 0;i < this.players.size(); ++i) {
							Player p1 = this.players.get(i);
							if (p1.moves.size() > turn) {
								done = false;
								Point2D cell = p1.moves.get(turn);
								int x = cell.x, y = cell.y;
								for (Player p2 : this.players)
									if (p2 != p1 && p2.state != Player.State.DISCONNECTED 
											&& (turn <= p2.turnOfDeath || p2.turnOfDeath == 0))
										p2.grid.attackCell(x, y);
							}
						}
						++turn;
					}
				}
				checkEndCondition();
				ActionManager.dismissProgressDialog();
			}
			catch (IOException | InterruptedException e) {
				ActionManager.dismissProgressDialog();
				ActionManager.showLongToast(StringManager.getString("connection_failed"));
				e.printStackTrace();
				Airplanes.game.back();
			}
			chat.hide();
		}
		else {
			addPlayer(session.username);
			player = players.get(players.size()-1);
			player.grid.setTargetable(false);
			player.grid.setLabel(StringManager.getString("my_grid"));
			player.grid.showPlanes();
		}
		
		leaveButton = new TextButton(StringManager.getString("leave_game"), Airplanes.game.skin);
		leaveButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				try {
					socket.sendMessage(new GameLeaveMessage(gameID));
					synchronized (session) {
						session.currentGame = null;
						session.getGame(gameID).leave();
					}
				}
				catch (IOException e) {
					ActionManager.showLongToast(StringManager.getString("connection_failed"));
					e.printStackTrace();
				}
				Airplanes.game.back();
		    }
		});
		stage.addActor(leaveButton);
		
		backButton = new TextButton(StringManager.getString("back_lobby"), Airplanes.game.skin);
		backButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				synchronized (session) {
					session.currentGame = null;
				}
				Airplanes.game.back();
		    }
		});
		stage.addActor(backButton);
		
		nextGridButton = new TextButton(">", Airplanes.game.skin);
		nextGridButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				showNextGrid();
		    }
		});
		nextGridButton.setVisible(false);
		stage.addActor(nextGridButton);
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		stage.act(delta);
		stage.draw();
		synchronized (this) {
		batch.begin(); 
		{
			if (shownGrid != null) {
				shownGrid.draw(batch, 1.f);
				int h = Gdx.graphics.getHeight();
				int fontHeight = h/20;
				float wrapWidth = shownGrid.getWidth() - nextGridButton.getWidth();
				BitmapFont font = FontManager.getFontForHeight(fontHeight);
				String text = shownGrid.getLabel() + "\n";
				if (game.getTurnNumber() > 0) {
					if (players.get(shownGrid.getPlayerID()).state == Player.State.DISCONNECTED) {
						//if (shownGrid.planesLeft() > 0)
						text += StringManager.getString("turn_disconnect%d").replace
								("%d", Integer.toString(players.get(shownGrid.getPlayerID()).turnOfDeath));
						//else text += StringManager.getString("turn_death%d").replace
							//	("%d", Integer.toString(players.get(shownGrid.getPlayerID()).turnOfDeath));
					}
					else if (shownGrid.planesLeft() > 1)
						text += StringManager.getString("%dplanes_left").replace
							("%d", Integer.toString(shownGrid.planesLeft()));
					else if (shownGrid.planesLeft() == 1)
						text += StringManager.getString("one_plane_left");
					else text += StringManager.getString("turn_death%d").replace
							("%d", Integer.toString(players.get(shownGrid.getPlayerID()).turnOfDeath));
				}
				while (font.getWrappedBounds(text, wrapWidth).height > h*0.09f)
					font = FontManager.getFontForHeight(--fontHeight);
				font.setColor(Color.BLACK);
				font.drawWrapped(batch, text, 0, Gdx.graphics.getHeight() - 2 - h/100, wrapWidth, HAlignment.CENTER);
				
				if (!game.isFinished()) {
					if (game.getTurnNumber() > 0) {
						text = "Turn " + game.getTurnNumber();
						if (player.state == Player.State.DEAD)
							text += (" - " + StringManager.getString("u_ded"));
						else if (player.moves.size() == game.getTurnNumber())
							text += (" - " + StringManager.getString("waiting_players_moves"));
					}
					else text = StringManager.getString("waiting_planes");
				}
				else text = StringManager.getString("finished_turn%d").replace("%d", Integer.toString(game.getTurnNumber()));
				fontHeight = h/20;
				font = FontManager.getFontForHeight(h/20);
				wrapWidth = Gdx.graphics.getWidth() * 0.95f;
				while (font.getWrappedBounds(text, wrapWidth).height > fontHeight)
					font = FontManager.getFontForHeight(--fontHeight);
				font.drawWrapped(batch, text, shownGrid.getX(), shownGrid.getY() - h/30, wrapWidth);
			}
				
		}
		batch.end();
		}
		try {
			Thread.sleep(Math.max(0, (long) (Airplanes.MAX_FRAME_TIME - Gdx.graphics.getRawDeltaTime())));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private static final int minPadding = Airplanes.minPadding, gridBorderWidth = Airplanes.gridBorderWidth;
	@Override
	public synchronized void resize(int width, int height) {
		if (batch != null)
			batch.dispose();
		batch = new SpriteBatch();
		
		stage.setViewport(width, height);
		chat.setBounds(0.01f*width, height*0.1f, width*0.98f, height*0.89f);
		Airplanes.game.setTextButtonFont(FontManager.getFontForHeight(height/20));
		backButton.setStyle(Airplanes.game.skin.get(TextButtonStyle.class));
		leaveButton.setStyle(Airplanes.game.skin.get(TextButtonStyle.class));
		backButton.setBounds(0.01f*width, 0.0025f*height, 0.54f*width, 0.095f*height);
		leaveButton.setBounds(0.56f*width, 0.0025f*height, 0.43f*width, 0.095f*height);
		
		int h = Gdx.graphics.getHeight();
		int availableHeight = Gdx.graphics.getHeight() - minPadding;
		int availableWidth = Gdx.graphics.getWidth() - 2*minPadding;
		availableHeight -= h/10; //Row on top;
		availableHeight -= h/10; //Row on bottom
		availableHeight -= 3*h/100;
		int size = game.config.gridSize;
		int maxCellWidth = (availableWidth - (size+1)*gridBorderWidth) / (size);
		int maxCellHeight = (availableHeight - (size)*gridBorderWidth) / (size);
		
		PlayingGrid shownGrid = new PlayingGrid(0, 1, game.config.gridSize, null, -1);	
		int buttonPad = h/100;
		int buttonHeight = h/10;
		
		if (maxCellWidth < maxCellHeight) {
			/*If width is the limiting factor, keep the grid to the top of the window*/
			shownGrid.setCellSize(maxCellWidth);
			shownGrid.setPosition((Gdx.graphics.getWidth() - shownGrid.getWidth()) / 2, 
					Gdx.graphics.getHeight() - shownGrid.getHeight() - buttonHeight - 2*buttonPad);
		}
		else {
			/*If height is the limiting factor, keep the grid to the top left of the window*/
			shownGrid.setCellSize(maxCellHeight);
			shownGrid.setPosition(minPadding, Gdx.graphics.getHeight() - shownGrid.getHeight() - buttonHeight - 2*buttonPad);
		}
		for (Player p : players) {
			p.grid.setPosition(shownGrid.getX(), shownGrid.getY());
			p.grid.setCellSize(shownGrid.getCellSize());
		}

		nextGridButton.setStyle(Airplanes.game.skin.get(TextButtonStyle.class));
		nextGridButton.setBounds(shownGrid.getX() + shownGrid.getWidth() - width/7, 
				Gdx.graphics.getHeight() - h/10 - buttonPad, 
				width/7, h/10);
		
		Gdx.graphics.requestRendering();
	}
	
	private int shown = -1;
	private Point2D selectedCell = new Point2D(-1, -1);
	private void showNextGrid() {
		if (shownGrid != null) {
			shownGrid.hide();
			Airplanes.game.input.removeProcessor(shownGrid);
		}
		shownGrid = players.get(shown = (shown+1)%players.size()).grid;
		shownGrid.show();
		Airplanes.game.input.addProcessor(shownGrid);
		shownGrid.selectCell(selectedCell);
		nextGridButton.setVisible(true);
	}

	private boolean startedPlacement = false;
	@Override
	public synchronized void show() {
		Gdx.input.setOnscreenKeyboardVisible(false);
		Airplanes.game.input.addProcessor(stage);
		if (player.state == Player.State.PLACING) {
			if (!startedPlacement) {
				Airplanes.game.setScreen(new PlacementScreen(player));
				startedPlacement = true;
				return;
			}
			else {
				Airplanes.game.back();
				return;
			}
		}
		if (player.state == Player.State.READY && startedPlacement) {
			startedPlacement = false;
			try {
				ActionManager.showProgressDialog(StringManager.getString("waiting_server"));
				StringBuilder str = new StringBuilder();
				for (Plane.Location loc : player.planes)
					str.append(loc.toString());
				socket.sendMessage(new PlaneLocationsMessage(game.gameID, player.name, str.toString()));
				ActionManager.dismissProgressDialog();
			}
			catch (IOException e) {
				ActionManager.dismissProgressDialog();
				ActionManager.showLongToast(StringManager.getString("connection_failed"));
				e.printStackTrace();
				Airplanes.game.back();
			}
		}
		if (game.getTurnNumber() >= 0) {
			do { ++shown; }
			while (player != players.get(shown));
			showNextGrid();
		}
	}

	@Override
	public void hide() {
		Airplanes.game.input.removeProcessor(stage);
		if (shownGrid != null) {
			Airplanes.game.input.removeProcessor(shownGrid);
			shownGrid = null;
		}
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public synchronized void dispose() {
		if (stage != null) {
			Airplanes.game.input.removeProcessor(stage);
			stage.dispose();
			stage = null;
		}
		if (batch != null)
			batch.dispose();
		for (Player p : players)
			p.dispose();
		players.clear();
		if (shownGrid != null) 
			Airplanes.game.input.removeProcessor(shownGrid);
		shownGrid = null;
	}

	@Override
	public void playerWasAttacked(int playerID, Point2D cell) {
		try {
			
			player.moves.add(cell);
			socket.sendMessage(new AttackCellMessage(game.gameID, player.name, cell.x, cell.y));
			ServerResponseMessage response = socket.responseQueue.poll(200, TimeUnit.MILLISECONDS);
			if (response == null) {
				ActionManager.showProgressDialog(StringManager.getString("waiting_server"));
				response = socket.responseQueue.poll(Airplanes.TIMEOUT, TimeUnit.SECONDS);
				ActionManager.dismissProgressDialog();
			}
			if (response == null)
				throw new IOException("Request timed out.");
			
			if (response.responseCode == RESPONSE_CODE.RESPONSE_OK) {
				for (Player p : this.players) {
					p.grid.setTargetable(false);
					if (p != player && p.state != Player.State.DEAD && p.state != Player.State.DISCONNECTED)
						p.grid.attackCell(cell.x, cell.y);
				}
				selectedCell.x = selectedCell.y = -1;
				if (player.state == Player.State.LASTSTAND) {
					player.state = Player.State.DEAD;
					checkEndCondition();
				}
			}
			else if (response.responseCode == RESPONSE_CODE.GAME_ALREADY_FINISHED){
				endGame();
			}
		}
		catch (IOException | InterruptedException e) {
			ActionManager.dismissProgressDialog();
			ActionManager.showLongToast(StringManager.getString("connection_failed"));
			e.printStackTrace();
			Airplanes.game.back();
		}
	}

	@Override
	public void playerWasAttacked(int playerID, int gridX, int gridY) {
		playerWasAttacked(playerID, new Point2D(gridX, gridY));
	}

	@Override
	public void cellSelected(Point2D cell) {
		cellSelected(cell.x, cell.y);
	}

	@Override
	public void cellSelected(int gridX, int gridY) {
		selectedCell.x = gridX;
		selectedCell.y = gridY;
	}

	@Override
	public synchronized void playerDied(int playerID) {
		Player player = players.get(playerID);
		if (game.isFinished()) {
			if (!(player.turnOfDeath == 0 && player.grid.planesLeft() == 0))
				return;
		}
		
		player.turnOfDeath = game.getTurnNumber();
		player.grid.showPlanes();
		player.grid.setTargetable(false);
		Player.State prev = this.player.state;
		if (player.moves.size() < game.getTurnNumber())
			player.state = Player.State.LASTSTAND;
		else player.state = Player.State.DEAD;
		checkEndCondition();
		if (player == this.player) {
			if (player.state == Player.State.DEAD) {
				for (Player p : players)
					p.grid.setTargetable(false);
				selectedCell.x = selectedCell.y = -1;
			}
			try {
				if (prev != Player.State.LASTSTAND && prev != Player.State.DEAD)
					socket.sendMessage(new PlayerDeathMessage(game.gameID, player.turnOfDeath));
			}
			catch (IOException e) {
				ActionManager.showLongToast(StringManager.getString("connection_failed"));
				e.printStackTrace();
				Airplanes.game.back();
			}
		}
	}

	@Override
	public synchronized void addPlayer(String username) {
		for (Player p : players)
			if (p.name.equals(username))
				return;
		
		chat.addPlayer(username);
		chat.addMessage(username + " joined.");
		game.config.playerNames[players.size()] = username;
		PlayingGrid grid = new PlayingGrid(0, 1, game.config.gridSize, this, players.size());
		grid.hidePlanes();
		grid.hide();
		Player p = new Player(grid, null, players.size());
		players.add(p);
	}

	@Override
	public synchronized void removePlayer(String username) {
		chat.removePlayer(username);
		chat.addMessage(username + " left.");
		Player p = getPlayerByName(username);
		if (p.state != Player.State.DEAD) {
			p.state = Player.State.DISCONNECTED;
			p.grid.showPlanes();
			p.grid.setTargetable(false);
			checkEndCondition();
			if (game.isFinished() && player.state == Player.State.PLACING && startedPlacement) {
				player.state = Player.State.READY;
				Airplanes.game.back();
			}
		}
	}

	@Override
	public synchronized void playerAttackedCell(String username, Point2D cell) {
		if (username.equals(player.name))
			for (Player p : players)
				p.grid.setTargetable(false);
		Player player = getPlayerByName(username);
		for (Player p : players) {
			if (p != player && p.state != Player.State.DEAD && p.state != Player.State.DISCONNECTED)
				p.grid.attackCell(cell.x, cell.y);
		}
		if (player.state == Player.State.LASTSTAND) {
			player.state = Player.State.DEAD;
			checkEndCondition();
		}
	}

	@Override
	public int getGameID() {
		return game.gameID;
	}

	@Override
	public synchronized void beginTurn(int turn) {
		if (turn >= 0) {
			chat.hide();
			if (turn == 0) {
				for (Player p : players)
					p.state = Player.State.PLACING;
				Airplanes.game.setScreen(new PlacementScreen(player));
				startedPlacement = true;
				return;
			}
			else for (Player p : this.players)
				p.grid.setTargetable(player.state != Player.State.DEAD && !game.isFinished());
			player.grid.setTargetable(false);
			game.setTurn(turn);
			selectedCell.x = selectedCell.y = -1;
		}
	}

	@Override
	public synchronized void addPlanes(String playerName, String planeLocations) {
		Player p = getPlayerByName(playerName);
		String[] locs = planeLocations.split("\\;");
		for (int i = 0; i+2 < locs.length; i += 3)
			p.planes.add(new Plane.Location(Integer.parseInt(locs[i])
					, Integer.parseInt(locs[i+1])
					, Plane.Orientation.valueOf(locs[i+2])));
		p.grid.addPlanes(p.planes);
	}
	
	private boolean checked = false;
	public synchronized void checkEndCondition()
	{
		if (checked)
			return;
		
		int dead = 0, dead2 = 0;
		boolean send = false;
		for (Player p : players)
		{
			if (p.state == Player.State.DEAD || p.state == Player.State.DISCONNECTED)
				++dead;
			if (p.state == Player.State.LASTSTAND)
				++dead2;
		}
		dead2 += dead;
		System.out.println("dead: " + dead + " dead2: " + dead2);
		if (dead2 == players.size())
		{
			send = !game.isFinished();
			endGame();
			int max = 0;
			for (Player p : players) {
				max = Math.max(p.turnOfDeath, max);
			}
			ArrayList<String> winners = new ArrayList<String>();
 			for (Player p : players)
				if (p.turnOfDeath == max)
					winners.add(p != player ? p.name : 
						(StringManager.getString(winners.size() > 0 ? "you" : "You")));
 			
 			if (winners.size() < players.size()) {
 				String out = winners.get(0);
	 			for (int i = 1;i < winners.size()-1;++i)
	 				out = out + ", " + winners.get(i);
	 			if (winners.size() >= 2)
	 				out = out + StringManager.getString("and") + winners.get(winners.size()-1);
	 			ActionManager.showLongToast(out + StringManager.getString("won"));
 			}
 			else ActionManager.showLongToast(StringManager.getString("tie"));
		}
		else if (dead2 == players.size() - 1)
		{
			Player alive = null;
			int threat = 0;
			for (Player p : players) {
				if (!(p.state == State.DEAD || p.state == State.LASTSTAND || p.state == State.DISCONNECTED))
					alive = p;
				if (p.state == State.LASTSTAND)
					threat++;
			}
			if (threat < alive.grid.planesLeft()) {
				send = !game.isFinished();
				endGame();
				if (player != alive)
					ActionManager.showLongToast(alive.name + StringManager.getString("won"));
				else ActionManager.showLongToast(StringManager.getString("You") + StringManager.getString("won"));
			}
		}
		else if (dead == players.size() - 1)
		{
			send = !game.isFinished();
			endGame();
			Player alive = null;
			for (Player p : players)
				if (!(p.state == State.DEAD || p.state == State.LASTSTAND || p.state == State.DISCONNECTED))
					alive = p;
			if (player != alive)
				ActionManager.showLongToast(alive.name + StringManager.getString("won"));
			else ActionManager.showLongToast(StringManager.getString("You") + StringManager.getString("won"));
		}
		if (send) {
			try {
				socket.sendMessage(new GameEndMessage(game.gameID));
			}
			catch (IOException e) {
				ActionManager.showLongToast(StringManager.getString("connection_failed"));
				e.printStackTrace();
				Airplanes.game.back();
			}
		}
	}
	
	private void endGame() {
		game.setFinished(true);
		System.out.println("finished");
		checked = true;
		for (Player p : players) {
			p.grid.showPlanes();
			p.grid.setTargetable(false);
		}
	}
}

class ChatView {
	private ScrollPane chatPane, namePane;
	private StringBuilder playerNames = new StringBuilder(), chatText = new StringBuilder();
	private ArrayList<String> players = new ArrayList<String>();
	private Label chatLabel, namesLabel;
	
	ChatView(Stage stage) {
		chatLabel = new Label("", Airplanes.game.skin);
		chatLabel.setWrap(true);
		chatLabel.setAlignment(Align.top | Align.left);
		chatPane = new ScrollPane(chatLabel, Airplanes.game.skin);
		chatText.ensureCapacity(2048);
		
		namesLabel = new Label("",	Airplanes.game.skin);
		namesLabel.setWrap(false);
		namesLabel.setAlignment(Align.top | Align.left);
		namePane = new ScrollPane(namesLabel, Airplanes.game.skin);
		namePane.setScrollingDisabled(false, false);
		namePane.setOverscroll(false, true);

		stage.addActor(chatPane);
		stage.addActor(namePane);
	}
	
	private float x = 0, y = 0, height;
	void setSize(float width, float height) {
		chatPane.setBounds(x, y, width, height*0.8f);
		namePane.setBounds(x, y + height*0.8f, width, height*0.2f);
		LabelStyle ls = chatLabel.getStyle();
		ls.font = FontManager.getFontForHeight(height/30);
		ls.fontColor = Color.WHITE;
		chatLabel.setStyle(ls);
		namesLabel.setStyle(ls);
		this.height = height;
	}
	
	void setPosition(float x, float y) {
		namePane.setPosition(x, y + height*0.8f);
		chatPane.setPosition(x, y);
		this.x = x;
		this.y = y;
	}
	
	void setBounds(float x, float y, float width, float height) {
		setPosition(x, y);
		setSize(width, height);
	}
	
	void addMessage(String message) {
		chatText.append(message);
		chatText.append('\n');
		chatLabel.setText(chatText);
	}
	
	void addPlayer(String playerName) {
		players.add(playerName);
		playerNames.append(playerName);
		playerNames.append('\n');
		namesLabel.setText(playerNames);
	}
	
	void removePlayer(String playerName) {
		if (players.remove(playerName)) {
			playerNames = new StringBuilder();
			for (String p : players) {
				playerNames.append(p);
				playerNames.append('\n');
			}
			namesLabel.setText(playerNames);
		}
	}
	
	void hide() { 
		chatPane.setVisible(false); namePane.setVisible(false);
		Gdx.graphics.setContinuousRendering(false);
		Gdx.graphics.requestRendering();
	}
	void show() { 
		chatPane.setVisible(true); namePane.setVisible(true); 
		Gdx.graphics.setContinuousRendering(true);
	}
}
