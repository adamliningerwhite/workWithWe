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

    private EncryptHelper encryptHelper;


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

                //TODO: send certificate? or send a file with a path to the certificate?
                dos.writeUTF("../Server/ServerKeys/serverpublic.key");
                dos.flush();


                String input = dis.readUTF();
                //TODO: decrypt message
                String[] parsed = input.split(",");
                option = parsed[0];
                username = parsed[1];
                password = parsed[2];

                switch(option) {
                  case "1":
                    createNewUser();
                    encryptHelper = new EncryptHelper(username);
                	userMap.get(username).setEncHelper(encryptHelper);
                    dos.writeUTF(exchangeSessionKey());
                    dos.flush();
                    //break;
                  case "2":
                    boolean res = logIn();
                    if(res){
                      encryptHelper = userMap.get(username).getEncHelper();
                      dos.writeUTF(exchangeSessionKey());
                    }
                    dos.flush();
                    //break;
                  default:
                    System.out.println("Incorrect input!");
                    break;
                }

//                while(true) {
//                	String noMac = dis.readUTF();
//                    String msg = dis.readUTF();
//
//                    msg = encryptHelper.getDecodedMessage(msg, noMac);
//
//                    System.out.println(msg);
//                }

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

    private boolean logIn() throws Exception{
      UserModel currentUser = userMap.get(username);
      //TODO: encrypt messages (or the ones that can be i think just last one)
      if(currentUser == null){
        dos.writeUTF("the username " + username + " does not exist");
        return false;
      } else if (!currentUser.checkPassword(password)) {
        dos.writeUTF("incorrect password");
        return false;
      } else {
        dos.writeUTF("successfully logged in");
      }
      dos.flush();
      return true;
    }

    private void createNewUser() throws Exception{
      if(!usernames.contains(username)) {
        UserModel user = new UserModel(username, password);
        userMap.put(username, user);
        System.out.println("New user connected");
        UserHandler t = new UserHandler(s, dis, dos, username, this);
        onlineUsers.add(t);
        //TODO: add public key to UserModel
        //TODO: encrypt message
        dos.writeUTF(username + " successfully logged in");
      } else {
        dos.writeUTF("the username " + username + " is taken");
      }
      dos.flush();
    }

    private void retrievePassword() {

    }

    private String exchangeSessionKey() {
    	encryptHelper = new EncryptHelper(username);
    	userMap.get(username).setEncHelper(encryptHelper);
    	//TODO: add session key to the UserModel
    	return encryptHelper.getKeyTransportMsg();
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
