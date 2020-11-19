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
        
        while (true)  { 
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
                
                if(msg.equals("Logoff")) {  
                    System.out.println(username + " is logging off...");
                    server.removeUser(username);
                    
                    msg = username + " successfully logged off";
        			String noMac = encHelper.createEncoded(msg);
        			msg = encHelper.createEncodedMessage(msg);
                    msg = noMac + '\n' + msg;
        			dos.writeUTF(msg);
                    dos.flush();
                    s.close(); 
                    break; 
                } else if (msg.equals("online")) {
                	sendList();
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

    private String requestFriend(String username) {
        UserModel potentialFriend = server.getUserMap().get(username);
        // Check that username exists
        if (potentialFriend == null) {
            return "Error";
        }
        // If it exists, then add to their list of pending friend requests 
        else { 
            potentialFriend.addFriendRequest(this.username);
        }
        return "Success";
    }
     
    private void sendList() throws Exception {
    	List<UserHandler> users = server.getUsersOnline();
    	String res;
    	if(users.size() == 1 && users.get(0).getUsername() == username) {
    		res = "Nobody is currently online";
    	} else {
    		res = "People online: \n";
    		for(UserHandler user : users) {
        		if(user.getUsername() != this.username) {
        			res += user.getUsername() + "\n";
        		}
        	}
    	}
    	
		String noMac = encHelper.createEncoded(res);
		res = encHelper.createEncodedMessage(res);
        res = noMac + '\n' + res;
    	dos.writeUTF(res);
    	dos.flush();
    }
    
    public String getUsername() {
    	return username;
    }
}
