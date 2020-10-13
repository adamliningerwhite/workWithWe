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

     /**
      * Constructor 
      * @param s
      * @param dis
      * @param dos
      */
     public UserHandler(Socket s, DataInputStream dis, DataOutputStream dos, String username, Server server) { 
         this.s = s; 
         this.dis = dis; 
         this.dos = dos; 
         this.username = username;
         this.server = server;

         this.start();
     } 

     @Override 
     public void run() {

        System.out.println("Thread started for: " + username);

        String msg;
        while (true)  { 
            try { 
                // receive message from user
                msg = dis.readUTF(); 

                if(msg.equals("logoff")) {  
                    System.out.println(username + " is logging off...");

                    msg = packageMessage(username + " successfully logged off");
                    server.removeUser(username);
                    dos.writeUTF(msg);
                    dos.flush();

                    s.close();  // take user off the list of people online
                    break; 
                } else if (msg.equals("online")) {
                	sendList();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /* Close resources */
        try { 
            this.dis.close(); 
            this.dos.close(); 
        } catch(IOException e){ 
            e.printStackTrace(); 
        } 
    }
     
    private void sendList() throws Exception {
    	List<UserHandler> users = server.getUsersOnline();
    	String res = "People online: \n";
    	if(users.size() == 1 && users.get(0).getUsername() == username) {
    		res = "Nobody is currently online";
    	} else {
    		for(UserHandler user : users) {
        		if(user.getUsername() != this.username) {
        			res += user.getUsername() + "\n";
        		}
        	}
    	}
    	res = packageMessage(res);
    	dos.writeUTF(res);
    	dos.flush();
    }
    
    public String getUsername() {
    	return username;
    }

    private String packageMessage(String message) throws Exception {
        StringBuilder acc = new StringBuilder();
        acc.append(message);
        return acc.toString();
    }
   
}
