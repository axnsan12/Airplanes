#include "stdafx.h"
#include "Player.h"
#include <exception>
#include "airplanes.h"
#include "ConnectionManager.h"

namespace airplanes {

bool Player::processMessages(std::queue<Message*>& messageQueue)
{
	if (socket == nullptr)
		return false;

	using namespace sql;
	using std::unique_ptr;
	assert(socket->isValid());
	unique_ptr<Message> msg = nullptr;
	while ((msg = unique_ptr<Message>(socket->nextMessage(false))) != nullptr)
	{
	MESSAGE_TYPE TYPE = msg->getMessageType();
	switch (TYPE)
	{
	case GAME_CREATE:
	{
		GameCreateMessage* gcm = (GameCreateMessage*)msg.get();
		uint8_t numPlayers = gcm->numPlayers;
		uint32_t timeout = gcm->timeout;
		const std::string& gamePassword = gcm->gamePassword;
		uint8_t gridSize = gcm->gridSize, numPlanes = gcm->numPlanes;
		bool headshots = gcm->headshots, reveal = gcm->reveal;
		if (!GAME_VALIDATE_PASSWORD(gamePassword))
			log::game_server(username + ": GAME_CREATE bad game password"),
			socket->disconnect(new ConnectionClosedMessage(BAD_REQUEST_PARAMETERS));
		else if (numPlayers < 2 || numPlayers > GAME_MAX_NUM_PLAYERS)
			log::game_server(username + ": GAME_CREATE invalid number of players"),
			socket->disconnect(new ConnectionClosedMessage(BAD_REQUEST_PARAMETERS));
		else {
			uint32_t gameID = Game::create(timeout, numPlayers, gamePassword, gridSize, numPlanes, headshots, reveal);
			socket->sendMessage(new ResponseMessage(RESPONSE_OK, std::to_string(gameID)));
			Game* g = Game::get(gameID);
			g->addPlayer(this, gamePassword, false);
			games.insert(g);
			messageQueue.push(new GameCreatedMessage(gameID, numPlayers, timeout, gridSize, numPlanes, headshots, reveal));
			messageQueue.push(new PlayerJoinedMessage(gameID, username));
		}
		break;
	}

	case GAME_JOIN:
	{
		GameJoinMessage* gjm = (GameJoinMessage*)msg.get();
		uint32_t gameID = gjm->gameID;
		const std::string& gamePassword = gjm->gamePassword;
		Game* g = Game::get(gameID);
		if (g != nullptr) {
			if (g->addPlayer(this, gamePassword))
				messageQueue.push(new PlayerJoinedMessage(gameID, username));
			games.insert(g);
		}
		else log::game_server(username + ": GAME_JOIN game does not exist" + std::to_string(gameID)),
			socket->disconnect(new ConnectionClosedMessage(BAD_GAME_ID));
		break;
	}

	case GAME_LEAVE:
	{
		GameLeaveMessage* glm = (GameLeaveMessage*)msg.get();
		uint32_t gameID = glm ->gameID;
		Game* g = Game::get(gameID);
		if (g != nullptr) {
			if (g->dropPlayer(this))
				messageQueue.push(new PlayerLeftMessage(gameID, username));
		}
		else log::game_server(username + ": GAME_LEAVE game does not exist" + std::to_string(gameID), log::LEVEL_WARNING),
			socket->disconnect(new ConnectionClosedMessage(BAD_GAME_ID));
		break;
	}

	case GET_GAME_LISTING:
	{
		auto conn = ConnectionManager::getConnection();
		conn->setSchema(GAME_DATABASE_NAME);
		unique_ptr<Statement> stmt(conn->createStatement());
		unique_ptr<ResultSet> res(stmt->
			executeQuery("SELECT game.id, game.timeout, COUNT( gameuser.gid ) AS `current`, game.num_players"
			", game.grid_size, game.num_planes, game.headshots, game.reveal "
			"FROM ( game LEFT JOIN gameuser ON gameuser.gid = game.id ) "
			"WHERE game.password = '' AND game.turn = -1 AND game.id NOT  IN ("
				"SELECT gid FROM gameuser WHERE uid = " + std::to_string(userid) + 
			") "
			"GROUP BY game.id "
			"ORDER BY game.id DESC "
			"LIMIT 20"));
		std::string response;
		while (res->next())
		{
			using std::to_string;
			response.append(to_string(res->getUInt("id"))).push_back(',');
			response.append(to_string(res->getUInt("timeout"))).push_back(',');
			response.append(to_string(res->getUInt("num_players"))).push_back(',');
			response.append(to_string(res->getUInt("grid_size"))).push_back(',');
			response.append(to_string(res->getUInt("num_planes"))).push_back(',');
			response.append(((res->getUInt("headshots")) ? "true" : "false")).push_back(',');
			response.append(((res->getUInt("reveal")) ? "true" : "false")).push_back(',');
			response.append(to_string(res->getUInt("current"))).push_back('.');
		}
		response.push_back('|');
		res = unique_ptr<ResultSet>(stmt->
			executeQuery("SELECT game.id, game.timeout, COUNT( gameuser.gid ) AS `current`, game.num_players"
			", game.grid_size, game.num_planes, game.headshots, game.reveal, game.turn, game.finished "
			"FROM ( game INNER JOIN gameuser ON gameuser.gid = game.id ) "
			"WHERE game.id IN ("
				"SELECT gid FROM gameuser WHERE uid = " + std::to_string(userid) + " AND dropped = false"
			") "
			"GROUP BY game.id "
			"ORDER BY game.id DESC"));
		while (res->next())
		{
			using std::to_string;
			response.append(to_string(res->getUInt("id"))).push_back(',');
			response.append(to_string(res->getUInt("timeout"))).push_back(',');
			response.append(to_string(res->getUInt("num_players"))).push_back(',');
			response.append(to_string(res->getUInt("grid_size"))).push_back(',');
			response.append(to_string(res->getUInt("num_planes"))).push_back(',');
			response.append(((res->getUInt("headshots")) ? "true" : "false")).push_back(',');
			response.append(((res->getUInt("reveal")) ? "true" : "false")).push_back(',');
			response.append(to_string(res->getUInt("current"))).push_back(',');
			response.append(to_string(res->getInt("turn"))).push_back(',');
			response.append(((res->getUInt("finished")) ? "true" : "false")).push_back('.');
		}
		printf("Game listing: %s\n", response.c_str());
		socket->sendMessage(new ResponseMessage(GAME_LISTING, response));
		break;
	}

	case GET_GAME_STATE:
	{
		unsigned gameID = ((GetGameMessage*)msg.get())->gameID;
		auto conn = ConnectionManager::getConnection();
		conn->setSchema(GAME_DATABASE_NAME);
		unique_ptr<Statement> stmt(conn->createStatement());
		unique_ptr<ResultSet> res(stmt->executeQuery("SELECT true FROM gameuser "
			"WHERE gid = " + std::to_string(gameID) + " AND uid = " + std::to_string(userid) +
			" AND dropped = false"));
		if (res->rowsCount() == 0) {
			log::game_server(username + ": requested state of inaccessible game " + std::to_string(gameID));
			socket->disconnect(new ConnectionClosedMessage(BAD_GAME_ID));
			break;
		}

		std::string response;
		Game* g = Game::get(gameID);
		if (g == nullptr) {
			log::game_server(username + ": GET_GAME_STATE game does not exist" + std::to_string(gameID)),
				socket->disconnect(new ConnectionClosedMessage(BAD_GAME_ID));
			break;
		}

		res.reset(stmt->executeQuery("SELECT uid, username, planes, turn_of_death, dropped, abandoned "
			"FROM gameuser INNER JOIN webauth.user ON (gid = " + std::to_string(gameID) + " AND uid = id)"));
		unique_ptr<PreparedStatement> pstmt(conn->prepareStatement("SELECT x, y FROM move "
			"WHERE gid = " + std::to_string(gameID) + " AND uid = ? AND turn > 0 ORDER BY turn"));
		
		response.push_back('[');
		while (res->next()) {
			response.append(res->getString("username")).push_back('|');
			uint16_t turnOfDeath = uint16_t(res->getUInt("turn_of_death"));
			bool dropped = res->getInt("dropped") != 0 ? true : false;
			bool abandoned = res->getInt("abandoned") != 0 ? true : false;
			SQLString planes = res->getString("planes");

			pstmt->setInt(1, res->getUInt("uid"));
			unique_ptr<ResultSet> moves(pstmt->executeQuery());

			if (turnOfDeath == 0) {
				if (!dropped) {
					if (planes.length() == 0)
						response.append("PLACING|");
					else response.append("READY|");
				}
				else response.append("READY|");
			}
			else if (abandoned) response.append("DISCONNECTED|");
			else if (moves->rowsCount() < turnOfDeath)
				response.append("LASTSTAND|");
			else response.append("DEAD|");
			
			response.append(planes).push_back('|');
			while (moves->next()) {
				response.append(std::to_string(moves->getUInt(1))).push_back(';');
				response.append(std::to_string(moves->getUInt(2))).push_back(';');
			}
			response.push_back('|');
			response.append("-1;-1;|").append(std::to_string(turnOfDeath)).push_back('|');
			response.append("1").push_back('|');
			response.append("false").push_back('|');
			if (res->isLast())
				response.push_back(']');
			else response.append(", ");
		}
		printf("Game state: %s\n", response.c_str());
		socket->sendMessage(new ResponseMessage(GAME_STATE, response));
		break;
	}

	case PLANE_LOCATIONS:
	{
		PlaneLocationsMessage* plm = (PlaneLocationsMessage*)msg.get();
		unsigned gameID = plm->gameID;
		if (plm->playerName != username) {
			log::game_server(username + ": PLANE_LOCATIONS wrong username in game" + std::to_string(gameID)),
				socket->disconnect(new ConnectionClosedMessage(BAD_REQUEST_PARAMETERS));
			break;
		}
		Game* g = Game::get(gameID);
		if (g == nullptr) {
			log::game_server(username + ": PLANE_LOCATIONS game does not exist " + std::to_string(gameID)),
				socket->disconnect(new ConnectionClosedMessage(BAD_GAME_ID));
			break;
		}
		else {
			if (g->addPlanes(this, plm->locations)) {
				g->broadcastMessage(plm, false, this);
				g->advanceTurn(time(NULL));
			}
		}
		break;
	}

	case ATTACK_CELL:
	{
		AttackCellMessage* acm = (AttackCellMessage*)msg.get();
		unsigned gameID = acm->gameID;
		if (acm->playerName != username) {
			log::game_server(username + ": ATTACK_CELL wrong username in game" + std::to_string(gameID)),
				socket->disconnect(new ConnectionClosedMessage(BAD_REQUEST_PARAMETERS));
			break;
		}
		Game* g = Game::get(gameID);
		if (g == nullptr) {
			log::game_server(username + ": ATTACK_CELL game does not exist " + std::to_string(gameID)),
				socket->disconnect(new ConnectionClosedMessage(BAD_GAME_ID));
			break;
		}
		else {
			if (g->addMove(this, acm->x, acm->y)) {
				g->broadcastMessage(acm, false, this);
				g->advanceTurn(time(NULL));
			}
		}
		break;
	}

	case PLAYER_DIED:
	{
		PlayerDeathMessage* pdm = (PlayerDeathMessage*)msg.get();
		unsigned gameID = pdm->gameID;
		Game* g = Game::get(gameID);
		if (g == nullptr) {
			log::game_server(username + ": PLAYER_DIED game does not exist " + std::to_string(gameID)),
				socket->disconnect(new ConnectionClosedMessage(BAD_GAME_ID));
			break;
		}
		else g->killPlayer(this, pdm->turnNumber);
		break;
	}

	case GAME_FINISHED:
	{
		GameEndMessage* gem = (GameEndMessage*)msg.get();
		unsigned gameID = gem->gameID;
		Game* g = Game::get(gameID);
		if (g == nullptr) {
			log::game_server(username + ": GAME_FINISHED game does not exist " + std::to_string(gameID)),
				socket->disconnect(new ConnectionClosedMessage(BAD_GAME_ID));
			break;
		}
		g->endGame();
		break;
	}

	case ACCOUNT_REGISTER:
		log::game_server(username + ": cannot register another account while logged in");
		socket->disconnect(new ConnectionClosedMessage(UNEXPECTED_MESSAGE));
		break;

	case ACCOUNT_LOGIN:
		log::game_server(username + ": cannot login again");
		socket->disconnect(new ConnectionClosedMessage(UNEXPECTED_MESSAGE));
		break;

	case SOCKET_CLOSED:
		switch (((SocketCloseMessage*)msg.get())->reason) {
		case SocketCloseMessage::CLIENT_DISCONNECTED:
			printf("%s closed the connection\n", socket->remoteAddress().c_str());
			break;
		case SocketCloseMessage::CONNECTION_DROPPED:
			printf("Lost connection to %s\n", socket->remoteAddress().c_str());
			break;
		}
		socket = nullptr;
		return false;

	default:
		log::game_server(username + ": unexpected message " + std::to_string(TYPE) + ". Disconnecting");
		socket->disconnect(new ConnectionClosedMessage(UNEXPECTED_MESSAGE));
	}
	};

	return true;
}

Player::Player(std::unique_ptr<TCPSocket> socket, unsigned userid, const std::string& username)
	: username(username), userid(userid), socket(std::move(socket))
{
	if (this->socket == nullptr || !this->socket->isValid())
		throw std::invalid_argument("Invalid socket passed as argument");

	printf("Constructing player %d %s\n", userid, username.c_str());
	using std::unique_ptr;
	using namespace sql;
	auto conn = ConnectionManager::getConnection();
	conn->setSchema(GAME_DATABASE_NAME);
	unique_ptr<Statement> stmt(conn->createStatement());
	unique_ptr<ResultSet> res = unique_ptr<ResultSet>(stmt->
		executeQuery("SELECT gid FROM gameuser WHERE uid = " + std::to_string(userid) + " AND dropped = false"));
	while (res->next())
	{
		uint32_t gameID = res->getUInt("gid");
		Game* g = Game::get(gameID);
		if (g == nullptr)
			throw std::logic_error("Game " + std::to_string(gameID) + " present in table gameuser not found in table game");

		g->registerPlayer(this);
		games.insert(g);
	}
}

Player::Player(Player&& that)
	: socket(std::move(that.socket)), username(std::move(that.username)), games(std::move(that.games))
{ 
	this->userid = that.userid;  that.userid = 0;
	for (Game* g : games) {
		g->unregisterPlayer(&that);
		g->registerPlayer(this);
	}
}

Player& Player::operator=(Player&& that)
{
	this->socket = std::move(that.socket);
	this->username = std::move(that.username);
	this->userid = that.userid;
	that.userid = 0;
	return *this;
}

Player::~Player()
{
	socket = nullptr;
	userid = 0;
	username = "";
	for (Game* g : games)
		g->unregisterPlayer(this);
	games.clear();
}
}