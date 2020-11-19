import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;


public class ServerHandler extends Thread {

	private Socket s;
	private DataOutputStream streamOut;
	private DataInputStream streamIn;
	private KeyGen keyGen;
	private boolean running = true;


	public ServerHandler( DataOutputStream streamOut, DataInputStream streamIn, KeyGen keyGen) {
		this.streamOut = streamOut;
		this.streamIn = streamIn;
		this.keyGen = keyGen;
		this.start();
	}


	@Override
	public void run() {
		try {
			String onlineMessage = packageMessage("online");
			while(running) {
				streamOut.writeUTF(onlineMessage);
				streamOut.flush();
				String res;
				String input = streamIn.readUTF();
        String[] lines = input.split("[\\r\\n]");
      	if(lines.length > 1) {
	    		String noMac = lines[0];
	        res = lines[1];
	    		if(lines.length > 2) {
	  			int num = noMac.length() + 1;
	  			res = input.substring(num);
    		}
        res = keyGen.getDecodedMessage(res, noMac);
      	} else {
      		res = input;
      	}	
				System.out.println(res);
				for(int i = 0; i < 1000; i++) {
					if(running) {
						Thread.sleep(10);
					} else {
						break;
					}
				}
			}
		} catch (Exception e) {
			System.out.println("Error: heartbeat message not sent");
			e.printStackTrace();
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
