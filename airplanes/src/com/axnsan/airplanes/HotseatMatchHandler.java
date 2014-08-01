package com.axnsan.airplanes;

import java.util.ArrayList;

import com.axnsan.airplanes.Player.State;
import com.axnsan.airplanes.screens.BeginTurnScreen;
import com.axnsan.airplanes.screens.HotseatScreen;
import com.axnsan.airplanes.screens.PlacementScreen;
import com.axnsan.airplanes.util.ActionManager;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.Point2D;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Touchable;

public class HotseatMatchHandler implements MatchHandler {
	private Player player;
	private PlayingGrid shownGrid;
	private int currentGrid = 0;
	private final Airplanes game;
	private HotseatScreen screen;
	
	public HotseatMatchHandler(HotseatScreen screen)
	{
		game = Airplanes.game;
		this.screen = screen;
	}

	public void draw(SpriteBatch batch, float delta)
	{
		if (shownGrid == null)
			return;
		
		if (shownGrid.getCellSize() < Grid.MIN_CELLSIZE_PX()) {
			FontManager.getFontForHeight(Gdx.graphics.getHeight() / 10).drawWrapped(batch, 
					"Your screen is too small to render a grid of this size", 
					20, Gdx.graphics.getHeight() - 50, Gdx.graphics.getWidth() - 40);
		} else {
			shownGrid.draw(batch, 1.f);
			int h = Gdx.graphics.getHeight();
			int fontHeight = h/20;
			float wrapWidth = shownGrid.getWidth() - screen.nextGridButton.getWidth();
			BitmapFont font = FontManager.getFontForHeight(fontHeight);
			String text = shownGrid.getLabel() + "\n";
			if (shownGrid.planesLeft() > 1)
				text += StringManager.getString("%dplanes_left").replace
					("%d", Integer.toString(shownGrid.planesLeft()));
			else if (shownGrid.planesLeft() == 1)
				text += StringManager.getString("one_plane_left");
			else text += StringManager.getString("turn_death%d").replace
					("%d", Integer.toString(game.players.get(shownGrid.getPlayerID()).turnOfDeath));
			while (font.getWrappedBounds(text, wrapWidth).height > h*0.09f)
				font = FontManager.getFontForHeight(--fontHeight);
			font.setColor(Color.BLACK);
			font.drawWrapped(batch, text, 0, Gdx.graphics.getHeight() - 2 - h/100, wrapWidth, HAlignment.CENTER);
			
			if (game.state.finished == false)
				text = "Turn " + game.state.turnNumber + " - " + player.name;
			else text = StringManager.getString("finished_turn%d").replace("%d", Integer.toString(game.state.turnNumber));
			fontHeight = h/20;
			font = FontManager.getFontForHeight(h/20);
			wrapWidth = Gdx.graphics.getWidth() * 0.95f;
			while (font.getWrappedBounds(text, wrapWidth).height > fontHeight)
				font = FontManager.getFontForHeight(--fontHeight);
			font.drawWrapped(batch, text, shownGrid.getX(), shownGrid.getY() - h/10 - h/100 - h/30, wrapWidth);
		}
	}
	
	@Override
	public void playerWasAttacked(int playerID, Point2D cell) {
		System.out.println("Player " + game.state.currentPlayer + " attacked cell " + cell.x + " " + cell.y);
		for (Player p : game.players)
			if (p != player)
				p.grid.attackCell(cell.x, cell.y);
		game.players.get(game.state.currentPlayer).moves.add(cell);
		
		if (player != null)
		{
			if (player.movesLeft <= 0)
				throw new RuntimeException("Player " + playerID + " attacked with no moves left");
			
			player.movesLeft--;
			if (player.movesLeft <= 0)
			{
				for (Player p : game.players)
					p.grid.setTargetable(false);
				if (!game.state.finished)
				{
					screen.endTurnButton.setVisible(true);
					screen.endTurnButton.setTouchable(Touchable.enabled);
				}
			}
		}
	}
	
