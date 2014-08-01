package com.axnsan.airplanes.online;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.util.ActionManager;
import com.axnsan.airplanes.util.FontManager;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class LoginScreen implements Screen {
	private Stage stage;
	private Airplanes game;
	private Table table;
	private TextButton backButton;

	public LoginScreen()
	{
		game = Airplanes.game;
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1,1,1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		
		stage.act(delta);
		stage.draw();
		
		try {
			Thread.sleep(Math.max(0, (long) (Airplanes.MAX_FRAME_TIME - Gdx.graphics.getRawDeltaTime())));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static final int MIN_USERNAME_LEN = 6, MAX_USERNAME_LEN = 15;
	public static final int MIN_PASSWORD_LEN = 6, MAX_PASSWORD_LEN = 40;
	
	private boolean registerPressed = false;
	private TextField usernameField;
	private TextField passwordField, passwordField2;
	private Label usernameLabel, passwordLabel, passwordLabel2;
	private TextButton loginButton, registerButton;
	@Override
	public void resize(int width, int height) {
		stage.setViewport(width, height);
		float w = Gdx.graphics.getWidth();
		float h = Gdx.graphics.getHeight();
		table.clearChildren();
		table.padTop(h/5);

		game.setLabelFont(FontManager.getFontForHeight(h/20));
		game.setTextFieldFont(FontManager.getFontForHeight(h/20));
		game.setTextButtonFont(FontManager.getFontForHeight(h/20));
		LabelStyle labelStyle = game.skin.get(LabelStyle.class);
		usernameLabel.setStyle(labelStyle);
		table.add(usernameLabel).pad(4).height(h/10);
		table.add(usernameField).pad(4).height(h/10).width(w/2);
		table.row();
		
		passwordLabel.setStyle(labelStyle);
		table.add(passwordLabel).pad(4).height(h/10);
		table.add(passwordField).pad(4).height(h/10).width(w/2);
		table.row();
		
		passwordLabel2.setStyle(labelStyle);
		table.add(passwordLabel2).pad(4).height(h/10);
		table.add(passwordField2).pad(4).height(h/10).width(w/2);
		table.row();

		loginButton.setStyle(Airplanes.game.skin.get(TextButtonStyle.class));
		registerButton.setStyle(Airplanes.game.skin.get(TextButtonStyle.class));
		table.add(loginButton).width(w/3).height(h/10).padTop(15);
		table.add(registerButton).width(w/3).height(h/10).padTop(15);

		backButton.setBounds(10, 10, w/2 - 15, h/10);
		backButton.setStyle(Airplanes.game.skin.get(TextButtonStyle.class));
	}
	
	private ClientSocket socket = null;
	private BlockingQueue<ServerResponseMessage> responseQueue = new LinkedBlockingQueue<ServerResponseMessage>();
	private BlockingQueue<Message> eventQueue = new LinkedBlockingQueue<Message>();
	
	@Override
	public void show() {
		try {
			ActionManager.showProgressDialog(StringManager.getString("connecting"));
			if (socket == null || !socket.isConnected()) {
				socket = new ClientSocket("axnsan.no-ip.org", 27015, eventQueue, responseQueue);
			}
			(new Thread(socket)).start();
		}
		catch (IOException e) {
			ActionManager.dismissProgressDialog();
			ActionManager.showLongToast(StringManager.getString("connection_failed"));
			e.printStackTrace();
			Airplanes.game.back();
		}
		/*try {
			Preferences pref = Gdx.app.getPreferences("LOGIN");
			String username = pref.getString("username", null);
			String password = pref.getString("password", null);
			if (username != null && password != null) {
				socket.sendMessage(new AccountLoginMessage(username, password));
				ServerResponseMessage response = responseQueue.poll(3, TimeUnit.SECONDS);
				if (response == null)
					throw new IOException("Request timed out");
				
				switch (response.responseCode) {
				case RESPONSE_CODE.BAD_USERNAME:
				case RESPONSE_CODE.WRONG_PASSWORD:
					break;
					
				case RESPONSE_CODE.RESPONSE_OK:
					pref.putString("username", username);
					pref.putString("password", password);
					pref.flush();
					ClientSocket socket = this.socket;
					this.socket = null;
					Airplanes.game.back();
					ActionManager.dismissProgressDialog();
					game.setScreen(new OnlineMenuScreen(socket, responseQueue, eventQueue, username));
					return;
				}
			}
		}
		catch (IOException | InterruptedException e) {
			ActionManager.dismissProgressDialog();
			ActionManager.showLongToast(StringManager.getString("connection_failed"));
			e.printStackTrace();
			Airplanes.game.back();
		}*/
		ActionManager.dismissProgressDialog();
		
		Preferences pref = Gdx.app.getPreferences("LOGIN");
		String username = pref.getString("username", "");
		usernameField = new TextField(username, game.skin);
		usernameField.setMaxLength(MAX_USERNAME_LEN);
		usernameField.setBlinkTime(0.5f);
		usernameField.setTextFieldFilter(new TextFieldFilter() {

			@Override
			public boolean acceptChar(TextField textField, char key) {
				return Character.isDigit(key) || Character.isLetter(key);
			}
		});
		usernameField.setFocusTraversal(true);
		usernameLabel = new Label(StringManager.getString("username_label"), game.skin);
		
		String password = pref.getString("password", "");
		passwordField = new TextField(password, game.skin);
		passwordField.setMaxLength(MAX_PASSWORD_LEN);
		passwordField.setPasswordMode(true);
		passwordField.setPasswordCharacter('*');
		passwordField.setFocusTraversal(true);
		passwordLabel = new Label(StringManager.getString("password_label"), game.skin);
		
		passwordField2 = new TextField("", game.skin);
		passwordField2.setMaxLength(MAX_PASSWORD_LEN);
		passwordField2.setPasswordMode(true);
		passwordField2.setPasswordCharacter('*');
		passwordField2.setFocusTraversal(true);
		passwordLabel2 = new Label(StringManager.getString("password_repeat_label"), game.skin);
		
		passwordField2.setVisible(registerPressed);
		passwordLabel2.setVisible(registerPressed);
		
		game.setTextButtonFont(FontManager.getFontForHeight(22));
		loginButton = new TextButton(StringManager.getString("login"), game.skin);
		loginButton.addListener(new ClickListener() {
			
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				try {
					ActionManager.showProgressDialog(StringManager.getString("waiting_server"));
					Preferences pref = Gdx.app.getPreferences("LOGIN");
					String username = usernameField.getText();
					String password = passwordField.getText();
					if (username != null && password != null) {
						socket.sendMessage(new AccountLoginMessage(username, password));
						ServerResponseMessage response = responseQueue.poll(Airplanes.TIMEOUT, TimeUnit.SECONDS);
						if (response == null)
							throw new IOException("Request timed out");
						
						switch (response.responseCode) {
						case RESPONSE_CODE.BAD_USERNAME:
							ActionManager.showLongToast(StringManager.getString("wrong_username"));
							break;
						case RESPONSE_CODE.WRONG_PASSWORD:
							ActionManager.showLongToast(StringManager.getString("wrong_password"));
							break;
							
						case RESPONSE_CODE.RESPONSE_OK:
							pref.putString("username", username);
							pref.putString("password", password);
							pref.flush();
							ClientSocket socket = LoginScreen.this.socket;
							LoginScreen.this.socket = null;
							Airplanes.game.back();
							ActionManager.dismissProgressDialog();
							game.setScreen(new OnlineMenuScreen(socket, responseQueue, eventQueue, username));
							break;
						}
					}
				}
				catch (IOException | InterruptedException e) {
					ActionManager.dismissProgressDialog();
					ActionManager.showLongToast(StringManager.getString("connection_failed"));
					e.printStackTrace();
					Airplanes.game.back();
				}
				ActionManager.dismissProgressDialog();
		    }
		});
		
		registerButton = new TextButton(StringManager.getString("register"), game.skin);
		registerButton.addListener(new ClickListener() {
			
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				if (!passwordField2.isVisible()) {
					passwordField2.setVisible(true);
					passwordLabel2.setVisible(true);
					registerPressed = true;
					return;
				}
				
				String username = usernameField.getText();
				String password = passwordField.getText();
				String password2 = passwordField2.getText();
				
				if (!password.equals(password2)) {
					ActionManager.showLongToast(StringManager.getString("register_not_match"));
					return;
				}
				if (username == null || username.length() < MIN_USERNAME_LEN) {
					ActionManager.showLongToast(StringManager.getString("%dregister_short_username")
							.replace("%d", Integer.toString(MIN_USERNAME_LEN)));
					return;
				}
				for (int i = 0; i < username.length(); ++i) {
					char c = username.charAt(i);
					if (c < '0' || (c > '9' && c < 'A') || (c > 'Z' && c < 'a') || c >'z') {
						ActionManager.showLongToast(StringManager.getString("register_username_alnum"));
						return;
					}
				}
				if (password == null || password.length() < MIN_PASSWORD_LEN) {
					ActionManager.showLongToast(StringManager.getString("%dregister_short_password")
							.replace("%d", Integer.toString(MIN_PASSWORD_LEN)));
					return;
				}
				try {
					ActionManager.showProgressDialog(StringManager.getString("waiting_server"));
					Preferences pref = Gdx.app.getPreferences("LOGIN");
					
					if (username != null && password != null) {
						socket.sendMessage(new AccountRegisterMessage(username, password));
						ServerResponseMessage response = responseQueue.poll(Airplanes.TIMEOUT, TimeUnit.SECONDS);
						if (response == null)
							throw new IOException("Request timed out");
						
						switch (response.responseCode) {
						case RESPONSE_CODE.REGISTER_BAD_USERNAME:
							ActionManager.showLongToast(StringManager.getString("bad_username"));
							break;
						case RESPONSE_CODE.REGISTER_BAD_PASSWORD:
							ActionManager.showLongToast(StringManager.getString("bad_password"));
							break;
						case RESPONSE_CODE.REGISTER_ALREADY_EXISTS:
							ActionManager.showLongToast(StringManager.getString("register_exists"));
							break;
							
						case RESPONSE_CODE.RESPONSE_OK:
							pref.putString("username", username);
							pref.putString("password", password);
							pref.flush();
							ClientSocket socket = LoginScreen.this.socket;
							LoginScreen.this.socket = null;
							Airplanes.game.back();
							game.setScreen(new OnlineMenuScreen(socket, responseQueue, eventQueue, username));
							break;
						}
					}
				}
				catch (IOException | InterruptedException e) {
					ActionManager.dismissProgressDialog();
					ActionManager.showLongToast(StringManager.getString("connection_failed"));
					e.printStackTrace();
					Airplanes.game.back();
				}
				ActionManager.dismissProgressDialog();
		    }
		});

		stage = new Stage();
		
		table = new Table();
		table.setFillParent(true);
		stage.addActor(table);
		table.align(Align.top);
		
		backButton = new TextButton(StringManager.getString("back"), game.skin);
		backButton.addListener(new ClickListener() {
			@Override
		    public void clicked(InputEvent event, float x, float y)
		    {
				Gdx.input.setOnscreenKeyboardVisible(false);
				game.back();
		    }
		});
		stage.addActor(backButton);
		game.input.addProcessor(stage);
	}

	@Override
	public void hide() {
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
		if (socket != null) {
			socket.disconnect();
			socket = null;
		}
	}

}
