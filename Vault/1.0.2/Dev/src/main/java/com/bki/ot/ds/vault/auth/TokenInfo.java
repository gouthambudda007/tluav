package com.bki.ot.ds.vault.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TokenInfo {
	
	private String issuer;
	private String subject;
	private String client;
	private String user;
	
	static public TokenInfo fromToken(Jws<Claims> jws) {
		TokenInfo ti = new TokenInfo();
		
		ti.setIssuer(jws.getBody().getIssuer());
		ti.setSubject(jws.getBody().getSubject());
		
		if (jws.getBody().get("client") != null) {
			ti.setClient(jws.getBody().get("client") + "");
		}
		if (jws.getBody().get("user") != null) {
			ti.setUser(jws.getBody().get("user") + "");
		}
		
		return ti;
	}
	
	static public TokenInfo justForTesting() {
		TokenInfo ti = new TokenInfo();
		
		ti.setClient("945");
		ti.setIssuer("tester");
		ti.setSubject("subject123");
		ti.setUser("user134");
		
		return ti;
	}
}
