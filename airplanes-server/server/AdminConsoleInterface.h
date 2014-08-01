#ifndef _ADMIN_CONSOLE_INTERFACE_H_
#define _ADMIN_CONSOLE_INTERFACE_H_
#include <string>
#include <vector>

namespace airplanes {
class AdminConsoleInterface
{
public:
	static void processCommand(const std::string& command);
	static void notifyNewConnection(const std::string& remoteAddress);

	AdminConsoleInterface() = delete;
	~AdminConsoleInterface() = delete;

private:
	static bool attached;
	static struct client {
		uint32_t cid = next_cid++;
		std::string remoteAddress, username;

		client(const std::string& address) : remoteAddress(address) {}

	private:
		static uint32_t next_cid;
	};
	static std::vector<client> clients;
};
}

#endif