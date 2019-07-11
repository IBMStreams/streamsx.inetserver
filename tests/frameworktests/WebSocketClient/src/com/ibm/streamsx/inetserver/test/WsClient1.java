package com.ibm.streamsx.inetserver.test;

import java.net.URI;
import java.util.concurrent.Future;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WsClient1 {

	public static void main(String[] args) {
		if (args.length != 1) {
			System.err.println("One argument required with uri");
			throw new IllegalArgumentException("One argument required with uri");
		}
		System.out.println("Start socket with uri: " + args[0]);
		URI uri = URI.create(args[0]);
		WebSocketClient webSocketClient = new WebSocketClient();
		
		try {
			webSocketClient.start();
			EventSocket socket1 = new EventSocket();
			EventSocket socket2 = new EventSocket();
			// Attempt Connect
			Future<Session> fut1 = webSocketClient.connect(socket1, uri);
			Future<Session> fut2 = webSocketClient.connect(socket2, uri);
			// Wait for Connect
			Session session1 = fut1.get();
			Session session2 = fut2.get();
			// Send a message
			System.out.println("Send1");
			session1.getRemote().sendPartialString("Hello1_123456789abcdefghijkl", false);
			session2.getRemote().sendString("Hello1_second_123456789abcdefghijkl");
			Thread.sleep(1000);
			System.out.println("Send2");
			session1.getRemote().sendPartialString("Hello2_123456789abcdefghijkl", true);
			session2.getRemote().sendString("Hello2_second_123456789abcdefghijkl");
			Thread.sleep(1000);
			System.out.println("Send3");
			session2.close();
			session1.getRemote().sendPartialString("Hello3_123456789abcdefghijkl", false);
			Thread.sleep(1000);
			System.out.println("Send4");
			session1.getRemote().sendPartialString("Hello4_123456789abcdefghijkl", true);
			
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
		} finally {
			try {
				System.out.println("Finally");
				webSocketClient.stop();
			} catch (Exception e) {
				System.out.println("Inner Exception");
				e.printStackTrace();
			}
		}
		
		System.out.println("ENDE");

	}

}
