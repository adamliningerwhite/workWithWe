import java.io.*;
import java.net.*;
import java.util.*;

public class User {

    private static final int USER_SERVER_PORT = 4232;
    private static final int MESSAGE_SERVER_PORT = 4771;
    private static final String SERVER_ADDRESS = "localhost";
    private String username;
    private String password;
    private Scanner console;
    private Socket s;
    private DataOutputStream streamOut;
    private DataInputStream streamIn;

    public User() throws Exception {
        createUser();
    }

    private void createUser() throws Exception {
        console = new Scanner(System.in);

        try{
            s = new Socket(SERVER_ADDRESS, USER_SERVER_PORT);
            streamOut = new DataOutputStream(s.getOutputStream());
            streamIn = new DataInputStream(s.getInputStream());
            System.out.println("Connected to Server");
            System.out.println("Type (1) to Create New User, (2) to Log In, or (3) to Retrieve Password");
            String option = console.nextLine();
            switch(option) {
              case "1":
                newUser();
                break;
              case "2":
                logIn();
                break;
              case "3":
                retrievePassword();
                break;
              default:
                System.out.println("Incorrect input!");
                break;

            }
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

    private void newUser() throws Exception {
      System.out.print("Enter username: ");
      username = console.nextLine();
      System.out.print("Enter password: ");
      String potentialPassword = console.nextLine();
      System.out.print("Re-enter password: ");
      String repeatedPassword = console.nextLine();
      if(potentialPassword.equals(repeatedPassword)){
        password = potentialPassword;
        System.out.println("User successfully created!");
      } else {
        System.out.println("Passwords do not match!");
        newUser();
        return;
      }
      streamOut.writeUTF("1," + username + "," + password);
      streamOut.flush();
    }

    private void logIn() throws Exception {
      System.out.print("Enter username: ");
      username = console.nextLine();
      System.out.print("Enter password: ");
      password = console.nextLine();
      streamOut.writeUTF("2," + username + "," + password);
      streamOut.flush();
    }

    private void retrievePassword() {

    }

    /**
     * args[0] ; username
     */
    public static void main(String[] args) {

        //check for correct # of parameters
        if (args.length != 0) {
            System.out.println("Incorrect number of parameters");
        } else {
            //Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

            //create Alice to start communication
            try {
                User user = new User();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
