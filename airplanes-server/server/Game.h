#ifndef __GAME_H__
#define __GAME_H__
#include <cstdint>
#include <unordered_map>
#include <unordered_set>
#include <ctime>
#include "Message.h"

namespace airplanes {

class Player;

class Game
{
public:

	///<summary><para>Create a new game with the given parameters. Parameters must be validated beforehand</para>
	///Returns the game ID of the newly created game</summary>
	static uint32_t create(uint32_t timeout, uint8_t numPlayers, const std::string& gamePassword,
		uint8_t gridSize, uint8_t numPlanes, bool headshots, bool reveal);
	static Game* get(uint32_t gameID);

	bool addPlayer(Player* player, const std::string& gamePassword, bool respond_ok = true);
	bool dropPlayer(Player* player);
	void registerPlayer(Player* player);
	void unregisterPlayer(Player* player);
	bool advanceTurn(time_t currentTime);
	bool addPlanes(Player* player, const std::string& locations);
	bool addMove(Player* player, uint8_t x, uint8_t y);
	void killPlayer(Player* player, uint16_t turnOfDeath);
	void broadcastMessage(Message* message, bool deleteAfter = true, Player* sender = nullptr);
	void endGame();

	inline int16_t getTurnNumber() const { return turn; }
	inline bool isFinished() const { return finished; }

	Game(const Game&) = delete;
	Game& operator=(const Game&) = delete;

	~Game();

private:
	uint32_t gameID;
	uint32_t timeout;
	int16_t turn;
	uint8_t numPlayers;
	bool finished;
	unsigned playerCount, movesPlayed, deadLastTurn;
	uint32_t deadline;
	std::string password;
	std::unordered_set<Player*> players;

	static std::unordered_map<uint32_t, std::unique_ptr<Game>> games;

	Game(uint32_t gameID);
	Game(uint32_t gameID, uint8_t numPlayers, uint32_t timeout, const std::string& password);
};

}
#endif