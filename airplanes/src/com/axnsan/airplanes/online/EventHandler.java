package com.axnsan.airplanes.online;

import java.util.concurrent.BlockingQueue;

import com.axnsan.airplanes.Airplanes;
import com.axnsan.airplanes.online.SocketClosedMessage.REASON;
import com.axnsan.airplanes.util.ActionManager;
import com.axnsan.airplanes.util.Point2D;
import com.axnsan.airplanes.util.StringManager;
import com.badlogic.gdx.Gdx;

public class EventHandler implements Runnable {
	private BlockingQueue<Message> eventQueue;
	private SessionData session;
	
	public EventHandler(BlockingQueue<Message> eventQueue, SessionData session) {
		this.eventQueue = eventQueue;
		this.session = session;
	}
	
	@Override
	public void run() {
		try {
			Message msg;
			while ((msg = eventQueue.take()) != null) {
				switch (msg.getMessageType()) {
				case MESSAGE_TYPE.EVENT_GAME_CREATED:
				{
					GameCreatedMessage gcm = (GameCreatedMessage)msg;
					synchronized (session) {
						session.addGame(new Game(gcm.gameID, gcm.timeout, gcm.numPlayers, gcm.gridSize
								,gcm.numPlanes, gcm.headshots, gcm.reveal, 0, -1, false));
						if (gcm.gameID == session.pendingJoin) {
							session.pendingJoin = -1;
							synchronized (session.waitJoin) {
								session.waitJoin.notifyAll();
							}
						}
						Gdx.graphics.requestRendering();
					}
					break;
				}
				case MESSAGE_TYPE.EVENT_PLAYER_JOINED_GAME:
				{
					final String playerName;
					synchronized (session) {
						final PlayerJoinedMessage gjm = (PlayerJoinedMessage)msg;
						Game g = session.getGame(gjm.gameID);
						if (g == null && gjm.playerName.equals(session.username))
							throw new RuntimeException("Join on uninitialized game");
						if (g != null) 
						{
							g.addPlayer();
							if (g.isFull() && !g.isJoined()) {
								session.games.remove(g.gameID);
							}
							if (session.currentGame != null && session.currentGame.getGameID() == gjm.gameID)
								playerName = gjm.playerName;
							else playerName = null;
						}
						else playerName = null;

						if (playerName != null)
							Airplanes.application.postRunnable(new Runnable() {
								@Override
								public void run() { session.currentGame.addPlayer(playerName); }
							});
						break;
					}
				}
				
				case MESSAGE_TYPE.EVENT_PLAYER_LEFT_GAME:
				{
					synchronized (session) {
						PlayerLeftMessage plm = (PlayerLeftMessage)msg;
						Game g = session.getGame(plm.gameID);
						if (g != null) 
						{
							g.removePlayer();
							if (session.currentGame != null && session.currentGame.getGameID() == plm.gameID)
								session.currentGame.removePlayer(plm.playerName);
							Gdx.graphics.requestRendering();
						}
					}
					break;
				}
				
				case MESSAGE_TYPE.EVENT_TURN_STARTED:
				{
					final int t;
					synchronized (session) {
						TurnStartedMessage tsm = (TurnStartedMessage)msg;
						Game g = session.getGame(tsm.gameID);
						int turn = tsm.turnNumber;
						if (g != null) 
						{
							g.setTurn(turn);
							if (session.currentGame != null && session.currentGame.getGameID() == tsm.gameID)
								t = turn;
							else t = -1;
						}
						else t = -1;

						if (t != -1)
							Airplanes.application.postRunnable(new Runnable() {
								@Override
								public void run() { session.currentGame.beginTurn(t); }
							});
						break;
					}
				}
					
				case MESSAGE_TYPE.PLANE_LOCATIONS:
				{
					synchronized (session) {
						PlaneLocationsMessage plm = (PlaneLocationsMessage)msg;
						if (session.currentGame != null && session.currentGame.getGameID() == plm.gameID) {
							session.currentGame.addPlanes(plm.playerName, plm.locations);
							Gdx.graphics.requestRendering();
						}
					}
					break;
				}
				case MESSAGE_TYPE.ATTACK_CELL:
				{
					synchronized (session) {
						AttackCellMessage acm = (AttackCellMessage)msg;
						if (session.currentGame != null && session.currentGame.getGameID() == acm.gameID) {
							session.currentGame.playerAttackedCell(acm.playerName, new Point2D(acm.x, acm.y));
							Gdx.graphics.requestRendering();
						}
					}
					break;
				}
				
				case MESSAGE_TYPE.SOCKET_CLOSED:
				{
					SocketClosedMessage.REASON reason = ((SocketClosedMessage)msg).reason;
					ActionManager.dismissProgressDialog();
					if (reason == REASON.DISCONNECTED_BY_SERVER || reason == REASON.CONNECTION_DROPPED) {
							Airplanes.application.postRunnable(new Runnable() {
								@Override
								public void run() {
									ActionManager.showLongToast(StringManager.getString("connection_failed"));
									Airplanes.game.resetToMainMenu();
								}
							}
						);
					}
					return;
				}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			Airplanes.game.resetToMainMenu();
		}
	}

}
