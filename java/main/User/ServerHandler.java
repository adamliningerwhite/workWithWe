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
	private HashSet<String> online = new HashSet<String>();
	private String status = "";

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
				Thread.sleep(500);
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
						accepts = compareLists(values, accepts);
						break;
					case "4":
						rejects = compareLists(values, rejects);
						break;
					case "5":
						checkStatus(values);
						break;
					case "6":
						online = compareLists(values, online);
						break;
					default:
						break;
				}
			}
		}
	}

	private void checkStatus(String[] values) {
		String incoming = values[1];
		if(!incoming.equals(status)){
			status = incoming;
			different = true;
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
			friendList = "You have no friends, get some friends";
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
		String acceptList = "Accepted friend requests: ";
		for (String accept : accepts) {
			acceptList += accept + ", ";
		}
		if(accepts.size() == 0)
			acceptList = "No accepted friend requests";
		else
			acceptList = acceptList.substring(0, acceptList.length() - 2);
		System.out.println(acceptList);
		String rejectList = "Rejected friend requests: ";
		for (String reject : rejects) {
			rejectList += reject + ", ";
		}
		if(rejects.size() == 0)
			rejectList = "No rejected friend requests";
		else
			rejectList = rejectList.substring(0, rejectList.length() - 2);
		System.out.println(rejectList);
		System.out.println("Status" + status);
		String onlineFriends = "Online friends: ";
		for(String on : online)
			onlineFriends += on + ", ";
		if(online.size() == 0)
			onlineFriends = "You have no friends online";
		else
			onlineFriends = onlineFriends.substring(0, onlineFriends.length() - 2);
		System.out.println(onlineFriends);
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
