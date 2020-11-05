import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;


public class ServerHandler extends Thread {

	private Socket s;
	private DataOutputStream streamOut;
	private DataInputStream streamIn;
	private boolean running = true;


	public ServerHandler( DataOutputStream streamOut, DataInputStream streamIn) {
		this.streamOut = streamOut;
		this.streamIn = streamIn;
		this.start();
	}


	@Override
	public void run() {
		try {
			String onlineMessage = packageMessage("online");
			while(running) {
				streamOut.writeUTF(onlineMessage);
				streamOut.flush();
				String res = streamIn.readUTF();
				//System.out.println(res);
				for(int i = 0; i < 1000; i++) {
					if(running) {
						Thread.sleep(10);
					} else {
						break;
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public void end() {
		running = false;
	}

	private String packageMessage(String message) throws Exception {
        StringBuilder acc = new StringBuilder();
        acc.append(message);
        return acc.toString();
    }

}
