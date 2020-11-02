import java.util.List;
import java.util.ArrayList;

public class UserModel {
    
    private String username;
    private String password;
    private List<String> friends;
    private String sessionKey;
    private String encKey;
    private String macKey;

    public UserModel(String username, String password) {
        this.username = username;
        this.password = password;
        this.friends = new ArrayList<String>();
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

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String newSessionKey) {
        this.sessionKey = newSessionKey;
    }

    public String getEncKey() {
        return encKey;
    }

    public void setEncKey(String newEncKey) {
        this.encKey = newEncKey;
    }

    public String getMacKey() {
        return macKey;
    }

    public void setMacKey(String newMacKey){
        this.macKey = newMacKey;
    }

    public String toString() {
        return "username: " + username + ", password: " + password + ", friends: " + friends + "\n";
    }
}
