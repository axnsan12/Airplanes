package com.axnsan.airplanes.screens;

import java.util.ArrayList;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.GameConfiguration;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class HotseatPlayersScreen implements Screen {
	private Stage stage;
	private Airplanes game;
	private Table table;
	private Button add;
	private TextField dummy;

	public HotseatPlayersScreen()
	{
		game = Airplanes.game;
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1,1,1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		stage.act(delta);
		stage.draw();

	}

	@Override
	public void resize(int width, int height) {
		if (stage != null) {
			game.input.removeProcessor(stage);
			stage.dispose();
		}
		stage = new Stage();
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		
		table = new Table();
		table.setFillParent(true);
		stage.addActor(table);
		table.align(Align.top | Align.center);
		table.padTop(10);
		
		add = new TextButton("+", game.skin);
		add.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) 
			{
				addPlayer();
			}
		});
		dummy = new TextField("", game.skin);
		dummy.setVisible(false);
		
		game.config.numPlayers = Math.max(2, game.config.numPlayers);
		for (int i = 0;i < GameConfiguration.maxPlayers;++i)
		{
			TextButton but = new TextButton("-", Airplanes.game.skin);
			but.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) 
				{
					removePlayer();
				}
			});
			but.setVisible(false);
			TextField tf = new TextField(game.config.playerNames[i], game.skin);
			tf.setMaxLength(GameConfiguration.maxNameLength);
			tf.setBlinkTime(0.5f);
			tf.setTextFieldFilter(new TextFieldFilter() {

				@Override
				public boolean acceptChar(TextField textField, char key) {
					return Character.isDigit(key) || Character.isLetter(key) || key == ' ';
				}
			});

			players.add(new Row(tf, but));
		}

		rebuildTable();
		
		game.setTextButtonFont(FontManager.getFontForHeight(h/20));
		TextButton nextButton = new TextButton(StringManager.getString("start"), game.skin);
		nextButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				game.back(); //Pop the settings screen off the stack
				game.setScreen(new HotseatScreen());
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
		
		game.input.addProcessor(stage);

	}
	
	private static class Row
	{
		public TextField tf;
		public Button but;
		
		public Row(TextField tf, Button but) { this.tf = tf; this.but = but; }
	}
	private ArrayList<Row> players = new ArrayList<Row>();
	
	private void addPlayer()
	{
		if (game.config.numPlayers >= GameConfiguration.maxPlayers)
			throw new RuntimeException("What sorcery is this?!");
		
		game.config.numPlayers += 1;
		rebuildTable();
	}
	private void removePlayer()
	{
		if (players.size() <= 2)
			throw new RuntimeException("What sorcery is this?");
		
		game.config.numPlayers -= 1;
		rebuildTable();
	}
	
	private void rebuildTable()
	{
		if (game.config.numPlayers > GameConfiguration.maxPlayers)
			throw new RuntimeException("What sorcery is this?!");
		
		table.clearChildren();
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		game.setTextButtonFont(FontManager.getFontForHeight(h/10));
		game.setTextFieldFont(FontManager.getFontForHeight(h/20));
		TextButtonStyle tbs = game.skin.get(TextButtonStyle.class);
		for (int i = 0;i < game.config.numPlayers;++i)
		{
			Row r = players.get(i);
			r.but.setVisible(false);
			r.but.setStyle(tbs);
			table.add(r.but).size(h/14).padRight(10);
			table.add(r.tf).pad(2).size(w/2, h/12);
			table.row();
		}
		table.add(add).size(h/14).padRight(10);
		add.setStyle(tbs);
		table.add(dummy).pad(2);
		add.setVisible(game.config.numPlayers < GameConfiguration.maxPlayers);
		if (game.config.numPlayers > 2)
			players.get(game.config.numPlayers-1).but.setVisible(true);
	}
	
	@Override
	public void show() {
		
	}

	@Override
	public void hide() {
		for (int i = 0;i < GameConfiguration.maxPlayers;++i)
		{
			String n = players.get(i).tf.getText().replace(",", "").replace("[", "").replace("]","");
			if (n.length() > 0)
				game.config.playerNames[i] = n;
		}
		game.config.dumpConfig();
		dispose();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		if (stage != null)
		{
			game.input.removeProcessor(stage);
			stage.dispose();
			stage = null;
		}
	}

}
