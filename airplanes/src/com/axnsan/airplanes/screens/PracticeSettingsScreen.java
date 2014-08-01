package com.axnsan.airplanes.screens;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.GameConfiguration.KillMode;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class PracticeSettingsScreen implements Screen {
	private Stage stage;
	protected Airplanes game;
	private Label gridLabel = null, planesLabel = null;
	private Slider gridSlider = null, planesSlider = null;
	private CheckBox head = null, reveal = null;
	
	public PracticeSettingsScreen()
	{
		this.game = Airplanes.game;
	}
	
	@Override
	public void show() {
		//game.resetCurrentGame();
	}

	@Override
	public void dispose() {
		Airplanes.game.input.removeProcessor(stage);
		
		if (stage != null)
			stage.dispose();
		
		stage = null;
		gridSlider = planesSlider = null;
		gridLabel = planesLabel = null;
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		if (gridLabel != null && gridSlider != null && planesSlider != null && planesLabel != null)
		{
			int gridSize = (int) gridSlider.getValue();
			int numPlanes = Math.min((int) planesSlider.getValue(), game.maxNumPlanes(gridSize));
			String labelText = StringManager.getString("grid_size").replace("%d", Integer.toString(gridSize));
			gridLabel.setText(labelText);
			planesSlider.setRange(1, game.maxNumPlanes(gridSize));
			planesSlider.setValue(numPlanes);
			labelText = StringManager.getString("num_planes").replace("%d", Integer.toString(numPlanes));
			planesLabel.setText(labelText);
		}
		
		stage.act(delta);
		stage.draw();
		
		try {
			Thread.sleep(Math.max(0, (long) (Airplanes.MAX_FRAME_TIME - Gdx.graphics.getRawDeltaTime())));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	protected void proceed()
	{
		game.setScreen(new PracticeScreen());
	}
	@Override
	public void resize(int width, int height)
	{
		if (stage != null) {
			Airplanes.game.input.removeProcessor(stage);
			stage.dispose();
		}
		
		stage = new Stage();
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		
		Table table = new Table();
		table.setSkin(game.skin);
		table.setFillParent(true);
		stage.addActor(table);
		table.align(Align.top);
		table.padTop(h/5);
		

		game.setCheckBoxFont(FontManager.getFontForHeight(h/25));
		reveal = new CheckBox("  " + StringManager.getString("reveal_dead"), game.skin);
		reveal.align(Align.left);
		reveal.setChecked(game.config.revealDeadPlanes);
		reveal.getCells().get(0).size(h/25);
		table.add(reveal).height(h/20).pad(3);
		table.row();
		
		head = new CheckBox("  " + StringManager.getString("headshots"), game.skin);
		head.setColor(Color.BLACK);
		head.setChecked(game.config.killMode == KillMode.Headshot);
		head.getCells().get(0).size(h/25);
		table.add(head).height(h/20).pad(3);
		table.row();
		
		game.setLabelFont(FontManager.getFontForHeight(h/20));
		gridLabel = new Label(""/*String.format(StringManager.getString("grid_size"), game.config.gridSize)*/, game.skin);
		table.add(gridLabel).pad(3);
		table.row();
		
		gridSlider = new Slider(game.minGridSize(), PracticeScreen.maxGridSize(), 1, false, game.skin);
		gridSlider.setValue(game.config.gridSize);
		table.add(gridSlider).width(w/2);
		table.row();
		
		planesLabel = new Label(/*String.format(StringManager.getString("num_planes"), game.config.numPlanes)*/"", game.skin);
		table.add(planesLabel);
		table.row();
		
		planesSlider = new Slider(1, game.maxNumPlanes(), 1, false, game.skin);
		planesSlider.setValue(game.config.numPlanes);
		table.add(planesSlider).width(w/2);
		table.row();
		

		game.setTextButtonFont(FontManager.getFontForHeight(h/20));
		TextButton nextButton = new TextButton(StringManager.getString("start"), game.skin);
		nextButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				game.config.gridSize = (int) gridSlider.getValue();
				game.config.killMode = (head.isChecked())?KillMode.Headshot:KillMode.Full;
				game.config.numPlanes = (int) planesSlider.getValue();
				game.config.revealDeadPlanes = reveal.isChecked();
				

				game.back(); //Pop the settings screen off the stack
				proceed(); //Start the game
		    }
		});
		stage.addActor(nextButton);
		
		TextButton backButton = new TextButton(StringManager.getString("back"), game.skin);
		backButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				game.back();
		    }
		});
		stage.addActor(backButton);
		
		backButton.setBounds(10, 10, w/2 - 15, h/10);
		nextButton.setBounds(Gdx.graphics.getWidth() + 5 - w/2, 10, w/2 - 15, h/10);
		Airplanes.game.input.addProcessor(stage);
		Gdx.graphics.requestRendering();
	}
	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	@Override
	public void hide() {
		game.config.dumpConfig();
		dispose();
	}

}
