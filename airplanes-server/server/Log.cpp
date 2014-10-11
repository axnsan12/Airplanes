#include "stdafx.h"
#include "log.h"
#include <ctime>
#include <iomanip>

namespace airplanes {
	const std::string log::LOG_PATH = "C:\\Projects\\airplanes\\logs\\";
	const std::string log::SERVER_LOG = "server.log";
	const std::string log::GAME_SERVER_LOG = "game.log";

	const std::map<log::Level, std::string> log::level_string = { 
		{ LEVEL_INFO, "INFO" }, 
		{ LEVEL_WARNING, "WARNING" }, 
		{ LEVEL_ERROR, "ERROR" },
		{ LEVEL_EXCEPTION, "EXCEPTION" },
		{ LEVEL_FATAL, "FATAL" }
	};

	std::ofstream log::srv_stream;
	std::mutex log::srv_mut;
	void log::server(const std::string& msg, Level level)
	{
		std::lock_guard<std::mutex> guard(srv_mut);
		if (!srv_stream.is_open())
			srv_stream.open(LOG_PATH + SERVER_LOG, std::ios::app | std::ios::binary);

		srv_stream << timestamp() << '[' << level_string.at(level) << "] " << msg << std::endl;
		srv_stream.flush();
	}

	std::ofstream log::gsrv_stream;
	std::mutex log::gsrv_mut;
	void log::game_server(const std::string& msg, Level level)
	{
		std::lock_guard<std::mutex> guard(gsrv_mut);
		if (!gsrv_stream.is_open())
			gsrv_stream.open(LOG_PATH + GAME_SERVER_LOG, std::ios::app | std::ios::binary);

		gsrv_stream << timestamp() << '[' << level_string.at(level) << "] " << msg << std::endl;
		gsrv_stream.flush();
	}

	std::ofstream log::_dbg_socket_log;
	std::mutex log::_dbg_socket_mut;

	void log::dbg_socket(const std::string& msg)
	{
		std::lock_guard<std::mutex> guard(_dbg_socket_mut);
		if (!_dbg_socket_log.is_open())
			_dbg_socket_log.open(LOG_PATH + "debug_socket.log", std::ios::app | std::ios::binary);

		_dbg_socket_log << timestamp() << msg << std::endl;
		_dbg_socket_log.flush();
	}

	void log::dbg_socket(const std::string& msg, const std::string& client, const uint8_t* bytes, unsigned len)
	{
		std::lock_guard<std::mutex> guard(_dbg_socket_mut);
		if (!_dbg_socket_log.is_open())
			_dbg_socket_log.open(LOG_PATH + "debug_socket.log", std::ios::app | std::ios::binary);

		_dbg_socket_log << timestamp() << msg << " (" << client << " sent " << std::hex;
		for (unsigned i = 0; i < len; ++i)
			_dbg_socket_log << "0x" << (unsigned)*(bytes + i) << ' ';
		_dbg_socket_log << ')' << std::endl;
		_dbg_socket_log.flush();
	}

	void log::game(uint32_t id, const std::string& msg)
	{
		std::ofstream g;
		g.open(LOG_PATH + "games\\" + std::to_string(id) + ".log", std::ios::app | std::ios::binary);

		g << timestamp() << msg << std::endl;
		g.flush();
		g.close();
	}

	std::string log::timestamp()
	{
		time_t utc_time = time(nullptr);
		tm *calendar_time = localtime(&utc_time);
		char buf[30];
		if (strftime(buf, sizeof buf, "[%d/%m/%Y %H:%M:%S]", calendar_time))
			return buf;
		else return "[BAD_TIMESTAMP]";
	}
}
