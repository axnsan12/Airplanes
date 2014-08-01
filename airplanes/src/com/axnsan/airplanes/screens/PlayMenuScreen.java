package com.axnsan.airplanes.screens;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.online.LoginScreen;
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

public class PlayMenuScreen implements Screen {
	private Stage stage;
	private Airplanes game;
	
	public PlayMenuScreen()
	{
		this.game = Airplanes.game;
	}
	
	@Override
	public void show() {
		game.input.clear();
		game.input.addProcessor(game);
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
		

		game.setTextButtonFont(FontManager.getFontForHeight(h/20));
		/*if (game.state.activeGameMode != GameMode.None)
			table.add(resumeButton).pad(5).width(w/2).height(h/10);*/
		
		table.row();
		TextButton newGameButton = new TextButton(StringManager.getString("practice"), game.skin);
		newGameButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) 
			{
				game.setScreen(new PracticeStartScreen());
			}
		});
		table.add(newGameButton).pad(5).width(w/2).height(h/10);
		
		table.row();
		
		TextButton hotseatButton = new TextButton(StringManager.getString("hotseat"), game.skin);
		hotseatButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) 
			{
				game.setScreen(new HotseatStartScreen());
			}
		});
		table.add(hotseatButton).pad(5).width(w/2).height(h/10);
		
		table.row();
		
		TextButton onlineButton = new TextButton(StringManager.getString("online"), game.skin);
		onlineButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) 
			{
				game.setScreen(new LoginScreen());
			}
		});
		table.add(onlineButton).pad(5).width(w/2).height(h/10);
		
		table.row();
		
		/*TextButton testButton = new TextButton("test", game.skin);
		testButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) 
			{
				game.setScreen(new OnlineGameScreen());
			}
		});
		table.add(testButton).pad(5).width(w/2).height(h/10);
		
		table.row();*/
		
		TextButton backButton = new TextButton(StringManager.getString("main_exit"), game.skin);
		backButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				game.exit();
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
	}

}
