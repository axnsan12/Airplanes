package com.axnsan.airplanes.screens;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.GameState;
import com.axnsan.airplanes.GameState.GameMode;
import com.axnsan.airplanes.Grid;
import com.axnsan.airplanes.GuardedScreen;
import com.axnsan.airplanes.HotseatMatchHandler;
import com.axnsan.airplanes.Player;
import com.axnsan.airplanes.PlayingGrid;
import com.axnsan.airplanes.util.ActionManager;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.Point2D;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;


public class HotseatScreen implements GuardedScreen {
	private SpriteBatch batch;
	private Airplanes game;
	private Stage stage;
	private static final int minPadding = Airplanes.minPadding, gridBorderWidth = Airplanes.gridBorderWidth;
	private HotseatMatchHandler handler;
	public boolean constructed = false;
	
	public TextButton endTurnButton, toggleGridButton, nextGridButton;
	
	public HotseatScreen() {
		this.game = Airplanes.game;
		
		game.state = new GameState("HOTSEAT");
		game.state.activeGameMode = GameMode.Hotseat;

		handler = new HotseatMatchHandler(this);
		if (game.players.size() == 0) {
			for (int i = 0;i < game.config.numPlayers;++i) {
				PlayingGrid grid = new PlayingGrid(0, 1, game.config.gridSize, handler, i);
				grid.setTargetable(true);
				grid.hidePlanes();
				grid.randomize();
				grid.hide();
				Player player = new Player(grid, grid.dumpPlaneLocations(), i);
				game.players.add(player);
			}
		}
		else {
			if (game.state.activeGameMode != GameMode.Hotseat) 
				throw new RuntimeException("Attempting to resume hotseat while the active game mode is not Hotseat");
			
			for (int i = 0;i < game.players.size(); ++i) {
				Player p = game.players.get(i);
				if (p.grid != null)
				{
					game.input.removeProcessor(p.grid);
					p.grid.dispose();
				}
				
				p.grid = new PlayingGrid(0, 1, game.config.gridSize, handler, i);
				p.grid.setTargetable(true);
				p.grid.hidePlanes();
				p.grid.hide();
				p.grid.addPlanes(p.planes);
			}
			ActionManager.showProgressDialog(StringManager.getString("loading"));
			int turn = 0;
			boolean done = false;
			while (!done) {
				done = true;
				for (int i = 0;i < game.players.size(); ++i) {
					Player p1 = game.players.get(i);
					if (p1.moves.size() > turn) {
						done = false;
						Point2D cell = p1.moves.get(turn);
						int x = cell.x, y = cell.y;
						for (Player p2 : game.players)
							if (p2 != p1)
								p2.grid.attackCell(x, y);
					}
				}
				++turn;
			}
			ActionManager.dismissProgressDialog();
		}
		Gdx.graphics.requestRendering();
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1,1,1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		batch.begin();
		{
			handler.draw(batch, delta);
			//font.draw(batch, "" + frame, Gdx.graphics.getWidth() - 50, 20);
		}
		batch.end();
		
		stage.draw();
		
		try {
			Thread.sleep(Math.max(0, (long) (Airplanes.MAX_FRAME_TIME - Gdx.graphics.getRawDeltaTime())));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static int maxGridSize() {
		int availableHeight = Gdx.graphics.getHeight() - minPadding;
		int availableWidth = Gdx.graphics.getWidth() - 2*minPadding;
		availableHeight -= 25; //Leave vertical space for at least a 20px button row with 5px padding
		availableHeight -= 25; //Another row for arrow buttons on top
		int maxSize = (int) Math.min(availableWidth/((float) (Grid.MIN_CELLSIZE_PX() + gridBorderWidth)), 
				availableHeight/((float) Grid.MIN_CELLSIZE_PX() + gridBorderWidth));
		return Math.min(maxSize, Airplanes.game.maxGridSize());
	}
	
	@Override
	public void resize(int width, int height) {
		if (batch != null)
			batch.dispose();
		batch = new SpriteBatch();
		
		if (stage != null)
			stage.dispose();
		stage = new Stage();
		Airplanes.game.input.addProcessor(stage);
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
		for (Player p : game.players) {
			p.grid.setPosition(shownGrid.getX(), shownGrid.getY());
			p.grid.setCellSize(shownGrid.getCellSize());
		}
		
		game.setTextButtonFont(FontManager.getFontForHeight(buttonHeight/2.2f));
		TextButton backButton = new TextButton(StringManager.getString("back"), game.skin);
		backButton.setBounds(shownGrid.getX() , shownGrid.getY() - buttonHeight - buttonPad, //x, y
				shownGrid.getWidth() / 4.f - 2, buttonHeight); //width, height
		backButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				game.back();
		    }
		});
		stage.addActor(backButton);
		
		toggleGridButton = new TextButton(StringManager.getString("show_mine"), game.skin);
		toggleGridButton.setBounds(shownGrid.getX() + shownGrid.getWidth() / 4.f + 2,  //x
				shownGrid.getY() - buttonHeight - buttonPad, //y
				shownGrid.getWidth() * 5f/12f - 4, buttonHeight); //width, height
		toggleGridButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				handler.toggleShownGrid();
		    }
		});
		toggleGridButton.setVisible(game.state.finished == false);
		toggleGridButton.setText(StringManager.getString((handler.showMy)? "show_enemy" : "show_mine"));
		stage.addActor(toggleGridButton);
		
		endTurnButton = new TextButton(StringManager.getString("end_turn"), game.skin);
		endTurnButton.setBounds(shownGrid.getX() + shownGrid.getWidth() - shownGrid.getWidth() / 3.f + 2, //x
							shownGrid.getY() - buttonHeight - buttonPad, //y
							shownGrid.getWidth() / 3.f - 2, buttonHeight); //width, height
		endTurnButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				handler.endTurn();
		    }
		});
		endTurnButton.setVisible(handler.canCurrentPlayerEndTurn() && game.state.finished == false);
		stage.addActor(endTurnButton);
		
		nextGridButton = new TextButton(">", game.skin);
		nextGridButton.setBounds(shownGrid.getX() + shownGrid.getWidth() - width/7, 
				Gdx.graphics.getHeight() - h/10 - buttonPad, 
				width/7, h/10);
		nextGridButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				handler.showNextGrid();
		    }
		});
		nextGridButton.setVisible((!handler.showMy && game.state.finished == false) || game.state.finished == true);
		stage.addActor(nextGridButton);
		
		
		Gdx.graphics.requestRendering();
		constructed = true;
	}

	@Override
	public void show() {
		handler.show(this);
	}

	@Override
	public void hide() {
		game.input.removeProcessor(stage);
		game.state.dumpState();
		handler.hide(this);
	}

	@Override
	public void pause() {
		game.state.dumpState();
		handler.pause(this);
	}

	@Override
	public void resume() {
		handler.resume(this);
	}

	@Override
	public void dispose() {
		game.input.removeProcessor(stage);
		
		System.out.println("dispose");
		handler.dispose();
		for (Player p : game.players) {
			if (p.grid != null)
			{
				game.input.removeProcessor(p.grid);
				p.grid.dispose();
			}
			p.grid = null;
		}
		if (stage != null)
			stage.dispose();
		if (batch != null)
			batch.dispose();
		
		stage = null;
		batch = null;
	}

	@Override
	public void liftGuard() {
		handler.liftGuard();
	}

	

}
