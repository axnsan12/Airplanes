package com.axnsan.airplanes.online;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.axnsan.airplanes.online.SocketClosedMessage.REASON;


public abstract class Message {
	public abstract int getMessageType();
	public abstract void serialize(ByteBuffer dst);
	public abstract int serializeLength();
	
	public static Message deserialize(ByteBuffer src) {
		switch ((int) (src.get() & 0xff)) {
			case MESSAGE_TYPE.ACCOUNT_REGISTER: 				return AccountRegisterMessage.deserialize(src);
			case MESSAGE_TYPE.ACCOUNT_LOGIN:					return AccountLoginMessage.deserialize(src);
			case MESSAGE_TYPE.SERVER_RESPONSE:					return ServerResponseMessage.deserialize(src);
			case MESSAGE_TYPE.EVENT_GAME_CREATED:				return GameCreatedMessage.deserialize(src);
			case MESSAGE_TYPE.EVENT_PLAYER_JOINED_GAME:			return PlayerJoinedMessage.deserialize(src);
			case MESSAGE_TYPE.EVENT_PLAYER_LEFT_GAME:			return PlayerLeftMessage.deserialize(src);
			case MESSAGE_TYPE.EVENT_TURN_STARTED:				return TurnStartedMessage.deserialize(src);
			case MESSAGE_TYPE.PLANE_LOCATIONS:					return PlaneLocationsMessage.deserialize(src);
			case MESSAGE_TYPE.ATTACK_CELL:						return AttackCellMessage.deserialize(src);
			case MESSAGE_TYPE.GAME_FINISHED:					return GameEndMessage.deserialize(src);
			case MESSAGE_TYPE.CONNECTION_CLOSED:				return new SocketClosedMessage(REASON.DISCONNECTED_BY_SERVER);
		}
		
		return null;
	}
	
	private static Charset CHARSET_UTF8 = Charset.forName("UTF-8");
	
	public static String getString(ByteBuffer bytes) {
		int len = (int) (bytes.getShort() & 0xffff);
		byte[] b = new byte[len];
		bytes.get(b);
		return new String(b, CHARSET_UTF8);
	}
	
	public static void putString(String string, ByteBuffer buffer) {
		buffer.putShort((short)string.length());
		buffer.put(string.getBytes(CHARSET_UTF8));
	}
}
