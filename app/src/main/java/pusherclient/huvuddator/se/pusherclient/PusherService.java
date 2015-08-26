package pusherclient.huvuddator.se.pusherclient;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.RemoteViews;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Simon on 2015-08-21.
 */
public class PusherService extends Service {

	@Nullable @Override public IBinder onBind(Intent intent) {
		return null;
	}

	private static PusherHandler pusherHandler;
	BroadcastReceiver broadcastReceiver;

	public static void send(String name, String message, Context context) {
		context.startService(
				new Intent(context, PusherService.class)
				.putExtra(PusherHandler.NAME, name)
				.putExtra(PusherHandler.MESSAGE, message));
	}

	@Override public void onCreate() {
		super.onCreate();
		pusherHandler = new PusherHandler();

		broadcastReceiver = new BroadcastReceiver() {
			@Override public void onReceive(Context context, Intent intent) {
				if(intent.getAction().equals("connected")) {
				} else if(intent.getAction().equals("disconnected")) {
				} else if(intent.getAction().equals(PusherHandler.INTENT_ACTION_EVENT)) {
					String name = intent.getStringExtra(PusherHandler.NAME);
					String message = intent.getStringExtra(PusherHandler.MESSAGE);
					try {
						String handledMessage = new JSONObject(message).getString("handled");
						sendNotification(handledMessage +" handled.", "Yes it's true. " +name +" did that!");
					} catch (JSONException e) {
						sendNotification(name, message);
					}

					sendBroadcast(new Intent("UPDATE_WIDGET"));

					RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
					ComponentName thisWidget = new ComponentName(context, WidgetProvider.class);
					AppWidgetManager.getInstance(PusherService.this).updateAppWidget(thisWidget, remoteViews);
				}
			}
		};

		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("connected"));
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("disconnected"));
		LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(PusherHandler.INTENT_ACTION_EVENT));

	}

	@Override public int onStartCommand(Intent intent, int flags, int startId) {
		handleIntent(intent);
		return START_NOT_STICKY;
	}

	private void handleIntent(Intent intent) {
		pusherHandler.send(intent.getStringExtra(PusherHandler.NAME), intent.getStringExtra(PusherHandler.MESSAGE), this);
	}

	@Override public void onDestroy() {
		pusherHandler.disconnect();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
		super.onDestroy();
	}

	private void sendNotification(String title, String content) {
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(this)
						.setSmallIcon(R.mipmap.ic_launcher)
						.setContentTitle(title)
						.setContentText(content);
		// Creates an explicit intent for an Activity in your app
		Intent resultIntent = new Intent(this, MainActivity.class);

		// The stack builder object will contain an artificial back stack for the
		// started Activity.
		// This ensures that navigating backward from the Activity leads out of
		// your application to the Home screen.
		TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
		// Adds the back stack for the Intent (but not the Intent itself)
		stackBuilder.addParentStack(MainActivity.class);
		// Adds the Intent that starts the Activity to the top of the stack
		stackBuilder.addNextIntent(resultIntent);
		PendingIntent resultPendingIntent =
				stackBuilder.getPendingIntent(
						0,
						PendingIntent.FLAG_UPDATE_CURRENT
				);
		mBuilder.setContentIntent(resultPendingIntent);
		NotificationManager mNotificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		// mId allows you to update the notification later on.
		mNotificationManager.notify(1337, mBuilder.build());
	}
}
