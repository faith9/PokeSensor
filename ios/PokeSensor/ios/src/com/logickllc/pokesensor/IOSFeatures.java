package com.logickllc.pokesensor;

import java.util.concurrent.TimeUnit;

import org.robovm.apple.dispatch.DispatchQueue;
import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.uikit.UIModalPresentationStyle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.logickllc.pokesensor.api.Features;
import com.pokegoapi.api.PokemonGo;
import com.pokegoapi.auth.PtcCredentialProvider;

import de.tomgrill.gdxdialogs.core.dialogs.GDXProgressDialog;
import okhttp3.OkHttpClient;

public class IOSFeatures extends Features {
	public static final String PREFS_NAME = "PokeSensor";
	public static final String PREF_TOKEN = "Token";
	public static final String PREF_USERNAME = "ProfileName";
	public static final String PREF_PASSWORD = "Nickname";
	public static final String PREF_FIRST_LOAD = "FirstLoad";
	
	public static final int LONG_MESSAGE_TIME = 3000;
	public static final int SHORT_MESSAGE_TIME = 1500;

	private GDXProgressDialog progressDialog;

	public synchronized void lockLogin() {
		if (!loginLocked()) loggingIn = true;
	}

	public synchronized void unlockLogin() {
		loggingIn = false;
	}

	public synchronized boolean loginLocked() {
		return loggingIn;
	}
	public void login() {
		if (!loginLocked()) lockLogin();
		else {
			print("PokeFinder", "Trying to call login() but it's already locked");
			return;
		}
		Thread loginThread = new Thread() {
			public void run() {
				print(TAG, "Attempting to login...");
				try {
					final Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
					token = prefs.getString(PREF_TOKEN);
					/*if (token != "") {
                        final ProgressDialog tryingDialog = showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
                        boolean trying = true;
                        int failCount = 0;
                        final int MAX_TRIES = 3;
                        while (trying) {
                            try {
                                print(TAG, "Attempting to login with token: " + token);

                                OkHttpClient httpClient = new OkHttpClient();
                                go = new PokemonGo(auth, httpClient);
                                tryTalkingToServer(); // This will error if we can't reach the server
                                shortMessage(R.string.loginSuccessfulMessage);
                                unlockLogin();
                                progressDialog.dismiss();
                                return;
                            } catch (Exception e) {
                                if (++failCount < MAX_TRIES) {
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e1) {
                                        e1.printStackTrace();
                                    }
                                } else {
                                    e.printStackTrace();
                                    token = "";
                                    print(TAG, "Erasing token because it seems to be expired.");
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putString(PREF_TOKEN, token);
                                    editor.commit();
                                    //longMessage(R.string.loginFailedMessage);
                                    unlockLogin();
                                    progressDialog.dismiss();
                                    login();
                                    return;
                                }
                            }
                        }
                    } else {*/
					Runnable runnable = new Runnable() {
						@Override
						public void run() {
							String pastUsername = prefs.getString(PREF_USERNAME, "");
							String pastPassword = prefs.getString(PREF_PASSWORD, "");

							if (!pastUsername.equals("") && !pastPassword.equals("")) {
								final String username = decode(pastUsername);
								final String password = decode(pastPassword);

								if (username.equals("") || password.equals("")) {
									// Erase username and pass and prompt for login again
									prefs.putString(PREF_USERNAME, "");
									prefs.putString(PREF_PASSWORD, "");
									prefs.flush();
									unlockLogin();
									login();
									return;
								}

								Thread thread = new Thread() {
									@Override
									public void run() {
										showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
										boolean trying = true;
										int failCount = 0;
										final int MAX_TRIES = 3;
										while (trying) {
											OkHttpClient httpClient = new OkHttpClient.Builder()
									          .connectTimeout(20, TimeUnit.SECONDS)
									          .writeTimeout(10, TimeUnit.SECONDS)
									          .readTimeout(30, TimeUnit.SECONDS)
									          .build();
											//RequestEnvelopeOuterClass.RequestEnvelope.AuthInfo auth = null;
											try {
												if (IOSLauncher.IS_AD_TESTING) print(TAG, "Attempting to login with Username: " + username + " and password: " + password);

												PtcCredentialProvider provider = new PtcCredentialProvider(httpClient, username, password);
												go = new PokemonGo(provider, httpClient);
												shortMessage(R.string.loginSuccessfulMessage);
												token = provider.getTokenId();
												//print(TAG, "Token: " + token);
												prefs.putString(PREF_TOKEN, token);
												prefs.flush();
												unlockLogin();
												progressDialog.dismiss();
												return;
											} catch (Exception e) {
												if (++failCount < MAX_TRIES) {
													try {
														Thread.sleep(3000);
													} catch (InterruptedException e1) {
														e1.printStackTrace();
													}
												} else {
													e.printStackTrace();
													longMessage(R.string.loginFailedMessage);
													unlockLogin();
													progressDialog.dismiss();
													return;
												}
											}
										}
									}
								};
								thread.start();
							} else {
								Runnable r = new Runnable() {
									public void run() {
										LoginController loginController = (LoginController) IOSLauncher.navigationController.getStoryboard().instantiateViewController("LoginController");
										if (Foundation.getMajorSystemVersion() >= 8) IOSLauncher.navigationController.setModalPresentationStyle(UIModalPresentationStyle.OverCurrentContext);
										else IOSLauncher.navigationController.setModalPresentationStyle(UIModalPresentationStyle.CurrentContext);
										IOSLauncher.navigationController.setProvidesPresentationContextTransitionStyle(true);
										IOSLauncher.navigationController.setDefinesPresentationContext(true);

										IOSLauncher.navigationController.presentViewController(loginController, true, null);
										
									}
								};
								runOnMainThread(r);
							}
						}
					};
					runOnMainThread(runnable);


				} catch (Exception e) {
					print(TAG, "Login failed...");
					e.printStackTrace();
					unlockLogin();
				}
			}
		};
		loginThread.start();
	}
	
