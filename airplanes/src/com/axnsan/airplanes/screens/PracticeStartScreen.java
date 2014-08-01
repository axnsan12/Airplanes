package com.axnsan.airplanes.screens;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.GameConfiguration;
import com.axnsan.airplanes.GameState;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class PracticeStartScreen implements Screen {
	private Stage stage;
	private Airplanes game;
	
	public PracticeStartScreen()
	{
		this.game = Airplanes.game;
	}
	
	@Override
	public void show() {
	}

	@Override
	public void dispose() {
		Airplanes.game.input.removeProcessor(stage);
		
		if (stage != null)
			stage.dispose();
		
		stage = null;
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
		
		try {
			Thread.sleep(Math.max(0, (long) (Airplanes.MAX_FRAME_TIME - Gdx.graphics.getRawDeltaTime())));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	 @Override
	public void resize(int width, int height)
	{
		if (stage != null)
			stage.dispose();
		
		stage = new Stage();
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		
		Table table = new Table();
		table.setFillParent(true);
		stage.addActor(table);
		table.align(Align.top);
		table.padTop(h/5);
		

		game.setTextButtonFont(FontManager.getFontForHeight(h/10 - h/20));
		TextButton resumeButton = new TextButton(StringManager.getString("resume"), game.skin);
		resumeButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				game.config = new GameConfiguration("PRACTICE");
				game.setScreen(new PracticeScreen());
		    }
		});
		if (GameState.isValidState("PRACTICE")) {
			table.add(resumeButton).pad(5).width(w/2).height(h/10);
			table.row();
		}
		
		table.row();
		TextButton newGameButton = new TextButton(StringManager.getString("new_game"), game.skin);
		newGameButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) 
			{
				GameState.ResetState("PRACTICE");
				game.config = new GameConfiguration("PRACTICE");
				game.setScreen(new PracticeSettingsScreen());
			}
		});
		table.add(newGameButton).pad(5).width(w/2).height(h/10);

		table.row();
		
		
		
		TextButton backButton = new TextButton(StringManager.getString("back"), game.skin);
		backButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				game.back();
		    }
		});
		stage.addActor(backButton);
		backButton.setBounds(w/4, h/5, w/2, h/10);
		
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
		Airplanes.game.input.removeProcessor(stage);
		dispose();
	}

}
