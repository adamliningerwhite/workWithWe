import java.io.*; 
import java.text.*; 
import java.util.*; 
import java.net.*; 

public class Server {

    private static final int USER_LISTEN_PORT = 4232;
    private static final int MESSAGE_LISTEN_PORT = 4771;

    private List<UserHandler> onlineUsers = new ArrayList<UserHandler>();
    private Set<String> usernames = new HashSet<String>();
	
    public Server() throws Exception {

        /* Start workWithMe server */
        System.out.println("Started WorkWithMe Server");
        ServerSocket mainServer = new ServerSocket(USER_LISTEN_PORT);

        /* infinite loop to accept user connections */
        while (true) {
            Socket s = null;
            String username;

            try {
                /* Accept incoming connections */
                s = mainServer.accept();
                System.out.println("New user connected");

                /* obtaining input and out streams */
                DataInputStream dis = new DataInputStream(s.getInputStream()); 
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());

                /* 1st message: new user object sends its username */ 
                username = dis.readUTF();
                
                if(!usernames.contains(username)) {
                	usernames.add(username);
                	System.out.println("New user connected");
                	/* create and start a new user thread */
                    UserHandler t = new UserHandler(s, dis, dos, username, this);
                    /* 1st response: notify user has been logged in */
                    onlineUsers.add(t);
                    dos.writeUTF(packageMessage(username + " successfully logged in")); 
                } else {
                	dos.writeUTF(packageMessage("the username " + username + "is taken"));
                }

            } catch (Exception e) {
                s.close();
                e.printStackTrace();

                break; // TODO: don't think we want to break here, but otherwise next block is unreachable
            }
        } 

        // NOT SURE WHEN/HOW TO HANDLE CLOSING MAIN SERVER
        try {
            mainServer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	private String packageMessage(String message) throws Exception {
        StringBuilder acc = new StringBuilder();
        acc.append(message);
        return acc.toString();
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
	}
   
    /**
     * main method 
     */
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
