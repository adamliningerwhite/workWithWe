import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.HashMap;

public class WriteHelper {
    
    private static final String DATA_PATH = "Data/";
    private static final String LOGIN_DATA_PATH = DATA_PATH + "login.txt";

    public void writeAllData(HashMap<String, UserModel> userMap) {

        try {
            PrintWriter loginWriter = new PrintWriter(new BufferedWriter(new FileWriter(LOGIN_DATA_PATH)));
            PrintWriter userWriter;
            for (UserModel user : userMap.values()) {
                loginWriter.println(user.getUsername() + "," + user.getPassword());
                try {
                    userWriter = new PrintWriter(new BufferedWriter(new FileWriter(DATA_PATH + user.getUsername() + ".txt")));
                    for (String friend : user.getFriends()) {
                        userWriter.println(friend);
                    }
                    userWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } 
            loginWriter.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
        
    }

    public void writeNewUser(UserModel user) {

    }

    public void writeChanges(UserModel user) {

    }

    // public static void main(String[] args) {
    //     ReadHelper reader = new ReadHelper();
    //     HashMap<String, UserModel> userMap = reader.readData(); 
    //     userMap.get("renaeeeeee").addFriend("adumb");
    //     WriteHelper writer = new WriteHelper();
    //     writer.writeAllData(userMap);
    // }
}
