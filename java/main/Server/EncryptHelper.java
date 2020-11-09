import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
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
	
	private String username;
	private String keyTransportMsg;
	
	public EncryptHelper(String username) {
		try {
			this.username = username;
			String keyPath = "../User/UserKeys/" + username + "public.key";
			userKey = readPublicKeyFromFile(keyPath);
			serverKey = readPrivateKeyFromFile("ServerKeys/serverprivate.key");
			
			keyTransportMsg = keyTransport();
		} catch(Exception e) {
			System.out.println("EncryptHelper not created");
			e.printStackTrace();
		}
		
	}
	
	public String getKeyTransportMsg() {
		return keyTransportMsg;
	}
	
	public String keyTransport() {
    	String message = null;
    	try {
			// generate new session key to be sent to Bob
			KeyGenerator keyGen;
			keyGen = KeyGenerator.getInstance("AES");
			SecureRandom random = new SecureRandom();
			keyGen.init(random);
			sessionKey = keyGen.generateKey();
			
			// create string then byte array of message to be encoded
			String encodedSessionKey = encoder.encodeToString(sessionKey.getEncoded());
	        
			byte[] s = decoder.decode(hashFunction(encodedSessionKey, "enc"));
			byte[] encKey = Arrays.copyOfRange(s, 0,32);
			SecretKeySpec macSks = new SecretKeySpec(decoder.decode(hashFunction(encodedSessionKey, "mac")), "AES");
			SecretKeySpec encSks = new SecretKeySpec(encKey, "AES");
			macKey = macSks;
			encodingKey = encSks;
			
			String msg = "Server" + "\n" + encodedSessionKey;
			byte[] toEncode = msg.getBytes();
			
			// create cipher and encode message with Alice identifier and session key
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, userKey, random);
			byte[] cipherText = cipher.doFinal(toEncode);
			
			// create string then byte array of message to be signed
			String cipherString = encoder.encodeToString(cipherText);
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date(System.currentTimeMillis()));
			String msg2 = username + "\n" + timeStamp + "\n" + cipherString;
			byte[] toSign = msg2.getBytes();
			
			// create signature with SHA256 and RSA then sign message
			Signature sign = Signature.getInstance("SHA256withRSA");
			sign.initSign(serverKey);
			sign.update(toSign);
			String signature = encoder.encodeToString(sign.sign());

			toSign = (msg2 + "\r\n" + signature).getBytes();
			
			// update message to new string 
			message = new String(toSign);

		} catch (Exception e) {
			System.out.println("Error: key transport msg not created");
			e.printStackTrace();
		} 
    	
    	return message;
    }
	
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
	
	public String decrypt(String cipherText) throws IOException, GeneralSecurityException {
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
	    IvParameterSpec ivspec = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, encodingKey, ivspec);
		return new String(cipher.doFinal(decoder.decode(cipherText)), "UTF-8");
	}

	public Boolean checkMac(String macStr, String msg) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(macKey);
			byte[] stringBytes = msg.getBytes();
			byte[] macBytes = mac.doFinal(stringBytes);
			String newMacStr = new String(macBytes);
			
			System.out.println("new mac str: " + newMacStr);
			System.out.println("mac str: " + macStr);

			return macStr.equals(newMacStr);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}
	
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
	
	public SecretKey getSessionKey() {
		return sessionKey;
	}
	
	public SecretKey getMacKey() {
		return macKey;
	}
	
	public SecretKey getEncodingKey() {
		return encodingKey;
	}
	public static void main(String[] args) {
		try {
			EncryptHelper eHelper = new EncryptHelper("test");
			//System.out.println(userKey);
			//System.out.println(serverKey);
			//System.out.println(sessionKey);
			//System.out.println(keyTransportMsg);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
	}
}
