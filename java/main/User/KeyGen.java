import java.security.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
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

public class KeyGen {

	public static Base64.Encoder encoder = Base64.getEncoder();
	public static Base64.Decoder decoder = Base64.getDecoder();

	private SecretKey macKey;
	private SecretKey encodingKey;
	private SecretKey sessionKey;

	private RSAPrivateKey userKey;
	private RSAPublicKey serverKey;

  private String username;
  private String securityQuestion;
	private String password;
	private String userLoginType;
	private String keyTransportMsg;

	public KeyGen(String username, String securityQuestion, String password, String userLoginType, RSAPublicKey serverKey) {
	//public KeyGen(String username) {
    this.username = username;
    this.securityQuestion = securityQuestion;
		this.password = password;
		this.userLoginType = userLoginType;
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
		//System.out.println(getKeyTransportMsg());

	}

	/* methods to save key pairs and create and get the key transport message */
	private void saveKeyPair(String name, KeyPair keyPair) throws IOException {
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

	public String getKeyTransportMsg() {
		keyTransportMsg = keyTransport();
		return keyTransportMsg;
	}

	private String keyTransport() {
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

			String msg = userLoginType + "," + username + "," + password + "," + securityQuestion + "\n" + encodedSessionKey;
			byte[] toEncode = msg.getBytes();

			// create cipher and encode message with Alice identifier and session key
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, serverKey, random);
			byte[] cipherText = cipher.doFinal(toEncode);

			// create string then byte array of message to be signed
			String cipherString = encoder.encodeToString(cipherText);
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date(System.currentTimeMillis()));
			String msg2 = "Server" + "\n" + timeStamp + "\n" + cipherString;
			byte[] toSign = msg2.getBytes();

			// create signature with SHA256 and RSA then sign message
			Signature sign = Signature.getInstance("SHA256withRSA");
			sign.initSign(userKey);
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

//	public void setSessionKey(SecretKey key) {
//		sessionKey = key;
//	}

//	private SecretKey getMacKey() {
//		try {
//			String encodedSessionKey = encoder.encodeToString(sessionKey.getEncoded());
//			SecretKeySpec macSks = new SecretKeySpec(decoder.decode(hashFunction(encodedSessionKey, "mac")), "AES");
//			macKey = macSks;
//		} catch(NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		}
//
//		return macKey;
//	}

//	private SecretKey getEncodeKey() {
//		try {
//			String encodedSessionKey = encoder.encodeToString(sessionKey.getEncoded());
//			byte[] s = decoder.decode(hashFunction(encodedSessionKey, "enc"));
//			byte[] encKey = Arrays.copyOfRange(s, 0, 32);
//			SecretKeySpec encSks = new SecretKeySpec(encKey, "AES");
//			encodingKey = encSks;
//		} catch(NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		}
//
//		return encodingKey;
//	}

	/* methods to encrypt messages from user with and without mac */
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

    /* methods to decrypt message from server */
    public String getDecodedMessage(String msg, String noMac) {
		try {
			if(!checkMac(msg, noMac)) {
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

	/* general methods such as hash function and reading keys from file */
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

//	private static RSAPublicKey readPublicKeyFromFile(String filePath) {
//		RSAPublicKey key = null;
//		File publicKey = new File(filePath);
//
//		try (FileInputStream is = new FileInputStream(publicKey)) {
//			byte[] encodedPublicKey = new byte[(int) publicKey.length()];
//			is.read(encodedPublicKey);
//
//			KeyFactory kf = KeyFactory.getInstance("RSA");
//			X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedPublicKey);
//			key = (RSAPublicKey) kf.generatePublic(keySpec);
//
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//		return key;
//	}

	public static void main(String[] args) {
		//KeyGen serverGen = new KeyGen("server");
		//RSAPublicKey publicServer = readPublicKeyFromFile("../Server/ServerKeys/serverpublic.key");
		//KeyGen keyGen = new KeyGen("jack", publicServer);

		//RSAPublicKey publicServer = readPublicKeyFromFile("../Server/ServerKeys/serverpublic.key");
		//SecretKey encoded = getEncodeKey();
		//SecretKey mac = getMacKey();
	}
}
