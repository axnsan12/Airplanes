package com.axnsan.airplanes.online;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.axnsan.airplanes.Airplanes;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL10;

public class OnlineMenuScreen implements Screen {

	private ClientSocket socket = null;
	private BlockingQueue<ServerResponseMessage> responseQueue = new LinkedBlockingQueue<ServerResponseMessage>();
	private BlockingQueue<Message> eventQueue = new LinkedBlockingQueue<Message>();
	private String username;
	
	public OnlineMenuScreen(ClientSocket socket, BlockingQueue<ServerResponseMessage> responseQueue
			, BlockingQueue<Message> eventQueue, String username) {
		this.socket = socket;
		this.responseQueue = responseQueue;
		this.eventQueue = eventQueue;
		this.username = username;
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(1,1,1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
	}

	@Override
	public void resize(int width, int height) {

	}

	@Override
	public void show() {
		ClientSocket socket = this.socket;
		this.socket = null;
		Airplanes.game.back();
		Airplanes.game.setScreen(new LobbyScreen(socket, responseQueue, eventQueue, username));
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub

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
		if (socket != null)
			socket.disconnect();
		socket = null;
	}

}
