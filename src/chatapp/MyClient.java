package chatapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;

/** 
 * @author rz2357
 * @version 1.0 
 */  

public class MyClient {
	
	public static HashMap<String, MyUser> userMap = new HashMap<String, MyUser>();
	public static String userName;
	public static String serverName;
	public static int serverPort;
	public static volatile boolean stopFlag = false;

	
	public static void main(String[] args) {
		userName = args[0];
		serverName = args[1];
		serverPort = Integer.parseInt(args[2]);
		int localPort = Integer.parseInt(args[3]);
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                System.out.println("See u!");
            }
        });

		try {
			// Create DatagramSocket
			
			Thread threadReceive = new Thread((new MyClient()).new ReceiverThread(localPort));
			threadReceive.start();
			// register to server 
			System.out.println("Connecting to " + serverName + " on port " + serverPort);
			SendSocketServer( "new#!" + userName + "&!" + InetAddress.getLocalHost().getHostAddress() + "&!" + localPort);
			System.out.println(">>> [Welcome, You are registered.]");
			// Begin to send
			BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
			
	        while (true) {
	        	//user requests
	        	System.out.print(">>> ");
				String inputString = input.readLine();
				String[] command = inputString.split(" ");
				String message = "";
				//send message requests
				if (command[0].equals("send")){
					if(command.length > 2 && userMap.containsKey(command[1])){
						if(command[1].equals(userName)) {
							System.out.println(">>> [You cannot send to yourself.]");
						}
						else {
							message = "msg#!" + userName + ": " + inputString.split(" ", 3)[2];
							SendSocketClient(message, command[1]);
						}
					}
					else {
						System.out.println("No user founded");
					}
				}
				else if (command.length > 1 && command[1].equals(userName)) {
					if (command[0].equals("dereg")) {
						SendSocketServer(command[0] + "#!" + command[1]);
						Thread.currentThread().interrupt();
						stopFlag = true;
						//threadReceive.join();
						
					}
					else if(command[0].equals("reg")) {
						stopFlag = false;
						threadReceive = new Thread((new MyClient()).new ReceiverThread(localPort));
						threadReceive.start();
						SendSocketServer( "reg#!" + userName + "&!" + serverName + "&!" + localPort);
						
					}
				}
				else { 
					//deal with invalid usage; also account for incorrect second args
					System.out.println("Usage: send <user> <message>; dereg <user>; reg <user>");
				}
	        }
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private static void SendSocketClient(String message, String toUser) throws IOException {
		byte[] sendBuffer = new byte[1024];
		byte[] receiveBuffer = new byte[1024];
		DatagramSocket sendSocket = new DatagramSocket();
		sendBuffer = message.getBytes();
		DatagramPacket requestPacket = new DatagramPacket(sendBuffer, sendBuffer.length, 
											InetAddress.getByName(userMap.get(toUser).getAddr()), 
											userMap.get(toUser).getPort());
		sendSocket.send(requestPacket);
		DatagramPacket responsePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
		// if time exceeds, break
		sendSocket.setSoTimeout(500);   // set the timeout in ms.
		try {
			sendSocket.receive(responsePacket);
			System.out.println(">>> [Message received by " + toUser + ".]");
		} catch (SocketTimeoutException e) {
			// timeout exception.
			System.out.println(">>> [No ACK from " + toUser + ", message sent to server.]");
			sendSocket.close();
			message = message.split("#!")[1].split(" ",2)[0] + "&!" + toUser + "&!" + message.split("#!")[1].split(" ",2)[1];
			SendSocketServer("save#!" + message);
		}
		sendSocket.close();
	}
	
	// A thread of listening port
	private static void SendSocketServer(String message) throws IOException {
		byte[] sendBuffer = new byte[1024];
		byte[] receiveBuffer = new byte[1024];
		DatagramSocket sendSocket = new DatagramSocket();
		sendBuffer = message.getBytes();
		DatagramPacket requestPacket = new DatagramPacket(sendBuffer, sendBuffer.length, InetAddress.getByName(serverName), serverPort);
		DatagramPacket responsePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
		sendSocket.setSoTimeout(500);   // set the timeout in ms.
        int tries = 0; 
        boolean receivedResponse = false;
        while(!receivedResponse && tries < 5){  
        	sendSocket.send(requestPacket);
            try{ 
            	sendSocket.receive(responsePacket);   
                receivedResponse = true;  
            }catch(SocketTimeoutException e){
                tries += 1;  
                System.out.println("Time out," + (5 - tries) + " more tries..." ); 
            }  
        }
        if(!receivedResponse){
            sendSocket.close();
        	System.out.println("Server shuts down, exit!" );
        	System.exit(0);
        }
        sendSocket.close();
	}
	
	public class ReceiverThread implements Runnable {
		DatagramSocket receiveSocket;
		public ReceiverThread(int localPort) throws SocketException {
			this.receiveSocket = new DatagramSocket(localPort);
		}

		@Override
		public void run() {
			byte[] receiveBuffer = new byte[1024];
			while (!stopFlag) {
				DatagramPacket ServerMessagePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
				try {
					receiveSocket.receive(ServerMessagePacket);
					String rcv = new String(receiveBuffer, 0, ServerMessagePacket.getLength());
					// received a message
					if(rcv.split("#!")[0].equals("msg")){
						System.out.println(rcv.split("#!")[1]);
						System.out.print(">>> ");
						ServerMessagePacket.setData("ack".getBytes());
						receiveSocket.send(ServerMessagePacket);
					}
					// received offline messages and process
					else if(rcv.split("#!")[0].equals("off")){
						System.out.println(rcv);
						String[] offMessage = rcv.split("#!")[1].split("&!");
						System.out.println("[You have messages]");
						for(int i = 0; i < offMessage.length; i++) {
							System.out.println(">>> " + offMessage[i]);
						}
						System.out.print(">>> ");
					}
					// received update command of users table
					else if(rcv.split("#!")[0].equals("update")){
						String[] users = rcv.split("#!")[1].split("&!");
						for(int i = 0; i < users.length; i++) {
							String[] user = users[i].split("%!");
							userMap.put(user[0], new MyUser(user[0], user[1], Integer.parseInt(user[2]), Boolean.valueOf(user[3])));
						}
						System.out.println("[Client table updated.]");
						System.out.print(">>> ");
					}
					// received a confliction, need to quit
					else if(rcv.split("#!")[0].equals("conf")){
						System.out.println("[Same user logged in, you are going to exit.]");
						System.out.print(">>> ");
						System.exit(0);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			receiveSocket.close();
			System.out.println("[You are Offline. Bye.]");
			System.out.print(">>> ");
		}
	}
	
}
