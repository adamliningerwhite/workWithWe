import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;

public class Server {

    private static final int USER_LISTEN_PORT = 4232;
    private static final int MESSAGE_LISTEN_PORT = 4771;

    private List<UserHandler> onlineUsers = new ArrayList<UserHandler>();
    private Set<String> usernames = new HashSet<String>();
    private Map<String, String> userPassCombo = new HashMap<String, String>();

    // Map of program users
    // key is username string, value is their UserModel object
    private HashMap<String, UserModel> userMap;

    private DataInputStream dis;
    private DataOutputStream dos;
    private String option;
    private String username;
    private String password;
    private Socket s = null;


    public Server() throws Exception {

        /* Start workWithMe server */
        System.out.println("Started WorkWithMe Server");
        ServerSocket mainServer = new ServerSocket(USER_LISTEN_PORT);

        ReadHelper userDataReader = new ReadHelper();
        userMap = userDataReader.readData();

        WriterThread userDataWriter = new WriterThread(this);

        /* infinite loop to accept user connections */
        while (true) {
            try {
                s = mainServer.accept();

                dis = new DataInputStream(s.getInputStream());
                dos = new DataOutputStream(s.getOutputStream());

                String input = dis.readUTF();
                String[] parsed = input.split(",");
                option = parsed[0];
                username = parsed[1];
                password = parsed[2];

                switch(option) {
                  case "1":
                    createNewUser();
                    break;
                  case "2":
                    logIn();
                    break;
                  default:
                    System.out.println("Incorrect input!");
                    break;
                }
            } catch (Exception e) {
                s.close();
                e.printStackTrace();
                break; // TODO: don't think we want to break here, but otherwise next block is unreachable
            }
        }

        try {
            mainServer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, UserModel> getUserMap() {
      return userMap;
    }

    public List<UserHandler> getUsersOnline() {
      return onlineUsers;
    }

    public void setUsersOnline(List<UserHandler> newOnlineList) {
      onlineUsers = newOnlineList;
    }

    public void removeUser(String username) {
      for(int i = 0; i < onlineUsers.size(); i++) {
        if(onlineUsers.get(i).getUsername() == username) {
          onlineUsers.remove(i);
          break;
        }
      }
      usernames.remove(username);
    }

    private void logIn() throws Exception{
      UserModel currentUser = userMap.get(username);
      if(currentUser == null){
        dos.writeUTF("the username " + username + " does not exist");
      } else if (!currentUser.checkPassword(password)) {
        dos.writeUTF("incorrect password");
      } else {
        dos.writeUTF("successfully logged in");
      }
    }

    private void createNewUser() throws Exception{
      if(!usernames.contains(username)) {
        UserModel user = new UserModel(username, password);
        userMap.put(username, user);
        System.out.println("New user connected");
        UserHandler t = new UserHandler(s, dis, dos, username, this);
        onlineUsers.add(t);
        dos.writeUTF(username + " successfully logged in");
      } else {
        dos.writeUTF("the username " + username + " is taken");
      }
    }

    private void retrievePassword() {

    }

    public static void main(String[] args) {
        //check for correct # of parameters
        if (args.length > 0) {
            System.out.println("Incorrect number of parameters");
            return;
        }
        try {
            Server server = new Server();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
