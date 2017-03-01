package chatapp;

/** 
 * @author rz2357
 * @version 1.0 
 */  
public class UdpChat {

	public static void main(String[] args) throws InterruptedException {
		if(args.length == 5 && args[0].equals("-c")) {
			MyClient.main(new String[]{args[1], args[2], args[3], args[4]});
			
		}
		else if(args.length == 2 && args[0].equals("-s")) {
			MyServer.main(new String[]{args[1]});
		}
		else {
			System.out.println("UdpChat -s <port>; UdpChat -c <client-name> <server-ip> <server-port> <local-port>");
		}
	}
}
