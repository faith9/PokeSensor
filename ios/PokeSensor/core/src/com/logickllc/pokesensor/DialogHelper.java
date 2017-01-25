package com.logickllc.pokesensor;

import java.util.ArrayList;

import de.tomgrill.gdxdialogs.core.GDXDialogs;
import de.tomgrill.gdxdialogs.core.GDXDialogsSystem;
import de.tomgrill.gdxdialogs.core.dialogs.GDXButtonDialog;
import de.tomgrill.gdxdialogs.core.dialogs.GDXProgressDialog;
import de.tomgrill.gdxdialogs.core.dialogs.GDXTextPrompt;
import de.tomgrill.gdxdialogs.core.listener.ButtonClickListener;
import de.tomgrill.gdxdialogs.core.listener.TextPromptListener;

public class DialogHelper {
	private static GDXDialogs dialogs;
	
	public static void initialize() {
		dialogs = GDXDialogsSystem.install();
	}
	
	public static GDXButtonDialog messageBox(String title, String message) {
		return messageBox(title, message, "Ok", null);
	}
	
	public static GDXButtonDialog messageBox(String title, String message, String buttonText, final Runnable runnable) {
		final GDXButtonDialog dialog = dialogs.newDialog(GDXButtonDialog.class);
		
		dialog.setTitle(title);
		dialog.setMessage(message);
		
		dialog.setClickListener(new ButtonClickListener() {

			@Override
			public void click(int button) {
				if (button == 0) {
					dialog.dismiss();
					if (runnable != null) runnable.run();
				}
			}
			
		});
		
		dialog.addButton(buttonText);
		
		return dialog;
	}
	
	public static GDXButtonDialog yesNoBox(String title, String message, final Runnable positiveRunnable, final Runnable negativeRunnable) {
		final GDXButtonDialog dialog = dialogs.newDialog(GDXButtonDialog.class);
		
		dialog.setTitle(title);
		dialog.setMessage(message);
		
		dialog.setClickListener(new ButtonClickListener() {

			@Override
			public void click(int button) {
				if (button == 1) {
					if (positiveRunnable != null) positiveRunnable.run();
					dialog.dismiss();
				} else if (button == 0) {
					if (negativeRunnable != null) negativeRunnable.run();
					dialog.dismiss();
				}
			}
			
		});
		
		dialog.addButton("No");
		dialog.addButton("Yes");
		
		return dialog;
	}
	
	public static GDXButtonDialog yesNoBox(String title, String message, String yesText, final Runnable positiveRunnable, String noText, final Runnable negativeRunnable) {
		final GDXButtonDialog dialog = dialogs.newDialog(GDXButtonDialog.class);
		
		dialog.setTitle(title);
		dialog.setMessage(message);
		
		dialog.setClickListener(new ButtonClickListener() {

			@Override
			public void click(int button) {
				if (button == 1) {
					if (positiveRunnable != null) positiveRunnable.run();
					dialog.dismiss();
				} else if (button == 0) {
					if (negativeRunnable != null) negativeRunnable.run();
					dialog.dismiss();
				}
			}
			
		});
		
		dialog.addButton(noText);
		dialog.addButton(yesText);
		
		return dialog;
	}
	
	public static GDXProgressDialog progressDialog(String title, String message) {
		GDXProgressDialog progressDialog = dialogs.newDialog(GDXProgressDialog.class);

		progressDialog.setTitle(title);
		progressDialog.setMessage(message);
		progressDialog.build().show();

		return progressDialog;
	}

	public static GDXTextPrompt textPrompt(String title, String message, String defaultValue, String confirmString, final Lambda confirmFunction, final Lambda cancelFunction) {
		final GDXTextPrompt dialog = dialogs.newDialog(GDXTextPrompt.class);

		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setValue(defaultValue);
		if (confirmString != null) dialog.setConfirmButtonLabel(confirmString);
		else dialog.setConfirmButtonLabel("Confirm");
		dialog.setCancelButtonLabel("Cancel");

		dialog.setTextPromptListener(new TextPromptListener() {

			@Override
			public void confirm(String text) {
				confirmFunction.params.put("Text", text);
				confirmFunction.execute();
				dialog.dismiss();
			}

			@Override
			public void cancel() {
				if (cancelFunction != null) cancelFunction.execute();
				dialog.dismiss();
			}
		});

		return dialog;
	}
}
