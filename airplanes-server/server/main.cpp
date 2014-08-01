#include "stdafx.h"
#include "ServerSocket.h"
#include "TCPSocket.h"
#include "log.h"
#include "messages.hpp"
#include "ConnectionManager.h"
#include "airplanes.h"
#include "include/BCrypt.hpp"
#include "GameServer.h"
#include "LoginServer.h"
#include "Exceptions.h"
#include "AdminConsoleInterface.h"

using namespace airplanes;
namespace airplanes {
	std::recursive_mutex locked_cout::mut;
	sync_cout cout;
}

bool running = true;
ServerSocket* server;
BOOL WINAPI Shutdown(DWORD)
{
	if (running && server != nullptr)
	{
		running = false;
		server->shutdown();
	}
	ExitThread(0);
}


int main(int argc, const char* argv[])
{
	using std::endl;
	SetConsoleCtrlHandler(&Shutdown, TRUE);
	uint16_t port = SERVER_PORT, fps = SERVER_FPS;

	for (int i = 1; i < argc; ++i)
	{
		if (strncmp(argv[i], "-port:", strlen("-port:")) == 0)
		{
			try {
				int arg = std::stoi(argv[i] + strlen("-port:"));
				if (arg < 0 || arg > 65535)
					throw std::out_of_range("Port number out of range");
				port = uint16_t(arg);
			} 
			catch (const std::logic_error&) {
				printf("Invalid port number\n"); 
				return EXIT_FAILURE;
			}
		}
		if (strncmp(argv[i], "-fps:", strlen("-fps:")) == 0)
		{
			try {
				int arg = std::stoi(argv[i] + strlen("-fps:"));
				if (arg < 0 || arg > 1000)
					throw std::out_of_range("FPS out of range");
				fps = uint16_t(arg);
			}
			catch (const std::logic_error&) {
				printf("Invalid fps\n");
				return EXIT_FAILURE;
			}
		}
	}

	GameServer game;
	LoginServer login(game);
	auto game_thread = std::async(std::launch::async, [&game, fps]() { game.start(fps); });
	auto input_thread = std::async(std::launch::async, []() 
		{ 
			std::string cmd;
			while (std::getline(std::cin, cmd))
				AdminConsoleInterface::processCommand(cmd);
		}
	);
	try {
		printf("Starting server on port %d...\n", port);
		ServerSocket server;
		try {
			server = ServerSocket(port);
		}
		catch (const SocketException& e)
		{
			printf("%s\n", e.what());
			log::server(std::string("Unhandled exception: ") + e.what(), log::LEVEL_FATAL);
			return EXIT_FAILURE;
		}
		::server = &server;
		while (running)
		{
			TCPSocket* client = server.accept(true);
			login.addPlayer(client);
		}
	}
	catch (const ServerSocket::ServerShutdownException&)
	{
		return 0;
	}
	catch (const std::exception& e)
	{
		printf("Unhandled exception: %s\n", e.what());
		log::server(std::string("Unhandled exception: ") + e.what(), log::LEVEL_FATAL);
		return EXIT_FAILURE;
	}
	catch (...)
	{
		printf("Unknown exception.\n");
		log::server("Unknown exception", log::LEVEL_FATAL);
		return EXIT_FAILURE;
	}

	return 0;
}