	public void tryLogin(final String username, final String password) {
		Thread thread = new Thread() {
            @Override
            public void run() {
            	   MapController.features.saveUsernamePassword(username, password);
            	   unlockLogin();
            	   login();
            	   if (true) return;
                MapController.features.showProgressDialog(R.string.tryingLoginTitle, R.string.tryingLoginMessage);
                boolean trying = true;
                int failCount = 0;
                final int MAX_TRIES = 10;
                while (trying) {
                    OkHttpClient httpClient = new OkHttpClient();
                    try {
                        //print(TAG, "Attempting to login with Username: " + username.getText().toString() + " and password: " + password.getText().toString());

                        PtcCredentialProvider provider = new PtcCredentialProvider(httpClient, username, password);
                        go = new PokemonGo(provider, httpClient);
                        shortMessage(R.string.loginSuccessfulMessage);
                        token = provider.getTokenId();
                        //print(TAG, "Token: " + token);
                        saveToken(token);
                        unlockLogin();
                        progressDialog.dismiss();
                        return;
                    } catch (Exception e) {
                        if (++failCount < MAX_TRIES) {
                            try {
                                Thread.sleep(3000);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }
                        } else {
                            e.printStackTrace();
                            longMessage(R.string.loginFailedMessage);
                            unlockLogin();
                            progressDialog.dismiss();
                            return;
                        }
                    }
                }
            }
        };
        thread.start();
	}

	public void logout() {

		Runnable yes = new Runnable() {
			
			public void run() {
				// Erase login creds so we can try again
				saveUsernamePassword("", "");
				saveToken("");
				login();
			}
		};

		DialogHelper.yesNoBox(R.string.logoutTitle, R.string.logoutMessage, yes, null).build().show();
	}

	@Override
	public void print(String tag, String message) {
		if (IOSLauncher.IS_AD_TESTING) System.out.println(tag + ": " + message);
	}

	@Override
	public void runOnMainThread(Runnable r) {
		DispatchQueue.getMainQueue().async(r);	
	}

	public Object showProgressDialog(int titleid, int messageid) {
		return null;
	}

	public Object showProgressDialog(final String title, final String message) {
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				progressDialog = DialogHelper.progressDialog(title, message);
			}
		};
		runOnMainThread(runnable);
		return progressDialog;
	}

	public void shortMessage(int resid) {
		
	}

	public void shortMessage(final String message) {
		MapController.instance.showMessageFor(message, SHORT_MESSAGE_TIME);
	}

	public void longMessage(int resid) {
		
	}

	public void longMessage(final String message) {
		MapController.instance.showMessageFor(message, LONG_MESSAGE_TIME);
		/*Runnable r = new Runnable() {
			@Override
			public void run() {
				DialogHelper.messageBox("Message", message).build().show();
			}
		};
		runOnMainThread(r);*/
	}

	public void saveUsernamePassword(String username, String password) {
		Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
		prefs.putString(PREF_USERNAME, encode(username));
		prefs.putString(PREF_PASSWORD, encode(password));
		prefs.flush();
	}
	
	public void saveToken(String token) {
		Preferences prefs = Gdx.app.getPreferences(PREFS_NAME);
		prefs.putString(PREF_TOKEN, token);
		prefs.flush();
	}
}
