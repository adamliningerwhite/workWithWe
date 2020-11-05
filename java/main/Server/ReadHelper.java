import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.Buffer;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class ReadHelper {

    private static final String DATA_PATH = "Data/";
    private static final String LOGIN_DATA_PATH = DATA_PATH + "login.txt";

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

            while (line != null) {
                String[] loginPieces = line.split(",");
                if (loginPieces.length != 2) {
                    continue;
                }
                String username = loginPieces[0].trim();
                String password = loginPieces[1].trim();
                UserModel user = new UserModel(username, password);
                addFriends(user);

                usersMap.put(username, user);

                line = reader.readLine();
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
            String line = reader.readLine();
            while (line != null) {
                user.addFriend(line.trim());
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            user.setFriends(new ArrayList<String>());
        }
    }
}
