import java.io.*;
import java.net.*;
import java.security.*;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

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
    private KeyGen keyGen;
    private RSAPublicKey serverKey;
    private RSAPrivateKey userKey;
    ServerHandler handler;
    String res;
    String keyTransportMsg;
    boolean first = true;
    boolean loop = true;

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

            //TODO: authenticate server here and save RSAPublicKey to file "serverKey"
            //currently just reads a key from file and saves it as the public key for the server
            if(first){
              String keyPath = streamIn.readUTF();
              serverKey = readPublicKeyFromFile(keyPath);
              first = false;
            }

            System.out.println("Type (1) to Create New User, (2) to Log In");
            String option = console.nextLine();
            switch(option) {
              case "1":
                newUser();
                res = "correct";
                break;
              case "2":
                logIn();
                res = "correct";
                break;
              default:
                System.out.println("Incorrect input!");
                res = "incorrect";
                break;
            }
            /* Recieve acknowledgement from server */
            if(res != "incorrect") {
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
            }
            System.out.println(res);

            if(res.contains("successfully logged in")) {
	            /* Loop to forward messages to server. Terminates when user types "logoff" */
	            String fromUser = "";
	            String toServer = "";
	            String fromServer = "";
	            handler = new ServerHandler(streamOut, streamIn, keyGen);
	            Thread.sleep(5);
	            while(loop) {
                System.out.println("Type (1) to Logoff, (2) to add new friend, (3) to respond to friend request, (4) to flip working status");
                fromUser = console.nextLine();
                switch (fromUser) {
                  case "1":
                    logoff();
                    loop = false;
                    break;
                  case "2":
                    addUser();
                    break;
                  case "3":
                    friendRequest();
                    break;
                  case "4":
                    flipStatus();
                    break;
                  default:
                    System.out.println("Invalid input");
                    break;
                }
	            }
            } else {
              createUser();
            }
        }
        catch(IOException e) {
            //print error
            System.out.println("Connection failed due to following reason");
            System.out.println(e);
        }
    }

    private void friendRequest() throws Exception {
      HashSet<String> requests = handler.getPendingRequets();
      if(requests.size() == 0) {
        System.out.println("No pending friend requests");
      } else {
        Object[] requestArray = requests.toArray();
        for(int i = 0; i < requestArray.length; i++)
          System.out.println("(" + i + "): " + requestArray[i].toString());
        System.out.print("Enter the number of you user you'd like to respond to: ");
        String res = console.nextLine();
        try {
          int option = Integer.parseInt(res);
          if(option < 0 | option >= requestArray.length){
            System.out.println("Input out of range");
          } else {
            String username = requestArray[option].toString();
            System.out.print("Enter (0) to reject the request, and (1) to accept the request: ");
            String response = console.nextLine();
            System.out.println("Response: " + response);
            if(response.equals("0") | response.equals("1")) {
              String message = "3," + username + "," + response;
              sendMessage(message);
            } else {
              System.out.println("Incorrect input");
            }
          }
        } catch (Exception e) {
          System.out.println("Incorrect input");
        }
      }
    }

    private void logoff() throws Exception {
      String msg = "1,";
      sendMessage(msg);
      receiverServerMessage();
      handler.end();
      console.close();
      streamOut.close();
      streamIn.close();
      s.close();
    }

    private void flipStatus() throws Exception {
      String msg = "4,";
      sendMessage(msg);
    }

    private void addUser() throws Exception {
      String msg;
      System.out.print("Enter friend request username: ");
      String username = console.nextLine();
      msg = "2," + username;
      sendMessage(msg);
      receiverServerMessage();
    }

    private void sendMessage(String msg) throws Exception {
      String noMac = keyGen.createEncoded(msg);
      msg = keyGen.createEncodedMessage(msg);
      msg = noMac + '\n' + msg;
      streamOut.writeUTF(msg);
      streamOut.flush();
    }

    private void receiverServerMessage() throws Exception {
      String msg;
      String noMac;
      String fromServer = streamIn.readUTF();
      String[] lines = fromServer.split("[\\r\\n]");
      if(lines.length > 1) {
        noMac = lines[0];
        msg = lines[1];
        if(lines.length > 2) {
          int num = noMac.length() + 1;
          msg = fromServer.substring(num);
        }
        msg = keyGen.getDecodedMessage(msg, noMac);
      } else {
        msg = fromServer;
      }
      System.out.println(msg);
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
        // creates new public and private keys, creates keyGen to encrypt and decrypt
        keyGen = new KeyGen(username, potentialPassword, "1", serverKey);
      } else {
        System.out.println("Passwords do not match!");
        newUser();
        return;
      }
      // key transport message includes login message as well
      keyTransportMsg = keyGen.getKeyTransportMsg();
      //System.out.println(keyTransportMsg);
      streamOut.writeUTF(keyTransportMsg);
      streamOut.flush();
    }

    private void logIn() throws Exception {
      System.out.print("Enter username: ");
      username = console.nextLine();
      System.out.print("Enter password: ");
      password = console.nextLine();
      // creates new public and private keys, creates keyGen to encrypt and decrypt
      keyGen = new KeyGen(username, password, "2", serverKey);
      // key transport message includes login message as well
      keyTransportMsg = keyGen.getKeyTransportMsg();
      //System.out.println(keyTransportMsg);
      streamOut.writeUTF(keyTransportMsg);
      streamOut.flush();
    }

    private void retrievePassword() {

    }

    private RSAPublicKey readPublicKeyFromFile(String filePath) {
      RSAPublicKey key = null;
      File publicKey = new File(filePath);
      try (FileInputStream is = new FileInputStream(publicKey)) {
        byte[] encodedPublicKey = new byte[(int) publicKey.length()];
        is.read(encodedPublicKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedPublicKey);
        key = (RSAPublicKey) kf.generatePublic(keySpec);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return key;
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
