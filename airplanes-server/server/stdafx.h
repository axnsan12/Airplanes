#include <iostream>
#include <string>
#include <exception>
#include <memory>
#include <cstdint>
#include <cassert>
#include <vector>
#include <queue>
#include <iterator>
#include <list>
#include <mutex>
#include <condition_variable>
#include <thread>
#include <chrono>
#include <future>
#include <unordered_map>
#include <unordered_set>
#include <sstream>
#include <ctime>

#include <WinSock2.h>
#define WIN32_LEAN_AND_MEAN
#include <Windows.h>

#include <iostream>

#ifdef max
	#undef max
#endif
#ifdef min
	#undef min
#endif
#include <algorithm>

#pragma warning(disable: 4251 4512)
#include "mysql_connection.h"
#include "mysql_driver.h"

#include <cppconn/driver.h>
#include <cppconn/connection.h>
#include <cppconn/exception.h>
#include <cppconn/resultset.h>
#include <cppconn/statement.h>
#include <cppconn/prepared_statement.h>

#pragma comment(lib, "libmysql.lib")
#pragma comment(lib, "mysqlcppconn.lib")
#pragma warning(default: 4251 4512)

#include <utf8.h>
#include <BCrypt.hpp>

