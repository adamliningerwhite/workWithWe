# Work With Me 

Riti Sharma, Renae Tamura, Jack Bernstein, Adam White


## User.java

A basic program to create a new client and connect to WorkWithMe server. 
Takes username as only argument (i.e. java User big_bernie100), which is sent as unique identifier to server. 
After connecting to the server, the program continuously prompts the user to enter a message -- nothing happens unless the user types "logoff", which closes the connection to the server and ends the program. 

## Server.java

A basic program to create the WorkWithMe server and listen for connections from users.
Each time the Server recieves a new user connection, it adds the user to a list of people online and creates a new UserHandler thread to handle communication with that particular user. 

## UserHandler.java

A thread that handles communication with a particular user. If it recieves the "logoff" message from the user, the program removes this user from the list of online users and closes all connections. 

## How to Run

1. Start server: java Server
2. Start client(s): java User adumb_white 
3. Send messages to server 
4. Logoff all users
5. Kill Server
