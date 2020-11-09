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
	private Set<String> usernames = new HashSet<String>();
	private Map<String, String> userPassCombo = new HashMap<String, String>();

	// Map of program users
	// key is username string, value is their UserModel object
	private HashMap<String, UserModel> userMap;

	private DataInputStream dis;
	private DataOutputStream dos;
	private String option;
	private String username;
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

		WriterThread userDataWriter = new WriterThread(this);

		/* infinite loop to accept user connections */
		while (true) {
			try {
				s = mainServer.accept();

				dis = new DataInputStream(s.getInputStream());
				dos = new DataOutputStream(s.getOutputStream());

				// TODO: send certificate? or send a file with a path to the certificate?

				if (first) {
					dos.writeUTF("../Server/ServerKeys/serverpublic.key");
					dos.flush();
					first = false;
				}

				String input = dis.readUTF();
				input = getInitialDecrypt(input);

				String[] parsed = input.split(",");
				option = parsed[0];
				username = parsed[1];
				password = parsed[2];
				//System.out.println("Option: " + option);
				switch (option) {
				case "1":
					createNewUser();
					 dos.writeUTF(exchangeSessionKey());
					 dos.flush();
					first = true;
					break;
				case "2":
					boolean res = logIn();
					//System.out.println(res);
					if (res) {
						 dos.writeUTF(exchangeSessionKey());
						 dos.flush();
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

		if (currentUser == null) {
			dos.writeUTF("the username " + username + " does not exist");
			return false;
		} else if (!currentUser.checkPassword(password)) {
			dos.writeUTF("incorrect password");
			return false;
		} else {
			System.out.println(username + " connected");
			UserHandler t = new UserHandler(s, dis, dos, username, this, currentUser);
			onlineUsers.add(t);
//			if (currentUser.hasEncHelper()) {
//				System.out.println("user has enc helper");
//				encryptHelper = currentUser.getEncHelper();
//			} else {
				//System.out.println("user doesnt have enc helper");
				encryptHelper = new EncryptHelper(username);
				//System.out.println(currentUser);
				//System.out.println(encryptHelper);
				currentUser.setEncHelper(encryptHelper);
//			}
			dos.writeUTF("successfully logged in");
		}
		dos.flush();
		return true;
	}

	private void createNewUser() throws Exception {
		if (!usernames.contains(username)) {
			UserModel user = new UserModel(username, password);
			encryptHelper = new EncryptHelper(username);
			user.setEncHelper(encryptHelper);
			userMap.put(username, user);
			System.out.println("New user connected");
			UserHandler t = new UserHandler(s, dis, dos, username, this, user);
			onlineUsers.add(t);
			// TODO: add public key to UserModel
			// TODO: encrypt message
			dos.writeUTF(username + " successfully logged in");
		} else {
			dos.writeUTF("the username " + username + " is taken");
		}
		dos.flush();
	}

	private void retrievePassword() {

	}

	private String exchangeSessionKey() {
		String res = encryptHelper.getKeyTransportMsg();
		//System.out.println("Transport: " + res);
		return res;
	}

	public String getInitialDecrypt(String msg) {
		try {
			serverKey = readPrivateKeyFromFile("ServerKeys/serverprivate.key");
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, serverKey);
			msg = new String(cipher.doFinal(decoder.decode(msg)), "UTF-8");
		} catch (Exception e) {
			System.out.println("Error: unable to decrypt username/password string");
			e.printStackTrace();
		}
		return msg;
	}

	private static RSAPrivateKey readPrivateKeyFromFile(String filePath) throws Exception {
		RSAPrivateKey key = null;

		byte[] encodedPrivateKey = Files.readAllBytes(Paths.get(filePath));
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
		key = (RSAPrivateKey) kf.generatePrivate(keySpec);

		return key;
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
