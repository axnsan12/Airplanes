#ifndef __AIR_TCPSOCKET_H__
#define __AIR_TCPSOCKET_H__
#include <WinSock2.h>
#include <string>
#include <cstdint>
#include <memory>
#include <queue>
#include "ServerSocket.h"
#include "messages.hpp"

#pragma comment(lib, "Ws2_32.lib")

namespace airplanes {

	///<summary>Wrapper around TCP sockets to parse protocol for client-server communications</summary>
	class TCPSocket
	{
	public:
		inline const std::string& remoteAddress() const { return mRemoteAddress; }

		///<summary>Check if the socket can still be used to receive messages
		///<para>Will cause any pending packets to be processed</para></summary>
		bool isValid();

		///<summary><para>Get the next message in this socket's queue</para>
		///<para>Returns a null pointer if there is no message pending.</para>
		///<para>Will drop the client if it violates communcation protocol.</para>
		///<para>If this function is not checked periodically, the client may time out</para></summary>
		///<param name='block'>whether to block the calling thread until a message is available</param>
		///<returns>A valid message, or null if there is no message pending</returns>
		///<exception cref='SocketException'/>
		Message* nextMessage(bool block = false);
		
		///<summary>Send a message to the client. Too much outgoing data might cause this function
		///to block while the network frees up.</summary>
		///<param name='msg'>the message to send</param>
		///<param name='deleteAfter'>if the message should be deleted by the function after sending</param>
		void sendMessage(const Message* msg, bool deleteAfter = true);

		///<summary>Forcefully disconnect the client. Can optionally send a message to explain.</summary>
		///<param name='msg'>the message to send</param>
		///<param name='deleteAfter'>if the message should be deleted by the function after sending</param>
		void disconnect(const ConnectionClosedMessage* msg = nullptr, bool deleteAfter = true);


		/*Copying a socket makes no sense*/
		TCPSocket(const TCPSocket&) = delete;
		TCPSocket& operator=(const TCPSocket&) = delete;

		~TCPSocket();

		friend ServerSocket;

	protected:
		SOCKET mSocket = INVALID_SOCKET;
		const std::string mRemoteAddress = "INVALID_SOCKET";
		std::queue<Message*> mMessageQueue;

	private:
		/*The protocol requires that messages begin with a 5-byte header consisting of
		2 verification bytes, 1 protocol version byte, and 2 message size bytes. The header is followed
		by the actual message, with length as given by the header.
		Any bytes in the stream that break this pattern are discarded.*/

		static const uint8_t protocol_header_1 = 0xDE, protocol_header_2 = 0xAD;
		static const uint8_t protocol_version = 0x01;
		static const uint8_t MAJOR_VERSION_MASK = 0xF0;
		static const int protocol_header_length = 5;

		uint8_t _recvbuf[1024];
		union {
			uint8_t bytes[2];
			uint16_t value;
		} _msglenbuf;
		std::unique_ptr<char> _msgbuf;
		unsigned _msglen = 0, _msgpos;
		unsigned _headerpos = 0;

		///<summary>Parse the socket stream to extract messages. 
		///When a new message is extracted, it will be placed in the message queue.</summary>
		///<param name='block'>whether to block the calling thread until packets are received</param>
		///<returns>false if the client is no longer connected</returns>
		///<exception cref='SocketException'/>
		bool recv(bool block = true);

		///<summary>Send bytes to the client</summary>
		///<param name='buffer'>buffer containing the bytes to send</param>
		///<param name='len'>the length of <paramref name='buffer'/></param>
		///<returns>false if the send failed</returns>
		///<exception cref='SocketException'/>
		bool send(const char* buffer, int len);

		void close();
		bool _closed = false;

		//Move constructor
		TCPSocket(TCPSocket&&);
		TCPSocket& operator=(TCPSocket&&);

		/*New sockets can only be spawned by accepting connections from a ServerSocket*/
		TCPSocket(SOCKET socket, const std::string& address);
	};
}

#endif
