#ifndef __AIR_SERVERSOCKET_H__
#define __AIR_SERVERSOCKET_H__
#include <WinSock2.h>
#include <cstdint>
#include <string>
#include <cstring>
#include <vector>

#pragma comment(lib, "Ws2_32.lib")

namespace airplanes {
	class TCPSocket;
	
	///<summary>TCP socket for establishing connections as a server</summary>
	class ServerSocket
	{
	public:
		ServerSocket() {}
		///<summary>Create a socket to listen on a port</summary>
		///<param name='port'>The port to listen on</param>
		///<param name='hostname'>Host IP addres to bind to. Default is all available addresses. Only IPv4 currently supported</param>
		///<exception cref='Socket::exception'/>
		ServerSocket(uint16_t port, const std::string& hostname = "0.0.0.0");

		///<summary><para>Accept an incoming connection</para>
		///<remarks>By default, this function blocks the calling thread until a connection is established</remarks></summary>
		///<param name='block'><para>Specifies whether to block the calling thread until a new connection is established</para>
		///<para>If set to false, the function returns null if no connection is pending</para></param>
		///<returns>A new TCPSocket ready to be used for communicating with the client, or null 
		///if no connection is pending and <paramref name='block'/> is false</returns>
		///<exception cref='Socket::exception'/>
		TCPSocket* accept(bool block = true);

		void shutdown();

		//Move constructor
		ServerSocket(ServerSocket&&);
		ServerSocket& operator=(ServerSocket&&);

		~ServerSocket();

		class ServerShutdownException : public std::exception { };

	protected:
		SOCKET mSocket = INVALID_SOCKET;
		const std::string address;

	private:

		//Copying a socket makes no sense
		ServerSocket(const ServerSocket&);
		ServerSocket& operator=(const ServerSocket&);
	};

}

#endif

