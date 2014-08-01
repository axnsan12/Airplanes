#ifndef __AIRPLANES_H__
#define __AIRPLANES_H__

#include "stdafx.h"
#include <stdint.h>
#include <string>
#include <exception>

namespace airplanes {
	int WSAStartup();

	const unsigned int SERVER_FPS = 20;
	const unsigned int SERVER_PORT = 27015;

	//If a new game doesn't start for this much time since it was created, it gets deleted
	const unsigned int GAME_TIMEOUT = 60 * 60 * 24; 

	const unsigned int GAME_MAX_NUM_PLAYERS = 8;
	const unsigned int GAME_MAX_PASSWORD_LENGTH = 5;
	const std::string GAME_DATABASE_NAME = "airplanes";
	const std::string ACCOUNT_DATABASE_NAME = "webauth";

	const unsigned int ACCOUNT_MIN_PASSWORD_LENGTH = 6;
	const unsigned int ACCOUNT_MAX_PASSWORD_LENGTH = 40;
	const unsigned int ACCOUNT_MIN_USERNAME_LENGTH = 6;
	const unsigned int ACCOUNT_MAX_USERNAME_LENGTH = 15;

	class sync_cout {

	};

	extern sync_cout nocout;

	class locked_cout {
		static std::recursive_mutex mut;

	public:
		locked_cout() { mut.lock(); }
		locked_cout(const locked_cout&) = delete;
		locked_cout& operator=(const locked_cout&) = delete;
		~locked_cout() { mut.unlock(); }

		static void lock() { mut.lock(); }
		static void unlock() { mut.unlock(); }
	};

	template<class T> inline locked_cout&& operator<<(sync_cout&, T data) {
		std::cout << data;
		return std::move(locked_cout());
	}

	inline locked_cout&& operator<<(sync_cout&, std::ostream& (*pf)(std::ostream&)) {
		std::cout << pf;
		return std::move(locked_cout());
	}

	template<class T> inline locked_cout&& operator<<(locked_cout&& out, T data) {
		std::cout << data;
		return std::move(out);
	}

	inline locked_cout&& operator<<(locked_cout&& out, std::ostream& (*pf)(std::ostream&)) {
		std::cout << pf;
		return std::move(out);
	}

	inline bool GAME_VALIDATE_PASSWORD(const std::string& gamePassword) {
		if (gamePassword.length() > GAME_MAX_PASSWORD_LENGTH)
			return false;

		for (char c : gamePassword)
			if (!isalnum(c)) return false;

		return true;
	}

	inline bool REGISTER_VALIDATE_PASSWORD(const std::wstring& password) {
		if (password.length() < ACCOUNT_MIN_PASSWORD_LENGTH || password.length() > ACCOUNT_MAX_PASSWORD_LENGTH)
			return false;

		for (wchar_t c : password)
			if (c == L'\0')
				return false;

		return true;
	}

	inline bool REGISTER_VALIDATE_USERNAME(const std::string& username) {
		if (username.length() < ACCOUNT_MIN_USERNAME_LENGTH || username.length() > ACCOUNT_MAX_USERNAME_LENGTH)
			return false;

		for (char c : username)
			if (!isalnum(c)) return false;

		return true;
	}
}

#endif