	private boolean checked = false;
	public void checkEndCondition()
	{
		if (!screen.constructed || checked)
			return;
		
		int dead = 0, dead2 = 0;
		for (Player p : game.players)
		{
			if (p.state == Player.State.DEAD || p.state == Player.State.DISCONNECTED)
				++dead;
			if (p.state == Player.State.LASTSTAND)
				++dead2;
		}
		dead2 += dead;
		System.out.println("dead: " + dead + " dead2: " + dead2);
		if (dead2 == game.players.size())
		{
			game.state.finished = true;
			screen.toggleGridButton.setVisible(false);
			screen.nextGridButton.setVisible(true);
			System.out.println("finished");
			checked = true;
			int max = 0;
			for (Player p : game.players) {
				max = Math.max(p.turnOfDeath, max);
			}
			ArrayList<Player> winners = new ArrayList<Player>();
 			for (Player p : game.players)
				if (p.turnOfDeath == max)
					winners.add(p);
 			String out = winners.get(0).name;
 			for (int i = 1;i < winners.size()-1;++i)
 				out = out + ", " + winners.get(i).name;
 			if (winners.size() >= 2)
 				out = out + StringManager.getString("and") + winners.get(winners.size()-1).name;
 			ActionManager.showLongToast(out + StringManager.getString("won"));
		}
		else if (dead2 == game.players.size() - 1)
		{
			Player alive = null;
			int threat = 0;
			for (Player p : game.players) {
				if (!(p.state == State.DEAD || p.state == State.LASTSTAND || p.state == State.DISCONNECTED))
					alive = p;
				if (p.state == State.LASTSTAND)
					threat++;
			}
			if (threat < alive.grid.planesLeft()) {
				game.state.finished = true;
				screen.toggleGridButton.setVisible(false);
				screen.nextGridButton.setVisible(true);
				System.out.println("finished");
				checked = true;
				ActionManager.showLongToast(alive.name + StringManager.getString("won"));
			}
		}
		else if (dead == game.players.size() - 1)
		{
			game.state.finished = true;
			screen.toggleGridButton.setVisible(false);
			screen.nextGridButton.setVisible(true);
			System.out.println("finished");
			checked = true;
			for (Player p : game.players)
				if (!(p.state == State.DEAD || p.state == State.LASTSTAND || p.state == State.DISCONNECTED))
					ActionManager.showLongToast(p.name + " won.");
		}
	}
	@Override
	public void playerDied(int playerID) {
		if (game.state.finished)
			return;
		
		if (playerID > game.state.currentPlayer)
			game.players.get(playerID).state = Player.State.LASTSTAND;
		else game.players.get(playerID).state = Player.State.DEAD;
		game.players.get(playerID).turnOfDeath = game.state.turnNumber;
		
		checkEndCondition();
	}

	@Override
	public void cellSelected(Point2D cell) {
		if (player == null)
			return;
		
		player.selectedCell.x = cell.x;
		player.selectedCell.y = cell.y;
	}
	
	public void nextPlayer() {
		int pid = game.state.currentPlayer;
		if (!game.state.finished)
		{
			if (showMy)
				toggleShownGrid();
			do {
				++pid;
				if (pid >= game.players.size())
				{
					pid = 0;
					++game.state.turnNumber;
				}
			} while (game.players.get(pid).state == Player.State.DEAD 
					|| game.players.get(pid).state == Player.State.DISCONNECTED);
		}
		setCurrentPlayer(pid);
	}
	
	public void endTurn()
	{
		if (player == null)
			return;
		
		player.endTurn();
		game.players.get(game.state.currentPlayer).grid.hidePlanes();
		if (shownGrid != null)
		{
			shownGrid.hide();
			game.input.removeProcessor(shownGrid);
		}
		nextPlayer();
	}
	
	public boolean canCurrentPlayerEndTurn()
	{
		if (player != null)
			return player.movesLeft <= 0;
		
		return false;
	}
	
	private void setShownGrid(int playerID)
	{
		if (playerID < 0 || playerID >= game.config.numPlayers)
			return;
		
		if ((!showMy && playerID == game.state.currentPlayer && game.state.finished == false))
		{
			toggleShownGrid();
			return;
		}
		
		if (shownGrid != null)
		{
			shownGrid.hide();
			game.input.removeProcessor(shownGrid);
		}
		
		currentGrid = playerID;
		shownGrid = game.players.get(currentGrid).grid;
		shownGrid.show();
		if (game.state.finished == true)
		{
			shownGrid.showPlanes();
			shownGrid.setTargetable(false);
		}
		else if (player.selectedCell != null && player.selectedCell.x >= 0 && player.selectedCell.y >= 0)
			shownGrid.selectCell(player.selectedCell);
		game.input.addProcessor(shownGrid);
		game.state.focusedGrid = playerID;
	}
	
	public void showNextGrid()
	{
		if (!game.state.finished)
		{
			do {
				currentGrid = (currentGrid+1)%game.config.numPlayers;
			} while (currentGrid == game.state.currentPlayer);
		}
		else 
		{
			for (Player p : game.players)
				p.grid.showPlanes();
			
			currentGrid = (currentGrid+1)%game.config.numPlayers;
		}
		setShownGrid(currentGrid);
	}
	
	@Override
	public void playerWasAttacked(int playerID, int gridX, int gridY) {
		playerWasAttacked(playerID, new Point2D(gridX, gridY));
	}
	@Override
	public void cellSelected(int gridX, int gridY) {
		cellSelected(new Point2D(gridX, gridY));
	}

