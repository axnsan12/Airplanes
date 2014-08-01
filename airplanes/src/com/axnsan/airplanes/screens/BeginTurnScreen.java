package com.axnsan.airplanes.screens;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.GuardedScreen;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.StringManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class BeginTurnScreen implements Screen {
	private Stage stage;
	private Airplanes game;
	private GuardedScreen screen;
	private String text;
	private SpriteBatch batch;
	private boolean resume;
	private boolean good = false;
	
	public BeginTurnScreen(String text, GuardedScreen screen, boolean resume)
	{
		this.game = Airplanes.game;
		this.screen = screen;
		this.text = text;
		batch = new SpriteBatch();
		this.resume = resume;
	}
	
	@Override
	public void show() {
		if (game.state.finished)
		{
			screen.liftGuard();
			good = true;
			game.back();
		}
	}

	@Override
	public void dispose() {
		Airplanes.game.input.removeProcessor(stage);
		
		if (stage != null)
			stage.dispose();
		if (batch != null)
			batch.dispose();
		
		stage = null;
		batch = null;
		
		//If turn start wasn't confirmed (for example, the back button was pressed), back out of the calling screen
		if (!good)
			Airplanes.game.back();
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		stage.act(delta);
		stage.draw();
		if (batch != null)
		{
			batch.begin();
			{
				float w = Gdx.graphics.getWidth();
				float h = Gdx.graphics.getHeight();
				
				BitmapFont font = FontManager.getFontForHeight(h/10);
				font.setColor(Color.GRAY);
				font.drawWrapped(batch, text, w/10, 2*h/3, 8*w/10, HAlignment.CENTER);
			}
			batch.end();
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
		
		if (batch != null)
			batch.dispose();
		
		batch = new SpriteBatch();
		stage = new Stage();
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();

		game.setTextButtonFont(FontManager.getFontForHeight(h/20));
		TextButton startButton = new TextButton(StringManager.getString("turn_start"), game.skin);
		if (resume)
			startButton.setText(StringManager.getString("turn_resume"));
		startButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				screen.liftGuard();
				good = true;
				game.back();
		    }
		});
		stage.addActor(startButton);
		startButton.setBounds(w/4, h/5, w/2, h/10);
		
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
