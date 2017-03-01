package chatapp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.HashMap;

/** 
 * @author rz2357
 * @version 1.0 
 */  

public class MyServer {
	
	public static HashMap<String, MyUser> userMap = new HashMap<String, MyUser>();
	
	public static void main(String[] args) {
		int localPort = Integer.parseInt(args[0]);
		// before shut down
		Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                System.out.println("Shutdown hook ran!");
            }
        });

		try {
			DatagramSocket receiveSocket = new DatagramSocket(localPort);
			byte[] receiveBuffer = new byte[1024];
			while (true) {
				DatagramPacket ServerMessagePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
				try {
					receiveSocket.receive(ServerMessagePacket);
					String rcv = new String(receiveBuffer, 0, ServerMessagePacket.getLength());
					if(rcv != null){
						// send ACK back to client
						ServerMessagePacket.setData("ack".getBytes());
						receiveSocket.send(ServerMessagePacket);
						String[] query = rcv.split("#!");
						// get a offline message
						if (query[0].equals("save")) {
							String[] reqUser = query[1].split("&!");
							userMap.get(reqUser[1]).setState(false);
							// if user is active
							if(userMap.get(reqUser[1]).getState()) {
								SendClient("active#!" + reqUser[1], reqUser[0]);
							}
							//"save#!fromUser&:!toUser&!message content"
							// save#!y:&!x&!hhhhhahaha
							// if not, save the offline message
							else {
								String message = reqUser[0] + new Date().toString()+ " "+ reqUser[2];
								userMap.get(reqUser[1]).AddOfflineMessage(message);
							}
						}
						// register a new user
						else if(query[0].equals("new")){
							String[] regUser = query[1].split("&!");
							System.out.println("new user in " + regUser[0] + " " + regUser[1] + " " + regUser[2]);
							if(userMap.containsKey(regUser[0]) &&
									(!userMap.get(regUser[0]).getAddr().equals(regUser[1]) || 
									userMap.get(regUser[0]).getPort() != Integer.parseInt(regUser[2]))) {
								if(userMap.get(regUser[0]).getState()) {
									SendClient("conf#!",regUser[0]);
									userMap.put(regUser[0], new MyUser(regUser[0], regUser[1], Integer.parseInt(regUser[2]), true));
								}
								// check new user's offline message
								else if(!userMap.get(regUser[0]).IsEmptyOfflineMessage()){
									String tmpMessage = userMap.get(regUser[0]).getOfflineMessage();
									userMap.put(regUser[0], new MyUser(regUser[0], regUser[1], Integer.parseInt(regUser[2]), true));
									userMap.get(regUser[0]).AddOfflineMessage(tmpMessage);
									SendOffline(regUser[0]);
									userMap.get(regUser[0]).ClearOfflineMessage();
								}
							}
							else {
								userMap.put(regUser[0], new MyUser(regUser[0], regUser[1], Integer.parseInt(regUser[2]), true));

							}
						}
						// register a user
						else if(query[0].equals("reg")){
							String[] regUser = query[1].split("&!");
							userMap.get(regUser[0]).regNew(regUser[1], Integer.parseInt(regUser[2]), true);
							if(!userMap.get(regUser[0]).IsEmptyOfflineMessage()){
								SendOffline(regUser[0]);
								userMap.get(regUser[0]).ClearOfflineMessage();
							}
						}
						// deregister a user
						else if(query[0].equals("dereg")){
							userMap.get(query[1]).setState(false);
						}
						// update users table
						new Thread(new Broadcast(userMap)).start(); 
					}
				} catch (IOException e) {
					e.printStackTrace();
					receiveSocket.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// send offline message to a client
	private static void SendOffline(String toUser) {
		byte[] sendBuffer = new byte[1024];
		try {
			DatagramSocket sendSocket = new DatagramSocket();
			sendBuffer = ("off#!" + userMap.get(toUser).getOfflineMessage()).getBytes();
			DatagramPacket requestPacket = new DatagramPacket(sendBuffer, sendBuffer.length, 
																InetAddress.getByName(userMap.get(toUser).getAddr()), 
																userMap.get(toUser).getPort());
			sendSocket.send(requestPacket);
			sendSocket.close();
			System.out.println(userMap.get(toUser).getOfflineMessage() + " " + userMap.get(toUser).getAddr() + " " + userMap.get(toUser).getPort());
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	// send message to client
	private static void SendClient(String message, String toUser) {
		byte[] sendBuffer = new byte[1024];
		try {
			DatagramSocket sendSocket = new DatagramSocket();
			sendBuffer = message.getBytes();
			DatagramPacket requestPacket = new DatagramPacket(sendBuffer, sendBuffer.length, 
																InetAddress.getByName(userMap.get(toUser).getAddr()), 
																userMap.get(toUser).getPort());
			sendSocket.send(requestPacket);
			sendSocket.close();
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}

// nroadcast thread to update userMap
class Broadcast implements Runnable {  
    private HashMap<String, MyUser> userMap;  
    public Broadcast(HashMap<String, MyUser> userMap){  
        this.userMap = userMap;  
    }  
    
    public void run() {
    	byte[] sendBuffer = new byte[1024];
        try {
        	String message = "update#!";
        	for (MyUser user: userMap.values()) {
        		if(!message.equals("update#!")) {
        			message += "&!";
        		}
        		message += user.getName() + "%!" + user.getAddr().toString() + "%!" + user.getPort() + "%!" + user.getState();
        	}
        	sendBuffer = message.getBytes();
			DatagramSocket sendSocket = new DatagramSocket();
			// send to all users
			for (MyUser user: userMap.values()) {
        		if(true) {
        			DatagramPacket requestPacket = new DatagramPacket(sendBuffer, sendBuffer.length, 
        															InetAddress.getByName(user.getAddr()), 
        															user.getPort());
        			sendSocket.send(requestPacket);
        		}
        	}
			sendSocket.close();
			System.out.println("update users table");
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }  
}
