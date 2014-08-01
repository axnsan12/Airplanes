#include "stdafx.h"
#include "LoginServer.h"
#include "airplanes.h"
#include "ConnectionManager.h"

namespace airplanes {

LoginServer::LoginServer(GameServer& gameServer) : server(gameServer)
{

}

void LoginServer::addPlayer(TCPSocket* conn)
{
	std::lock_guard<std::mutex> guard(lock);
	sockets.push_back(std::unique_ptr<TCPSocket>(conn));
	threads.push_back(std::async(std::launch::async, _processPlayer, &sockets.back(), &server, &lock));
}

void LoginServer::_processPlayer(std::unique_ptr<TCPSocket>* sock, GameServer* server, std::mutex* lock)
{
	using namespace sql;
	using std::unique_ptr;

	unique_ptr<TCPSocket>& socket = *sock;
	unique_ptr<Message> msg = nullptr;

	//Keep processing messages until the client either succesfully authenticates,
	//disconnects or sends a message that is not permitted
	try { while ((msg = unique_ptr<Message>(socket->nextMessage(true))) != nullptr) {
		switch (msg->getMessageType()) {
		case ACCOUNT_REGISTER:
		{
			AccountRegisterMessage* arm = (AccountRegisterMessage*)msg.get();
			const std::string& username = arm->username;
			const std::wstring& password = arm->password;
			if (!REGISTER_VALIDATE_USERNAME(username))
				log::server(socket->remoteAddress() + ": ACCOUNT_REGISTER bad username"),
				socket->sendMessage(new ResponseMessage(REGISTER_BAD_USERNAME));
			else if (!REGISTER_VALIDATE_PASSWORD(password))
				log::server(socket->remoteAddress() + ": ACCOUNT_REGISTER bad password"),
				socket->sendMessage(new ResponseMessage(REGISTER_BAD_PASSWORD));
			else {
				auto conn = ConnectionManager::getConnection();
				conn->setSchema(ACCOUNT_DATABASE_NAME);
				static const SQLString sql = "INSERT INTO user(username, password) VALUES (?, ?)";
				unique_ptr<PreparedStatement> pstmt(conn->prepareStatement(sql));
				pstmt->setString(1, username);
				pstmt->setString(2, BCrypt::hashPassword(password));
				try {
					pstmt->executeUpdate();
					unique_ptr<Statement> stmt(conn->createStatement());
					unique_ptr<ResultSet> res(stmt->executeQuery("SELECT LAST_INSERT_ID() as `id`"));
					res->next();
					std::string password_utf8;
					utf8::utf16to8(password.cbegin(), password.cend(), std::back_inserter(password_utf8));
					log::server("Registered new account " + username + " " + password_utf8);
					log::server("Succesfully authenticated " + socket->remoteAddress() + " as " + username);
					socket->sendMessage(new ResponseMessage(RESPONSE_OK));
					std::lock_guard<std::mutex> guard(*lock);
					server->addPlayer(std::move(socket), res->getUInt("id"), username);
					return;
				}
				catch (const sql::SQLException& e) {
					if (e.getErrorCode() == 1062) //duplicate entry
						log::server(socket->remoteAddress() + ": ACCOUNT_REGISTER duplicate username"),
						socket->sendMessage(new ResponseMessage(REGISTER_ALREADY_EXISTS));
					else throw;
				}
			}
			break;
		}

		case ACCOUNT_LOGIN:
		{
			AccountLoginMessage* alm = (AccountLoginMessage*)msg.get();
			const std::string& username = alm->username;
			const std::wstring& password = alm->password;
			if (!REGISTER_VALIDATE_USERNAME(username))
				log::server(socket->remoteAddress() + ": ACCOUNT_LOGIN bad username"),
				socket->sendMessage(new ResponseMessage(BAD_USERNAME, "Username not registered"));
			else if (!REGISTER_VALIDATE_PASSWORD(password))
				log::server(username + ": ACCOUNT_LOGIN bad password"),
				socket->sendMessage(new ResponseMessage(WRONG_PASSWORD, "Wrong password"));
			else {
				auto conn = ConnectionManager::getConnection();
				conn->setSchema(ACCOUNT_DATABASE_NAME);
				static const SQLString sql = "SELECT * FROM user WHERE username = ?";
				unique_ptr<PreparedStatement> pstmt(conn->prepareStatement(sql));
				pstmt->setString(1, username);
				unique_ptr<ResultSet> res(pstmt->executeQuery());
				if (res->rowsCount() == 0)
					log::server(socket->remoteAddress() + ": ACCOUNT_LOGIN inexistent username"),
					socket->sendMessage(new ResponseMessage(BAD_USERNAME));
				else {
					res->next();
					std::string db_password = res->getString("password");
					if (BCrypt::checkPassword(password, db_password)) {
						log::server("Succesfully authenticated " + socket->remoteAddress() + " as " + username);
						socket->sendMessage(new ResponseMessage(RESPONSE_OK));
						std::lock_guard<std::mutex> guard(*lock);
						server->addPlayer(std::move(socket), res->getUInt("id"), username);
						return;
					}
					else log::server(username + ": ACCOUNT_LOGIN wrong password"),
						socket->sendMessage(new ResponseMessage(WRONG_PASSWORD));
				}
			}
			break;
		}

		case SOCKET_CLOSED:
			switch (((SocketCloseMessage*)msg.get())->reason) {
			case SocketCloseMessage::CLIENT_DISCONNECTED:
				printf("%s closed the connection\n", socket->remoteAddress().c_str());
				break;
			case SocketCloseMessage::CONNECTION_DROPPED:
				printf("Lost connection to %s\n", socket->remoteAddress().c_str());
				break;
			}
			return;

		default:
			log::server(socket->remoteAddress() + ": unexpected message " 
				+ std::to_string(msg->getMessageType()) + ". Disconnecting");
			socket->disconnect(new ConnectionClosedMessage(AUTHENTICATION_REQUIRED));
		}
	}}
	catch (const sql::SQLException& e) {
		if (socket != nullptr) {
			socket->disconnect(new ConnectionClosedMessage(INTERNAL_SERVER_ERROR));
			log::server(socket->remoteAddress() + ": Unhandled exception: " + std::string("# ERR: ") +
				e.what() + " (MySQL error code: " + std::to_string(e.getErrorCode()) + ", SQLState: "
				+ e.getSQLState() + " )", log::LEVEL_WARNING);
		}
		else log::server("Unhandled exception: " + std::string("# ERR: ") +
			e.what() + " (MySQL error code: " + std::to_string(e.getErrorCode()) + ", SQLState: "
			+ e.getSQLState() + " )", log::LEVEL_WARNING);
	}
	catch (const std::exception& e)
	{
		if (socket != nullptr) {
			socket->disconnect(new ConnectionClosedMessage(INTERNAL_SERVER_ERROR));
			log::server(socket->remoteAddress() + ": Unhandled exception: " + e.what(), log::LEVEL_WARNING);
		}
		else log::server(std::string("Unhandled exception: ") + e.what(), log::LEVEL_WARNING);
	}
}

LoginServer::~LoginServer()
{
	std::lock_guard<std::mutex> guard(lock);
	for (auto& socket : sockets)
		if (socket != nullptr)
			socket->disconnect();
	for (auto& thread : threads)
		thread.wait();
}

}