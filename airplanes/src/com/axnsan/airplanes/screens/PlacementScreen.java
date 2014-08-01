package com.axnsan.airplanes.screens;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.PlacementGrid;
import com.axnsan.airplanes.Player;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class PlacementScreen implements Screen {

	private Airplanes game;
	private SpriteBatch batch;
	private PlacementGrid grid;
	private Stage stage;
	private Player player;
	
	/**Minimum distance from edges of screen**/
	private static final int minPadding = Airplanes.minPadding;
	
	/**Border width higher than 1 is not currently supported**/
	private static final int gridBorderWidth = Airplanes.gridBorderWidth;
	
	public PlacementScreen(Player player) {
		this.game = Airplanes.game;
		this.player = player;
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1,1,1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		batch.begin();
		//font.draw(batch, "" + frame, Gdx.graphics.getWidth() - 50, 20);
		batch.end();

		if (stage != null) {
			stage.act(delta);
			stage.draw();
		}
		
		if (grid != null)
			grid.draw(batch, 1.f);
		
		try {
			Thread.sleep(Math.max(0, (long) (Airplanes.MAX_FRAME_TIME - Gdx.graphics.getRawDeltaTime())));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
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
		TextButton randomButton = new TextButton(StringManager.getString("random"), game.skin);
		randomButton.setBounds(grid.getX(), grid.getY() - buttonHeight - buttonPad, grid.getWidth() / 2.3f, buttonHeight);
		randomButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				grid.randomize();
		    }
		});
		stage.addActor(randomButton);
		
		TextButton doneButton = new TextButton(StringManager.getString("done"), game.skin);
		doneButton.setBounds(grid.getX() + grid.getWidth() - grid.getWidth() / 2.3f, grid.getY() - buttonHeight - buttonPad, grid.getWidth() / 2.3f, buttonHeight);
		doneButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				player.state = Player.State.READY;
				game.back();
		    }
		});
		stage.addActor(doneButton);
		
		Gdx.graphics.requestRendering();
	}
	
	@Override
	public void show() {
		grid = new PlacementGrid(0, gridBorderWidth, game.config.gridSize);
		Airplanes.game.input.addProcessor(grid);
		if (player == null || player.planes == null || player.planes.size() < game.config.numPlanes)
			grid.randomize();
		else grid.addPlanes(player.planes);
		Gdx.graphics.requestRendering();
	}

	@Override
	public void hide() {
		Airplanes.game.input.removeProcessor(grid);
		Airplanes.game.input.removeProcessor(stage);
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {
		Airplanes.game.input.removeProcessor(grid);
		Airplanes.game.input.removeProcessor(stage);
		
		if (batch != null)
			batch.dispose();
		if (grid != null)
		{
			player.planes = grid.dumpPlaneLocations();
			if (player.grid == null || !player.grid.addPlanes(player.planes))
				throw new RuntimeException("Failed to add planes");
			grid.dispose();
			if (game.state != null)
				game.state.dumpState();
		}
		if (stage != null)
			stage.dispose();
		
		grid = null;
		batch = null;
		stage = null;
	}

}
