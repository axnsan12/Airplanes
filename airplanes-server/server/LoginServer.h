#ifndef _LOGIN_SERVER_H_
#define _LOGIN_SERVER_H_
#include "GameServer.h"
#include "TCPSocket.h"
#include <vector>
#include <future>
#include <mutex>

namespace airplanes {

class LoginServer
{
public:
	LoginServer(GameServer& gameServer);
	void addPlayer(TCPSocket* conn);
	LoginServer(const LoginServer&) = delete;
	LoginServer& operator=(const LoginServer&) = delete;
	~LoginServer();

private:
	GameServer& server;
	std::vector<std::unique_ptr<TCPSocket>> sockets;
	std::vector<std::future<void>> threads;
	std::mutex lock;

	static void _processPlayer(std::unique_ptr<TCPSocket>* sock, GameServer* server, std::mutex* lock);
	
};

}
#endif
