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
                s = mainServer.accept();
                
                DataInputStream dis = new DataInputStream(s.getInputStream()); 
                DataOutputStream dos = new DataOutputStream(s.getOutputStream());
                
                username = dis.readUTF();
                
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
