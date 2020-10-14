import java.io.*;
import java.net.*;
import java.util.*;

public class User {
    
    private static final int USER_SERVER_PORT = 4232;
    private static final int MESSAGE_SERVER_PORT = 4771;
    private static final String SERVER_ADDRESS = "localhost";
    private String username;

    public User(String username) throws Exception {

        this.username = username;
        createUser();
    }
    
    private void createUser() throws Exception {
        Scanner console = new Scanner(System.in);
            
        try{
            Socket s = new Socket(SERVER_ADDRESS, USER_SERVER_PORT);
            System.out.println("Connected to Server");
            
            DataOutputStream streamOut = new DataOutputStream(s.getOutputStream());
            DataInputStream streamIn = new DataInputStream(s.getInputStream());
            
            
                
            /* 1st message to server: my username */
            streamOut.writeUTF(packageMessage(username));
            streamOut.flush();

            /* Recieve acknowledgement from server */
            String res = streamIn.readUTF();
            System.out.println(res);
            
            if(res.contains("successfully logged in")) {
	            /* Loop to forward messages to server. Terminates when user types "logoff" */
	            String fromUser = "";
	            String toServer = "";
	            String fromServer = "";
	            ServerHandler handler = new ServerHandler(streamOut, streamIn);
	            Thread.sleep(5);
	            System.out.println("Type 'Logoff' to sign out");
	            while(!fromUser.equals("Logoff")) {
	                try {
	                    fromUser = console.nextLine();
	                    
	                    /* Send message to server */ 
	                    toServer = packageMessage(fromUser);
	                    streamOut.writeUTF(toServer);
	                    streamOut.flush();
	                    
	
	                    if (fromUser.equals("Logoff")) {
	                    	handler.end();
	                        fromServer = streamIn.readUTF();
	                        System.out.println(fromServer);
	                    }
	                } catch(IOException ioe) {  
	                    System.out.println("Sending error: " + ioe.getMessage());
	                }
	            }
	            
	            //close all the sockets and console 
	            console.close();
	            streamOut.close();
	            streamIn.close();
	            s.close();
            }
            
        }
        catch(IOException e) {
            //print error
            System.out.println("Connection failed due to following reason");
            System.out.println(e);
        }
    }
	
    
    private String packageMessage(String message) throws Exception {
        StringBuilder acc = new StringBuilder();
        acc.append(message);
        return acc.toString();
    }
    
    /**
     * args[0] ; username
     */
    public static void main(String[] args) {
        
        //check for correct # of parameters
        if (args.length != 1) {
            System.out.println("Incorrect number of parameters");
        } else {
            //Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            
            //create Alice to start communication
            try {
                User user = new User(args[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
    }
}
