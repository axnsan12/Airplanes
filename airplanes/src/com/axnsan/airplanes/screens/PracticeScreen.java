package com.axnsan.airplanes.screens;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.GameState;
import com.axnsan.airplanes.Grid;
import com.axnsan.airplanes.GameState.GameMode;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.Point2D;
import com.axnsan.airplanes.util.StringManager;
import com.axnsan.airplanes.MatchHandler;
import com.axnsan.airplanes.Player;
import com.axnsan.airplanes.PlayingGrid;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;


public class PracticeScreen implements Screen, MatchHandler {
	private SpriteBatch batch;
	private Airplanes game;
	private BitmapFont font;
	private Stage stage;
	private PlayingGrid grid;
	private static final int minPadding = Airplanes.minPadding, gridBorderWidth = Airplanes.gridBorderWidth;
	private Player player = null;
	
	public PracticeScreen() {
		this.game = Airplanes.game;
		
		game.state = new GameState("PRACTICE");
		game.state.activeGameMode = GameMode.Practice;
		/*if (startNew) {
			for (Player p : game.players)
				p.dispose();

			game.players.clear();
		}*/
		game.input.removeProcessor(grid);
		if (grid != null)
			grid.dispose();
		
		grid = new PlayingGrid(0, 1, game.config.gridSize, this, 0);
		grid.setTargetable(true);
		grid.hidePlanes();
		if (game.players.size() == 0) {
			grid.randomize();
			player = new Player(null, grid.dumpPlaneLocations(), 0);
			game.players.add(player);
		}
		else {
			if (game.state.activeGameMode != GameMode.Practice) 
				throw new RuntimeException("Attempting to resume practice while the active game mode is not Practice");
			
			player = game.players.get(0);
			System.out.println("resume " + player.moves.size() +  " " + player.planes.size());
			if (!grid.addPlanes(player.planes)) {
				System.out.println("failed to add");
			}
			grid.addMoves(player.moves);
		}
		game.input.addProcessor(grid);
		Gdx.graphics.requestRendering();
		
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1,1,1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		batch.begin();
		{
			if (grid.getCellSize() < Grid.MIN_CELLSIZE_PX()) {
				FontManager.getFontForHeight(Gdx.graphics.getHeight() / 10).drawWrapped(batch, 
						"Your screen is too small to render a grid of this size", 
						20, Gdx.graphics.getHeight() - 50, Gdx.graphics.getWidth() - 40);
			} else {
				font.drawMultiLine(batch, "Moves: " + player.moves.size() + "\n" 
					+ "Planes left: " + grid.planesLeft(), grid.getX(), grid.getY() - Gdx.graphics.getHeight()/30);
				grid.draw(batch, 1.f);
			}
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
		
		int availableHeight = Gdx.graphics.getHeight() - minPadding;
		int availableWidth = Gdx.graphics.getWidth() - 2*minPadding;
		availableHeight -= height/10;
		int size = game.config.gridSize;
		int maxCellWidth = (availableWidth - (size+1)*gridBorderWidth) / (size);
		int maxCellHeight = (availableHeight - (size)*gridBorderWidth) / (size);
			
		if (maxCellWidth < maxCellHeight) {
			/*If width is the limiting factor, keep the grid to the top of the window*/
			grid.setCellSize(maxCellWidth);
			grid.setPosition((Gdx.graphics.getWidth() - grid.getWidth()) / 2, Gdx.graphics.getHeight() - grid.getHeight() - minPadding);
		}
		else {
			/*If height is the limiting factor, keep the grid to the top left of the window*/
			grid.setCellSize(maxCellHeight);
			grid.setPosition(minPadding, Gdx.graphics.getHeight() - minPadding - grid.getHeight());
		}
		
		int buttonPad = height/100;
		int buttonHeight = height/10;
		
		game.setTextButtonFont(FontManager.getFontForHeight(buttonHeight/1.7f));
		TextButton backButton = new TextButton(StringManager.getString("back"), game.skin);
		backButton.setBounds(grid.getX() , buttonPad, grid.getWidth() / 2.3f, buttonHeight);
		backButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				game.back();
		    }
		});
		stage.addActor(backButton);
		
		font = FontManager.getFontForHeight(buttonHeight/1.7f);
		Gdx.graphics.requestRendering();
	}

	@Override
	public void show() {
	}

	@Override
	public void hide() {
		game.input.removeProcessor(stage);
		game.input.removeProcessor(grid);
		game.state.dumpState();
		dispose();
	}

	@Override
	public void pause() {
		game.state.dumpState();
	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void dispose() {
		game.input.removeProcessor(stage);
		game.input.removeProcessor(grid);
		
		if (stage != null)
			stage.dispose();
		if (batch != null)
			batch.dispose();
		if (grid != null)
			grid.dispose();
		
		grid = null;
		stage = null;
		font = null;
		batch = null;
	}

	@Override
	public void playerWasAttacked(int playerID, Point2D cell) {
		if (player != null)
			player.moves.add(cell);
	}

	@Override
	public void playerWasAttacked(int playerID, int gridX, int gridY) {
		playerWasAttacked(playerID, new Point2D(gridX, gridY));
	}

	@Override
	public void playerDied(int playerID) {
		System.out.println("you won!");
		
	}

	@Override
	public void cellSelected(Point2D cell) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void cellSelected(int gridX, int gridY) {
		// TODO Auto-generated method stub
		
	}

}
