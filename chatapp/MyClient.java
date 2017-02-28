package chatapp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.HashMap;


public class MyClient {
	
	public static HashMap<String, MyUser> userMap = new HashMap<String, MyUser>();
	public static String userName;
	public static String serverName;
	public static int serverPort;
	
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
			
			DatagramSocket receiveSocket = new DatagramSocket(localPort);
			Thread threadReceive = new Thread((new MyClient()).new ReceiverThread(receiveSocket));
			threadReceive.start();
			// register to server 
			System.out.println("Connecting to " + serverName + " on port " + serverPort);
			SendSocketServer( "new#!" + userName + "&!" + serverName + "&!" + localPort);
			System.out.println(">>> [Welcome, You are registered.]");
			// Begin to send
			BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
			
	        while (true) {
				/*
				 * client request
				 */
	        	System.out.print(">>> ");
				String inputString = input.readLine();
				String[] command = inputString.split(" ");
				String message = "";
				if (command[0].equals("send")){
					if(command.length > 2 && userMap.containsKey(command[1])){
						if(userMap.get(command[1]).getState()) {
							message = "msg#!" + userName + ": "+ inputString.split(" ",3)[2];
							SendSocketClient(message, command[1]);
						}
						else {
//							message = "msg#!" + userName + ": "+ inputString.split(" ",3)[2];
//							SendSocketClient(message, command[1]);
							System.out.println(">>> [No ACK from " + command[1] + ", message sent to server.]");
							message = userName + "&!" + command[1] + "&!: "+ inputString.split(" ",3)[2];
							SendSocketServer("save#!" + message);
						}
					}
					else {
						System.out.println("No user founded");
					}
				}
				else if (command.length > 1 && command[1].equals(userName)) {
					if (command[0].equals("dereg")) {
						SendSocketServer(command[0] + "#!" + command[1]);
						threadReceive.interrupt();
						System.out.println("[You are Offline. Bye.]");
					}
					else if(command[0].equals("reg")) {
						SendSocketServer( "reg#!" + userName + "&!" + serverName + "&!" + localPort);
						threadReceive = new Thread((new MyClient()).new ReceiverThread(receiveSocket));
						threadReceive.start();
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
		public ReceiverThread(DatagramSocket receiveSocket) {
			this.receiveSocket = receiveSocket;
		}

		@Override
		public void run() {
			byte[] receiveBuffer = new byte[1024];
			while (!Thread.currentThread().isInterrupted()) {
				DatagramPacket ServerMessagePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
				try {
					receiveSocket.receive(ServerMessagePacket);
					String rcv = new String(receiveBuffer, 0, ServerMessagePacket.getLength());
					if(rcv.split("#!")[0].equals("msg")){
						System.out.println(rcv.split("#!")[1]);
						System.out.print(">>> ");
						ServerMessagePacket.setData("ack".getBytes());
						receiveSocket.send(ServerMessagePacket);
					}
					else if(rcv.split("#!")[0].equals("off")){
						String[] offMessage = rcv.split("#!")[1].split("&!");
						System.out.println("[You have messages]");
						for(int i = 0; i < offMessage.length; i++) {
							System.out.println(">>> " + offMessage[i]);
						}
						System.out.print(">>> ");
					}
					else if(rcv.split("#!")[0].equals("update")){
						String[] users = rcv.split("#!")[1].split("&!");
						for(int i = 0; i < users.length; i++) {
							String[] user = users[i].split("%!");
							userMap.put(user[0], new MyUser(user[0], user[1], Integer.parseInt(user[2]), Boolean.valueOf(user[3])));
						}
						System.out.println("[Client table updated.]");
						System.out.print(">>> ");
					}
					else if(rcv.split("#!")[0].equals("conf")){
						System.out.println("[Same user logged in, you are going to exit.]");
						System.out.print(">>> ");
						System.exit(0);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
