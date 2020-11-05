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
            String keyPath = streamIn.readUTF();
            serverKey = readPublicKeyFromFile(keyPath);
            
            
            System.out.println("Type (1) to Create New User, (2) to Log In");
            String option = console.nextLine();
            switch(option) {
              case "1":
                newUser();
                break;
              case "2":
                logIn();
                break;
              default:
                System.out.println("Incorrect input!");
                break;
            }
            /* Recieve acknowledgement from server */
            String res = streamIn.readUTF();
            //TODO: decrypt message
            System.out.println(res);
            
            String transport = streamIn.readUTF();
            keyGen.decryptKeyTransport(transport);
            
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
	                    String msg = fromUser;
	                    //create encoded message and encoded MAC message to send
	                    streamOut.writeUTF(keyGen.createEncoded(msg));
	                    msg = keyGen.createEncodedMessage(msg);
	                    streamOut.writeUTF(msg);
	                    streamOut.flush();
	                    
	                    if (fromUser.equals("Logoff")) {
	                    	handler.end();
	                    	fromServer = streamIn.readUTF();
	                    	//TODO: decrypt message
                        
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
        keyGen = new KeyGen(username, serverKey);
      } else {
        System.out.println("Passwords do not match!");
        newUser();
        return;
      }
      
      //TODO: package message (keep in mind password is not hashed currently) 
      streamOut.writeUTF("1," + username + "," + password);
      streamOut.flush();
    }

    private void logIn() throws Exception {
      System.out.print("Enter username: ");
      username = console.nextLine();
      System.out.print("Enter password: ");
      password = console.nextLine();
      
      // creates new public and private keys, creates keyGen to encrypt and decrypt
      keyGen = new KeyGen(username, serverKey);
      
      //TODO: package message (without hashing password
      streamOut.writeUTF("2," + username + "," + password);
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
