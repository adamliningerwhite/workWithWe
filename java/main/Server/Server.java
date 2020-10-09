import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
	
    public Server(String listenPortStr) throws Exception {

        //notify the identity of the server to the user
        System.out.println("This is the Server");
        
        //attempt to create a server with the given port number
        int listenPort = Integer.parseInt(listenPortStr);
        try {
            System.out.println("Connecting to port "+listenPort+"...");
            ServerSocket bobServer = new ServerSocket(listenPort);
            System.out.println("Server started at port "+listenPort);
            
            //accept the client(a.k.a. Alice)
            Socket clientSocket = bobServer.accept();
            System.out.println("Client connected");
            DataInputStream streamIn = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                
            boolean finished = false;
                
            //read input from Alice
            while(!finished) {
                try {
                    String incomingMsg = streamIn.readUTF();
                    System.out.println("Recieved msg: " + incomingMsg);
                    
                    finished = incomingMsg.equals("done");
                }
                catch(IOException ioe) {
                    //disconnect if there is an error reading the input
                    finished = true;
                }
            }
            
            //clean up the connections before closing
            bobServer.close();
            streamIn.close();
            System.out.println("Server closed");
        } 
        catch (IOException e) {
            //print error if the server fails to create itself
            System.out.println("Error in creating the server");
            System.out.println(e);
        }
    }
	
    /**
     * args[0] ; port that Alice will connect to
     * args[1] ; program configuration
     */
    public static void main(String[] args) {
        //check for correct # of parameters
        if (args.length != 1) {
            System.out.println("Incorrect number of parameters");
            return;
        }
        
        //Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        
        //create Bob
        try {
            Server server = new Server(args[0]);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }		
    }
}
