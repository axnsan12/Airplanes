package com.axnsan.airplanes;

import java.util.ArrayList;
import java.util.Stack;

import com.axnsan.airplanes.screens.MainMenuScreen;
import com.axnsan.airplanes.util.ActionManager;
import com.axnsan.airplanes.util.ActionResolver;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.FontManagerInterface;
import com.axnsan.airplanes.util.RandomizedQueue;
import com.axnsan.airplanes.util.StringManager;
import com.axnsan.airplanes.util.StringXmlParser;
import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox.CheckBoxStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle;

public class Airplanes extends Game implements InputProcessor {
	public static Airplanes game;
	public static int TIMEOUT = 10;
	public static Application application;

	public static String[] colors = {"302055", "bf0808" , "e3c100",  "455520", };
	public static final int minPadding = 5, gridBorderWidth = 1;
	public static final double MAX_FPS = 30.0, MAX_FRAME_TIME = 1000/MAX_FPS;
	
	public ArrayList<Player> players;
	public InputMultiplexer input = new InputMultiplexer();
	public Skin skin;
	
	private Screen mainMenuScreen;
	private Stack<Screen> history = new Stack<Screen>();
	private TextButton dummyTextButton;
	private CheckBox dummyCheckBox;
	private Label dummyLabel;
	private TextField dummyTextField;
	
	public GameConfiguration config;
	public GameState state;
	
	public Airplanes(FontManagerInterface fontEngine, StringXmlParser xmlParser, ActionResolver action) {
		FontManager.initialize(fontEngine);
		StringManager.initialize(xmlParser);
		ActionManager.initialize(action);
	}
	
	public void setTextButtonFont(BitmapFont font) {
		TextButtonStyle tbs = skin.get(TextButtonStyle.class);
		tbs.font = font;
		dummyTextButton.setStyle(tbs);
	}
	
	public void setCheckBoxFont(BitmapFont font) {
		CheckBoxStyle cbs = skin.get(CheckBoxStyle.class);
		cbs.font = font;
		cbs.fontColor = Color.GRAY;
		dummyCheckBox.setStyle(cbs);
	}
	
	public void setLabelFont(BitmapFont font) {
		LabelStyle ls = skin.get(LabelStyle.class);
		ls.font = font;
		ls.fontColor = Color.GRAY;
		dummyLabel.setStyle(ls);
	}
	
	public void setTextFieldFont(BitmapFont font) {
		TextFieldStyle tfs = skin.get(TextFieldStyle.class);
		tfs.font = font;
		tfs.fontColor = Color.WHITE;
		dummyTextField.setStyle(tfs);
	}
	
	@Override
	public void create() {
		game = this;
		Gdx.input.setInputProcessor(input);
		Gdx.input.setCatchBackKey(true);
		Gdx.graphics.setContinuousRendering(false);
		//StringManager.setLocale("ro_RO");
		input.addProcessor(this);
		skin = new Skin(Gdx.files.internal("data/ui/uiskin.json"));
		dummyTextButton = new TextButton("", skin);
		setTextButtonFont(FontManager.getFontForHeight(20));
		dummyCheckBox = new CheckBox("", skin);
		setCheckBoxFont(FontManager.getFontForHeight(20));
		dummyLabel = new Label("", skin);
		setLabelFont(FontManager.getFontForHeight(20));
		dummyTextField = new TextField("", skin);
		setTextFieldFont(FontManager.getFontForHeight(20));

		mainMenuScreen = new MainMenuScreen();
		super.setScreen(mainMenuScreen);
	}
	
	public void back() {
		if (!history.empty()) {
			history.pop().dispose();
			if (!history.empty())
				super.setScreen(history.peek());
			else super.setScreen(mainMenuScreen);
		}
		System.gc();
	}
	
	public void resetToMainMenu() {
		super.setScreen(null);
		while (!history.empty())
			history.pop().dispose();
		super.setScreen(mainMenuScreen);
	}
	
	public int maxNumPlanes() {
		return Plane.maxPlanesOnGridSize(config.gridSize);
	}
	
	public int maxNumPlanes(int gridSize) {
		return Plane.maxPlanesOnGridSize(gridSize);
	}
	
	public int minGridSize() {
		Plane.Model model = Plane.templateModel();
		
		return Math.max(model.width, model.height);
	}
	
	public int maxGridSize() {
		
		return 50;
	}
	
	@Override 
	public void setScreen(Screen screen)
	{
		history.push(screen);
		super.setScreen(screen);
		System.gc();
	}
	
	@Override
	public void dispose() {
		super.setScreen(null);
		while (!history.empty())
			history.pop().dispose();
		mainMenuScreen.dispose();
		if (players != null)
			for (Player p: players)
				p.dispose();
		skin.dispose();
		FontManager.dispose();
	}

	public void exit() {
		dispose();
		Gdx.app.exit();
	}
	
	public static RandomizedQueue<String> colorsRandom = new RandomizedQueue<String>();
	public static ArrayList<String> colorsList = new ArrayList<String>();
	static {
		for (String s : colors)
		{
			colorsRandom.push(s);
			colorsList.add(s);
		}
	}
	
	@Override
	public boolean keyDown(int keycode) {
		switch (keycode) {
		case Keys.BACK:
			back();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}
}
