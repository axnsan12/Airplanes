package com.axnsan.airplanes.online;

import java.nio.ByteBuffer;

class MESSAGE_TYPE {
	public final static int UNKNOWN_MESSAGE = 0;
	public final static int GAME_CREATE = 1; /*Create new game room*/
	public final static int GAME_JOIN = 2; /*Join game rooms*/
	public final static int GAME_LEAVE = 3; /*Leave game room & abandon game*/
	public final static int ACCOUNT_REGISTER = 4; /*Sent by clients to register a new account; Server will respond with success value*/
	public final static int ACCOUNT_LOGIN = 5; /*Verify the given login information; Server will respond with success value*/
	public final static int GET_GAME_LISTING = 6; /*Request a list of currently joinable games*/
	public final static int GET_GAME_STATE = 7;
	public final static int PLAYER_DIED = 8;
	public final static int GAME_FINISHED = 9;
	
	public final static int PLANE_LOCATIONS = 128;
	public final static int ATTACK_CELL = 129;

/*Messages sent by the server. DO NOT MODIFY VALUE.*/
	public final static int EVENT_PLAYER_JOINED_GAME = 192; /*Sent by the server to notify clients of new players joining games*/
	public final static int EVENT_PLAYER_LEFT_GAME = 193; /*Notify clients of players leaving games*/
	public final static int EVENT_GAME_CREATED = 194; /*Notify clients of new games*/
	public final static int EVENT_TURN_STARTED = 195; /*A new turn started in a game*/
	public final static int TURN_TIMEOUT = 196;
	public final static int SERVER_RESPONSE = 197; /*Sent by the server in response to some client requests*/
	public final static int CONNECTION_CLOSED = 198; /*Notify the client that it was disconnected*/

	public final static int SOCKET_CLOSED = 255;
}

class RESPONSE_CODE {
	public final static int UNKNOWN_CODE = 0;
	public final static int RESPONSE_OK = 1; /*Notify clients their request was successful; if necessary*/
	public final static int BAD_USERNAME = 2; /*There is no account with the specified username*/
	public final static int WRONG_PASSWORD = 3; /*The given password is not correct for the specified account*/
	public final static int AUTHENTICATION_REQUIRED = 4; /*The client tried to perform an action that requires authentication before authenticating*/
	public final static int BAD_GAME_ID = 5; /*No game exists with the given ID*/
	public final static int WRONG_GAME_PASSWORD = 6; /*The game has a password and the client did not give the correct password*/
	public final static int REGISTER_BAD_PASSWORD = 7; /*The password is not a suitable password to register an account with; metadata may contain additional info*/
	public final static int REGISTER_ALREADY_EXISTS = 8; /*An account with the name requested to be registered already exists*/
	public final static int REGISTER_BAD_USERNAME = 9; /*The username requested to be registered is not valid; metadata may contain additional info*/
	public final static int UNEXPECTED_MESSAGE = 10; /*The client sent a message that it shouldn't have; metadata may contain additional info*/
	public final static int BAD_REQUEST_PARAMETERS = 11; /*The client sent a message requesting something but the server refused it because the parameters did not meet expectations*/
	public final static int INTERNAL_SERVER_ERROR = 12; /*The server encountered an unknown error while processing a request*/
	public final static int SERVER_SHUTTING_DOWN = 13;
	public final static int GAME_FULL = 14; /*Client tried to join a game that is full*/
	public final static int GAME_LISTING = 15; /*List of currently joinable games*/
	public final static int GAME_STATE = 16;
	public final static int ALREADY_PLAYED = 17;
	public final static int GAME_ALREADY_FINISHED = 18;

}

class AccountRegisterMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.ACCOUNT_REGISTER;
	public String username;
	public String password;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		return 1 
			+ 2 + username.length() 
			+ 2 + password.length();
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		dst.put((byte)_MESSAGE_TYPE);
		putString(username, dst);
		putString(password, dst);
	}
	
	public static AccountRegisterMessage deserialize(ByteBuffer bytes) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public AccountRegisterMessage(String username, String password) { this.username = username; this.password = password; }
}

class AccountLoginMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.ACCOUNT_LOGIN;
	public String username;
	public String password;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		return 1 
			+ 2 + username.length() 
			+ 2 + password.length();
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		dst.put((byte)_MESSAGE_TYPE);
		putString(username, dst);
		putString(password, dst);
	}
	
	public static AccountLoginMessage deserialize(ByteBuffer src) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public AccountLoginMessage(String username, String password) { this.username = username; this.password = password; }
}

class GameCreateMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.GAME_CREATE;
	public int numPlayers;
	public int timeout;
	public String gamePassword;
	public int gridSize;
	public int numPlanes;
	public boolean headshots;
	public boolean reveal;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		return 1 //type
			+ 1 //numPlayers
			+ 4 //timeout
			+ 2 + gamePassword.length()
			+ 4; //grid, planes, headshots, reveal
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		dst.put((byte)_MESSAGE_TYPE);
		dst.put((byte) (numPlayers));
		dst.putInt(timeout);
		putString(gamePassword, dst);
		dst.put((byte) gridSize);
		dst.put((byte) numPlanes);
		dst.put((byte) ((headshots)?1:0));
		dst.put((byte) ((reveal)?1:0));
	}
	
	public static GameCreateMessage deserialize(ByteBuffer src) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public GameCreateMessage(int numPlayers, int timeout, String gamePassword, int gridSize
			, int numPlanes, boolean headshots, boolean reveal) { 
		this.numPlayers = numPlayers; this.timeout = timeout; this.headshots = headshots;
		this.gamePassword = gamePassword; this.numPlanes = numPlanes; this.reveal = reveal;
		this.gridSize = gridSize; }
}

class RequestGamesMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.GET_GAME_LISTING;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		return 1;
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		dst.put((byte)_MESSAGE_TYPE);
	}
	
	public static RequestGamesMessage deserialize(ByteBuffer src) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public RequestGamesMessage() { }
}

class GameJoinMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.GAME_JOIN;
	public int gameID;
	public String gamePassword;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		return 1
			+ 4
			+ 2 + gamePassword.length();
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		dst.put((byte)_MESSAGE_TYPE);
		dst.putInt(gameID);
		putString(gamePassword, dst);
	}
	
	public static GameJoinMessage deserialize(ByteBuffer src) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public GameJoinMessage(int gameID, String gamePassword) { this.gameID = gameID; this.gamePassword = gamePassword; }
}

class GameLeaveMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.GAME_LEAVE;
	public int gameID;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		return 1
			+ 4;
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		dst.put((byte)_MESSAGE_TYPE);
		dst.putInt(gameID);
	}
	
	public static GameLeaveMessage deserialize(ByteBuffer src) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public GameLeaveMessage(int gameID) { this.gameID = gameID; }
}

class GetGameStateMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.GET_GAME_STATE;
	public int gameID;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		return 1
			+ 4;
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		dst.put((byte)_MESSAGE_TYPE);
		dst.putInt(gameID);
	}
	
	public static GetGameStateMessage deserialize(ByteBuffer src) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public GetGameStateMessage(int gameID) { this.gameID = gameID; }
}

class PlayerDeathMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.PLAYER_DIED;
	public int gameID;
	public int turnNumber;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		return 1
			+ 4
			+ 2;
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		dst.put((byte)_MESSAGE_TYPE);
		dst.putInt(gameID);
		dst.putShort((short) turnNumber);
	}
	
	public static PlayerDeathMessage deserialize(ByteBuffer src) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public PlayerDeathMessage(int gameID, int turnNumber) { this.gameID = gameID; this.turnNumber = turnNumber; }
}

class GameEndMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.GAME_FINISHED;
	public int gameID;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		return 1
			+ 4;
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		dst.put((byte)_MESSAGE_TYPE);
		dst.putInt(gameID);
	}
	
	public static GameEndMessage deserialize(ByteBuffer src) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public GameEndMessage(int gameID) { this.gameID = gameID; }
}

class PlaneLocationsMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.PLANE_LOCATIONS;
	public int gameID;
	public String playerName;
	public String locations;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		return 1 +
				4 +
				2 + playerName.length() +
				2 + locations.length();
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		dst.put((byte) _MESSAGE_TYPE);
		dst.putInt(gameID);
		putString(playerName, dst);
		putString(locations, dst);
	}
	
	public static PlaneLocationsMessage deserialize(ByteBuffer src) {
		PlaneLocationsMessage ret = new PlaneLocationsMessage();
		ret.gameID = src.getInt();
		ret.playerName = getString(src);
		ret.locations = getString(src);
		
		return ret;
	}
	
	public PlaneLocationsMessage(int gameID, String playerName, String locations) 
	{  this.locations = locations; this.gameID = gameID; this.playerName = playerName; }
	
	private PlaneLocationsMessage() { }
}

class AttackCellMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.ATTACK_CELL;
	public int gameID;
	public String playerName;
	public int x;
	public int y;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		return 1 +
				4 +
				2 + playerName.length() +
				1 +
				1;
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		dst.put((byte) _MESSAGE_TYPE);
		dst.putInt(gameID);
		putString(playerName, dst);
		dst.put((byte) x);
		dst.put((byte) y);
	}
	
	public static AttackCellMessage deserialize(ByteBuffer src) {
		AttackCellMessage ret = new AttackCellMessage();
		ret.gameID = src.getInt();
		ret.playerName = getString(src);
		ret.x = src.get();
		ret.y = src.get();
		
		return ret;
	}
	
	public AttackCellMessage(int gameID, String playerName, int x, int y) 
	{ this.gameID = gameID; this.playerName = playerName; this.x = x; this.y = y; }
	
	private AttackCellMessage() { }
}

class GameCreatedMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.EVENT_GAME_CREATED;
	public int gameID;
	public int numPlayers;
	public int timeout;
	public int gridSize;
	public int numPlanes;
	public boolean headshots;
	public boolean reveal;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		throw new java.lang.UnsupportedOperationException();
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public static GameCreatedMessage deserialize(ByteBuffer src) {
		GameCreatedMessage ret = new GameCreatedMessage();
		ret.gameID = src.getInt();
		ret.numPlayers = (int) (src.get() & 0xff);
		ret.timeout = src.getInt();
		ret.gridSize = (int) (src.get() & 0xff);
		ret.numPlanes = (int) (src.get() & 0xff);
		ret.headshots = src.get() != 0;
		ret.reveal = src.get() != 0;
		
		return ret;
	}
	
	private GameCreatedMessage() { }
}

class PlayerJoinedMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.EVENT_PLAYER_JOINED_GAME;
	public int gameID;
	public String playerName;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		throw new java.lang.UnsupportedOperationException();
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public static PlayerJoinedMessage deserialize(ByteBuffer src) {
		PlayerJoinedMessage ret = new PlayerJoinedMessage();
		ret.gameID = src.getInt();
		ret.playerName = getString(src);
		
		return ret;
	}
	
	private PlayerJoinedMessage() { }
}

class TurnStartedMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.EVENT_TURN_STARTED;
	public int gameID;
	public int turnNumber;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		throw new java.lang.UnsupportedOperationException();
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public static TurnStartedMessage deserialize(ByteBuffer src) {
		TurnStartedMessage ret = new TurnStartedMessage();
		ret.gameID = src.getInt();
		ret.turnNumber = (src.getShort() & 0xffff);
		
		return ret;
	}
	
	private TurnStartedMessage() { }
}

class PlayerLeftMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.EVENT_PLAYER_LEFT_GAME;
	public int gameID;
	public String playerName;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		throw new java.lang.UnsupportedOperationException();
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public static PlayerLeftMessage deserialize(ByteBuffer src) {
		PlayerLeftMessage ret = new PlayerLeftMessage();
		ret.gameID = src.getInt();
		ret.playerName = getString(src);
		
		return ret;
	}
	
	private PlayerLeftMessage() { }
}

class ServerResponseMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.SERVER_RESPONSE;
	public int responseCode;
	public String metadata;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		throw new java.lang.UnsupportedOperationException();
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public static ServerResponseMessage deserialize(ByteBuffer src) {
		ServerResponseMessage ret = new ServerResponseMessage();
		ret.responseCode = (int)(src.get() & 0xff);
		ret.metadata = Message.getString(src);
		
		return ret;
	}
	
	private ServerResponseMessage() { }
}

class SocketClosedMessage extends Message {
	private static final int _MESSAGE_TYPE = MESSAGE_TYPE.SOCKET_CLOSED;
	enum REASON{
		CLIENT_DISCONNECTED,
		DISCONNECTED_BY_SERVER,
		CONNECTION_DROPPED,
		CONNECTION_DISCARDED,
	};
	REASON reason = REASON.CONNECTION_DROPPED;
	
	@Override
	public int getMessageType() {
		return _MESSAGE_TYPE;
	}
	
	@Override
	public int serializeLength() {
		throw new java.lang.UnsupportedOperationException();
	}
	
	@Override
	public void serialize(ByteBuffer dst) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public static ServerResponseMessage deserialize(ByteBuffer src) {
		throw new java.lang.UnsupportedOperationException();
	}
	
	public SocketClosedMessage(REASON reason) { this.reason = reason; }
}