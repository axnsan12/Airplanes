#include "stdafx.h"
#include "TCPSocket.h"
#include "airplanes.h"
#include "Exceptions.h"
#include "log.h"

namespace airplanes {

	int WSAStartup()
	{
		static bool started = false;

		if (started == true)
			return 0;

		WSADATA wsaData;
		int iResult = ::WSAStartup(MAKEWORD(2, 2), &wsaData);
		started = (iResult == 0);
		return iResult;
	}

	TCPSocket::TCPSocket(SOCKET socket, const std::string& address) : mRemoteAddress(address)
	{
		if (socket != INVALID_SOCKET)
		{
			printf("Accepted new connection from %s\n", address.c_str());
			mSocket = socket;
		}
	}

	Message* TCPSocket::nextMessage(bool block)
	{
		if (!isValid())
			return nullptr;

		try {
			while (block == true && mMessageQueue.empty())
				recv(true);
		} catch (const NetworkProtocolException&) { disconnect(); }

		if (mMessageQueue.empty()) {
			if (block == true)
				throw std::logic_error("Blocking socket returned with empty message queue");

			return nullptr;
		}
		
		Message* ret = mMessageQueue.front();
		mMessageQueue.pop();
		return ret;
	}

	bool TCPSocket::recv(bool block)
	{
		if (mSocket == INVALID_SOCKET) {
			if (!_closed)
				log::server("Attempting to receive on an invalid socket", log::LEVEL_WARNING);

			return false;
		}

		unsigned long uBlock = (block) ? 0 : 1;
		if (ioctlsocket(mSocket, FIONBIO, &uBlock) == SOCKET_ERROR)
			throw SocketException("Failed to set blocking mode");

		int bytesReceived = ::recv(mSocket, (char*)_recvbuf, sizeof(_recvbuf), 0);
		int WSAError = 0;
		if (bytesReceived < 0)
		{
			WSAError = WSAGetLastError();
			switch (WSAError)
			{
			case WSAEWOULDBLOCK: //No data pending
				return true;

			case WSAEMSGSIZE: //Data larger than buffer
				bytesReceived = sizeof(_recvbuf);
				break;

			case WSAECONNRESET: case WSAETIMEDOUT: case WSAENETRESET: //Client dropped
				log::server("Lost connection to " + mRemoteAddress);
				mMessageQueue.push(new SocketCloseMessage(SocketCloseMessage::CONNECTION_DROPPED));
				close();
				return false;

			case WSAECONNABORTED: //Fatal error on socket
				log::server("Unknown failure on connection to " + mRemoteAddress
					+ ". Dropping client from server.");
				mMessageQueue.push(new SocketCloseMessage(SocketCloseMessage::CONNECTION_DROPPED));
				close();
				return false;

			case WSAEINTR:
				mMessageQueue.push(new SocketCloseMessage(SocketCloseMessage::DISCONNECTED_BY_SERVER));
				return false;

			default:
				throw SocketException("Failed to recv data from socket", WSAError);
			}
		}
		if (bytesReceived == 0) {
			//Client disconnected gracefully
			log::server("Client " + mRemoteAddress + " closed the connection");
			mMessageQueue.push(new SocketCloseMessage(SocketCloseMessage::CLIENT_DISCONNECTED));
			close();
			return false;
		}

		uint8_t *p = _recvbuf, *end = _recvbuf + bytesReceived;

		while (p < end)
		{
			//Attempt to resolve header
			if (_headerpos == 0)
			{
				if (*p++ == protocol_header_1)
					_headerpos = 1;
				else {
					log::dbg_socket("Failed to match header, client spewing garbage?",
						mRemoteAddress, p - 1, std::min(protocol_header_length, end - p));
					throw NetworkProtocolException("Protocol violation by client " + mRemoteAddress + "; see socket debug log", 0);
				}
			}
			if (_headerpos == 1 && p < end)
			{
				if (*p == protocol_header_2)
					_headerpos = 2, ++p;
				else {
					_headerpos = 0;
					log::dbg_socket("Failed to match header, client spewing garbage?",
						mRemoteAddress, p - 1, std::min(protocol_header_length, end - p));
					throw NetworkProtocolException("Protocol violation by client " + mRemoteAddress + "; see socket debug log", 0);
				}
			}
			if (_headerpos == 2 && p < end)
			{
				uint8_t ver = *p;
				if ((ver & MAJOR_VERSION_MASK) != (protocol_version & MAJOR_VERSION_MASK)) {
					_headerpos = 0, log::dbg_socket("Major version mismatch, expected "
						+ std::to_string((protocol_version & MAJOR_VERSION_MASK)) + ", got "
						+ std::to_string((ver & MAJOR_VERSION_MASK)) + "; dropping message from " + mRemoteAddress);
					throw NetworkProtocolException("Protocol violation by client " + mRemoteAddress + "; see socket debug log", 0);
				}
				else _headerpos = 3, ++p;
			}
			if (_headerpos == 3 && p < end)
				_msglenbuf.bytes[0] = *p++, _headerpos = 4;
			if (_headerpos == 4 && p < end)
				_msglenbuf.bytes[1] = *p++, _headerpos = 5;

			if (_headerpos == 5)
			{
				//Header was matched, begin building message
				_msglen = ntohs(_msglenbuf.value);
				_msgbuf = std::unique_ptr<char>(new char[_msglen]);
				_msgpos = 0;
				_headerpos = 6;
			}

			if (_headerpos == 6)
			{
				//Message building is in progress
				unsigned num_readable = std::min((unsigned)(end - p), _msglen - _msgpos);
				memcpy(_msgbuf.get() + _msgpos, p, num_readable);
				p += num_readable;
				_msgpos += num_readable;
				assert("Eror code 10T" && _msgpos <= _msglen);
			}

			if (_msgpos == _msglen && _msglen > 0)
			{
				//Succesfully got a whole message!
				Message *msg = nullptr;
				try {
					msg = Message::deserialize(_msgbuf.get(), _msglen);
				}
				catch (const Message::ProtocolViolationException& e) {
					log::dbg_socket("Client " + mRemoteAddress + " sent malformed message of type "
						+ std::to_string(*(uint8_t*)_msgbuf.get()) + ": " + e.what());
					throw NetworkProtocolException("Protocol violation by client " + mRemoteAddress + "; see socket debug log", 0);
				}

				printf("Received message %s from %s\n", msg->print().c_str(), mRemoteAddress.c_str());

				mMessageQueue.push(msg);
				_headerpos = _msgpos = _msglen = 0;
				_msgbuf.reset();
			}
		}

		//If the message was truncated because of buffer length, retrieve what's left
		if (WSAError == WSAEMSGSIZE)
			return recv();

		return true;
	}

