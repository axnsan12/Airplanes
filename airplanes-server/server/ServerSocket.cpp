#include "stdafx.h"
#include "ServerSocket.h"
#include "TCPSocket.h"
#include "airplanes.h"
#include "log.h"
#include "Exceptions.h"

namespace airplanes {

	ServerSocket::ServerSocket(uint16_t port, const std::string& hostname) 
		: address(hostname + ':' + std::to_string(port))
	{
		if (WSAStartup() != 0)
			throw SocketException("WSAStartup failed");

		mSocket = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
		if (mSocket == INVALID_SOCKET)
			throw SocketException("socket() failed");

		sockaddr_in host;
		host.sin_family = AF_INET;
		host.sin_addr.s_addr = inet_addr(hostname.c_str());
		if (host.sin_addr.s_addr == INADDR_NONE) {
			closesocket(mSocket);
			throw SocketException("bad hostname \"" + hostname + '\"', 0);
		}
		host.sin_port = htons(port);

		if (bind(mSocket, (SOCKADDR*)&host, sizeof (host)) == SOCKET_ERROR) {
			int WSAError = WSAGetLastError();
			closesocket(mSocket);
			if (WSAError == WSAEADDRINUSE)
				throw SocketException("port already in use (" + address + ')', 0);

			throw SocketException("bind() failed on " + address, WSAError);
		}

		if (listen(mSocket, SOMAXCONN) == SOCKET_ERROR) {
			int WSAError = WSAGetLastError();
			closesocket(mSocket);
			throw SocketException("listen() failed", WSAError);
		}

		printf("Server started successfully.\n");
		log::server("Started server on address " + this->address);
	}

	TCPSocket* ServerSocket::accept(bool block)
	{
		if (mSocket == INVALID_SOCKET)
			throw SocketException("cannot accept connection after server is shutdown", 0);

		SOCKET client = INVALID_SOCKET;
		if (airplanes::WSAStartup() != 0)
			throw SocketException("WSAStartup failed");

		unsigned long uBlock = (block) ? 0 : 1;
		if (ioctlsocket(mSocket, FIONBIO, &uBlock) == SOCKET_ERROR)
			throw SocketException("failed to set blocking mode");

		sockaddr_in addr;
		int len = sizeof(addr);
		addr.sin_family = AF_INET;
		addr.sin_addr.s_addr = INADDR_NONE;
		addr.sin_port = 0;
		while (client == INVALID_SOCKET)
		{
			client = ::accept(mSocket, (sockaddr*)&addr, &len);
			if (client == INVALID_SOCKET) {
				int WSAError = WSAGetLastError();

				if (WSAError == WSAEWOULDBLOCK && block == false)
					//No connection pending
					return nullptr; 

				if (WSAError == WSAEINTR)
					throw ServerShutdownException();

				if (WSAError != WSAECONNRESET)
					throw SocketException("accept() failed", WSAError);
			}
		}

		std::string address = std::string(inet_ntoa(addr.sin_addr)) + ':' + std::to_string(addr.sin_port);
		log::server("Accepted connection from " + address);
		return new TCPSocket(client, address);
	}

	ServerSocket::ServerSocket(ServerSocket&& old) : address(std::move(old.address))
	{
		this->mSocket = old.mSocket;
		old.mSocket = INVALID_SOCKET;
	}

	ServerSocket& ServerSocket::operator=(ServerSocket&& old)
	{
		this->mSocket = old.mSocket;
		old.mSocket = INVALID_SOCKET;
		const_cast<std::string&>(this->address) = std::move(old.address);
		return *this;
	}

	void ServerSocket::shutdown()
	{
		if (mSocket != INVALID_SOCKET)
		{
			SOCKET sock = mSocket;
			mSocket = INVALID_SOCKET;
			::shutdown(sock, SD_BOTH);
			closesocket(sock);
			log::server("Server shutting down (" + address + ')');
			printf("Server shutting down.\n");
		}
	}

	ServerSocket::~ServerSocket()
	{
		shutdown();
	}

}
