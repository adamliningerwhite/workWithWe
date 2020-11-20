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

	private HashSet<String> friends = new HashSet<String>();
	private HashSet<String> requests = new HashSet<String>();
	private HashSet<String> accepts = new HashSet<String>();
	private HashSet<String> rejects = new HashSet<String>();

	boolean different = true;


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
				parser(res);
				if(different) {
					results();
				}
				different = false;
				Thread.sleep(10);
			}
		} catch (Exception e) {
			System.out.println("Error: heartbeat message not sent");
			e.printStackTrace();
		}
	}

	private void parser(String input) {
		String[] lines = input.split("\n");
		for(String line : lines){
			String[] values = line.split(",");
			if(values.length > 0) {
				switch(values[0]) {
					case "1":
						friends = compareLists(values, friends);
						break;
					case "2":
						requests = compareLists(values, requests);
						break;
					case "3":
						break;
					case "4":
						break;
					default:
						break;
				}
			}
		}
	}

	public HashSet<String> getPendingRequets() {
		return requests;
	}

	private HashSet<String> compareLists(String[] values, HashSet<String> list) {
		HashSet<String> freshList = new HashSet<String>();
		for(int i = 1; i < values.length; i++)
			freshList.add(values[i]);
		if(freshList.size() != list.size()) {
			different = true;
			return freshList;
		} else {
			for(String friend : freshList) {
				if(!list.contains(friend)) {
					different = true;
					return freshList;
				}
			}
		}
		return list;
	}

	private void results() {
		String friendList = "Friend list: ";
		for(String friend : friends)
			friendList += friend + ", ";
		if(friends.size() == 0)
			friendList = "You have no friends ya loser";
		else
			friendList = friendList.substring(0, friendList.length() - 2);
		System.out.println(friendList);
		String pendingList = "Pending friend requests: ";
		for(String friend : requests)
			pendingList += friend + ", ";
		if(requests.size() == 0)
			pendingList = "No pending friend requests";
		else
			pendingList = pendingList.substring(0, pendingList.length() - 2);
		System.out.println(pendingList);
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
