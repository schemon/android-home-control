package pusherclient.huvuddator.se.pusherclient;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Created by Simon on 2015-08-22.
 */
public class WidgetProvider extends AppWidgetProvider {

	public void onUpdate(final Context context,final AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.d(WidgetProvider.class.getSimpleName(), "Come on: " +appWidgetIds);
		final int N = appWidgetIds.length;

		// Perform this loop procedure for each App Widget that belongs to this provider
		for (int i=0; i<N; i++) {
			final int appWidgetId = appWidgetIds[i];

			// Create an Intent to launch ExampleActivity
			Intent intent = new Intent(context, MainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

			// Get the layout for the App Widget and attach an on-click listener
			// to the button
			final RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
			views.setOnClickPendingIntent(R.id.content_command, pendingIntent);

			Command.get(new Command.CommandListener() {
				@Override public void onResult(final JSONArray commands) {
					RemoteViews row = null;
					for (int i = 0; i < commands.length(); i++) {
						if (i % 2 == 0) {
							row = new RemoteViews(context.getPackageName(), R.layout.row);
						}

						final JSONObject command = commands.optJSONObject(i);
						RemoteViews button = new RemoteViews(context.getPackageName(), R.layout.button_command);
						button.setTextViewText(R.id.button, command.optString("name"));
						Intent intent = new Intent(context, PusherService.class)
								.putExtra(PusherHandler.NAME, "android")
								.putExtra(PusherHandler.MESSAGE, command.toString());
						button.setOnClickPendingIntent(R.id.button, PendingIntent.getService(context, i, intent, PendingIntent.FLAG_UPDATE_CURRENT));
						row.addView(R.id.row, button);

						if (i % 2 == 1) {
							views.addView(R.id.content_command, row);
						}

					}
					appWidgetManager.updateAppWidget(appWidgetId, views);
				}
			});

			// Tell the AppWidgetManager to perform an update on the current app widget
		}
	}

}
