#ifndef __PLAYER_H__
#define __PLAYER_H__
#include <string>
#include <queue>
#include <memory>
#include <unordered_set>
#include "Message.h"
#include "TCPSocket.h"
#include "Game.h"

namespace airplanes {
	class GameServer;

	class Player
	{
	public:

		///<summary><para>Process any messages pending on the network. This function should 
		///be called periodically to avoid dropping the connection.</para>
		///<para>Any message that can be processed independently of other players will be.</para>
		///<para>Any message that requires notifying other players will be posted to <paramref name='messageQueue'/>.</para>
		///<para>Any message posted to <paramref name='messageQueue'/> has been processed sucessfully and is to be used for notyfing affected players.</para>
		///<para>If this function returns false, the object has become invalid (perhaps 
		///due to dropped connection) and should be discarded by its handler.</para></summary>
		///<param name='messageQueue'>Queue which will receive messages requiring attention</param>
		///<returns>false if the object is no longer valid</returns>
		bool processMessages(std::queue<Message*>& messageQueue);

		///<exception cref='std::invalid_argument'/>
		Player(std::unique_ptr<TCPSocket> socket, unsigned userid, const std::string& username);

		Player(const Player&) = delete;
		Player& operator=(const Player&) = delete;
		Player(Player&& that);
		Player& operator=(Player&& that);
		~Player();

		friend Game;
		friend GameServer;

	private:
		std::unique_ptr<TCPSocket> socket;
		std::unordered_set<Game*> games;
		std::string username;
		unsigned userid;
	};
}

#endif