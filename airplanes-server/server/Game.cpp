#include "stdafx.h"
#include "Game.h"
#include "ConnectionManager.h"
#include "airplanes.h"
#include "Player.h"

namespace airplanes {
std::unordered_map<uint32_t, std::unique_ptr<Game>> Game::games;

Game::Game(uint32_t gameID)
{
	this->gameID = gameID;
	using std::unique_ptr;
	using namespace sql;
	auto conn = ConnectionManager::getConnection();
	conn->setSchema(GAME_DATABASE_NAME);
	unique_ptr<Statement> stmt(conn->createStatement());
	unique_ptr<ResultSet> res(stmt->
		executeQuery("SELECT timeout, num_players, password, turn, deadline, finished FROM game "
		"WHERE id = " + std::to_string(gameID)));
	if (res->rowsCount() == 0)
		throw std::logic_error(std::to_string(gameID) + " not in database");
	res->next();
	timeout = res->getUInt("timeout");
	password = res->getString("password");
	turn = (int16_t)res->getInt("turn");
	numPlayers = (uint8_t)res->getUInt("num_players");
	deadline = res->getUInt("deadline");
	finished = res->getUInt("finished") != 0 ? true : false;
	res = unique_ptr<ResultSet>(stmt->
		executeQuery("SELECT COUNT(*) FROM gameuser "
		"WHERE gid = " + std::to_string(gameID) + " AND (turn_of_death = 0 OR " 
		"turn_of_death = " + std::to_string(turn) + ") AND dropped = false"));
	res->next();
	playerCount = res->getUInt(1);
	res = unique_ptr<ResultSet>(stmt->
		executeQuery("SELECT COUNT(move.uid) "
		"FROM ( move INNER JOIN gameuser ON gameuser.gid = move.gid AND gameuser.uid = move.uid ) "
		"WHERE move.gid = " + std::to_string(gameID) + " AND turn = " + std::to_string(turn) + " AND dropped = false"));
	res->next();
	movesPlayed = res->getUInt(1);
	advanceTurn(time(NULL));
}

Game::Game(uint32_t gameID, uint8_t numPlayers, uint32_t timeout, const std::string& password) : password(password)
{
	this->numPlayers = numPlayers;
	this->gameID = gameID;
	this->timeout = timeout;
	turn = -1;
	movesPlayed = 0;
	playerCount = 0;
}

uint32_t Game::create(uint32_t timeout, uint8_t numPlayers, const std::string& gamePassword,
	uint8_t gridSize, uint8_t numPlanes, bool headshots, bool reveal)
{
	using std::unique_ptr;
	using namespace sql;
	auto conn = ConnectionManager::getConnection();
	conn->setSchema(GAME_DATABASE_NAME);
	SQLString sql = "INSERT INTO game(timeout, num_players, password, grid_size, num_planes, headshots, reveal) "
		"VALUES (" + std::to_string(timeout) + ", " + std::to_string(numPlayers) + ", ?, " +
		std::to_string(gridSize) + ", " + std::to_string(numPlanes) + ", " +
		((headshots)?"true, ":"false, ") + ((reveal)?"true)":"false)");

	unique_ptr<PreparedStatement> pstmt(conn->prepareStatement(sql));
	pstmt->setString(1, gamePassword);
	pstmt->executeUpdate();
	unique_ptr<Statement> stmt(conn->createStatement());
	unique_ptr<ResultSet> res(stmt->executeQuery("SELECT LAST_INSERT_ID()"));
	res->next();
	uint32_t gameID = res->getUInt(1);
	games[gameID] = unique_ptr<Game>(new Game(gameID, numPlayers, timeout, gamePassword));
	log::game(gameID, "Created");
	return gameID;
}

bool Game::addPlayer(Player* player, const std::string& gamePassword, bool respond_ok)
{
	using namespace sql;
	using std::unique_ptr;

	TCPSocket* socket = player->socket.get();
	const std::string& username = player->username;
	uint32_t userid = player->userid;

	if (password != gamePassword) {
		log::game(gameID, username + ": GAME_JOIN wrong game password" + std::to_string(gameID)),
		socket->sendMessage(new ResponseMessage(WRONG_GAME_PASSWORD));
		return false;
	}
	if (turn >= 0) {
		log::game_server(username + ": GAME_JOIN game in progress " + std::to_string(gameID)),
			socket->sendMessage(new ResponseMessage(GAME_FULL));
		return false;
	}
	if (playerCount < numPlayers) {
		std::string response;
		try {
			auto conn = ConnectionManager::getConnection();
			conn->setSchema(GAME_DATABASE_NAME);
			unique_ptr<Statement> stmt(conn->createStatement());
			if (respond_ok) {
				unique_ptr<ResultSet> res(stmt->executeQuery("SELECT username "
					"FROM gameuser INNER JOIN webauth.user ON user.id = gameuser.uid "
					"WHERE gid = " + std::to_string(gameID)));
				while (res->next())
					response.append(res->getString("username")).push_back(',');
			}

			stmt->executeUpdate("INSERT INTO gameuser(gid, uid) VALUES ("
				+ std::to_string(gameID) + ", " + std::to_string(userid) + ")");

			playerCount++;
			log::game(gameID, username + " joined");
		}
		catch (const SQLException& e) {
			if (e.getErrorCode() == 1062) //duplicate entry 
			{
				log::game_server(username + ": GAME_JOIN user already in game " + std::to_string(gameID), log::LEVEL_WARNING);
				if (respond_ok)
					socket->sendMessage(new ResponseMessage(RESPONSE_OK, response));
				registerPlayer(player);
				return false;
			}
			else throw;
		}

		if (respond_ok)
			socket->sendMessage(new ResponseMessage(RESPONSE_OK, response));
		registerPlayer(player);
		advanceTurn(time(NULL));
		return true;
	}
	else log::game_server(username + ": GAME_JOIN game is full " + std::to_string(gameID)),
		socket->sendMessage(new ResponseMessage(GAME_FULL, "Game is full"));
	return false;
}

bool Game::dropPlayer(Player* player)
{
	if (players.erase(player))
	{
		player->games.erase(this);
	}
	auto conn = ConnectionManager::getConnection();
	conn->setSchema(GAME_DATABASE_NAME);
	std::unique_ptr<sql::Statement> stmt(conn->createStatement());
	int affected;
	if (finished)
		affected = stmt->executeUpdate("UPDATE gameuser SET dropped = true "
		"WHERE gid = " + std::to_string(gameID) + " AND uid = " + std::to_string(player->userid));
	else if (turn > 0) {
		affected = stmt->executeUpdate("UPDATE gameuser SET dropped = true, turn_of_death = "
			+ std::to_string(turn) + ", abandoned = true WHERE gid = " + std::to_string(gameID) + " AND uid = "
			+ std::to_string(player->userid) + " AND turn_of_death = 0");
		if (affected == 0)
			affected = stmt->executeUpdate("UPDATE gameuser SET dropped = true WHERE gid = " 
			+ std::to_string(gameID) + " AND uid = " + std::to_string(player->userid));
	}
	else if (turn == 0) {
		affected = stmt->executeUpdate("UPDATE gameuser SET dropped = true, turn_of_death = 65535, abandoned = true"
			" WHERE gid = " + std::to_string(gameID) + " AND uid = " + std::to_string(player->userid));
	}
	else affected = stmt->executeUpdate("DELETE FROM gameuser "
		"WHERE gid = " + std::to_string(gameID) + " AND uid = " + std::to_string(player->userid));
	if (affected != 0) {
		playerCount--;
		std::unique_ptr<sql::ResultSet> res(stmt->executeQuery("SELECT true FROM move "
			"WHERE gid = " + std::to_string(gameID) + " AND turn = " + std::to_string(turn) + 
			" AND uid = " + std::to_string(player->userid)));
		if (res->rowsCount() > 0)
			movesPlayed--;
		advanceTurn(time(NULL));
		log::game(gameID, player->username + " left");
		return true;
	}
	log::game_server(player->username + ": GAME_LEAVE player not in game" + std::to_string(gameID), log::LEVEL_WARNING);
	return false;
}

bool Game::advanceTurn(time_t currentTime)
{
	if (finished)
		return false;

	if ((turn < 0 && playerCount == numPlayers) 
		|| (turn >= 0 && movesPlayed == playerCount)) //Everyone that needs to play a move did
	{
		movesPlayed = 0;
		turn++;
		deadline = (uint32_t)currentTime + timeout;
		auto conn = ConnectionManager::getConnection();
		conn->setSchema(GAME_DATABASE_NAME);
		std::unique_ptr<sql::Statement> stmt(conn->createStatement());
		stmt->executeUpdate("UPDATE game "
			"SET turn = " + std::to_string(turn) + ", deadline = " + std::to_string(deadline) +
			" WHERE id = " + std::to_string(gameID));
		for (Player* p : players)
			p->socket->sendMessage(new TurnStartedMessage(gameID, (uint16_t)turn));
		log::game(gameID, "Begin turn " + std::to_string(turn));
		playerCount -= deadLastTurn;
		deadLastTurn = 0;
		return true;
	}
	else if (turn >= 0 && timeout > 0 && currentTime >= deadline) //Turn timed out and not everyone played
	{
		using namespace sql;
		using std::unique_ptr;
		auto conn = ConnectionManager::getConnection();
		conn->setSchema(GAME_DATABASE_NAME);
		unique_ptr<Statement> stmt(conn->createStatement());
		//Find the players that did not play their turn
		unique_ptr<ResultSet> res(stmt->executeQuery("SELECT gameuser.uid, user.username "
			"FROM gameuser INNER JOIN webauth.user ON user.id = gameuser.uid "
			"WHERE gameuser.gid = " + std::to_string(gameID) + " AND gameuser.dropped = false "
			"AND gameuser.uid NOT IN ("
				"SELECT move.uid FROM move "
				"WHERE move.gid = " + std::to_string(gameID) + " AND move.turn = " + std::to_string(turn) + 
			")"));

		//And remove them from the game
		std::string update = "UPDATE gameuser SET dropped = true, abandoned = true, turn_of_death = " 
			+ std::to_string(turn) + "WHERE gid = " + std::to_string(gameID) + " AND uid IN (";
		while (res->next())
		{
			uint32_t id = res->getUInt(1);
			std::string username = res->getString(2);
			update.append(std::to_string(id));
			if (!res->isLast())
				update.push_back(',');
			unique_ptr<Message> msg(new PlayerLeftMessage(gameID, username));
			for (auto iterator = players.begin(); iterator != players.end(); ++iterator)
			{
				Player* player = *iterator;
				if (player->userid == id) {
					player->games.erase(this);
					if ((iterator = players.erase(iterator)) == players.end()) break;
				}
				player->socket->sendMessage(msg.get(), false);
			}
			log::game(gameID, "Kicked " + username + " because of turn timeout");
		}
		update.push_back(')');
		playerCount -= res->rowsCount();
		stmt->executeUpdate(update);

		//Then proceed to advance the turn
		movesPlayed = 0;
		turn++;
		deadline = (uint32_t)currentTime + timeout;
		stmt->executeUpdate("UPDATE game "
			"SET turn = " + std::to_string(turn) + ", deadline = " + std::to_string(deadline) +
			" WHERE id = " + std::to_string(gameID));
		for (Player* p : players)
			p->socket->sendMessage(new TurnStartedMessage(gameID, (uint16_t)turn));
		log::game(gameID, "Begin turn " + std::to_string(turn));
		playerCount -= deadLastTurn;
		deadLastTurn = 0;
		return true;
	}
	return false;
}

bool Game::addPlanes(Player* player, const std::string& locations)
{
	using std::unique_ptr;
	using namespace sql;
	try {
		auto conn = ConnectionManager::getConnection();
		conn->setSchema(GAME_DATABASE_NAME);
		unique_ptr<Statement> stmt(conn->createStatement());
		int affected = stmt->executeUpdate("UPDATE gameuser SET planes = '" + locations + "'"
			" WHERE gid = " + std::to_string(gameID) + " AND uid = " + std::to_string(player->userid) +
			" AND planes IS NULL AND dropped = false AND turn_of_death = 0");
		if (affected == 0) {
			log::game_server(player->username + ": PLANE_LOCATIONS user already placed planes or not "
				"in game " + std::to_string(gameID), log::LEVEL_WARNING);
			player->socket->disconnect(new ConnectionClosedMessage(ALREADY_PLAYED));
			return false;
		}

		stmt->executeUpdate("INSERT INTO move(gid, turn, uid, x, y) VALUES ("
			+ std::to_string(gameID) + ", 0, " + std::to_string(player->userid) + ", -1, -1)");
		movesPlayed++;
		log::game(gameID, player->username + " placed his planes.");
		return true;
	}
	catch (const SQLException& e) {
		if (e.getErrorCode() == 1062) //duplicate entry 
		{
			log::game_server(player->username + ": PLANE_LOCATIONS user already placed planes "
				"in game " + std::to_string(gameID), log::LEVEL_WARNING);
			player->socket->disconnect(new ConnectionClosedMessage(ALREADY_PLAYED));
			return false;
		}
		else throw;
	}
}

bool Game::addMove(Player* player, uint8_t x, uint8_t y)
{
	using std::unique_ptr;
	using namespace sql;
	if (players.count(player) == 0)
	{
		log::game_server(player->username + ": ATTACK_CELL player not "
			"in game " + std::to_string(gameID), log::LEVEL_WARNING);
		player->socket->disconnect(new ConnectionClosedMessage(BAD_REQUEST_PARAMETERS));
		return false;
	}
	if (finished) {
		log::game_server(player->username + ": ATTACK_CELL game already finished", log::LEVEL_WARNING);
		player->socket->sendMessage(new ResponseMessage(GAME_ALREADY_FINISHED));
		return false;
	}
	try {
		auto conn = ConnectionManager::getConnection();
		conn->setSchema(GAME_DATABASE_NAME);
		unique_ptr<Statement> stmt(conn->createStatement());
		unique_ptr<ResultSet> res(stmt->executeQuery("SELECT true FROM gameuser WHERE "
			"gid = " + std::to_string(gameID) + " AND uid = " + std::to_string(player->userid) + 
			" AND dropped = false AND (turn_of_death = 0 OR turn_of_death = " + std::to_string(turn) + ")"));
		if (res->rowsCount() == 0)
		{
			log::game_server(player->username + ": ATTACK_CELL player dead or not "
				"in game " + std::to_string(gameID), log::LEVEL_WARNING);
			player->socket->disconnect(new ConnectionClosedMessage(BAD_REQUEST_PARAMETERS));
			return false;
		}
		stmt->executeUpdate("INSERT INTO move(gid, turn, uid, x, y) VALUES ("
			+ std::to_string(gameID) + ", " + std::to_string(turn) + ", " + std::to_string(player->userid) + ", "
			+ std::to_string(x) + ", " + std::to_string(y) + ")");
		movesPlayed++;
		log::game(gameID, player->username + " attacked cell (" + std::to_string(x) + ", " + std::to_string(y) + ").");
		player->socket->sendMessage(new ResponseMessage(RESPONSE_OK));
		return true;
	}
	catch (const SQLException& e) {
		if (e.getErrorCode() == 1062) //duplicate entry 
		{
			log::game_server(player->username + ": ATTACK_CELL player already played on turn " + std::to_string(turn) +
				"in game " + std::to_string(gameID), log::LEVEL_WARNING);
			player->socket->disconnect(new ConnectionClosedMessage(ALREADY_PLAYED));
			return false;
		}
		else throw;
	}
}

void Game::killPlayer(Player* player, uint16_t turnOfDeath)
{
	using std::unique_ptr;
	using namespace sql;
	auto conn = ConnectionManager::getConnection();
	conn->setSchema(GAME_DATABASE_NAME);
	unique_ptr<Statement> stmt(conn->createStatement());
	int affected = stmt->executeUpdate("UPDATE gameuser SET turn_of_death = " + std::to_string(turnOfDeath) 
		+ " WHERE gid = " + std::to_string(gameID) + " AND uid = " + std::to_string(player->userid) +
		" AND (turn_of_death = 0 OR turn_of_death = " + std::to_string(turnOfDeath) + ") AND dropped = false");
	if (affected == 0)
		log::game_server(player->username + ": PLAYER_DIED user already dead or not in game " + std::to_string(gameID)),
		player->socket->disconnect(new ConnectionClosedMessage(BAD_REQUEST_PARAMETERS));
	else {
		deadLastTurn++;
		log::game(gameID, player->username + " died.");
	}
}

void Game::endGame()
{
	using namespace sql;
	using std::unique_ptr;
	auto conn = ConnectionManager::getConnection();
	conn->setSchema(GAME_DATABASE_NAME);
	unique_ptr<Statement> stmt(conn->createStatement());
	int affected = stmt->executeUpdate("UPDATE game SET finished = true WHERE "
		"id = " + std::to_string(gameID) + " AND finished = false");
	if (affected)
		log::game(gameID, "Game finished.");
	finished = true;
}

void Game::broadcastMessage(Message* message, bool deleteAfter, Player* sender)
{
	for (Player* p : players) {
		if (sender == nullptr || sender->userid != p->userid)
			p->socket->sendMessage(message, false);
	}
	if (deleteAfter)
		delete message;
}

Game* Game::get(uint32_t gameID)
{
	Game* ret = games[gameID].get();
	if (ret == nullptr)
	{
		try {
			Game* g = new Game(gameID);
			ret = g;
			games[gameID] = std::unique_ptr<Game>(g);
		} catch (std::logic_error&) { return nullptr; }
	}
	return ret;
}

void Game::registerPlayer(Player* player)
{
	players.insert(player);
}
void Game::unregisterPlayer(Player* player)
{
	players.erase(player);
}
Game::~Game()
{
	for (Player* p : players)
		p->games.erase(this);
	players.clear();
}

}
