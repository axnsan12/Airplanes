#include "stdafx.h"
#include "ConnectionManager.h"
#include <mutex>

namespace airplanes {

	unsigned ConnectionManager::openConnections = 0;
	const std::string ConnectionManager::username = "airplanes";
	const std::string ConnectionManager::password = "KLHnEnH23D2EfHH6";
	bool ConnectionManager::inUse[ConnectionManager::CONNECTION_POOL_SIZE] = { 0 };
	sql::Connection* ConnectionManager::connections[ConnectionManager::CONNECTION_POOL_SIZE] = { nullptr };
	sql::Driver* ConnectionManager::_driver = nullptr;

	sql::Connection* ConnectionManager::getRawConnection()
	{
		static std::mutex pool_mutex;
		pool_mutex.lock();
		sql::Connection* ret = nullptr;
		for (unsigned i = 0; i < openConnections; ++i)
			if (!inUse[i]) { inUse[i] = true; ret = connections[i]; break; }

		if (ret == nullptr)
		{
			ret = driver()->connect("tcp://localhost:3306", username, password);
			if (openConnections < CONNECTION_POOL_SIZE) {
				connections[openConnections] = ret; inUse[openConnections] = true; openConnections++;
			}
		}

		pool_mutex.unlock();
		return ret;
	}

	std::unique_ptr<sql::Connection, std::function<void(sql::Connection*)>> ConnectionManager::getConnection()
	{
		return std::unique_ptr<sql::Connection, std::function<void(sql::Connection*)>>
			(getRawConnection(), releaseConnection);
	}

	sql::Connection* ConnectionManager::getConnection(unsigned channel)
	{
		static sql::Connection* _conns[11] = { nullptr };
		if (_conns[channel] == nullptr)
			_conns[channel] = driver()->connect("tcp://localhost:3306", username, password);

		return _conns[channel];
	}

	void ConnectionManager::releaseConnection(sql::Connection* conn)
	{
		//No need for locking; no tragedy if a connection is released asynchronously
		if (conn == nullptr)
			return;

		for (unsigned i = 0; i < openConnections; ++i)
			if (connections[i] == conn)
				{ inUse[i] = false; return; }

		if (!conn->isClosed())
			conn->close();

		delete conn;
	}

	void ConnectionManager::threadInit() { driver()->threadInit(); }
	void ConnectionManager::threadEnd() { driver()->threadEnd(); }

	sql::Driver* ConnectionManager::driver() 
	{
		if (_driver == nullptr) 
			_driver = sql::mysql::get_driver_instance(); 
	
		return _driver;
	}
}
