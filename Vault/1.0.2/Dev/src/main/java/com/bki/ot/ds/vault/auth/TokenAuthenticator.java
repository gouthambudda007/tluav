package com.bki.ot.ds.vault.auth;

import java.security.Key;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.bki.ot.ds.vault.exception.AuthenticationException;
import com.bki.ot.ds.vault.util.AsymetricKeyUtils;
import com.bki.ot.ds.vault.util.Logger;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SigningKeyResolver;
import io.jsonwebtoken.SigningKeyResolverAdapter;
import lombok.Data;

@Data
public class TokenAuthenticator {

	private static final String VAULT_IDENTIFIER = "vault.sellerdigital.bkiclouddev.com";

	protected static final Logger log = Logger.LOG;

	protected Map<String, String> rawJwtKeys;
	protected String authHeader;
	protected String rawToken;
	protected Jws<Claims> jws;

	public static TokenAuthenticator forAuthorizationHeader(String authHeader, Map<String, String> rawJwtKeys) {

		if (StringUtils.isBlank(authHeader)) {
			throw new AuthenticationException("Authorization header is empty");
		}

		TokenAuthenticator ta = new TokenAuthenticator();
		ta.setAuthHeader(authHeader);
		ta.setRawJwtKeys(rawJwtKeys);

		return ta;
	}

	// assign the appropriate public key to verify the signature, 
	// based on the issuer information in the header:
	protected SigningKeyResolver signingKeyResolver = new SigningKeyResolverAdapter() {
		@SuppressWarnings("rawtypes") 
		@Override
		public Key resolveSigningKey(JwsHeader header, Claims claims) {

			// verify the algorithm first:
			String algorithm = header.getAlgorithm();
			if (!StringUtils.isNotBlank(algorithm)) {
				throw new AuthenticationException("Missing required 'algorithm' JWT header with header: " + header);//TODO generic auth exception?
			} else if (!algorithm.equals(SignatureAlgorithm.RS256.name())) {
				throw new AuthenticationException("Incorrect signature algorithm designated in header: " + algorithm);	            	
			}

			// we need to know who ths issuer was, to choose the matching key:
			String issuer = claims.getIssuer();
			if (!StringUtils.isNotBlank(issuer)) {
				throw new AuthenticationException("Missing required 'issuer' claim param in JWT with claims: " + claims);
			}

			// get the raw (pem formatted) key that was read from parameter store, based on the issuer:
			String rawKey = rawJwtKeys.get(issuer);
			if (rawKey == null) {
				throw new AuthenticationException("Could not find public key in parameter store for issuer = " + issuer);	
			}
			try {
				return AsymetricKeyUtils.publicKeyFromPem(rawKey); //TODO: cache it?
			} catch (Exception e) {
				throw new AuthenticationException("Failed to convert parameter store data into a public key", e);
			}		    	
		}
	};

	public TokenInfo authenticate() {

		String[] headerParts = authHeader.split(" ");
		if (headerParts.length != 2 || !"Bearer".equals(headerParts[0])) {
			throw new AuthenticationException("Malformed authetication header: " + authHeader);
		}

		rawToken = headerParts[1];
		jws = null;

		try {
			// throws JwtException upon errors:
			jws = Jwts.parserBuilder() 
					//.setSigningKey(<some key>.getBytes())     
					.setSigningKeyResolver(signingKeyResolver)
					.setAllowedClockSkewSeconds(1 * 60)
					.requireAudience(VAULT_IDENTIFIER)
					.build()
					.parseClaimsJws(rawToken);

		} catch (Exception e) {
			throw new AuthenticationException(e);
		}

		log.debug("Token = " + jws);
		return TokenInfo.fromToken(jws);
	}

}