	void TCPSocket::sendMessage(const Message* msg, bool deleteAfter)
	{
		if (msg == nullptr)
			return;

		if (mSocket == INVALID_SOCKET) {
			if (!_closed)
				log::server("Attempting to receive on an invalid socket", log::LEVEL_WARNING);
			
			if (deleteAfter) delete msg;
			return;
		}

		Message::bytes mbuf = msg->serialize();
		union {
			uint8_t bytes[2];
			uint16_t value;
		} len;
		len.value = htons((uint16_t)mbuf.length());

		uint8_t header[protocol_header_length];
		header[0] = protocol_header_1;
		header[1] = protocol_header_2;
		header[2] = protocol_version;
		header[3] = len.bytes[0];
		header[4] = len.bytes[1];

		//Send the header followed by the actual message
		try {
			if (send((char*)header, sizeof header) && send(mbuf.data(), mbuf.length()))
				printf("Sent message %s to %s\n", msg->print().c_str(), mRemoteAddress.c_str());
		} catch (...) { if (deleteAfter) delete msg; throw; }
		
		if (deleteAfter) delete msg;
	}

	bool TCPSocket::send(const char* buffer, int len)
	{
		if (mSocket == INVALID_SOCKET)
			return false;

		unsigned long uBlock = 0;
		if (ioctlsocket(mSocket, FIONBIO, &uBlock) == SOCKET_ERROR)
			throw SocketException("Failed to set blocking mode");

		int bytesSent = ::send(mSocket, buffer, len, 0);
		if (bytesSent == SOCKET_ERROR)
		{
			int WSAError = WSAGetLastError();
			switch (WSAError)
			{
			case WSAEHOSTUNREACH: case WSAETIMEDOUT:
				mMessageQueue.push(new SocketCloseMessage(SocketCloseMessage::CONNECTION_DROPPED));
				close();
				return false;

			case WSAECONNRESET: case WSAENETRESET: //Client dropped
				log::server("Lost connection to " + mRemoteAddress);
				mMessageQueue.push(new SocketCloseMessage(SocketCloseMessage::CONNECTION_DROPPED));
				close();
				return false;

			case WSAECONNABORTED: //Fatal error on socket
				log::server("Unknown failure on connection to " + mRemoteAddress
					+ ". Dropping client from server.");
				mMessageQueue.push(new SocketCloseMessage(SocketCloseMessage::CONNECTION_DROPPED));
				close();
				return false;

			case WSAEINTR:
				return false;

			default:
				throw SocketException("Failed to send data through socket", WSAError);
			}
		}

		if (bytesSent != len)
			throw SocketException("Failed to send all bytes. This should never happen");

		return true;
	}

	bool TCPSocket::isValid() 
	{ 
		try { recv(false); }
		catch (const NetworkProtocolException&) { disconnect(); }

		return mSocket != INVALID_SOCKET || !mMessageQueue.empty(); 
	}

	TCPSocket::TCPSocket(TCPSocket&& old) : mRemoteAddress(std::move(old.mRemoteAddress))
	{
		this->mSocket = old.mSocket;
		old.mSocket = INVALID_SOCKET;
		_headerpos = old._headerpos;
		_msglenbuf = old._msglenbuf;
		_msgbuf = std::move(old._msgbuf);
		_msglen = old._msglen;
		_msgpos = old._msgpos;
	}

	TCPSocket& TCPSocket::operator=(TCPSocket&& old)
	{
		this->mSocket = old.mSocket;
		old.mSocket = INVALID_SOCKET;
		const_cast<std::string&>(mRemoteAddress) = std::move(old.mRemoteAddress);
		_headerpos = old._headerpos;
		_msglenbuf = old._msglenbuf;
		_msgbuf = std::move(old._msgbuf);
		_msglen = old._msglen;
		_msgpos = old._msgpos;
		return *this;
	}

	void TCPSocket::disconnect(const ConnectionClosedMessage* msg, bool deleteAfter)
	{
		if (mSocket != INVALID_SOCKET)
		{
			if (msg != nullptr)
				sendMessage(msg, deleteAfter);
			log::server("Disconnecting client " + mRemoteAddress);
			printf("Disconnecting %s\n", mRemoteAddress.c_str());
			mMessageQueue.push(new SocketCloseMessage(SocketCloseMessage::DISCONNECTED_BY_SERVER));
			shutdown(mSocket, SD_BOTH);
			close();
		}
	}

	void TCPSocket::close()
	{
		if (mSocket != INVALID_SOCKET)
		{
			closesocket(mSocket);
			mSocket = INVALID_SOCKET;
			_closed = true;
		}
	}
	TCPSocket::~TCPSocket()
	{
		disconnect();
	}

}
