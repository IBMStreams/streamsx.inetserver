package com.ibm.streamsx.inetserver.test;

import java.net.URI;
import java.util.concurrent.Future;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.WebSocketClient;

public class WsClient2 {

	public static String directory = ".";
	
	public static void main(String[] args) throws Exception {
		if (args.length != 2) {
			System.err.println("Two arguments required with uri");
			throw new IllegalArgumentException("Two argument required with uri, directory");
		}
		System.out.println("Start socket with uri: " + args[0]);
		directory = args[1];
		System.out.println("Start socket with directory: " + directory);
		URI uri = URI.create(args[0]);
		WebSocketClient webSocketClient = new WebSocketClient();
		
		try {
			webSocketClient.start();
			EventSocket2 socket1 = new EventSocket2();
			EventSocket2 socket2 = new EventSocket2();
			// Attempt Connect
			Future<Session> fut1 = webSocketClient.connect(socket1, uri);
			// Wait for Connect
			Session session1 = fut1.get();
			Thread.sleep(500);
			Future<Session> fut2 = webSocketClient.connect(socket2, uri);
			// Wait for Connect
			Session session2 = fut2.get();
			// Send a message
			
			Thread.sleep(5000);
			System.out.println("close session");
			// Close session1
			session1.close();
			session2.close();
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

}
