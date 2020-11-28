import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class User {

	public static Base64.Decoder decoder = Base64.getDecoder();
    private static final int USER_SERVER_PORT = 4232;
    private static final int MESSAGE_SERVER_PORT = 4771;
    private static final String SERVER_ADDRESS = "localhost";
    private static final HashMap<String, String> trustedPublicKeys = new HashMap<String, String>();
    private static final String KEY_STRING = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrUTFKJLC+nrh5KoH9fj26IudBglTvf94rWyrkNgs42Cy6qx0j4jzdaFdqFn51AIQYRsD/074Rr3tLK52PBTgwD7TA9DWHprK0qW6RCEtCnQ++74J29JVmtWx+XhT4QwATPHQyv04n1ZgnDuVvg2aOFFLQrsfyegNmx2ia19E57wIDAQAB";
    private static final String HASHED_KEY_STRING = "9d50a5aecf355bb7957d94b12a9916980dcfa42a8f7b728cb2261bff896eb5aacab51e966bb10615ef443159acf275e975df89389f310814763c937060f9a23b";
    
    private String username;
    private String securityQuestion;
    private String password;
    private Scanner console;
    private Socket s;
    private DataOutputStream streamOut;
    private DataInputStream streamIn;
    private KeyGen keyGen;
    private RSAPublicKey serverKey;
    ServerHandler handler;
    String res;
    String keyTransportMsg;
    boolean first = true;
    boolean loop = true;

    public User() throws Exception {
    	trustedPublicKeys.put(KEY_STRING, HASHED_KEY_STRING);
        createUser();
    }

    private void createUser() throws Exception {
        console = new Scanner(System.in);

        try{
            s = new Socket(SERVER_ADDRESS, USER_SERVER_PORT);
            streamOut = new DataOutputStream(s.getOutputStream());
            streamIn = new DataInputStream(s.getInputStream());
            System.out.println("Connected to Server");
            
            if(first){
              String keyString = streamIn.readUTF();
              String hashedKeyString = hashFunction(keyString, "Server");
              
              if(!trustedPublicKeys.containsKey(keyString) || !trustedPublicKeys.get(keyString).equals(hashedKeyString)) {
            	  System.out.println("Error: public key not trusted, closing connection");
            	  streamOut.writeUTF("closing connection");
                  console.close();
                  streamOut.close();
                  streamIn.close();
                  s.close();
                  System.exit(0);
              }
              
              System.out.println("Server public key authenticated");
              streamOut.writeUTF("confirmed");
              byte[] pubKeyBytes = decoder.decode(keyString);
              
              KeyFactory kf = KeyFactory.getInstance("RSA");
              X509EncodedKeySpec keySpec = new X509EncodedKeySpec(pubKeyBytes);
              serverKey = (RSAPublicKey) kf.generatePublic(keySpec);
              first = false;
            }
            

            System.out.println("Type (1) to Create New User, (2) to Log In, (3) to Reset Password");
            String option = console.nextLine();
            switch(option) {
              case "1":
                newUser();
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
	          	  if(res.contains("successfully created!")) {
	          		res = "correct";
	          	  } else {
	          		  res = "incorrect";
	          	  }
                break;
              case "2":
                logIn();
                res = "correct";
                break;
              case "3":
                forgotPassword();
                res = "correct";
                break;
              default:
                wrongInput();
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
	          	System.out.println(res);
            }

            if(res.contains("successfully logged in")) {
	            /* Loop to forward messages to server. Terminates when user types "logoff" */
	            String fromUser = "";
	            String toServer = "";
	            String fromServer = "";
	            handler = new ServerHandler(streamOut, streamIn, keyGen);
	            Thread.sleep(5);
	            while(loop) {
                System.out.println("Type (1) to Logoff, (2) to add new friend, (3) to respond to friend request, (4) to remove a friend, (5) to flip your status, (6) to block another user");
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
                    removeFriend(); 
                    break;
                  case "5":
                    flipStatus();
                    break;
                  case "6":
                    blockUser();
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

    private void removeFriend() throws Exception {
      String msg = "4,";
      System.out.print("Enter username of friend to remove: ");
      String friend = console.nextLine().trim();
      msg += friend;
      sendMessage(msg);
      receiverServerMessage();
    }
    
    private void flipStatus() throws Exception {
      String msg = "5,";
      sendMessage(msg);
    }

    private void blockUser() throws Exception {
      String msg = "6,";
      System.out.print("Enter username to block: ");
      String blockedUser = console.nextLine().trim();
      msg += blockedUser;
      sendMessage(msg);
      receiverServerMessage();
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

    private boolean isLegalUsername(String username) {
      String illegalChars = "!@#$%^&*()_+-={}|[]:;<>?,./`~'\\";
      for(int i = 0; i < username.length(); i++) {
        if (illegalChars.indexOf(username.charAt(i)) >= 0) {
          return false;
        }
      }
      if (username.equals("login") || username.equals("pending_requests")) {
        return false;
      }
      return true;
    }

    private void newUser() throws Exception {
      System.out.print("Enter username: ");
      username = console.nextLine();
      if (!isLegalUsername(username)) {
        System.out.println("Illegal username. Special characters are not allowed.");
        newUser();
        return;
      }
      System.out.print("Security Question: What is your mother's maiden name? ");
      securityQuestion = console.nextLine();
      System.out.print("Enter password: ");
      String potentialPassword = console.nextLine();
      System.out.print("Re-enter password: ");
      String repeatedPassword = console.nextLine();
      if(potentialPassword.equals(repeatedPassword)){
        password = potentialPassword;

        Classify classifier = new Classify();
        String classify = classifier.evaluatePassword(password);
        System.out.println("Password strength: " + classify);
        System.out.println("Type (1) to use password, (2) to change password");
        String next = console.nextLine();
        if(next.equals("2")) {
        	newUser();
        	return;
        } else {
        	System.out.println("User being created...");
            // creates new public and private keys, creates keyGen to encrypt and decrypt
            keyGen = new KeyGen(username, securityQuestion, potentialPassword, "1", serverKey);
        }
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
      keyGen = new KeyGen(username, securityQuestion, password, "2", serverKey); // question 
      // key transport message includes login message as well
      keyTransportMsg = keyGen.getKeyTransportMsg();
      streamOut.writeUTF(keyTransportMsg);
      streamOut.flush();
    }

    private void wrongInput() throws Exception {
      keyGen = new KeyGen("admin", "admin", "admin", "3", serverKey);
      keyTransportMsg = keyGen.getKeyTransportMsg();
      streamOut.writeUTF(keyTransportMsg);
      streamOut.flush();
    }

    private void forgotPassword() throws Exception {
      System.out.print("Enter username: ");
      username = console.nextLine();
      System.out.print("Security Question: What is your mother's maiden name? ");
      securityQuestion = console.nextLine();
      System.out.print(securityQuestion);
      System.out.print("Enter a new password: ");
          String potentialPassword = console.nextLine();
          System.out.print("Re-enter password: ");
          String repeatedPassword = console.nextLine();
          if(potentialPassword.equals(repeatedPassword)){
            password = potentialPassword;
          keyGen = new KeyGen(username, securityQuestion, potentialPassword, "3", serverKey); 
          System.out.print(securityQuestion);
        } else {
          System.out.print("Passwords do not match!");
          newUser();
          return;
        }
      // key transport message includes login message as well
      keyTransportMsg = keyGen.getKeyTransportMsg();
      //System.out.println(keyTransportMsg);
      streamOut.writeUTF(keyTransportMsg);
      streamOut.flush();
    }
    
    private String hashFunction(String input, String concat) throws NoSuchAlgorithmException {
		 // getInstance() method is called with algorithm SHA-512
      MessageDigest md = MessageDigest.getInstance("SHA-512");

      // digest() method is called
      // to calculate message digest of the input string
      // returned as array of byte
      byte[] messageDigest = md.digest((input+concat).getBytes());

      // Convert byte array into signum representation
      BigInteger no = new BigInteger(1, messageDigest);

      // Convert message digest into hex value
      String hashtext = no.toString(16);

      // Add preceding 0s to make it 32 bit
      while (hashtext.length() < 32) {
          hashtext = "0" + hashtext;
      }
      return hashtext;
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
            try {
                User user = new User();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
