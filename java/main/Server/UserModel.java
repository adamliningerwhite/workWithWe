import java.util.List;
import java.util.ArrayList;
import javax.crypto.SecretKey;

public class UserModel {

    private String username;
    private String password;
    private List<String> friends;
    private SecretKey sessionKey;
    private SecretKey encKey;
    private SecretKey macKey;
    private EncryptHelper encHelper;
    private List<String> friendRequests;

   // private RSAPublicKey publicKey;

    public UserModel(String username, String password) {
        this.username = username;
        this.password = password;
        this.encHelper = null;
        this.friends = new ArrayList<String>();
        this.friendRequests = new ArrayList<String>();
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String newName) {
        this.username = newName;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String newPass) {
        this.password = newPass;
    }

    public boolean checkPassword(String pass) {
      return password.equals(pass);
    }

    public List<String> getFriends() {
        return this.friends;
    }

    public void setFriends(List<String> newFriends) {
        this.friends = newFriends;
    }

    public void addFriend(String newFriend) {
        friends.add(newFriend);
    }

    public void removeFriend(String oldFriend) {
        friends.remove(oldFriend);
    }

    public void setEncHelper(EncryptHelper encHelper) {
    	this.encHelper = encHelper;
    	this.sessionKey = encHelper.getSessionKey();
    	this.macKey = encHelper.getMacKey();
    	this.encKey = encHelper.getEncodingKey();
    }

    public EncryptHelper getEncHelper() {
    	return encHelper;
    }

    public boolean hasEncHelper() {
    	return !(encHelper == null);
    }

    public SecretKey getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(SecretKey newSessionKey) {
        this.sessionKey = newSessionKey;
        setEncKey(newSessionKey);
        setMacKey(newSessionKey);
    }

    public SecretKey getEncKey() {
        return encKey;
    }

    public void setEncKey(SecretKey newEncKey) {
        this.encKey = newEncKey;
    }

    public SecretKey getMacKey() {
        return macKey;
    }

    public void setMacKey(SecretKey newMacKey){
        this.macKey = newMacKey;
    }

    public List<String> getFriendRequests() {
        return friendRequests;
    }
    
    public void addFriendRequest(String username) {
        friendRequests.add(username);
    }

    public boolean removeFriendRequest(String username) {
        return friendRequests.remove(username);
    }
}
