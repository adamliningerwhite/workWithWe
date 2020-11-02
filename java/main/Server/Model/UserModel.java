package java.main.Server.Model;

import java.util.List;
import java.util.ArrayList;

public class UserModel {
    
    private String username;
    private String password;
    private List<UserModel> friends;
    private String sessionKey;
    private String encKey;
    private String macKey;

    public UserModel(String username, String password) {
        this.username = username;
        this.password = password;
        this.friends = new ArrayList<UserModel>();
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

    public List<UserModel> getFriends() {
        return this.friends;
    }

    public void addFriend(UserModel newFriend) {
        friends.add(newFriend);
    }

    public void removeFriend(UserModel oldFriend) {
        friends.remove(oldFriend);
    }
}
