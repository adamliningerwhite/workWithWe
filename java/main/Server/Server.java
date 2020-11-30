import java.io.*;
import java.text.*;
import java.util.*;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

public class Server {

	public static Base64.Encoder encoder = Base64.getEncoder();
	public static Base64.Decoder decoder = Base64.getDecoder();

	private static final int USER_LISTEN_PORT = 4232;

	private static final String ADMIN_PASSWORD = "teamwork makes the dream work";
	private static final String SALT_PATH = "salt";

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
	WriterThread userDataWriter;
	private boolean first = true;

	public Server() throws Exception {

		/* Start workWithMe server */
		System.out.println("Started WorkWithMe Server");

		/* PBE scheme */
		// Step 1: require administrator password
		Scanner console = new Scanner(System.in);
		System.out.print("Administrator password: ");
		String adminPassword = console.nextLine().trim();
		// Step 2: ensure user entered correct password
		if (!adminPassword.equals(ADMIN_PASSWORD)) {
			System.out.println("Incorrect password...killing application!");
			System.exit(0);
		}
		System.out.println("Authentication successful");
		// Step 3: generate secret key from password
		byte[] salt = new byte[8];
		try { // attempt to read salt from file
			FileInputStream saltReader = new FileInputStream(SALT_PATH);
			saltReader.read(salt);
		} catch (Exception e) { // if none exists, make one from scratch and write for next time
			System.out.println("No existing salt, creating one from scratch.");
			SecureRandom random = new SecureRandom();
			random.nextBytes(salt);
			FileOutputStream saltWriter = new FileOutputStream(SALT_PATH);
			saltWriter.write(salt);
		}
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		KeySpec spec = new PBEKeySpec(adminPassword.toCharArray(), salt, 65536, 256);
		SecretKey tmp = factory.generateSecret(spec);
		SecretKey secret = new SecretKeySpec(tmp.getEncoded(), "AES");

		ServerSocket mainServer = new ServerSocket(USER_LISTEN_PORT);

		ReadHelper userDataReader = new ReadHelper(secret);

		userMap = userDataReader.readData();
		getTakenUsernames();

		userDataWriter = new WriterThread(this, secret);

		/* infinite loop to accept user connections */
		while (true) {
			try {
				s = mainServer.accept();
				dis = new DataInputStream(s.getInputStream());
				dos = new DataOutputStream(s.getOutputStream());
				encryptHelper = new EncryptHelper();

				// TODO: "certificate" workaround sending public key
				if (first) {
					String pubKeyString = encryptHelper.getPublicKeyString();
					dos.writeUTF(pubKeyString);
					dos.flush();
					first = false;

					String confirm = dis.readUTF();
					if (confirm.equals("closing connection")) {
						first = true;
						continue;
					}
				}
				String transport = dis.readUTF();
				// System.out.println(transport);
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
		for (Map.Entry<String, UserModel> users : userMap.entrySet()) {
			takenUsernames.add(users.getKey());
		}
	}

	public HashMap<String, UserModel> getUserMap() {
		return userMap;
	}
	
	public void setUserMap(HashMap<String, UserModel> newUserMap) {
		userMap = newUserMap;
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
	
	public void deleteUser(String username) {
		UserModel user = userMap.get(username);
		userMap.remove(username);
		takenUsernames.remove(username);
		userDataWriter.write();
	}

	private boolean logIn() throws Exception {
		UserModel currentUser = userMap.get(username);
		if (currentUser != null) {
			byte[] salt = currentUser.getSalt();
			password = encryptHelper.hashPassword(password, salt);
			
			if (currentUser.getIncorrectAttempts() < 5) {
				if (usernames.contains(username)) {
					String msg = "username is currently logged in";
					String noMac = encryptHelper.createEncoded(msg);
					msg = encryptHelper.createEncodedMessage(msg);
					msg = noMac + '\n' + msg;
					dos.writeUTF(msg);
					return false;
				} else if (!currentUser.checkPassword(password)) {
					String msg = "incorrect password";
					currentUser.incrementIncorrectAttempts();
					String noMac = encryptHelper.createEncoded(msg);
					msg = encryptHelper.createEncodedMessage(msg);
					msg = noMac + '\n' + msg;
					dos.writeUTF(msg);
					return false;
				} else {
					System.out.println(username + " connected");
					currentUser.resetIncorrectAttempts();
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
			} else {
				String msg = "You are locked out! You must reset your password.";
				String noMac = encryptHelper.createEncoded(msg);
				msg = noMac + '\n' + msg;
				dos.writeUTF(msg);
				return false;
			}
		} else {
			String msg = "the username " + username + " does not exist";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
			msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
			return false;
		}
		dos.flush();
		return true;
	}

	private boolean forgotPassword() throws Exception {
		UserModel currentUser = userMap.get(username);
		if (currentUser != null) {
			byte[] salt = currentUser.getSalt();
			password = encryptHelper.hashPassword(password, salt);
			securityQuestion = encryptHelper.hashPassword(securityQuestion, salt);
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
			currentUser.resetIncorrectAttempts();
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
			securityQuestion = encryptHelper.hashPassword(securityQuestion, salt);
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
