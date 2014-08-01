package com.axnsan.airplanes.screens;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class MainMenuScreen implements Screen {
	private Stage stage;
	private Airplanes game;
	private Table table;
	private TextButton exitButton;
	
	public MainMenuScreen()
	{
		this.game = Airplanes.game;
		stage = new Stage();
		float h = Gdx.graphics.getHeight();
		
		table = new Table();
		table.setFillParent(true);
		stage.addActor(table);
		table.align(Align.top);
		table.padTop(h/5);
		
		exitButton = new TextButton(StringManager.getString("main_exit"), game.skin);
		exitButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				game.exit();
		    }
		});
		stage.addActor(exitButton);
	}
	
	@Override
	public void show() {
		game.setScreen(new PlayMenuScreen());
		/*game.input.clear();
		game.input.addProcessor(game);
		game.input.addProcessor(stage);*/
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
		
		if (stage != null) {
			stage.act(delta);
			stage.draw();
		}
		
		try {
			Thread.sleep(Math.max(0, (long) (Airplanes.MAX_FRAME_TIME - Gdx.graphics.getRawDeltaTime())));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	 @Override
	public void resize(int width, int height)
	{
		 stage.setViewport(width, height);
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		table.clearChildren();
		
		game.setTextButtonFont(FontManager.getFontForHeight(h/10 - h/20));
		TextButton startGameButton = new TextButton(StringManager.getString("main_play"), game.skin);
		startGameButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				game.setScreen(new PlayMenuScreen());
		    }
		});
		table.add(startGameButton).pad(5).width(w/2).height(h/10);
		
		table.row();
		
		TextButton optionsButton = new TextButton(StringManager.getString("main_options"), game.skin);
		table.add(optionsButton).pad(5).width(w/2).height(h/10);
		
		table.row();

		exitButton.setBounds(w/4, h/5, w/2, h/10);
		exitButton.setStyle(game.skin.get(TextButtonStyle.class));
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
	}

}
