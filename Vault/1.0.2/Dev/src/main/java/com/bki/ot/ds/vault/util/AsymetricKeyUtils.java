package com.bki.ot.ds.vault.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.apache.commons.io.FileUtils;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;

public class AsymetricKeyUtils {

	protected static final Logger log = Logger.LOG;

	// do not instantiate...
	private AsymetricKeyUtils() {}

	// generate an RS256 asymetric key pair and saves the to PEM files
	public static KeyPair createKeyPair(String keyPairName) throws Exception {
		log.log("Creating key pair " + keyPairName + "...");
		KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
		//PrivateKey signingKey = keyPair.getPrivate();
		//PublicKey decodingKey = keyPair.getPublic();	
		saveKeyPair(keyPairName, keyPair);

		return keyPair;
	}

	// saves a key pair (PKCS#8 private key, X509 public key) in two PEM formatted files
	public static void saveKeyPair(String keyPairName, KeyPair keyPair) throws Exception {
		PrivateKey privateKey = keyPair.getPrivate();
		PublicKey publicKey = keyPair.getPublic();

		// PKCS#8 private key: ------------------------------------------------

		final File privateKeyFile = new File("src/main/resources/" + keyPairName + "-private.pem");
		byte[] privateKeyEnc = privateKey.getEncoded();
		String privateKeyPem = Encoders.BASE64.encode(privateKeyEnc);

		try (FileOutputStream fos = new FileOutputStream(privateKeyFile)) {

			System.out.println("-----BEGIN PRIVATE KEY-----");
			fos.write("-----BEGIN PRIVATE KEY-----\n".getBytes());

			int column = 0;
			for(int n = 0; n < privateKeyPem.length(); n ++) {
				System.out.print(privateKeyPem.charAt(n));
				fos.write(privateKeyPem.charAt(n));
				column ++;
				if(column == 64) {
					System.out.println();
					fos.write('\n');
					column = 0;
				}
			}
			System.out.println("\n-----END PRIVATE KEY-----");
			fos.write("\n-----END PRIVATE KEY-----\n".getBytes());
		}

		System.out.println("\n");

		// X509 public key: --------------------------------------------------

		final File publicKeyFile = new File("src/main/resources/" + keyPairName + "-public.pem");
		byte[] publicKeyEnc = publicKey.getEncoded();
		String publicKeyPem = Encoders.BASE64.encode(publicKeyEnc);

		try (FileOutputStream fos = new FileOutputStream(publicKeyFile)) {
			System.out.println("-----BEGIN PUBLIC KEY-----");
			fos.write("-----BEGIN PUBLIC KEY-----\n".getBytes());
			int column = 0;
			for(int n = 0; n < publicKeyPem.length(); n ++) {
				System.out.print(publicKeyPem.charAt(n));
				fos.write(publicKeyPem.charAt(n));
				column ++;
				if(column == 64) {
					System.out.println();
					fos.write('\n');
					column = 0;
				}
			}
			System.out.println("\n-----END PUBLIC KEY-----");
			fos.write("\n-----END PUBLIC KEY-----\n".getBytes());
		}

		System.out.println("\n");
	}

	// gets a public key (X509) from a pem-formatted file
	public static PublicKey publicKeyFromFile(String keyPairName) throws Exception {
		byte[] byteKey = readPemFile("src/main/resources/" + keyPairName + "-public.pem");
		return publicKeyFromBytes(byteKey);
	}
	
	// gets a private key (PKCS#8) from a pem-formatted file
	public static PrivateKey privateKeyFromFile(String keyPairName) throws Exception {
		byte[] byteKey = readPemFile("src/main/resources/" + keyPairName + "-private.pem");
		return privateKeyFromBytes(byteKey);
	}
	
	// gets a public key from a pem-formatted string
	public static PublicKey publicKeyFromPem(String pemString) throws Exception {
		return publicKeyFromBytes(pemToBytes(pemString));
	}

