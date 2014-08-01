#ifndef __LOG_H__
#define __LOG_H__
#include <string>
#include <fstream>
#include <map>
#include <cstdint>
#include <mutex>

namespace airplanes {

	class log
	{
	public:
		enum Level { LEVEL_INFO, LEVEL_WARNING, LEVEL_ERROR, LEVEL_EXCEPTION, LEVEL_FATAL };
		static void server(const std::string& msg, Level level = LEVEL_INFO);
		static void dbg_socket(const std::string& msg);
		static void dbg_socket(const std::string& msg, const std::string& client, const uint8_t* bytes, unsigned len);
		static void game_server(const std::string& msg, Level level = LEVEL_INFO);
		static void game(uint32_t id, const std::string& msg);

	private:
		static const std::string LOG_PATH, SERVER_LOG, GAME_SERVER_LOG;
		static const std::map<Level, std::string> level_string;
		static std::string timestamp();

		static std::ofstream _dbg_socket_log;
		static std::mutex _dbg_socket_mut;
		static std::ofstream srv_stream;
		static std::mutex srv_mut;
		static std::ofstream gsrv_stream;
		static std::mutex gsrv_mut;
		log();
		~log();
	};
}

#endif
