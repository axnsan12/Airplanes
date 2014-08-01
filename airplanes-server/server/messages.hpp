#ifndef __MESSAGES_HPP__
#define __MESSAGES_HPP__
#include "Message.h"
#include "log.h"

namespace airplanes {

	enum MESSAGE_TYPE : uint8_t {
		UNKNOWN_MESSAGE = 0,
		GAME_CREATE, /*Create new game room*/
		GAME_JOIN, /*Join game rooms*/
		GAME_LEAVE, /*Leave game room & abandon game*/
		ACCOUNT_REGISTER, /*Sent by clients to register a new account; Server will respond with success value*/
		ACCOUNT_LOGIN, /*Verify the given login information; Server will respond with success value*/
		GET_GAME_LISTING, /*Request a list of currently joinable games*/
		GET_GAME_STATE,
		PLAYER_DIED,
		GAME_FINISHED,

		PLANE_LOCATIONS = 128,
		ATTACK_CELL = 129,

		/*Messages sent by the server. DO NOT MODIFY VALUE.*/
		EVENT_PLAYER_JOINED_GAME = 192, /*Sent by the server to notify clients of new players joining games*/
		EVENT_PLAYER_LEFT_GAME, /*Notify clients of players leaving games*/
		EVENT_GAME_CREATED, /*Notify clients of new games*/
		EVENT_TURN_STARTED, /*A new turn started in a game*/
		TURN_TIMEOUT,
		SERVER_RESPONSE, /*Sent by the server in response to some client requests*/
		CONNECTION_CLOSED, /*Notify the client that it was disconnected*/

		SOCKET_CLOSED = 255
	};

	enum RESPONSE_CODE : uint8_t {
		UNKNOWN_CODE = 0,
		RESPONSE_OK, /*Notify clients their request was successful, if necessary*/
		BAD_USERNAME, /*There is no account with the specified username*/
		WRONG_PASSWORD, /*The given password is not correct for the specified account*/
		AUTHENTICATION_REQUIRED, /*The client tried to perform an action that requires authentication before authenticating*/
		BAD_GAME_ID, /*No game exists with the given ID*/
		WRONG_GAME_PASSWORD, /*The game has a password and the client did not give the correct password*/
		REGISTER_BAD_PASSWORD, /*The password is not a suitable password to register an account with; metadata may contain additional info*/
		REGISTER_ALREADY_EXISTS, /*An account with the name requested to be registered already exists*/
		REGISTER_BAD_USERNAME, /*The username requested to be registered is not valid; metadata may contain additional info*/
		UNEXPECTED_MESSAGE, /*The client sent a message that it shouldn't have; metadata may contain additional info*/
		BAD_REQUEST_PARAMETERS, /*The client sent a message requesting something but the server refused it because the parameters did not meet expectations*/
		INTERNAL_SERVER_ERROR, /*The server encountered an unknown error while processing a request*/
		SERVER_SHUTTING_DOWN,
		GAME_FULL, /*Client tried to join a game that is full*/
		GAME_LISTING, /*List of currently joinable games*/
		GAME_STATE,
		ALREADY_PLAYED, /*The client already played the current turn*/
		GAME_ALREADY_FINISHED, /*Response if the client tries to attack before noticing the game was finished*/

		RESPONSE_CODE_MAX, //DO NOT PLACE VALUES AFTER THIS; DO NOT BREAK SEQUENCE
	};

