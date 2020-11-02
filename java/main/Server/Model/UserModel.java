package java.main.Server.Model;

import java.util.List;

public class UserModel {
    
    String username;
    String password;
    List<User> friends;

    public UserModel(String username, String password) {
        this.username = username;
        this.password = password;
        this.friends = new ArrayList<User>();
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

    public List<User> getFriends() {
        return this.friends;
    }

    public void addFriend(User newFriend) {
        friends.add(newFriend);
    }

    public void removeFriend(User oldFriend) {
        friends.remove(oldFriend);
    }
}
