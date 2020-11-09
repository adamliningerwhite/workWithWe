import java.security.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
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
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyGen {
	
	public static Base64.Encoder encoder = Base64.getEncoder();
	public static Base64.Decoder decoder = Base64.getDecoder();
	
	private SecretKey macKey;
	private SecretKey encodingKey;
	private SecretKey sessionKey;
	
	private RSAPrivateKey userKey;
	private RSAPublicKey serverKey;
	
	private String username;
	
	public KeyGen(String username, RSAPublicKey serverKey) {
	//public KeyGen(String username) {
		this.username = username;
		this.serverKey = serverKey;
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
			keyGen.initialize(1024, random);
			
			KeyPair userPair = keyGen.generateKeyPair();
			saveKeyPair(username, userPair);
		} catch(Exception e) {
			e.printStackTrace();
		}
	
		this.userKey = readPrivateKeyFromFile("UserKeys/" + username + "private.key");
		
	}
	
	public void saveKeyPair(String name, KeyPair keyPair) throws IOException {
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();
		
		X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
		FileOutputStream fos = new FileOutputStream("UserKeys/" + name + "public.key");
		fos.write(x509KeySpec.getEncoded());
		fos.close();
		
		PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
		fos = new FileOutputStream("UserKeys/" + name + "private.key");
		fos.write(pkcs8KeySpec.getEncoded());
		fos.close();
	}
	
	public void setSessionKey(SecretKey key) {
		sessionKey = key;
	}
	
	private SecretKey getMacKey() {
		try {
			String encodedSessionKey = encoder.encodeToString(sessionKey.getEncoded());
			SecretKeySpec macSks = new SecretKeySpec(decoder.decode(hashFunction(encodedSessionKey, "mac")), "AES");
			macKey = macSks;	
		} catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return macKey;	
	}
	
	private SecretKey getEncodeKey() {
		try {
			String encodedSessionKey = encoder.encodeToString(sessionKey.getEncoded());
			byte[] s = decoder.decode(hashFunction(encodedSessionKey, "enc"));
			byte[] encKey = Arrays.copyOfRange(s, 0, 32);
			SecretKeySpec encSks = new SecretKeySpec(encKey, "AES");
			encodingKey = encSks;
		} catch(NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		return encodingKey;
	}
	
	public String getInitialEncode(String msg) {
		try {
			SecureRandom random = new SecureRandom();
			byte[] msgBytes = msg.getBytes();
			
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, serverKey, random);
			byte[] cipherText = cipher.doFinal(msgBytes);
			
			msg = encoder.encodeToString(cipherText);
		} catch(Exception e) {
			System.out.println("Error: unable to create password/username encryption");
			e.printStackTrace();
		}
		
		return msg;
	}
	
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
	
    //public String decryptEncodedMessage(String msg) {
    	
    //}
    
    public String decryptKeyTransport(String transport) {
    	try {
			String signature = transport.substring(transport.indexOf("\r\n")+2);
			transport = transport.substring(0,transport.indexOf("\r\n"));
			String[] transportComponents = transport.split("\\n");

			for(int i = 0; i < transportComponents.length; i++) {
				System.out.println(transportComponents[i]);
			}
			
			if (transportComponents[0].equals(username)) {
				Date messageTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").parse(transportComponents[1]);
				Date currentTime = new Date(System.currentTimeMillis());
				boolean isRecent = (((currentTime.getTime() - messageTime.getTime()) / (1000 * 60)) % 60) < 2;

				if (isRecent && checkSignature(transport, signature)) {
					// create cipher and encode message with Alice identifier and session key
					Cipher cipher = Cipher.getInstance("RSA");
					cipher.init(Cipher.DECRYPT_MODE, userKey);
					String decodedSessionKey = new String(cipher.doFinal(decoder.decode(transportComponents[2])),
							"UTF-8");
					String[] splitSessionKey = decodedSessionKey.split("\\n");
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
					return "done";
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return "";
    }
    
    private boolean checkSignature(String signedObj, String signature) {
		try {
			Signature sign = Signature.getInstance("SHA256withRSA");
			sign.initVerify(serverKey);
			sign.update(signedObj.getBytes());
			return sign.verify(decoder.decode(signature));
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
	
	private static RSAPrivateKey readPrivateKeyFromFile(String filePath) {
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
		//KeyGen serverGen = new KeyGen("server");
		
		//KeyGen keyGen = new KeyGen("jack");
		
		//RSAPublicKey publicServer = readPublicKeyFromFile("UserKeys/serverpublic.key");
		//SecretKey encoded = getEncodeKey();
		//SecretKey mac = getMacKey();
	}
}