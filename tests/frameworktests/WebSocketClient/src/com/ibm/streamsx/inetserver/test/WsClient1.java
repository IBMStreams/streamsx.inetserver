package com.ibm.streamsx.inetserver.test;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.Future;

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WsClient1 {

	static boolean binaryMode = false;
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Two argument required with uri bin|text");
			throw new IllegalArgumentException("Two argument required with uri bin|text");
		}
		if (args[1].equalsIgnoreCase("bin"))
			binaryMode = true;
		System.out.println("Start socket with uri: " + args[0] +"\n binaryMode: " + binaryMode);
		URI uri = URI.create(args[0]);
		WebSocketClient webSocketClient = new WebSocketClient();
		
		try {
			webSocketClient.start();
			EventSocket1 socket1 = new EventSocket1();
			EventSocket1 socket2 = new EventSocket1();
			// Attempt Connect
			Future<Session> fut1 = webSocketClient.connect(socket1, uri);
			Future<Session> fut2 = webSocketClient.connect(socket2, uri);
			// Wait for Connect
			Session session1 = fut1.get();
			Session session2 = fut2.get();
			// Send a message
			for (int i=0; i<10; i++) { //20 messages
				System.out.println("Send1");
				sendPartial(session1.getRemote(), i + "Hello1_123456789abcdefghijkl", false);
				send(session2.getRemote(), i + "Hello1_second_123456789abcdefghijkl");
				Thread.sleep(100);
				System.out.println("Send2");
				sendPartial(session1.getRemote(), i + "Hello2_123456789abcdefghijkl", true);
			}
			Thread.sleep(1000);
			System.out.println("Send3");
			session2.close();
			for (int i=0; i<10; i++) { //10 Messages
				sendPartial(session1.getRemote(), i + "Hello3_123456789abcdefghijkl", false);
				Thread.sleep(100);
				System.out.println("Send4");
				sendPartial(session1.getRemote(), i + "Hello4_123456789abcdefghijkl", true);
			}
			
			//String data = "You There?";
			//ByteBuffer payload = ByteBuffer.wrap(data.getBytes());
			//session.getRemote().sendPing(payload);
			System.out.println("sleep 5 sec.");
			Thread.sleep(5000);
			System.out.println("close session");
			// Close session1
			session1.close();
			//fut.cancel(true);
		} catch (Exception e) {
			System.out.println("Exception");
			e.printStackTrace();
			throw e;
		} finally {
			try {
				System.out.println("Finally");
				webSocketClient.stop();
			} catch (Exception e) {
				System.out.println("Inner Exception");
				e.printStackTrace();
				throw e;
			}
		}
		
		System.out.println("ENDE");

	}

	static void sendPartial(RemoteEndpoint remote, String text, boolean fin) throws IOException {
		if (binaryMode) {
			ByteBuffer bb = ByteBuffer.wrap(text.getBytes());
			remote.sendPartialBytes(bb, fin);
		} else {
			remote.sendPartialString(text, fin);
		}
	}
	
	static void send(RemoteEndpoint remote, String text) throws IOException {
		if (binaryMode) {
			ByteBuffer bb = ByteBuffer.wrap(text.getBytes());
			remote.sendBytes(bb);
		} else {
			remote.sendString(text);
		}
	}

}
