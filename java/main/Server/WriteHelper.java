import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Base64;
import java.util.HashMap;

public class WriteHelper {

    private static final String DATA_PATH = "Data/";
    private static final String LOGIN_DATA_PATH = DATA_PATH + "login.txt";
    private static final String PENDING_REQUEST_PATH = DATA_PATH + "pending_requests.txt";
    private HashMap<String, UserModel> userMap;

    public void writeAllData(HashMap<String, UserModel> map) {
        userMap = map;
        try {
            PrintWriter loginWriter = new PrintWriter(new BufferedWriter(new FileWriter(LOGIN_DATA_PATH)));
            PrintWriter pendingRequestWriter = new PrintWriter(new BufferedWriter(new FileWriter(PENDING_REQUEST_PATH, false)));
            PrintWriter userWriter;
            for (UserModel user : userMap.values()) {
            	String salt = Base64.getEncoder().encodeToString(user.getSalt());
                loginWriter.println(user.getUsername() + "," + user.getPassword() + "," + salt);
                try {
                    userWriter = new PrintWriter(new BufferedWriter(new FileWriter(DATA_PATH + user.getUsername() + ".txt")));
                    for (String friend : user.getFriends()) {
                        userWriter.println(friend);
                    }
                    userWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // add their pending friend requests to file 
                pendingRequestWriter.flush();
                for (String pendingFriend : user.getFriendRequests()) {
                    String recipient = user.getUsername();
                    pendingRequestWriter.println(recipient + "," + pendingFriend);
                }
            }
            pendingRequestWriter.close();
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
