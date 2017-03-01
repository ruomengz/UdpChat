package chatapp;

import java.io.IOException;

public class MyUser {
	
	private String userName = "";
	private String address = "";
	private int port = 0;
	private boolean state = true;
	private String OfflineMessage = "";
	
	public MyUser( String n, String a, int p, boolean s) throws IOException{
        this.userName = n;
        this.address = a;
        this.port = p;
        this.state = s;
	}
	
	public boolean IsEmptyOfflineMessage() {
		if(this.OfflineMessage.isEmpty()) {
			return true;
		}
		return false;
	}
	
	public String getOfflineMessage() {
		return this.OfflineMessage;
	}
	
	public void AddOfflineMessage(String n) {
		if(this.OfflineMessage.equals("")) {
			this.OfflineMessage = this.OfflineMessage + n;
		}
		else{
			this.OfflineMessage = this.OfflineMessage + "&!" + n;
		}
	}
	
	public void ClearOfflineMessage() {
		this.OfflineMessage = "";
	}
	
	public void setState(boolean s) {
		this.state = s;
	}
	
	public void setAddr(String a) {
		this.address = a;
	}
	
	public void setPort(int p) {
		this.port = p;
	}
	
	public void regNew(String a, int p, boolean s) {
		this.address = a;
		this.port = p;
		this.state = s;
	}
	
	public String getAddr() {
		return this.address;
	}
	
	public int getPort() {
		return this.port;
	}
	
	public String getName() {
		return this.userName;
	}

	public boolean getState() {
		return this.state;
	}
}
