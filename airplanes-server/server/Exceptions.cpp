#include "stdafx.h"
#include "exceptions.h"
#include "log.h"

namespace airplanes {

	SocketException::SocketException(const std::string& msg) : SocketException(msg, WSAGetLastError()) { }

	SocketException::SocketException(const std::string& msg, int errorCode) : errorCode(errorCode)
	{
		this->msg = msg;
		if (errorCode != 0)
		{
			this->msg += (" with error " + std::to_string(errorCode) + ": ");
			LPSTR err = NULL;
			FormatMessageA(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM,
				NULL, errorCode, 0, (LPSTR)&err, 0, NULL);
			this->msg += err;
			LocalFree(err);
		}
		log::server(msg, log::LEVEL_EXCEPTION);
	}

	NetworkProtocolException::NetworkProtocolException(const std::string& msg) : SocketException(msg) { }
	NetworkProtocolException::NetworkProtocolException(const std::string& msg, int errorCode) : SocketException(msg, errorCode) { }
}