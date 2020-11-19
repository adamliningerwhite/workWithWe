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

				String transport = dis.readUTF();
				//System.out.println(transport);
				encryptHelper = new EncryptHelper();
				encryptHelper.decryptKeyTransport(transport);


				option = encryptHelper.getLoginType();
				username = encryptHelper.getUsername();
				password = encryptHelper.getPassword();

				switch (option) {
				case "1":
					createNewUser();
					first = true;
					break;
				case "2":
					boolean res = logIn();
					if (res) {
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
			String msg = "the username " + username + " does not exist";
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

			String msg = "successfully logged in";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
		}
		dos.flush();
		return true;
	}

	private void createNewUser() throws Exception {
		if (!usernames.contains(username)) {
			UserModel user = new UserModel(username, password);
			user.setEncHelper(encryptHelper);
			userMap.put(username, user);
			System.out.println("New user connected");
			UserHandler t = new UserHandler(s, dis, dos, username, this, user);
			onlineUsers.add(t);

			String msg = username + " successfully logged in";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
		} else {
			String msg = "the username " + username + " is taken";
			String noMac = encryptHelper.createEncoded(msg);
			msg = encryptHelper.createEncodedMessage(msg);
            msg = noMac + '\n' + msg;
			dos.writeUTF(msg);
		}
		dos.flush();
	}

	private void retrievePassword() {

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
