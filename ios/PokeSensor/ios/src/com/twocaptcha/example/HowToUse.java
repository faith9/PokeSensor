package com.twocaptcha.example;

import java.io.IOException;

import com.twocaptcha.api.ProxyType;
import com.twocaptcha.api.TwoCaptchaService;

public class HowToUse {
	
	public static void main(String[] args) {
		
		String apiKey = "Your 2captcha.com captcha key";
		String googleKey = "6Le-wvkSAAAAAPBMRTvw0Q4Muexq9bi0DJwx_mJ-";
		String pageUrl = "https://www.google.com/recaptcha/api2/demo";
		String proxyIp = "183.38.231.131";
		String proxyPort = "8888";
		String proxyUser = "username";
		String proxyPw = "password";
		
		/**
		 * With proxy and user authentication
		 */
		TwoCaptchaService service = new TwoCaptchaService(apiKey, googleKey, pageUrl, proxyIp, proxyPort, proxyUser, proxyPw, ProxyType.HTTP);
		
		/**
		 * Without proxy and user authentication
		 * TwoCaptchaService service = new TwoCaptchaService(apiKey, googleKey, pageUrl);
		 */
		
		try {
			String responseToken = service.solveCaptcha();
			System.out.println("The response token is: " + responseToken);
		} catch (InterruptedException e) {
			System.out.println("ERROR case 1");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("ERROR case 2");
			e.printStackTrace();
		}
	}
}
