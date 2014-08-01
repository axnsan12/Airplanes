#ifndef __GAME_SERVER_H__
#define __GAME_SERVER_H__
#include <list>
#include <mutex>
#include "Player.h"
#include "messages.hpp"
#include <queue>
#include <condition_variable>

namespace airplanes {

	class GameServer
	{
	public:
		GameServer();

		///<summary>Starts running the server loop. Blocks indefinitely</summary>
		///<param name='max_fps'>the maximum number of times per second to execute the server loop</param>
		void start(unsigned max_fps = 20); 

		///<summary>Add a new player to the server. The server takes ownership of the player.</summary>
		///<param name='player'>the player to add</param>
		void addPlayer(std::unique_ptr<TCPSocket>&& socket, uint32_t userid, const std::string& username);

		///<summary>Stops the server loop and does any eventual cleanup. Automatically called by the destructor. Blocks until the server thread stops.</summary>
		void stop();

		GameServer(const GameServer&) = delete;
		GameServer& operator=(const GameServer&) = delete;
		~GameServer();

	private:
		std::list<Player> players;
		std::mutex loop_mutex, stop_mutex;
		std::condition_variable stop_cond;
		bool stopped = false;
		std::queue<Message*> eventQueue;
	};
}

#endif