import java.util.List;
import java.util.ArrayList;
import javax.crypto.SecretKey;

public class UserModel {

    private String username;
    private String password;
    private String securityQuestion;
    private byte[] salt;
    private List<String> friends;
    private SecretKey sessionKey;
    private SecretKey encKey;
    private SecretKey macKey;
    private EncryptHelper encHelper;
    private boolean working;
    private int incorrectAttempts;

    private List<String> friendsOnline;
    private List<String> pendingFriendRequests;
    private List<String> acceptedRequests;
    private List<String> rejectedRequests;

    private List<String> blockedUsers;

   // private RSAPublicKey publicKey;

    public UserModel(String username, String securityQuestion, String password) {
        this.username = username;
        this.securityQuestion = securityQuestion;
        this.password = password;
        this.encHelper = null;
        this.friends = new ArrayList<String>();
        this.friendsOnline = new ArrayList<String>();
        this.pendingFriendRequests = new ArrayList<String>();
        this.acceptedRequests = new ArrayList<String>();
        this.rejectedRequests = new ArrayList<String>();
        this.blockedUsers = new ArrayList<String>();
        incorrectAttempts = 0;
        working = false;
    }

    public UserModel(String username, String securityQuestion, String password, int incorrectAttempts) {
        this.username = username;
        this.securityQuestion = securityQuestion;
        this.password = password;
        this.encHelper = null;
        this.friends = new ArrayList<String>();
        this.friendsOnline = new ArrayList<String>();
        this.pendingFriendRequests = new ArrayList<String>();
        this.acceptedRequests = new ArrayList<String>();
        this.rejectedRequests = new ArrayList<String>();
        this.blockedUsers = new ArrayList<String>();
        this.incorrectAttempts =  incorrectAttempts;
        working = false;
    }

    public int getIncorrectAttempts() {
      return incorrectAttempts;
    }

    public void resetIncorrectAttempts() {
      incorrectAttempts = 0;
    }

    public void setIncorrectAttempts(int attempts) {
      incorrectAttempts = attempts;
    }

    public void incrementIncorrectAttempts() {
      incorrectAttempts++;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String newName) {
        this.username = newName;
    }

    public String getSecurityQuestion() {
      return this.securityQuestion;
    }

    public void setSecurityQuestion(String securityQuestion){
      this.securityQuestion = securityQuestion;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String newPass) {
        this.password = newPass;
    }

    public byte[] getSalt() {
    	return this.salt;
    }

    public void setSalt(byte[] salt) {
    	this.salt = salt;
    }

    public boolean checkPassword(String pass) {
      return password.equals(pass);
    }

    public boolean checkSecurityQuestion(String question) {
      return securityQuestion.equals(question);
    }

    public List<String> getFriends() {
        return this.friends;
    }

    public void setFriends(List<String> newFriends) {
        this.friends = newFriends;
    }

    public void addFriend(String newFriend) {
        if(!friends.contains(newFriend)) {
            friends.add(newFriend);
        }
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
        return pendingFriendRequests;
    }

    public void addFriendRequest(String friend) {
        if(!pendingFriendRequests.contains(friend)) {
            pendingFriendRequests.add(friend);
        }
    }

    public boolean removeFriendRequest(String username) {
        return pendingFriendRequests.remove(username);
    }

    public void addAccepted(String friend) {
        acceptedRequests.add(friend);
    }
    public List<String> getAccepted() {
        return acceptedRequests;
    }
    public void clearAccepted() {
        acceptedRequests.clear();
    }

    public void addRejected(String enemy) {
        rejectedRequests.add(enemy);
    }
    public List<String> getRejected() {
        return rejectedRequests;
    }
    public void clearRejected() {
        rejectedRequests.clear();
    }

    public boolean isWorking() {
        return working;
    }
    public void setWorkingStatus(boolean status) {
        working = status;
    }
    public void flipStatus() {
        working = !working;
    }

    public void addFriendOnline(String friend) {
        if (!friendsOnline.contains(friend)) {
            friendsOnline.add(friend);
        }
    }
    public List<String> getFriendsOnline() {
        return friendsOnline;
    }
    public void removeFriendOnline(String friend) {
        friendsOnline.remove(friend);
    }

    public void addBlockedUser(String user) {
        if (!blockedUsers.contains(user)) {
            blockedUsers.add(user);
        }
    }
    public List<String> getBlockedUsers() {
        return blockedUsers;
    }
    public void removeBlockedUser(String user) {
        blockedUsers.remove(user);
    }
}
