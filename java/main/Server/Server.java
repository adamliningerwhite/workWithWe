import java.io.*;
import java.text.*;
import java.util.*;

import javax.crypto.Cipher;

import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;

public class Server {

	public static Base64.Encoder encoder = Base64.getEncoder();
	public static Base64.Decoder decoder = Base64.getDecoder();

	private static final int USER_LISTEN_PORT = 4232;
	private static final int MESSAGE_LISTEN_PORT = 4771;

	private List<UserHandler> onlineUsers = new ArrayList<UserHandler>();
	private List<String> takenUsernames = new ArrayList<String>();
	private Set<String> usernames = new HashSet<String>();

	// Map of program users
	// key is username string, value is their UserModel object
	private HashMap<String, UserModel> userMap;

	private DataInputStream dis;
	private DataOutputStream dos;
	private String option;
  private String username;
  private String securityQuestion;
	private String password;
	private Socket s = null;

	private EncryptHelper encryptHelper;
	private RSAPrivateKey serverKey;
	private boolean first = true;

	public Server() throws Exception {

		/* Start workWithMe server */
		System.out.println("Started WorkWithMe Server");
		ServerSocket mainServer = new ServerSocket(USER_LISTEN_PORT);

		ReadHelper userDataReader = new ReadHelper();
		
		userMap = userDataReader.readData();
		getTakenUsernames();

		WriterThread userDataWriter = new WriterThread(this);

		/* infinite loop to accept user connections */
		while (true) {
			try {
				s = mainServer.accept();

				dis = new DataInputStream(s.getInputStream());
				dos = new DataOutputStream(s.getOutputStream());

				//TODO: "certificate" workaround sending public key
				if (first) {
					dos.writeUTF("../Server/ServerKeys/serverpublic.key");
					dos.flush();
					first = false;
				}

				String transport = dis.readUTF();
				//System.out.println(transport);
				encryptHelper = new EncryptHelper();
				encryptHelper.decryptKeyTransport(transport);


				option = encryptHelper.getLoginType();
				username = encryptHelper.getUsername();
        password = encryptHelper.getPassword();
        securityQuestion = encryptHelper.getSecurityQuestion();

				switch (option) {
				case "1":
					boolean res1 = createNewUser();
					if (res1) {
						first = true;
					}
					break;
				case "2":
					boolean res = logIn();
					if (res) {
						first = true;
					}
          break;
        case "3":
          boolean res2 = forgotPassword();
          if (res2) {
            first = true;
          }
          break;
				default:
					System.out.println("Incorrect input!");

					break;
				}
			} catch (Exception e) {
				s.close();
				e.printStackTrace();
				break; // TODO: don't think we want to break here, but otherwise next block is
						// unreachable
			}
		}

		try {
			mainServer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getTakenUsernames() {
		for(Map.Entry<String, UserModel> users : userMap.entrySet()) {
			takenUsernames.add(users.getKey());
		}
	}
	
	public HashMap<String, UserModel> getUserMap() {
		return userMap;
	}

	public List<UserHandler> getUsersOnline() {
		return onlineUsers;
	}

	public void setUsersOnline(List<UserHandler> newOnlineList) {
		onlineUsers = newOnlineList;
	}

	public void removeUser(String username) {
		for (int i = 0; i < onlineUsers.size(); i++) {
			if (onlineUsers.get(i).getUsername() == username) {
				onlineUsers.remove(i);
				break;
			}
		}
		usernames.remove(username);
	}

	private boolean logIn() throws Exception {
		UserModel currentUser = userMap.get(username);
		if(currentUser != null) {
			byte[] salt = currentUser.getSalt();
			password = encryptHelper.hashPassword(password, salt);
		}
		
		if (currentUser == null) {
			String msg = "the username " + username + " does not exist";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
			return false;
		} else if (usernames.contains(username)) {
			String msg = "username is currently logged in";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
			return false;
		} else if (!currentUser.checkPassword(password)) {
			String msg = "incorrect password";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
			return false;
		} else {
			System.out.println(username + " connected");
			currentUser.setEncHelper(encryptHelper);
			UserHandler t = new UserHandler(s, dis, dos, username, this, currentUser);
			onlineUsers.add(t);
			usernames.add(username);

			String msg = "successfully logged in";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
		}
		dos.flush();
		return true;
  }
  
  private boolean forgotPassword() throws Exception {
    UserModel currentUser = userMap.get(username);
    if(currentUser != null) {
			byte[] salt = currentUser.getSalt();
			password = encryptHelper.hashPassword(password, salt);
		}
		if (currentUser == null) {
			String msg = "the username " + username + " does not exist";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
			return false;
		} else if (usernames.contains(username)) {
			String msg = "username is currently logged in";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
			return false;
		} else if (!currentUser.checkSecurityQuestion(securityQuestion)) {
      String msg = "Incorrect answer.";
      String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
			return false;
    } else {
      currentUser.setPassword(password);
			System.out.println(username + " connected");
			currentUser.setEncHelper(encryptHelper);
			UserHandler t = new UserHandler(s, dis, dos, username, this, currentUser);
			onlineUsers.add(t);
			usernames.add(username);

			String msg = "successfully logged in";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
		}
		dos.flush();
		return true;
  }

	private boolean createNewUser() throws Exception {
		if (!takenUsernames.contains(username)) {
			byte[] salt = encryptHelper.getSalt();
			password = encryptHelper.hashPassword(password, salt);
			UserModel user = new UserModel(username, securityQuestion, password);
			user.setSalt(salt);
			user.setEncHelper(encryptHelper);
			userMap.put(username, user);
			System.out.println("New user connected");
			UserHandler t = new UserHandler(s, dis, dos, username, this, user);
			onlineUsers.add(t);
			usernames.add(username);
			takenUsernames.add(username);

			String msg = "User " + username + " successfully created!";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
			
			msg = username + " successfully logged in";
			noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
			
			return true;
		} else {
			String msg = "the username " + username + " is taken";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
		}
		dos.flush();
		
		return false;
	}

	public static void main(String[] args) {
		// check for correct # of parameters
		if (args.length > 0) {
			System.out.println("Incorrect number of parameters");
			return;
		}
		try {
			Server server = new Server();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
