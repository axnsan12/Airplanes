using System;
using System.Net.Sockets;

namespace console
{
	class ServerConnection
	{
		private Socket socket;
		public ServerConnection()
		{
			socket = new Socket(AddressFamily.InterNetwork, SocketType.Stream, ProtocolType.Tcp);
		}

		public bool Connect(ushort port, TimeSpan timeout)
		{
			if (socket != null && socket.Connected)
				throw new InvalidOperationException("The connection was already succesfully initialized");
			try
			{
				socket.Connect("localhost", port, timeout);
				return true;
			}
			catch (SocketException e)
			{
				Console.WriteLine(e.ErrorCode);
				return false;
			}
		}

		~ServerConnection()
		{
			if (socket != null)
				socket.Disconnect(false);
			socket = null;
		}
	}

	public static class SocketExtensions
	{
		/// <summary>
		/// Connects the specified socket.
		/// </summary>
		/// <param name="socket">The socket.</param>
		/// <param name="endpoint">The IP endpoint.</param>
		/// <param name="timeout">The timeout.</param>
		public static void Connect(this Socket socket, string hostname, Int32 port, TimeSpan timeout)
		{
			var result = socket.BeginConnect(hostname, port, null, null);

			bool success = result.AsyncWaitHandle.WaitOne(timeout, true);
			if (success)
			{
				socket.EndConnect(result);
			}
			else
			{
				throw new SocketException(10060); // Connection timed out.
			}
		}
	}
}
