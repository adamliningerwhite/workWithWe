import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import java.util.Base64;
import java.util.HashMap;

public class WriteHelper {

    private static final String DATA_PATH = "Data/";
    private static final String LOGIN_DATA_PATH = DATA_PATH + "login.txt";
    private static final String PENDING_REQUEST_PATH = DATA_PATH + "pending_requests.txt";

    public static Base64.Encoder encoder = Base64.getEncoder();

    private HashMap<String, UserModel> userMap;

    private SecretKey encryptKey;

    public WriteHelper(SecretKey encryptKey) {
        this.encryptKey = encryptKey;
    }

    public void writeAllData(HashMap<String, UserModel> map) {
        userMap = map;
        try {
            PrintWriter loginWriter = new PrintWriter(new BufferedWriter(new FileWriter(LOGIN_DATA_PATH)));
            PrintWriter pendingRequestWriter = new PrintWriter(new BufferedWriter(new FileWriter(PENDING_REQUEST_PATH, false)));
            PrintWriter userWriter;
            for (UserModel user : userMap.values()) {
            	String salt = Base64.getEncoder().encodeToString(user.getSalt());
                loginWriter.println(user.getUsername() + "," + user.getPassword() + "," + salt + "," + user.getSecurityQuestion());
                try {
                    userWriter = new PrintWriter(new BufferedWriter(new FileWriter(DATA_PATH + user.getUsername() + ".txt")));
                    for (String friend : user.getFriends()) {
                        try {
                            String encryptedFriend = encrypt(friend);
                            userWriter.println(encryptedFriend);
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    userWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // add their pending friend requests to file 
                pendingRequestWriter.flush();
                for (String pendingFriend : user.getFriendRequests()) {
                    String recipient = user.getUsername();
                    String request = recipient + "," + pendingFriend;
                    try {
                        String encryptedRequest = encrypt(request);
                        pendingRequestWriter.println(encryptedRequest);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
            pendingRequestWriter.close();
            loginWriter.close();
        } catch(IOException e) {
            e.printStackTrace();
        }

    }

    private String encrypt(String message) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	    IvParameterSpec ivspec = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, encryptKey, ivspec);
		byte[] encrypted = cipher.doFinal(message.getBytes());
		String encodedString = encoder.encodeToString(encrypted);
		return encodedString;
	}
}