	struct GameCreateMessage : public Message {
	/*Sent by clients to reate new game rooms*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = GAME_CREATE;
	public:
		uint8_t numPlayers;
		uint32_t timeout;
		std::string gamePassword;
		uint8_t gridSize;
		uint8_t numPlanes;
		bool headshots;
		bool reveal;

		bytes serialize() const override {
			return serializeByte(_MESSAGE_TYPE) + serializeByte(numPlayers) 
				+ serializeLong(timeout) + serializeString(gamePassword) + serializeByte(gridSize)
				+ serializeByte(numPlanes) + serializeByte((uint8_t)headshots) + serializeByte((uint8_t)reveal);
		}

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~GameCreateMessage() override { Message::~Message(); }

		inline static GameCreateMessage* deserialize(const char* bytes, unsigned len)  {
			GameCreateMessage* ret = new GameCreateMessage;

			const char *end = bytes + len;
			ret->numPlayers = Message::readByte(&bytes, end);
			ret->timeout = Message::readLong(&bytes, end);
			ret->gamePassword = Message::readString(&bytes, end);
			ret->gridSize = Message::readByte(&bytes, end);
			ret->numPlanes = Message::readByte(&bytes, end);
			ret->headshots = (Message::readByte(&bytes, end) != 0)?true:false;
			ret->reveal = (Message::readByte(&bytes, end) != 0) ? true : false;

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

	private:
		//The server cannot send game create messages, so do not allow instantiation
		inline GameCreateMessage() { }
	};

	struct GameJoinMessage : public Message {
	/*Sent by clients to request info about games*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = GAME_JOIN;
	public:
		uint32_t gameID;
		std::string gamePassword;

		bytes serialize() const override 
			{ return serializeByte(_MESSAGE_TYPE) + serializeLong(gameID) + serializeString(gamePassword); }

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~GameJoinMessage() override { Message::~Message(); }

		inline static GameJoinMessage* deserialize(const char* bytes, unsigned len) {
			GameJoinMessage* ret = new GameJoinMessage;

			const char *end = bytes + len;
			ret->gameID = Message::readLong(&bytes, end);
			ret->gamePassword = Message::readString(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

	private:
		//The server cannot send game join messages, so do not allow instantiation
		inline GameJoinMessage() { }
	};

	struct GameLeaveMessage : public Message {
	/*Sent by clients to signal they no longer want live updates about a game*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = GAME_LEAVE;
	public:
		uint32_t gameID;

		bytes serialize() const override {
			return serializeByte(_MESSAGE_TYPE) + serializeLong(gameID);
		}

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~GameLeaveMessage() override { Message::~Message(); }

		inline static GameLeaveMessage* deserialize(const char* bytes, unsigned len) {
			GameLeaveMessage* ret = new GameLeaveMessage;

			const char *end = bytes + len;
			ret->gameID = Message::readLong(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

	private:
		//The server cannot send game leave messages, so do not allow instantiation
		inline GameLeaveMessage() { }
	};

	struct AccountRegisterMessage : public Message {
	/*Sent by clients to register new accounts; Server will respond with success status*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = ACCOUNT_REGISTER;
	public:
		std::string username;
		utf16string password;

		bytes serialize() const override
			{ return serializeByte(_MESSAGE_TYPE) + serializeString(username) + serializeUTF16(password); }

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~AccountRegisterMessage() override { Message::~Message(); }

		inline static AccountRegisterMessage* deserialize(const char* bytes, unsigned len) {
			AccountRegisterMessage* ret = new AccountRegisterMessage;

			const char *end = bytes + len;
			ret->username = Message::readString(&bytes, end);
			ret->password = Message::readUTF8(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

	private:
		//The server cannot send account registration messages, so do not allow instantiation
		inline AccountRegisterMessage() { }
	};

	struct AccountLoginMessage : public Message {
	/*Sent by clients to check credentials; Server will respond with success status*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = ACCOUNT_LOGIN;
	public:
		std::string username;
		utf16string password;

		bytes serialize() const override
			{ return serializeByte(_MESSAGE_TYPE) + serializeString(username) + serializeUTF16(password); }

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~AccountLoginMessage() override { Message::~Message(); }

		inline static AccountLoginMessage* deserialize(const char* bytes, unsigned len) {
			AccountLoginMessage* ret = new AccountLoginMessage;

			const char *end = bytes + len;
			ret->username = Message::readString(&bytes, end);
			ret->password = Message::readUTF8(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

	private:
		//The server cannot send account registration messages, so do not allow instantiation
		inline AccountLoginMessage() { }
	};

	struct PlayerJoinedMessage : public Message {
	/*Sent by the server to notify clients of new players joining games they are in*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = EVENT_PLAYER_JOINED_GAME;
	public:
		uint32_t gameID;
		std::string playerName;

		bytes serialize() const override
		{ return _serialized.empty() ? (_serialized = serializeByte(_MESSAGE_TYPE) + serializeLong(gameID) 
				+ serializeString(playerName)) : _serialized; }

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~PlayerJoinedMessage() override { Message::~Message(); }

		inline static PlayerJoinedMessage* deserialize(const char* bytes, unsigned len) {
			PlayerJoinedMessage* ret = new PlayerJoinedMessage;
			if (ret->getMessageType() == EVENT_PLAYER_JOINED_GAME)
				throw ProtocolViolationException("This message should only be sent by the server");

			const char *end = bytes + len;
			ret->gameID = Message::readLong(&bytes, end);
			ret->playerName = Message::readString(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

		inline PlayerJoinedMessage(uint32_t gameID, const std::string& playerName)
			: gameID(gameID), playerName(playerName) { }

	private:
		inline PlayerJoinedMessage() { }
		mutable std::string _serialized;
	};

	struct PlayerLeftMessage : public Message {
		/*Sent by the server to notify clients of players leaving games*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = EVENT_PLAYER_LEFT_GAME;
	public:
		uint32_t gameID;
		std::string playerName;

		bytes serialize() const override
		{ return _serialized.empty() ? (_serialized = serializeByte(_MESSAGE_TYPE) + serializeLong(gameID) 
				+ serializeString(playerName)) : _serialized; }

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~PlayerLeftMessage() override { Message::~Message(); }

		inline static PlayerLeftMessage* deserialize(const char* bytes, unsigned len) {
			PlayerLeftMessage* ret = new PlayerLeftMessage;
			if (ret->getMessageType() == EVENT_PLAYER_LEFT_GAME)
				throw ProtocolViolationException("This message should only be sent by the server");

			const char *end = bytes + len;
			ret->gameID = Message::readLong(&bytes, end);
			ret->playerName = Message::readString(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

		inline PlayerLeftMessage(uint32_t gameID, const std::string& playerName)
			: gameID(gameID), playerName(playerName) { }

	private:
		inline PlayerLeftMessage() { }
		mutable std::string _serialized;
	};

	struct GameCreatedMessage : public Message {
	/*Sent by the server to notify clients of new players joining games they are in*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = EVENT_GAME_CREATED;
	public:
		uint32_t gameID;
		uint8_t numPlayers;
		uint32_t timeout;
		uint8_t gridSize;
		uint8_t numPlanes;
		bool headshots;
		bool reveal;

		bytes serialize() const override {
			return _serialized.empty() ? (_serialized = serializeByte(_MESSAGE_TYPE) + serializeLong(gameID) 
				+ serializeByte(numPlayers) + serializeLong(timeout) + serializeByte(gridSize) 
				+ serializeByte(numPlanes) + serializeByte((uint8_t)headshots) + serializeByte((uint8_t)reveal))
				: _serialized;
		}

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~GameCreatedMessage() override { Message::~Message(); }

		inline static GameCreatedMessage* deserialize(const char* bytes, unsigned len) {
			GameCreatedMessage* ret = new GameCreatedMessage;
			if (ret->getMessageType() == EVENT_PLAYER_JOINED_GAME)
				throw ProtocolViolationException("This message should only be sent by the server");

			const char *end = bytes + len;
			ret->gameID = Message::readLong(&bytes, end);
			ret->numPlayers = Message::readByte(&bytes, end);
			ret->timeout = Message::readLong(&bytes, end);
			ret->gridSize = Message::readByte(&bytes, end);
			ret->numPlanes = Message::readByte(&bytes, end);
			ret->headshots = (Message::readByte(&bytes, end) != 0) ? true : false;
			ret->reveal = (Message::readByte(&bytes, end) != 0) ? true : false;

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

		inline GameCreatedMessage(uint32_t gameID, uint8_t numPlayers, uint32_t timeout, 
			uint8_t gridSize, uint8_t numPlanes, bool headshots, bool reveal)
			: gameID(gameID), numPlayers(numPlayers), timeout(timeout), gridSize(gridSize),
			numPlanes(numPlanes), headshots(headshots), reveal(reveal) { }

	private:
		inline GameCreatedMessage() { }
		mutable std::string _serialized;
	};

	struct TurnStartedMessage : public Message {
		/*Sent by the server to notify clients of new turns in games they are in*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = EVENT_TURN_STARTED;
	public:
		uint32_t gameID;
		uint16_t turnNumber;

		bytes serialize() const override
		{ return _serialized.empty() ? (_serialized = serializeByte(_MESSAGE_TYPE) + serializeLong(gameID) 
				+ serializeShort(turnNumber)) : _serialized; }

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~TurnStartedMessage() override { Message::~Message(); }

		inline static TurnStartedMessage* deserialize(const char* bytes, unsigned len) {
			TurnStartedMessage* ret = new TurnStartedMessage;
			if (ret->getMessageType() == EVENT_TURN_STARTED)
				throw ProtocolViolationException("This message should only be sent by the server");

			const char *end = bytes + len;
			ret->gameID = Message::readLong(&bytes, end);
			ret->turnNumber = Message::readShort(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

		inline TurnStartedMessage(uint32_t gameID, uint16_t turnNumber)
			: gameID(gameID), turnNumber(turnNumber) { }

	private:
		inline TurnStartedMessage() { }
		mutable std::string _serialized;
	};

	struct PlaneLocationsMessage : public Message {
		/*Sent by the server to notify clients of new turns in games they are in*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = PLANE_LOCATIONS;
	public:
		uint32_t gameID;
		std::string playerName;
		std::string locations;

		bytes serialize() const override
		{
			return _serialized.empty() ? (_serialized = serializeByte(_MESSAGE_TYPE) + serializeLong(gameID) 
				+serializeString(playerName) + serializeString(locations)) :_serialized;
		}

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~PlaneLocationsMessage() override { Message::~Message(); }

		inline static PlaneLocationsMessage* deserialize(const char* bytes, unsigned len) {
			PlaneLocationsMessage* ret = new PlaneLocationsMessage;

			const char *end = bytes + len;
			ret->gameID = Message::readLong(&bytes, end);
			ret->playerName = Message::readString(&bytes, end);
			ret->locations = Message::readString(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

		inline PlaneLocationsMessage(unsigned gameID, const std::string& playerName, const std::string& locations)
			: gameID(gameID), playerName(playerName), locations(locations) { }

	private:
		inline PlaneLocationsMessage() { }
		mutable std::string _serialized;
	};

	struct AttackCellMessage : public Message {
		/*Sent by the server to notify clients of new turns in games they are in*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = ATTACK_CELL;
	public:
		uint32_t gameID;
		std::string playerName;
		uint8_t x;
		uint8_t y;

		bytes serialize() const override
		{
			return _serialized.empty() ? (_serialized = serializeByte(_MESSAGE_TYPE) + serializeLong(gameID)
				+ serializeString(playerName) + serializeByte(x) + serializeByte(y)) : _serialized;
		}

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~AttackCellMessage() override { Message::~Message(); }

		inline static AttackCellMessage* deserialize(const char* bytes, unsigned len) {
			AttackCellMessage* ret = new AttackCellMessage;

			const char *end = bytes + len;
			ret->gameID = Message::readLong(&bytes, end);
			ret->playerName = Message::readString(&bytes, end);
			ret->x = Message::readByte(&bytes, end);
			ret->y = Message::readByte(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

		inline AttackCellMessage(unsigned gameID, const std::string& playerName, uint8_t x, uint8_t y)
			: gameID(gameID), playerName(playerName), x(x), y(y) { }

	private:
		inline AttackCellMessage() { }
		mutable std::string _serialized;
	};

	struct ResponseMessage : public Message {
	/*May be sent by server in response to certain messages*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = SERVER_RESPONSE;
	public:
		RESPONSE_CODE errorCode;
		std::string metadata;

		bytes serialize() const override {
			if (errorCode >= RESPONSE_CODE_MAX) throw std::logic_error("Invalid error response code");
			return serializeByte(_MESSAGE_TYPE) + serializeByte(errorCode) + serializeString(metadata); 
		}

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~ResponseMessage() override { Message::~Message(); }

		inline static ResponseMessage* deserialize(const char* bytes, unsigned len) {
			ResponseMessage* ret = new ResponseMessage;
			if (ret->getMessageType() == SERVER_RESPONSE) {
				throw ProtocolViolationException("This message should only be sent by the server");
			}

			const char *end = bytes + len;
			ret->errorCode = (RESPONSE_CODE) Message::readByte(&bytes, end);
			if (ret->errorCode >= RESPONSE_CODE_MAX) 
				throw FormatException("Invalid error response code");

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

		inline ResponseMessage(RESPONSE_CODE errorCode, const std::string& metadata = "") : metadata(metadata), errorCode(errorCode)
			{ if (errorCode >= RESPONSE_CODE_MAX) throw std::logic_error("Invalid error response code");}

	private:
		inline ResponseMessage() { }
	};

	struct ConnectionClosedMessage : public Message {
		/*Notify the client it was disconnected*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = CONNECTION_CLOSED;
	public:
		RESPONSE_CODE errorCode;
		std::string metadata;

		bytes serialize() const override {
			if (errorCode >= RESPONSE_CODE_MAX) throw std::logic_error("Invalid error response code");
			return serializeByte(_MESSAGE_TYPE) + serializeByte(errorCode) + serializeString(metadata);
		}

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~ConnectionClosedMessage() override { Message::~Message(); }

		inline static ConnectionClosedMessage* deserialize(const char* bytes, unsigned len) {
			ConnectionClosedMessage* ret = new ConnectionClosedMessage;
			if (ret->getMessageType() == CONNECTION_CLOSED) {
				throw ProtocolViolationException("This message should only be sent by the server");
			}

			const char *end = bytes + len;
			ret->errorCode = (RESPONSE_CODE)Message::readByte(&bytes, end);
			if (ret->errorCode >= RESPONSE_CODE_MAX)
				throw FormatException("Invalid error response code");

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

		inline ConnectionClosedMessage(RESPONSE_CODE errorCode, const std::string& metadata = "") : metadata(metadata), errorCode(errorCode)
		{ if (errorCode >= RESPONSE_CODE_MAX) throw std::logic_error("Invalid error response code"); }

	private:
		inline ConnectionClosedMessage() { }
	};

	struct PlayerDeathMessage : public Message {
		/*Sent by clients to signal they no longer want live updates about a game*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = PLAYER_DIED;
	public:
		uint32_t gameID;
		uint16_t turnNumber;

		bytes serialize() const override {
			return serializeByte(_MESSAGE_TYPE) + serializeLong(gameID) + serializeShort(turnNumber);
		}

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~PlayerDeathMessage() override { Message::~Message(); }

		inline static PlayerDeathMessage* deserialize(const char* bytes, unsigned len) {
			PlayerDeathMessage* ret = new PlayerDeathMessage;

			const char *end = bytes + len;
			ret->gameID = Message::readLong(&bytes, end);
			ret->turnNumber = Message::readShort(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

	private:
		inline PlayerDeathMessage() { }
	};

	struct GameEndMessage : public Message {
		/*Sent by clients to signal they no longer want live updates about a game*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = GAME_FINISHED;
	public:
		uint32_t gameID;

		bytes serialize() const override {
			return serializeByte(_MESSAGE_TYPE) + serializeLong(gameID);
		}

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~GameEndMessage() override { Message::~Message(); }

		inline static GameEndMessage* deserialize(const char* bytes, unsigned len) {
			GameEndMessage* ret = new GameEndMessage;

			const char *end = bytes + len;
			ret->gameID = Message::readLong(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

	private:
		inline GameEndMessage() { }
	};

	struct GetGameMessage : public Message {
		/*Sent by clients to signal they no longer want live updates about a game*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = GET_GAME_STATE;
	public:
		uint32_t gameID;

		bytes serialize() const override {
			return serializeByte(_MESSAGE_TYPE) + serializeLong(gameID);
		}

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~GetGameMessage() override { Message::~Message(); }

		inline static GetGameMessage* deserialize(const char* bytes, unsigned len) {
			GetGameMessage* ret = new GetGameMessage;

			const char *end = bytes + len;
			ret->gameID = Message::readLong(&bytes, end);

			if (bytes != end) //Message was not an exact fit to length; Mistake on client side
				throw FormatException("Actual length doesn't match expected length for message");

			return ret;
		}

	private:
		//The server cannot send game leave messages, so do not allow instantiation
		inline GetGameMessage() { }
	};

	struct SocketCloseMessage : public Message {
	/*Posted by sockets in their message streams when they are closed*/
	private: const static MESSAGE_TYPE _MESSAGE_TYPE = SOCKET_CLOSED;
	public:
		enum REASON : uint8_t {
			CLIENT_DISCONNECTED,
			DISCONNECTED_BY_SERVER,
			CONNECTION_DROPPED
		} reason = CONNECTION_DROPPED;

		bytes serialize() const override { throw std::logic_error("This message is for internal use only"); }

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~SocketCloseMessage() override { Message::~Message(); }

		inline static SocketCloseMessage* deserialize(const char*, unsigned) { throw std::logic_error("This message is for internal use only"); }

		inline SocketCloseMessage(REASON reason) : reason(reason) { }

	private:
		inline SocketCloseMessage() { }
	};

	struct EmptyMessage : public Message {
	public:
		MESSAGE_TYPE _MESSAGE_TYPE;

		bytes serialize() const override { return Message::serializeByte(_MESSAGE_TYPE); }

		MESSAGE_TYPE getMessageType() const override { return _MESSAGE_TYPE; }
		~EmptyMessage() override { Message::~Message(); }

		inline static EmptyMessage* deserialize(const char*, unsigned) { throw std::logic_error("This message is for internal use only"); }

		inline EmptyMessage(MESSAGE_TYPE type) : _MESSAGE_TYPE(type) { }
	};
}

#endif