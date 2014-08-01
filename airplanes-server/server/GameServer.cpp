#include "stdafx.h"
#include "GameServer.h"
#include <thread>
#include <chrono>
#include "airplanes.h"

extern BOOL WINAPI Shutdown(DWORD);
namespace airplanes {

	GameServer::GameServer()
	{
	}

	void GameServer::start(unsigned fps)
	{
		log::game_server("Game server started");
		const auto maxFrameTime = std::chrono::milliseconds(1000 / fps);
		std::lock_guard<std::mutex> stop_lock(stop_mutex);
		std::unique_lock<std::mutex> loop_lock = std::unique_lock<std::mutex>(loop_mutex);
		stopped = false;
		while (!stopped)
		{
			auto start = std::chrono::high_resolution_clock::now();
			try {
				for (auto player = players.begin(); player != players.end(); ++player)
				{
					try {
						if (!player->processMessages(eventQueue))
							if ((player = players.erase(player)) == players.end()) break;
					}
					catch (const std::exception& e) {
						log::server("Dropping player " + player->username + " because of unhandled exception "
							+ e.what(), log::LEVEL_ERROR);
						if ((player = players.erase(player)) == players.end()) break;
					}
				}
				while (!eventQueue.empty())
				{
					Message* msg = eventQueue.front();
					for (Player& p : players)
						p.socket->sendMessage(msg, false);
					eventQueue.pop();
					delete msg;
				}
			}
			catch (const sql::SQLException& e) {
				std::string message = std::string("# ERR: ") + e.what()
					+ " (MySQL error code: " + std::to_string(e.getErrorCode())
					+ ", SQLState: " + e.getSQLState() + " )";
				printf("Game thread crashed with unhandled exception %s\n", message.c_str());
				log::game_server("Unhandled exception: " + message, log::LEVEL_FATAL);
				std::thread([] { Shutdown(0); }).detach();
				return;
			}
			catch (const std::exception& e) {
				printf("Game thread crashed with unhandled exception %s\n", e.what());
				log::game_server(std::string("Unhandled exception: ") + e.what(), log::LEVEL_FATAL);
				std::thread([] { Shutdown(0); }).detach();
				return;
			}
			catch (...) {
				printf("Game thread crashed with unknown exception\n");
				log::game_server(std::string("Unknown exception"), log::LEVEL_FATAL);
				std::thread([] { Shutdown(0); }).detach();
				return;
			}

			auto frameTime = std::chrono::high_resolution_clock::now() - start;
			//If the frame took less time than the maximum allowed, sleep for a bit
			if (frameTime < maxFrameTime)
				//Releases the lock and sleeps for the given time
				stop_cond.wait_for(loop_lock, maxFrameTime - frameTime);

			/*Else don't sleep, but still momentarily release the lock 
			to give other threads a chance to add new players*/
			else { loop_lock.unlock(); loop_lock.lock(); }
		}
		log::game_server("Game server shutting down.", log::LEVEL_INFO);
	}

	void GameServer::addPlayer(std::unique_ptr<TCPSocket>&& socket, uint32_t userid, const std::string& username)
	{
		std::lock_guard<std::mutex> loop_lock(loop_mutex);
		players.push_back(Player(std::move(socket), userid, username));
	}

	void GameServer::stop()
	{
		if (!stopped)
		{
			stopped = true;
			stop_cond.notify_all();
			std::lock_guard<std::mutex> stop_lock(stop_mutex);
		}
	}

	GameServer::~GameServer()
	{
		stop();
	}

}
