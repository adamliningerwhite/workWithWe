import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 

public class UserHandler extends Thread {
    
    private final DataInputStream dis; 
    private final DataOutputStream dos; 
    private final Socket s; 

    private String username;
    private List<String> onlineUsers;

     /**
      * Constructor 
      * @param s
      * @param dis
      * @param dos
      */
     public UserHandler(Socket s, DataInputStream dis, DataOutputStream dos, String username, List<String> onlineUsers) { 
         this.s = s; 
         this.dis = dis; 
         this.dos = dos; 
         this.username = username;
         this.onlineUsers = onlineUsers;

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
                    dos.writeUTF(msg);
                    dos.flush();

                    s.close(); 
                    onlineUsers.remove(username); // take user off the list of people online
                    break; 
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

    private String packageMessage(String message) throws Exception {
        StringBuilder acc = new StringBuilder();
        acc.append(message);
        return acc.toString();
    }
   
}
