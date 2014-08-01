#ifndef __CONNECTION_MANAGER_H__
#define __CONNECTION_MANAGER_H__
#include <mysql_driver.h>
#include <cppconn/connection.h>
#include <string>
#include <memory>

namespace airplanes {

	class ConnectionManager
	{
	public:
		///<summary>Get an std::unique_ptr owning an sql connection, which will get
		///released automatically by the pointer's destructor</summary>
		static std::unique_ptr<sql::Connection, std::function<void(sql::Connection*)>> getConnection();

		///<summary><para>Get a connection that is always the samme if the channel parameter is the same</para>
		///One channel should not be used by multiple threads concurrently.
		///Do not delete or close.</summary>
		///<param name='channel'>A number between 0 and 10</param>
		static sql::Connection* getConnection(unsigned channel);

		///<summary>Must be called before using any database related function in a thread</summary>
		static void threadInit();

		///<summary>Must be called when any thread that called threadInit() ends</summary>
		static void threadEnd();

		ConnectionManager() = delete;
		~ConnectionManager() = delete;

	protected:
		///<summary>Get an sql connection. The caller must release the connection with releaseConnection().</summary>
		static sql::Connection* getRawConnection();

		///<summary>Release a connection received from getConnection(). This function can 
		///be used as a deleter for std::unique_ptr and std::shared_ptr</summary>
		static void releaseConnection(sql::Connection*);

	private:
		static const std::string username, password;

		static const unsigned CONNECTION_POOL_SIZE = 1024;
		static bool inUse[CONNECTION_POOL_SIZE];
		static sql::Connection* connections[CONNECTION_POOL_SIZE];
		static unsigned openConnections;
		static sql::Driver* _driver;
		static sql::Driver* driver();
	};
}

#endif
