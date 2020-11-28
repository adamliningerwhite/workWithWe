import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.Buffer;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

import java.util.ArrayList;
import java.util.Base64;

public class ReadHelper {

    private static final String DATA_PATH = "Data/";
    private static final String LOGIN_DATA_PATH = DATA_PATH + "login.txt";
    private static final String PENDING_REQUEST_PATH = DATA_PATH + "pending_requests.txt";

	public static Base64.Decoder decoder = Base64.getDecoder();

    private SecretKey decryptKey;

    public ReadHelper(SecretKey decryptKey) {
        this.decryptKey = decryptKey;
    }
    /**
     * 
     * A method that reads in stored data to "remember" prior users and 
     * store their information in a list of UserModel objects. 
     * 
     * This list will change as people create and delete accounts, add new friends,
     * or change usernames and passwords. For this reason, we only call this method once.
     * 
     * @return the list of workWithMe Users 
     */
    public HashMap<String, UserModel> readData() {
        HashMap<String, UserModel> usersMap = new HashMap<String, UserModel>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(LOGIN_DATA_PATH));
            String line = reader.readLine();
            // if (loginPieces.length < 4) {
            //   "answer" = loginPieces[4].trim();
            // } else {
            while (line != null) {
              String securityQuestion;

                String[] loginPieces = line.split(",");
                if (loginPieces.length != 4) {
                    line = reader.readLine();
                    continue;
                } else {
                  securityQuestion = loginPieces[3].trim();
                }
                String username = loginPieces[0].trim();
                // String securityQuestion = loginPieces[3].trim();
                String password = loginPieces[1].trim();
                String saltString = loginPieces[2].trim();
                byte[] salt = Base64.getDecoder().decode(saltString);
                UserModel user = new UserModel(username, securityQuestion, password);
                user.setSalt(salt);
                addFriends(user);

                usersMap.put(username, user);

                line = reader.readLine();
            }//}
            reader.close();

            // Read in pending friend requests 
            reader = new BufferedReader(new FileReader(PENDING_REQUEST_PATH));
            String encryptedLine = reader.readLine();
            while(encryptedLine != null) {
                try {
                    line = decrypt(encryptedLine);
                    String[] requestPieces = line.split(",");
                    if(requestPieces.length != 2) {
                        encryptedLine = reader.readLine();
                        continue;
                    }
                    String recipient = requestPieces[0];
                    String originator = requestPieces[1];
                    usersMap.get(recipient).addFriendRequest(originator);
                } catch (Exception e) {
                    System.out.println("Error decrypting text from pending_request file");
                }
                encryptedLine = reader.readLine();
            }
            reader.close(); 

        } catch (IOException e) {
            e.printStackTrace();
        } 

        return usersMap;
    }

    private void addFriends(UserModel user) {
        String path = DATA_PATH + user.getUsername() + ".txt";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String encryptedLine = reader.readLine();
            while (encryptedLine != null) {
                try {
                    String line = decrypt(encryptedLine);
                    user.addFriend(line.trim());
                } catch (Exception e) {
                    System.out.println("Error decrypting text from " + user.getUsername() + "'s text file");
                }
                encryptedLine = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            user.setFriends(new ArrayList<String>());
        }
    }

    private String decrypt(String cipherText) throws IOException, GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	    IvParameterSpec ivspec = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, decryptKey, ivspec);
		return new String(cipher.doFinal(decoder.decode(cipherText)), "UTF-8");
	}
}
