package prob.pix;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class ghsSocket {
	Socket socket = null;
	String host = null;
	int port = -1;
	int timeout = -1;
	
	public ghsSocket(){}
	public ghsSocket(String host,int port){
		this.host = host;
		this.port = port;
	}
	public ghsSocket(String host,int port,int timeout){
		this(host,port);
		this.timeout = timeout;
	}
	
	public void setHost(String host){
		this.host = host;
	}
	
	public void setPort(int port){
		this.port = port;
	}
	
	public void setTimeout(int timeout){
		this.timeout = timeout;
	}
	
	public void connect() throws UnknownHostException, IOException{
		if(host == null || port < 1){
			throw new IllegalStateException("Please specify the host and port.");
		}

		socket = new Socket(host,port);
		socket.setSoTimeout(timeout);
	}
}
