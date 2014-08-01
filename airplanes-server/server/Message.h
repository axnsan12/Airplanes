#ifndef __MESSAGE_H__
#define __MESSAGE_H__
#include <cstdint>
#include <string>
#include <exception>
#include <sstream>

namespace airplanes {
	enum MESSAGE_TYPE : uint8_t;

	struct Message {
	public:
		typedef std::string bytes;
		typedef std::wstring utf16string;

		static Message* deserialize(const char* bytes, unsigned len);

		virtual bytes serialize() const = 0;
		inline virtual std::string print() const {
			bytes ser = serialize();
			std::ostringstream out;
			for (char c : ser)
				out << (unsigned)(unsigned char)c << ' ';
			out << "(length " << std::dec << ser.length() << ")";
			return out.str();
		};
		virtual MESSAGE_TYPE getMessageType() const = 0;

		Message() { }
		Message(const Message&) = delete;
		Message& operator=(const Message&) = delete;
		virtual ~Message() = 0 { }

		class ProtocolViolationException : public std::runtime_error
		{
		public:
			inline ProtocolViolationException(const std::string& what_arg) : std::runtime_error(what_arg) { }
		};

		class FormatException : public ProtocolViolationException
		{
		public:
			inline FormatException() : FormatException("Message layout expectations violated") { }
			inline FormatException(const std::string& what_arg) : ProtocolViolationException(what_arg) { }
		};

	protected:
		static std::string	readString(const char** source, const char* bound);
		static std::wstring	readUTF8(const char** source, const char* bound);
		static uint8_t		readByte(const char** source, const char* bound);
		static uint16_t		readShort(const char** source, const char* bound);
		static uint32_t		readLong(const char** source, const char* bound);

		static bytes serializeString(const std::string&);
		static bytes serializeUTF16(const std::wstring&);
		static bytes serializeByte(uint8_t);
		static bytes serializeShort(uint16_t);
		static bytes serializeLong(uint32_t);
	};
}

#endif