package com.axnsan.airplanes.online;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.BlockingQueue;


public class ClientSocket implements Runnable
{
	private final String mRemoteAddress;
	private SocketChannel socket;
	public final BlockingQueue<Message> eventQueue;
	public final BlockingQueue<ServerResponseMessage> responseQueue;
	
	public ClientSocket(String host, int port
			, BlockingQueue<Message> eventQueue, BlockingQueue<ServerResponseMessage> responseQueue)
			throws UnknownHostException, IOException {
		mRemoteAddress = host + port;
		socket = SelectorProvider.provider().openSocketChannel();
		socket.socket().connect(new InetSocketAddress(host, port), 1000);
		this.eventQueue = eventQueue;
		this.responseQueue = responseQueue;
	}
	public String remoteAddress()  { return mRemoteAddress; }

	/*The protocol requires that messages begin with a 5-byte header consisting of
	2 verification bytes, 1 protocol version byte, and 2 message size bytes. The header is followed
	by the actual message, with length as given by the header.
	Any bytes in the stream that break this pattern are discarded.*/
	private static final byte protocol_header_1 = (byte) 0xDE, protocol_header_2 = (byte) 0xAD;
	private static final byte protocol_version = 0x01;
	private static final byte MAJOR_VERSION_MASK = (byte) 0xF0;
	private static final int protocol_header_length = 5;
	
	private int headerpos = 0;
	private ByteBuffer msgbuf = null;

	@Override
	public void run() {
		ByteBuffer recvbuf = ByteBuffer.allocate(1024);
		try {
			while (socket.read(recvbuf) > 0) {
				recvbuf.flip(); //Read from buffer
				while (recvbuf.hasRemaining()) {
					//Attempt to resolve header
					if (headerpos == 0 && recvbuf.remaining() >= 1)
					{
						if (recvbuf.get() == protocol_header_1)
							headerpos = 1;
						else {
							System.out.println("Failed to match header, server spewing garbage?");
							return;
						}
					}
					if (headerpos == 1 && recvbuf.remaining() >= 1)
					{
						if (recvbuf.get() == protocol_header_2)
							headerpos = 2;
						else {
							headerpos = 0;
							System.out.println("Failed to match header, server spewing garbage?");
							return;
						}
					}
					if (headerpos == 2 && recvbuf.remaining() >= 1)
					{
						byte ver = recvbuf.get();
						if ((ver & MAJOR_VERSION_MASK) != (protocol_version & MAJOR_VERSION_MASK)) {
							headerpos = 0;
							System.out.println("Major version mismatch, expected " + (protocol_version & MAJOR_VERSION_MASK) 
									+ ", got " + (ver & MAJOR_VERSION_MASK));
							return;
						}
						else headerpos = 3;
					}
					if (headerpos == 3 && recvbuf.remaining() >= 2)
					{
						int msglen = recvbuf.getShort();
						if (msglen == 0) {
							System.out.println("Message length of 0. What?");
							return;
						}
						msgbuf = ByteBuffer.allocate(msglen);
						headerpos = 5;
					}
	
					if (headerpos == 5)
					{
						if (recvbuf.remaining() <= msgbuf.remaining())
							msgbuf.put(recvbuf);
						else while (msgbuf.hasRemaining())
							msgbuf.put(recvbuf.get());
					}
					
					if (headerpos == 5 && !msgbuf.hasRemaining())
					{
						msgbuf.flip(); //Ready to deserialize message
						Message msg = Message.deserialize(msgbuf);
						System.out.println("Received message of type " + msg.getMessageType());
						if (msg.getMessageType() == MESSAGE_TYPE.SERVER_RESPONSE)
							responseQueue.put((ServerResponseMessage)msg);
						else eventQueue.put(msg);
						
						headerpos = 0;
						msgbuf = null;
					}
				}
				recvbuf.clear(); //Prepare buffer for writing
			}
			if (disconnected)
				eventQueue.put(new SocketClosedMessage(SocketClosedMessage.REASON.CLIENT_DISCONNECTED));
			else { 
				eventQueue.put(new SocketClosedMessage(SocketClosedMessage.REASON.DISCONNECTED_BY_SERVER));
			}
		} catch (ClosedChannelException e) {
			try {
				eventQueue.put(new SocketClosedMessage(SocketClosedMessage.REASON.CONNECTION_DISCARDED));
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		catch (IOException | InterruptedException e) {
			try {
				eventQueue.put(new SocketClosedMessage(SocketClosedMessage.REASON.CONNECTION_DROPPED));
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		try {
			socket.close();
		} catch (IOException e) { }
	}
	
	boolean isConnected() {
		return socket.isConnected() && socket.isOpen();
	}
	
	public synchronized void sendMessage(Message msg) throws IOException {
		int serlen = msg.serializeLength();
		ByteBuffer sndbuf = ByteBuffer.allocate(protocol_header_length + serlen);
		sndbuf.put(protocol_header_1);
		sndbuf.put(protocol_header_2);
		sndbuf.put(protocol_version);
		sndbuf.putShort((short)serlen);
		msg.serialize(sndbuf);
		sndbuf.flip();
		socket.write(sndbuf);
	}
	
	private boolean disconnected = false;
	public void disconnect() {
		if (disconnected)
			return;
		
		disconnected = true;
		try {
			socket.close();
		} catch (IOException e) { }
	}
	
	@Override 
	public void finalize() {
		disconnect();
	}
};