	public void setCurrentPlayer(int playerID)
	{
		setCurrentPlayer(playerID, false);
	}
	
	
	boolean requestedPlacement = false;
	public void setCurrentPlayer(int playerID, boolean resume) {
		if (player != null)
		{
			player.grid.hidePlanes();
			player.grid.setTargetable(true);
		}
		
		requestedPlacement = false;
		game.state.currentPlayer = playerID;
		player = game.players.get(playerID);
		checkEndCondition();
		
		if (player.state == Player.State.PLACING || player.state == Player.State.NONE)
		{
			if (player.moves.size() > 0)
				throw new RuntimeException("Player " + playerID + " has move history and is in placement phase");
			
			requestedPlacement = true;
			game.setScreen(new PlacementScreen(player));
			game.setScreen(new BeginTurnScreen(StringManager.getString("%splayer_place").replace("%s", player.name), screen, resume));
			return;
		}
		if (game.state.finished == false &&
				(player.state == Player.State.DEAD || player.state == Player.State.DISCONNECTED))
			throw new RuntimeException("Dead players can't play");
		
		if (!resume)
			player.beginTurn();
		
		for (Player p : game.players)
		{
			if (game.state.finished)
				p.grid.showPlanes();
			else p.grid.hidePlanes();
			p.grid.setTargetable(player.movesLeft > 0 && game.state.finished == false);
		}
		player.grid.showPlanes();
		player.grid.setTargetable(false);
		if (game.state.finished == false) {
			int grid = playerID;
			while (playerID == grid || game.players.get(grid).grid.planesLeft() == 0)
				grid = (grid + 1)%game.players.size();
			setShownGrid(grid);
		}
		else setShownGrid(0);
		game.setScreen(new BeginTurnScreen(player.name + "'s turn", screen, resume));
		prompted = true;
		if (game.state.finished)
			checkEndCondition();
	}
	
	public void dispose() {
		for (Player p : game.players)
		{
			game.input.removeProcessor(p.grid);
		}
	}
	
	private boolean safeToShow = false, prompted = false;
	
	public void show(HotseatScreen screen) {
		this.screen = screen;
		if (game.state.finished == true) {
			
		}
		if (player == null)
		{
			if (game.state.currentPlayer == -1)
			{
				setCurrentPlayer(0);
				return;
			}
			else
			{
				int grid = game.state.focusedGrid;
				setCurrentPlayer(game.state.currentPlayer, true);
				setShownGrid(grid);
				return;
			}
		}
		
		if (!player.isTurn())
		{
			if (player.state != Player.State.PLACING && player.state != Player.State.NONE)
			{
				nextPlayer();
			}
			else if (!requestedPlacement)
				setCurrentPlayer(game.state.currentPlayer);
			else game.back();
			return;
		}
		
		if (!safeToShow && !prompted)
		{
			game.setScreen(new BeginTurnScreen(StringManager.getString("%splayer_turn").replace("%s", player.name), screen, true));
			prompted = true;
			return;
		}
		if (!safeToShow && prompted && game.getScreen() == screen)
		{
			prompted = false;
			return;
		}
		
		game.input.clear();
		game.input.addProcessor(game);
		if (shownGrid != null)
			game.input.addProcessor(shownGrid);
	}

	
	public void hide(HotseatScreen screen) {
		this.screen = screen;
		game.input.removeProcessor(shownGrid);
		safeToShow = false;
	}

	public void pause(HotseatScreen screen) {
		this.screen = screen;
		safeToShow = false;
		game.setScreen(new BeginTurnScreen(player.name + "'s turn", screen, true));
		prompted = true;
	}

	public void resume(HotseatScreen screen) {
		this.screen = screen;
		System.out.println("resume");
		screen.nextGridButton.setVisible((!showMy) || game.state.finished == true);
		screen.toggleGridButton.setText(StringManager.getString((showMy)? "show_enemy" : "show_mine"));
		if (!safeToShow && prompted)
		{
			prompted = false;
		}
	}
	
	public void liftGuard()
	{
		safeToShow = true;
		prompted = false;
	}
	
	public boolean showMy = false;
	private int oldShown = -1;
	public void toggleShownGrid() {
		if (game.state.finished)
		{
			screen.nextGridButton.setVisible(true);
			screen.toggleGridButton.setVisible(false);
			return;
		}
		
		showMy ^= true;
		if (screen != null && screen.nextGridButton != null && screen.toggleGridButton != null)
		{
			screen.nextGridButton.setVisible((!showMy) || game.state.finished == true);
			screen.toggleGridButton.setText(StringManager.getString((showMy)? "show_enemy" : "show_mine"));
		}
		if (showMy)
		{
			oldShown = game.state.focusedGrid;
			setShownGrid(game.state.currentPlayer);
		}
		else setShownGrid(oldShown);
	}
}
