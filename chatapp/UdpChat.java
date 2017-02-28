package chatapp;

public class UdpChat {

	public static void main(String[] args) {
		if(args[0].equals("-c") && args.length == 5) {
			MyClient.main(new String[]{args[1], args[2], args[3], args[4]});
			
		}
		else if(args[0].equals("-s") && args.length == 2) {
			MyServer.main(new String[]{args[1]});
		}
		else {
			System.out.println("UdpChat -s <port>; UdpChat -c <client-name> <server-ip> <server-port> <local-port>");
		}
	}
}