	// gets a public key from a pem-formatted byte array
	public static PublicKey publicKeyFromBytes(byte[] byteKey) throws Exception {
		
		KeyFactory kf = KeyFactory.getInstance("RSA");
		//RSAPublicKey key = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(byteKey));
		PublicKey key = kf.generatePublic(new X509EncodedKeySpec(byteKey));
		
		//log.log("key = " + key.getAlgorithm() + ": " + key.getFormat());
		return key;
	}

	// gets a private key from a pem-formatted file
	public static PrivateKey privateKeyFromBytes(byte[] byteKey) throws Exception {
		
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PrivateKey key = kf.generatePrivate(new PKCS8EncodedKeySpec(byteKey));
		
		//log.log("key = " + key.getAlgorithm() + ": " + key.getFormat());
		return key;
	}
	
	// gets a public key from a pem-formatted string
	public static PrivateKey privateKeyFromPem(String pemString) throws Exception {
		return privateKeyFromBytes(pemToBytes(pemString));
	}
	
	// reads a pem file and filters out the header/footer:
	private static byte[] readPemFile(String fileName) throws IOException {
		String fileContents = FileUtils.readFileToString(new File(fileName), StandardCharsets.UTF_8);
		
		return pemToBytes(fileContents);
	}

	private static byte[] pemToBytes(String pemString) {
		// remove haeder/footer:
		String keyString = pemString
				.replaceAll("-----(.+)-----(\\n)?", "")//;
				.replaceAll("(\\n)", "");

		return Decoders.BASE64.decode(keyString);
	}

	public static void main(String[] args) throws Exception {
		String keyPairName = "test-keys-01";
		createKeyPair(keyPairName);
		publicKeyFromFile(keyPairName);
		privateKeyFromFile(keyPairName);
	}

	//	public static void saveKeyPair(String keyPairName, KeyPair keyPair) throws IOException {
	//		log.log("Saving key pair...");
	//		final PrivateKey privateKey = keyPair.getPrivate();
	//		final PublicKey publicKey = keyPair.getPublic();
	//
	//		// Store Public Key
	//		final File publicKeyFile = new File("src/main/resources/" + keyPairName + "-public.pem");
	//		//publicKeyFile.getParentFile().mkdirs(); // make directories if they do not exist
	//		final X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
	//		try (FileOutputStream fos = new FileOutputStream(publicKeyFile)) {
	//			fos.write(x509EncodedKeySpec.getEncoded());
	//		}
	//
	//		// Store Private Key.
	//		final File privateKeyFile = new File("src/main/resources/" + keyPairName + "-private.pem");
	//		//privateKeyFile.getParentFile().mkdirs(); // make directories if they do not exist
	//		final PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
	//		try (FileOutputStream fos = new FileOutputStream(privateKeyFile)) {
	//			fos.write(pkcs8EncodedKeySpec.getEncoded());
	//		}
	//	}
	
//	public static PrivateKey get(String filename)
	//			throws Exception {
	//
	//		byte[] keyBytes = Files.readAllBytes(Paths.get(filename));
	//
	//		PKCS8EncodedKeySpec spec =
	//				new PKCS8EncodedKeySpec(keyBytes);
	//		KeyFactory kf = KeyFactory.getInstance("RSA");
	//		return kf.generatePrivate(spec);
	//	}

	//	public static void readKeyFile(String keyPairName) throws Exception {
	//		final File privateKeyFile = new File("src/main/resources/" + keyPairName + "-private.pem");///
	//        //ClassPathResource resource = new ClassPathResource("keystore.jks");
	//        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	//        keystore.load(new FileInputStream(privateKeyFile), null);
	//
	//        Key key = keystore.getKey("jwtkey", null);
	//        Certificate cert = keystore.getCertificate("jwtkey");
	//        PublicKey publicKey = cert.getPublicKey();
	//       
	//	}
	
}
