package pusherclient.huvuddator.se.pusherclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;
import java.util.Scanner;


public class MainActivity extends AppCompatActivity {

    BroadcastReceiver broadcastReceiver;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Command.get(new Command.CommandListener() {
			@Override public void onResult(final JSONArray commands) {
				runOnUiThread(new Runnable() {
					@Override public void run() {
						View view = Command.createCommandControlViews(commands, MainActivity.this);
						((ViewGroup) findViewById(R.id.content_command)).addView(view);
					}
				});
			}
		});

		broadcastReceiver = new BroadcastReceiver() {
			@Override public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals("connected")) {
					updateChat("bot", "connected");
				} else if(intent.getAction().equals("disconnected")) {
					updateChat("bot", "disconnected");
				} else if(intent.getAction().equals(PusherHandler.INTENT_ACTION_EVENT)) {
					String name = intent.getStringExtra(PusherHandler.NAME);
					String message = intent.getStringExtra(PusherHandler.MESSAGE);
					try {
						String handledMessage = new JSONObject(message).getString("handled");
						updateChat("bot", name + " performed " + handledMessage);
						final Button button = (Button) findViewById(R.id.content_command).findViewWithTag(handledMessage);
						if(button != null) {
							Animation anim = new AlphaAnimation(1f, 0f);
							anim.setDuration(100);
							anim.setAnimationListener(new Animation.AnimationListener() {
								@Override public void onAnimationStart(Animation animation) {

								}

								@Override public void onAnimationEnd(Animation animation) {
									Animation anim = new AlphaAnimation(0f, 1f);
									anim.setDuration(100);
									button.startAnimation(anim);
								}

								@Override public void onAnimationRepeat(Animation animation) {

								}
							});
							button.startAnimation(anim);
						}
					} catch (JSONException e) {
						updateChat(name, message);
					}
				}
			}
		};

		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("connected"));
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("disconnected"));
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(PusherHandler.INTENT_ACTION_EVENT));

		((Button) findViewById(R.id.button_send)).setOnClickListener(new View.OnClickListener() {
			@Override public void onClick(View v) {
				try {
					EditText editText = (EditText) findViewById(R.id.edit_text_message);
					if (!editText.getText().toString().equals("")) {
						String name = "android";
						String message = editText.getText().toString();
						updateChat(name, message);
						try {
							PusherService.send(name, message, MainActivity.this);
						} catch (Exception e) {
							updateChat("bot", "" + e);
						}
						editText.setText("");
					}
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	@Override protected void onDestroy() {
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}

	private void updateChat(String name, String message) {
		try {
			updateChat(new JSONObject().put("name", name).put("message", message));
		} catch (JSONException e) {
			// Ooppss
		}
	}

	private void updateChat(final JSONObject data) {
		runOnUiThread(new Runnable() {
			@Override public void run() {
				TextView textViewChat = (TextView) findViewById(R.id.text_view_chat);
				textViewChat.append("\n" + data.optString("name") +"> " + data.optString("message"));
				final ScrollView scroll = (ScrollView) findViewById(R.id.scroll_view_chat);
				scroll.post(new Runnable() {
					@Override
					public void run() {
						scroll.fullScroll(View.FOCUS_DOWN);
					}
				});
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
