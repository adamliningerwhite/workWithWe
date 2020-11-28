  import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;

public class UserHandler extends Thread {

    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final Socket s;
    private String username;
    private Server server;
    private UserModel userModel;
    private EncryptHelper encHelper;
    boolean loop = true;

     /**
      * Constructor
      * @param s
      * @param dis
      * @param dos
      */
     public UserHandler(Socket s, DataInputStream dis, DataOutputStream dos, String username, Server server, UserModel userModel) {
         this.s = s;
         this.dis = dis;
         this.dos = dos;
         this.username = username;
         this.server = server;
         this.userModel = userModel;
         this.encHelper = userModel.getEncHelper();
         this.start();
     }

     @Override
     public void run() {
        System.out.println("Thread started for: " + username);
        while (loop)  {
            try {
            	String msg;
            	String input = dis.readUTF();
            	String[] lines = input.split("[\\r\\n]");
            	if(lines.length > 1) {
            		String noMac = lines[0];
                msg = lines[1];
            		if(lines.length > 2) {
            			int num = noMac.length() + 1;
            			msg = input.substring(num);
            		}
                msg = encHelper.getDecodedMessage(msg, noMac);
                System.out.println(msg);
            	} else {
            		msg = input;
            	}
              if(msg.equals("online")){
                heartbeatResponse();
              } else {
                String[] values = msg.split(",");
                String option = values[0];
                switch (option) {
                  case "1":
                    logoff();
                    loop = false;
                    break;
                  case "2":
                    if (values.length == 2)
                      requestFriend(values[1]);
                    break;
                  case "3":
                    if(values.length == 3){
                      requestResponse(values[1], values[2].equals("1"));
                    }
                    break;
                  case "4":
                    if (values.length == 2)
                      removeFriend(values[1]); 
                    break;
                  case "5":
                    flipStatus();
                    break;
                  case "6":
                    if (values.length == 2)
                      blockUser(values[1]);
                    break;
                  default:
                    incorrectInput();
                    break;
                }
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
        }
        try {
            this.dis.close();
            this.dos.close();
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private void flipStatus() {

      HashMap<String,UserModel> userMap = server.getUserMap();

      if (userModel.isWorking()) {
        // Switch to not working 
        userModel.flipStatus();
      
        // Remove myself from all friends onlineFriends lists
        for (String friendUsername : userModel.getFriends()) {
          UserModel friendModel = userMap.get(friendUsername);
          friendModel.removeFriendOnline(this.username);
        }
        
      } else {
        // Switch to working 
        userModel.flipStatus();
      
        // Add myself to all friends onlineFriends lists 
        for (String friendUsername : userModel.getFriends()) {
          UserModel friendModel = userMap.get(friendUsername);
          friendModel.addFriendOnline(this.username);
        }
      }
    }

    private void removeFriend(String friendString) throws Exception {
      String msg;
      // Check that this is actually one of the user's friends
      if (!userModel.getFriends().contains(friendString)) {
        msg = "Error: no friend with the name " + friendString;
      } else {
        // Get friend's user model from server 
        UserModel friend = server.getUserMap().get(friendString);
        // Remove friend from user's list 
        userModel.removeFriend(friendString);
        friend.removeFriend(this.username);

        userModel.removeFriendOnline(friendString);
        friend.removeFriendOnline(this.username);

        msg = "Success! " + friendString + " is no longer your friend";
      }
    
      // Send response message 
      String noMac = encHelper.createEncoded(msg);
      msg = encHelper.createEncodedMessage(msg);
      msg = noMac + '\n' + msg;
      dos.writeUTF(msg);
      dos.flush();
    }

    private void blockUser(String blockedUserString) throws Exception {
      String msg = "blank";
      UserModel blockedUser = server.getUserMap().get(blockedUserString);

      // Check that user exists 
      if (blockedUser == null ) {
        msg = "Error: no users with the name " + blockedUserString;
      } else if (userModel.getBlockedUsers().contains(blockedUserString)) {
        msg = "Error: user " + blockedUserString + " is already blocked";
      } else if (blockedUserString.equals(this.username)) {
        msg = "Error: cannot block yourself";
      }
      else {
        // If friends, remove from both people's friends list
        userModel.removeFriend(blockedUserString);
        blockedUser.removeFriend(this.username);

        // Neither party should see the other's status 
        userModel.removeFriendOnline(blockedUserString);
        blockedUser.removeFriendOnline(this.username);

        // If pending friend requests exist, then cancel them
        userModel.removeFriendRequest(blockedUserString);
        blockedUser.removeFriendRequest(this.username);

        // Add person to my list of blocked users
        userModel.addBlockedUser(blockedUserString);

        msg = "Success! You have blocked " + blockedUserString + ". You can unblock by sending them a friend request.";
      }

      String noMac = encHelper.createEncoded(msg);
      msg = encHelper.createEncodedMessage(msg);
      msg = noMac + '\n' + msg;
      dos.writeUTF(msg);
      dos.flush();
    }

    private void requestFriend(String friend) throws Exception {

      System.out.println(this.username + " wants to friend request " + friend);

      String msg;
      UserModel potentialFriend = server.getUserMap().get(friend);
      // Check that username exists
      if (potentialFriend == null) {
          msg = "Error: " + friend + " is not a valid username.";
      }
      else if (userModel.getFriends().contains(friend))  {
        msg = "Error: " + friend + " is already your friend.";
      }
      else if (friend.equals(this.username)){
        msg = "Error: cannot add yourself as a friend."; 
      }
      else if (potentialFriend.getFriendRequests().contains(this.username)) {
        msg = "Error: the friend request you previously sent is still pending.";
      }
      else if (userModel.getFriendRequests().contains(friend)) {
        msg = "Error: you already have a pending friend request from this user. ";
      }
      else if (potentialFriend.getBlockedUsers().contains(this.username)) {
        msg = "Error: this user has blocked you from sending friend requests.";
      }
      // If it exists, then add to their list of pending friend requests
      else {
          potentialFriend.addFriendRequest(this.username);
          msg = "Success! Friend request sent to " + friend;
          if (userModel.getBlockedUsers().contains(friend)) {
            userModel.removeBlockedUser(friend); // if person was blocked, then this unblocks them
            System.out.println(this.username + " unblocked " + friend);
          }
      }

      String noMac = encHelper.createEncoded(msg);
      msg = encHelper.createEncodedMessage(msg);
      msg = noMac + '\n' + msg;
      dos.writeUTF(msg);
      dos.flush();
    }

    private void logoff() throws Exception {
      // Remove myself from all friends onlineFriends lists
      Map<String, UserModel> userMap = server.getUserMap();
      for (String friendUsername : userModel.getFriends()) {
        UserModel friendModel = userMap.get(friendUsername);
        friendModel.removeFriendOnline(this.username);
      }
      userModel.setWorkingStatus(false);
      String msg;
      System.out.println(username + " is logging off...");
      server.removeUser(username);
      msg = username + " successfully logged off";
      String noMac = encHelper.createEncoded(msg);
      msg = encHelper.createEncodedMessage(msg);
      msg = noMac + '\n' + msg;
      dos.writeUTF(msg);
      dos.flush();
      s.close();
    }

    private void requestResponse(String friend, boolean accept) {
      UserModel friendModel = server.getUserMap().get(friend);
      if (accept) {
        friendModel.addFriend(this.username);
        userModel.addFriend(friend);
        friendModel.addAccepted(this.username);
      } else {
        friendModel.addRejected(this.username);
      }
      userModel.removeFriendRequest(friend);
    }

    private void heartbeatResponse() throws Exception {
      // 1st line: This user's friends
      String firstLine = "1";
      for (String friend : userModel.getFriends()) {
        firstLine += "," + friend;
      }

      // 2nd line : This user's pending friend requests
      String secondLine = "2";
      for (String pendingFriend : userModel.getFriendRequests()) {
        secondLine += "," + pendingFriend;
      }

      // 3rd line: friend request acceptances
      String thirdLine = "3";
      for (String accepted : userModel.getAccepted()) {
        thirdLine += "," + accepted;
      }
      userModel.clearAccepted();

      // 4th line: friend request rejections
      String fourthLine = "4";
      for (String rejection : userModel.getRejected()) {
        fourthLine += "," + rejection;
      }
      userModel.clearRejected();

      // 5th line: user's working status 
      String fifthLine = "5";
      if (userModel.isWorking()) {
        fifthLine += ",grinding";
      } else {
        fifthLine += ",hangin' ten";
      }

      // 6th line: friends who are currently working 
      // step 1: update the list of friends who are working 
      HashMap<String,UserModel> userMap = server.getUserMap();
      for (String friend : userModel.getFriends()) {
        if (userMap.get(friend).isWorking()) {
          userModel.addFriendOnline(friend);
        }
      }
      String sixthLine = "6";
      for (String friendOnline : userModel.getFriendsOnline()) {
        sixthLine += "," + friendOnline;
      }

      // 7th line : users who have been blocked 
      String seventhLine = "7";
      for (String blockedUser : userModel.getBlockedUsers()) {
        seventhLine += "," + blockedUser;
      }

      String res = firstLine + "\n" + secondLine + "\n" + thirdLine + "\n" + fourthLine + "\n" + fifthLine + "\n" + sixthLine + "\n" + seventhLine;
      String noMac = encHelper.createEncoded(res);
      res = encHelper.createEncodedMessage(res);
      res = noMac + '\n' + res;
      dos.writeUTF(res);
      dos.flush();
    }

    public String getUsername() {
    	return username;
    }

    private void incorrectInput() {

    }
}
