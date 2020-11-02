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

        /* infinite loop to accept user connections */
        while (true) {
            try {
                s = mainServer.accept();

                DataInputStream dis = new DataInputStream(s.getInputStream());
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());

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
                  case "3":
                    retrievePassword();
                    break;
                  default:
                    System.out.println("Incorrect input!");
                    break;

                }

                if(!usernames.contains(username)) {
                	usernames.add(username);
                	System.out.println("New user connected");
                    UserHandler t = new UserHandler(s, dis, dos, username, this);
                    onlineUsers.add(t);
                    dos.writeUTF(username + " successfully logged in");
                } else {
                	dos.writeUTF("the username " + username + " is taken");
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

  private void logIn() {
    if(usernames.contains(username)){
      
    } else{
      dos.writeUTF("the username " + username + " does not exist");
    }
  }

  private void createNewUser() throws Exception{
    if(!usernames.contains(username)) {
      usernames.add(username);
      userPassCombo.put(username, password);
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
