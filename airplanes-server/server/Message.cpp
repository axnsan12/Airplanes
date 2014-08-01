#include "stdafx.h"
#include "messages.hpp"
#include <WinSock2.h>
#include "include\utf8.h"

namespace airplanes {
	Message* Message::deserialize(const char* bytes, unsigned len)
	{
		if (len == 0)
			return nullptr;

		uint8_t TYPE = *bytes;
		bytes++, len--;
		switch (TYPE) {
		case GAME_CREATE:				return GameCreateMessage::deserialize(bytes, len);
		case GAME_JOIN:					return GameJoinMessage::deserialize(bytes, len);
		case GAME_LEAVE:				return GameLeaveMessage::deserialize(bytes, len);
		case ACCOUNT_REGISTER:			return AccountRegisterMessage::deserialize(bytes, len);
		case ACCOUNT_LOGIN:				return AccountLoginMessage::deserialize(bytes, len);
		case GET_GAME_LISTING:			return new EmptyMessage(GET_GAME_LISTING);
		case GET_GAME_STATE:			return GetGameMessage::deserialize(bytes, len);
		case PLAYER_DIED:				return PlayerDeathMessage::deserialize(bytes, len);
		case GAME_FINISHED:				return GameEndMessage::deserialize(bytes, len);
			
		case PLANE_LOCATIONS:			return PlaneLocationsMessage::deserialize(bytes, len);
		case ATTACK_CELL:				return AttackCellMessage::deserialize(bytes, len);

		case EVENT_PLAYER_JOINED_GAME:	return PlayerJoinedMessage::deserialize(bytes, len);
		case SERVER_RESPONSE:			return ResponseMessage::deserialize(bytes, len);

		default:
			throw ProtocolViolationException("Message type has no associated class." 
				"Sent wrong type, or forgot to define a class?");
		}
	}

	uint8_t Message::readByte(const char** source, const char* bound)
	{
		if (*source < bound)
			return *((*source)++);

		throw FormatException();
	}

	uint16_t Message::readShort(const char** source, const char* bound)
	{
		const char *bytes = *source;
		if (bytes + 1 < bound)
		{
			union {
				uint8_t bytes[2];
				uint16_t value;
			} ret;
			(*source) += 2;
			ret.bytes[0] = *bytes++; ret.bytes[1] = *bytes;
 			return ntohs(ret.value);
		}

		throw FormatException();
	}

	uint32_t Message::readLong(const char** source, const char* bound)
	{
		const char *bytes = *source;
		if (bytes + 3 < bound)
		{
			union {
				uint8_t bytes[4];
				uint32_t value;
			} ret;
			(*source) += 4;
			ret.bytes[0] = *bytes++; ret.bytes[1] = *bytes++; ret.bytes[2] = *bytes++; ret.bytes[3] = *bytes;
			return ntohl(ret.value);
		}

		throw FormatException();
	}

	std::string Message::readString(const char** source, const char* bound)
	{
		unsigned len = readShort(source, bound);
		const char *bytes = *source;
		if (bytes + len - 1 < bound)
		{
			(*source) += len;
			return std::string(bytes, len);
		}

		throw FormatException();
	}

	std::wstring Message::readUTF8(const char** source, const char* bound)
	{
		unsigned len = readShort(source, bound);
		const char *bytes = *source;
		if (bytes + len - 1 < bound)
		{
			(*source) += len;
			std::wstring ret;
			try {
				utf8::utf8to16(bytes, bytes + len, std::back_inserter(ret));
			} catch (const utf8::exception&) { throw FormatException("Bad UTF-8"); }
				
			return ret;
		}

		throw FormatException();
	}

	Message::bytes Message::serializeString(const std::string& str)
	{
		bytes ret;
		ret.reserve(str.length() + 2);
		uint16_t len = htons((uint16_t)str.length());
		ret.append(std::string((char*)&len, 2));
		ret.append(str);
		return ret;
	}
	Message::bytes Message::serializeUTF16(const std::wstring& str)
	{
		std::string u;
		utf8::utf16to8(str.cbegin(), str.cend(), std::back_inserter(u));
		return serializeString(u);
	}
	Message::bytes Message::serializeByte(uint8_t x)
	{
		return std::string((char*)&x, 1);
	}
	Message::bytes Message::serializeShort(uint16_t x)
	{
		x = htons(x);
		return std::string((char*)&x, 2);
	}
	Message::bytes Message::serializeLong(uint32_t x)
	{
		x = htonl(x);
		return std::string((char*)&x, 4);
	}
}