package com.zoomstt.beta.zoombeta.initsdk;

public interface AuthConstants {

	public final static String appKey = "j40iFKV9wzkOsJwf4FR4zGDnOzKkKgati8gu";
	public final static String appSecret = "itYtnKGqkiQFBc6CxjjFV9xbvUxtRtKp9Q9Q";
	// TODO Change it to your web domain
	public final static String WEB_DOMAIN = "zoom.us";

	/**
	 * We recommend that, you can generate jwttoken on your own server instead of hardcore in the code.
	 * We hardcore it here, just to run the demo.
	 *
	 * You can generate a jwttoken on the https://jwt.io/
	 * with this payload:
	 * {
	 *
	 *     "appKey": "string", // app key
	 *     "iat": long, // access token issue timestamp
	 *     "exp": long, // access token expire time
	 *     "tokenExp": long // token expire time
	 * }
	 */
	public final static String SDK_JWTTOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBLZXkiOiJqNDBpRktWOXd6a09zSndmNEZSNHpHRG5PektrS2dhdGk4Z3UiLCJ0b2tlbkV4cCI6MTY0NTYzNjMxOSwiaWF0IjoxNjQ1NDM2MjIwLCJleHAiOjE2NDU2MzYzMTl9.w6Wyleo6NO_SBxrjRZ2xPEeiijGOSZuzCOdxObeCyyw";

}
