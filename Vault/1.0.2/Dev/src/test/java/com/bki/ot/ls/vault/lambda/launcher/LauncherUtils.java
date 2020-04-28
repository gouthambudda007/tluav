package com.bki.ot.ls.vault.lambda.launcher;

import java.security.PrivateKey;
import java.util.Date;

import com.amazonaws.regions.Regions;
import com.bki.ot.ds.vault.util.AsymetricKeyUtils;
import com.bki.ot.ls.vault.lambda.EnvUtils;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class LauncherUtils {

	public static String generateJwt() throws Exception {
		String jwt = generateJwt("aiva", "user134", "subject123", "945");
		System.err.println("jwt = \n" + jwt);
		return jwt;
	}

	public static String generateJwt(String issuer, String user, String subject, String client) throws Exception {
		long nowMillis = System.currentTimeMillis();
		Date now = new Date(nowMillis);
		PrivateKey signingKey = AsymetricKeyUtils.privateKeyFromFile("test-keys-01");
		
		return Jwts.builder()
				//.setId("some-id")
				.setIssuer(issuer)
				.setIssuedAt(now)
				//.setNotBefore(now)
				.setAudience("vault.sellerdigital.bkiclouddev.com")
				.setExpiration(new Date(nowMillis + 3600000))
				.setSubject(subject)
				.setHeaderParam("typ", "JWT")
				.claim("client", client)
				.claim("user", user)
				.signWith(signingKey, SignatureAlgorithm.RS256)
				.compact();
	}
	
	// NOTE: these also need to be set in pom.xml, surefire plugin, for maven-invoked unit tests
	public static void initEnvVars() throws Exception {
		EnvUtils.setEnv("AWS_REGION", Regions.US_EAST_1.getName());
		EnvUtils.setEnv("DEBUG_LOGGING", "true");
		EnvUtils.setEnv("BUCKET_PREFIX", "bki-ot");
		EnvUtils.setEnv("BUCKET_SUFFIX", "sellerdigital-vault");
		EnvUtils.setEnv("DEFAULT_BUCKET_ID", "docs");
		EnvUtils.setEnv("ENV_NAME", "sb1-usb");
		EnvUtils.setEnv("EXPIRATION_SECONDS", "3600");
	}
	// bucket name is: bki-ot-sb1-usb-docs-sellerdigital-vault
	
}
