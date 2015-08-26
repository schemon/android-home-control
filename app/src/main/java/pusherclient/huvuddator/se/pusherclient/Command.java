package pusherclient.huvuddator.se.pusherclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URL;
import java.util.Scanner;

/**
 * Created by Simon on 2015-08-22.
 */
public class Command {

	private final static String GOOGLE_DRIVE_COMMAND_FILE = "0B2JjHFhi0l1EczhkOVB3U2VWZUE";

	public interface CommandListener {
		public void onResult(JSONArray commands);
	}

	public static void get(final CommandListener commandListener) {
		new Thread(new Runnable() {
			@Override public void run() {
				try {
					Scanner sc = new Scanner(
							new URL("https://drive.google.com/uc?export=download&id=" + GOOGLE_DRIVE_COMMAND_FILE)
									.openConnection().getInputStream());
					String jsonString = "";
					while (sc.hasNext()) {
						jsonString += sc.nextLine();
					}
					final JSONArray jsonArray = new JSONArray(jsonString);
					commandListener.onResult(jsonArray);
				} catch (Exception e) {
				}
			}
		}).start();
	}

	public static View createCommandControlViews(JSONArray commands, final Context context) {
		ViewGroup contentHolder = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.widget, null);
		LinearLayout buttonHolder = null;
		for(int i = 0; i < commands.length(); i++) {
			if(i%2 == 0) {
				buttonHolder = new LinearLayout(context);
			}

			Button button = new Button(context);
			final JSONObject command = commands.optJSONObject(i);
			String commandName = command.optString("name");
			button.setText(commandName);
			button.setTag(commandName);
			button.setOnClickListener(new View.OnClickListener() {
				@Override public void onClick(View v) {
					PusherService.send("android", command.toString(), context);
				}
			});
			buttonHolder.addView(button);

			if(i%2 == 1) {
				contentHolder.addView(buttonHolder);
			}
		}
		return contentHolder;
	}
}
