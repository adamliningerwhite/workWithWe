import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptHelper {
	public static Base64.Encoder encoder = Base64.getEncoder();
	public static Base64.Decoder decoder = Base64.getDecoder();
	
	private SecretKey sessionKey;
	private SecretKey macKey;
	private SecretKey encodingKey;
	
	private RSAPublicKey userKey;
	private RSAPrivateKey serverKey;
	public RSAPublicKey serverPublicKey;
	
  private String username;
  private String securityQuestion;
	private String password;
	private String loginType;
	private String keyTransportMsg;
	
	public EncryptHelper() {
		try {
			serverKey = readPrivateKeyFromFile("ServerKeys/serverprivate.key");
			serverPublicKey = readPublicKeyFromFile("ServerKeys/serverpublic.key");
			
		} catch(Exception e) {
			System.out.println("EncryptHelper not created");
			e.printStackTrace();
		}
		
	}
	
	public String getPublicKeyString() {
		String encodedPublicKey = encoder.encodeToString(serverPublicKey.getEncoded());
		return encodedPublicKey;
	}
	
	/* methods to set and get username, password, and login type */
	private void setUsername(String username) {
		try {
			this.username = username;
			String keyPath = "../User/UserKeys/" + username + "public.key";
			userKey = readPublicKeyFromFile(keyPath);
			
		} catch(Exception e) {
			System.out.println("Username not set");
			e.printStackTrace();
		}
	}

	public String getUsername() {
		return username;
  }

  public String getSecurityQuestion() {
    return securityQuestion;
  }
  
  private void setSecurityQuestion(String securityQuestion) {
    this.securityQuestion = securityQuestion;
  }
	
	private void setPassword(String password) {
		this.password = password;
  }
	
	public String getPassword() {
		return password;
	}
	
	private void setLoginType(String loginType) {
		this.loginType = loginType;
	}
	
	public String getLoginType() {
		return loginType;
	}
	
	/* methods to decrypt the key transport msg */
	public String decryptKeyTransport(String transport) {
    	try {
			String signature = transport.substring(transport.indexOf("\r\n")+2);
			transport = transport.substring(0,transport.indexOf("\r\n"));
			String[] transportComponents = transport.split("\\n");
			
			if (transportComponents[0].equals("Server")) {
				Date messageTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").parse(transportComponents[1]);
				Date currentTime = new Date(System.currentTimeMillis());
				boolean isRecent = (((currentTime.getTime() - messageTime.getTime()) / (1000 * 60)) % 60) < 2;

				if (isRecent) {
					// create cipher and encode message with Alice identifier and session key
					Cipher cipher = Cipher.getInstance("RSA");
					cipher.init(Cipher.DECRYPT_MODE, serverKey);
					String decodedSessionKey = new String(cipher.doFinal(decoder.decode(transportComponents[2])),
							"UTF-8");
					String[] splitSessionKey = decodedSessionKey.split("\\n");
					String loginString = splitSessionKey[0];
					String[] login = loginString.split(",");
					setLoginType(login[0]);
					setUsername(login[1]);
          setPassword(login[2]);
          setSecurityQuestion(login[3]);
					
					if(checkSignature(transport, signature)) {
					
						String sessionKeyString = splitSessionKey[1];
						
						byte[] decodedKey = decoder.decode(sessionKeyString);
						SecretKeySpec sks = new SecretKeySpec(decodedKey, "AES");
						sessionKey = sks;
		
						byte[] s = decoder.decode(hashFunction(sessionKeyString, "enc"));
						byte[] decKey = Arrays.copyOfRange(s, 0,32);
						SecretKeySpec macSks = new SecretKeySpec(decoder.decode(hashFunction(sessionKeyString, "mac")), "AES");
						SecretKeySpec decSks = new SecretKeySpec(decKey, "AES");
						macKey = macSks;
						encodingKey = decSks;
		
						return "Received Secret Key";
					} else {
						System.out.println("Signature not verified");
					}

				} else {
					System.out.println("Key Transport not recent");
				}
			} else {
				System.out.println("Key Transport not for server");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

    	return "Secret Key not recieved";
    }
	
	private boolean checkSignature(String signedObj, String signature) {
		try {
			Signature sign = Signature.getInstance("SHA256withRSA");
			sign.initVerify(userKey);
			sign.update(signedObj.getBytes());
			return sign.verify(decoder.decode(signature));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/* methods to get the session key, mac key, and encoding key */
	public SecretKey getSessionKey() {
		return sessionKey;
	}
	
	public SecretKey getMacKey() {
		return macKey;
	}
	
	public SecretKey getEncodingKey() {
		return encodingKey;
	}
	
	/* methods to decrypt regular messages from users */
	public String getDecodedMessage(String msg, String noMac) {
		try {
			if(!checkMac(msg, noMac)) {
				System.out.println("Mac not verified");
				//return "break";
			}
			msg = decrypt(noMac);
		} catch(Exception e) {
			System.out.println("Message not decoded");
			e.printStackTrace();
		}
		
		return msg;
	}
	
	private String decrypt(String cipherText) throws IOException, GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	    IvParameterSpec ivspec = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, encodingKey, ivspec);
		return new String(cipher.doFinal(decoder.decode(cipherText)), "UTF-8");
	}

	private Boolean checkMac(String macStr, String msg) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(macKey);
			byte[] stringBytes = msg.getBytes();
			byte[] macBytes = mac.doFinal(stringBytes);
			String newMacStr = new String(macBytes);
			
			//System.out.println("new mac str: " + newMacStr);
			//System.out.println("mac str: " + macStr);

			return macStr.equals(newMacStr);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
	
	/* methods to encrypt message with and without mac under session key */
	public String createEncoded(String msg) {
		try {
			msg = packageMessage(msg);
			msg = encrypt(msg);
		} catch(Exception e) {
			System.out.println("Error: package could not be encoded");
			e.printStackTrace();
		}
		
		return msg;
	}
	
	public String createEncodedMessage(String msg) {
		try {
			msg = packageMessage(msg);
			msg = encrypt(msg);
			msg = createMac(msg);
		} catch(Exception e) {
			System.out.println("Error: package could not be encoded with MAC");
			e.printStackTrace();
		}
		
		return msg;
	}
	
	private String encrypt(String message) throws Exception {
		
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	    IvParameterSpec ivspec = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, encodingKey, ivspec);
		byte[] encrypted = cipher.doFinal(message.getBytes());
		String encodedString = encoder.encodeToString(encrypted);
		
		return encodedString;
	}
    
    public String createMac(String message) throws Exception {
    	String encrypted = "";
    	
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(macKey);
		byte[] stringBytes = message.getBytes();
    	byte[] macBytes = mac.doFinal(stringBytes);
    	encrypted = new String(macBytes);

    	return encrypted;
    }
    
    private String packageMessage(String message) throws Exception {
		StringBuilder acc = new StringBuilder();
		acc.append(message);

		return acc.toString();
	}
	
    /* general methods including hash function, and reading keys from file */
	private String hashFunction(String input, String concat) throws NoSuchAlgorithmException {
		// getInstance() method is called with algorithm SHA-512 
		MessageDigest md = MessageDigest.getInstance("SHA-512"); 

		// digest() method is called 
		// to calculate message digest of the input string 
		// returned as array of byte 
		byte[] messageDigest = md.digest((input+concat).getBytes()); 
	
		// Convert byte array into signum representation 
		BigInteger no = new BigInteger(1, messageDigest); 
	
		// Convert message digest into hex value 
		String hashtext = no.toString(16); 
	
		// Add preceding 0s to make it 32 bit 
		while (hashtext.length() < 32) { 
			hashtext = "0" + hashtext; 
		} 
		
		return hashtext;
	}
	
	public byte[] getSalt() {
    	SecureRandom random = new SecureRandom();
    	byte[] salt = new byte[16];
    	random.nextBytes(salt);
    	return salt;
    }
    
    public String hashPassword(String password, byte[] salt) {
    	String genPassword = null;
    	try {
    		MessageDigest md = MessageDigest.getInstance("SHA-512");
    		md.update(salt);
    		byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
    		StringBuilder sb = new StringBuilder();
    		for(int i = 0; i < bytes.length; i++) {
    			sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
    		}
    		genPassword = sb.toString();
    		
    	} catch(Exception e) {
    		System.out.println("Error unable to hash password");
    		e.printStackTrace();
    	}
    	
    	return genPassword;
    }
	
	private RSAPublicKey readPublicKeyFromFile(String filePath) {
		RSAPublicKey key = null;
		File publicKey = new File(filePath);

		try (FileInputStream is = new FileInputStream(publicKey)) {
			byte[] encodedPublicKey = new byte[(int) publicKey.length()];
			is.read(encodedPublicKey);

			KeyFactory kf = KeyFactory.getInstance("RSA");
			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedPublicKey);
			key = (RSAPublicKey) kf.generatePublic(keySpec);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return key;
	}
	
	private RSAPrivateKey readPrivateKeyFromFile(String filePath) {
		RSAPrivateKey key = null;

		try {
			byte[] encodedPrivateKey = Files.readAllBytes(Paths.get(filePath));
			KeyFactory kf = KeyFactory.getInstance("RSA");
			PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
			key = (RSAPrivateKey) kf.generatePrivate(keySpec);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return key;
	}
	
	public static void main(String[] args) {
//		try {
//			//EncryptHelper eHelper = new EncryptHelper("test");
//			//System.out.println(userKey);
//			//System.out.println(serverKey);
//			//System.out.println(sessionKey);
//			//System.out.println(keyTransportMsg);
//		} catch(Exception e) {
//			e.printStackTrace();
//		}
		
	}
}
