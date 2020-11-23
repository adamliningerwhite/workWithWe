import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Classify program that takes in a password string as an argument and returns either 
 * "strong" or "weak" indicating the password strength
 * 
 * Note that there is a file used here that represents the top 100000 most common passwords
 * This file is from github by @g0tmi1k and @danielmiessler with the url: 
 * https://github.com/danielmiessler/SecLists/blob/aad07fff50ca37af2926de4d07ff670bf3416fbc/Passwords/10_million_password_list_top_100000.txt
 * 
 * @author Renae Tamura
 *
 */
public class Classify {

	// instantiate hash map of top 100000 passwords and string for the strength of the
	// password (either "weak" or "strong")
	public static HashMap<String, Boolean> weakPasswords = new HashMap<String, Boolean>();
	public int strength = 0;
	public static int MAX_SCORE = 8;
	
	public Classify() {
		try {
			getWeakPasswords("10_million_password_list_top_100000.txt");
		} catch(Exception e) {
			System.out.println("File not found");
			System.out.println(e.getStackTrace());
		}
	}
	
	/**
	 * getWeakPasswords method takes in a filename and adds all weak passwords to a hashmap
	 * each line in the file is a different weak password
	 * 
	 * @param filename – file with weak passwords
	 * @throws Exception 
	 */
	public void getWeakPasswords(String filename) throws Exception {
		// create new buffered reader and read the first line
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		String line = br.readLine();
		
		// while there are still lines, add the weak password to the hash map and 
		// read the next line
		while(line != null) {
			weakPasswords.put(line, true);
			line = br.readLine();
		}
		
		// close buffered reader
		br.close();
	}
	
	/**
	 * evalWealList method increases strength count if the password
	 * is not in the blacklisted passwords
	 * 
	 * @param password
	 */
	public void evalWeakList(String password) {
		if(!weakPasswords.containsKey(password))
			strength += 2;
	}
	
	/**
	 * evalLength method increases strength count if the password
	 * is 8 or greater or 10 or greater
	 * 
	 * @param password
	 */
	public void evalLength(String password) {
		if(password.length() >= 10)
			strength += 2;
		else if(password.length() > 7)
			strength++;
	}
	
	/**
	 * evalCase method increases strength count if there is an upper and 
	 * lower case letter in the password
	 * 
	 * @param password
	 */
	public void evalCase(String password) {
		if(password.matches(".*[A-Z].*") && password.matches(".*[a-z].*"))
			strength++;
	}
	
	/**
	 * evalNum method increases strength count if there is a number in the
	 * password
	 * 
	 * @param password
	 */
	public void evalNum(String password) {
		if(password.matches(".*[0-9].*"))
			strength++;
	}
	
	/**
	 * evalSpecialChars method increases strength count if there is a special 
	 * character in the password
	 * 
	 * @param password
	 */
	public void evalSpecialChars(String password) {
		if(password.matches(".*[^A-Za-z0-9].*"))
			strength += 2;
	}
	
	/**
	 * evaluatePassword method takes in password string and evaluates it as weak or strong
	 * runs through if statements for "weak" passwords and changes strength if the 
	 * given password satisfies any of them
	 * 
	 * @param password
	 */
	public String evaluatePassword(String password) {
		strength = 0;
		evalWeakList(password);
		evalLength(password);
		evalCase(password);
		evalNum(password);
		evalSpecialChars(password);
		
		String classify;
		if(strength > 5)
			classify = "strong";
		else
			classify = "weak";
		return strength + "/" + MAX_SCORE + " (" + classify + ")";
	}
}