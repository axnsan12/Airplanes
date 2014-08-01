#include "stdafx.h"
#include "AdminConsoleInterface.h"

extern BOOL WINAPI Shutdown(DWORD);

namespace airplanes {
uint32_t AdminConsoleInterface::client::next_cid = 0;
std::vector<AdminConsoleInterface::client> AdminConsoleInterface::clients;
bool AdminConsoleInterface::attached = false;

void AdminConsoleInterface::processCommand(const std::string& cmd)
{
	if (cmd == "attach") {
		attached = true;
		clients.push_back(std::string("127.0.0.1"));

	}
	if (cmd == "exit") {
		std::thread([]() { Shutdown(0); }).detach();
	}
}

}
