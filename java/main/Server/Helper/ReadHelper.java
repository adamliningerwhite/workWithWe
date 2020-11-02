package java.main.Server.Helper;

public class ReadHelper {

    private static final String LOGIN_DATA_PATH = "../Data/login.txt";

    HashMap<String, User> users;

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
    public List<User> readData() {

    }
}
