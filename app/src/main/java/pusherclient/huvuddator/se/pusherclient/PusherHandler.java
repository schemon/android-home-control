package pusherclient.huvuddator.se.pusherclient;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.pusher.client.AuthorizationFailureException;
import com.pusher.client.Authorizer;
import com.pusher.client.Pusher;
import com.pusher.client.PusherOptions;
import com.pusher.client.channel.PrivateChannel;
import com.pusher.client.channel.PrivateChannelEventListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by Simon on 2015-08-20.
 */
public class PusherHandler {

	public static final String NAME = "name";
	public static final String MESSAGE = "message";
	public static final String INTENT_ACTION_EVENT = "event";

	private static final String EVENT = "client-talk";
	private static final String CHANNEL = "private-rpi";

	private static final String YOUR_APP_KEY = "599cb5ed77cd5efb659a";
	private static final String SECRET = "76a57ea82e311bcbbb1f";

	private PrivateChannel channel;
	private Pusher pusher;

	public void disconnect() {
		if(pusher != null) {
			pusher.disconnect();
		}
	}

	public void send(final String name, final String message, Context context) {
		init(context, new OnConnectedListener() {
			@Override public void onConnected() {
				try {
					channel.trigger(EVENT, new JSONObject().put(NAME, name).put(MESSAGE, message).toString());
				} catch (JSONException e) {
				}
			}
		});

	}

	public interface OnConnectedListener {
		public void onConnected();
	}

	public void init(final Context context, final OnConnectedListener onConnectedListener) {
		if(pusher == null || pusher.getConnection().getState().equals(ConnectionState.DISCONNECTED)) {
			// yes go ahead and init
		} else {
			onConnectedListener.onConnected();
			return;
		}

		PusherOptions options = new PusherOptions().setAuthorizer(new Authorizer() {
			@Override public String authorize(String channelName, String socketId) throws AuthorizationFailureException {
				String auth = "";
				String secret = SECRET;
				String stringToSign = socketId +":" +channelName;
				try {

					SecretKeySpec keySpec = new SecretKeySpec(
							secret.getBytes(),
							"HmacSHA256");

					Mac mac = Mac.getInstance("HmacSHA256");
					mac.init(keySpec);
					mac.update(stringToSign.getBytes());
					String digest = toHex(mac.doFinal());
					auth = YOUR_APP_KEY +":" +digest;
					return new JSONObject().put("auth", auth).toString();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		pusher = new Pusher(YOUR_APP_KEY, options);


		pusher.connect(new ConnectionEventListener() {
			@Override
			public void onConnectionStateChange(ConnectionStateChange change) {
				System.out.println("State changed to " + change.getCurrentState() +
						" from " + change.getPreviousState());
				if(change.getCurrentState().equals(ConnectionState.DISCONNECTED)) {
					LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("disconnected"));
				}

			}

			@Override
			public void onError(String message, String code, Exception e) {
				LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("error"));
				System.out.println("There was a problem connecting! " + message +" " +code +" " +e);
			}
		});

		subscribeToPrivateChannel(CHANNEL, context, onConnectedListener);
	}


	private static String toHex(byte[] arg) throws UnsupportedEncodingException {
		return String.format("%040x", new BigInteger(1, arg));
	}


	private void subscribeToPrivateChannel(final String channelName, final Context context, final OnConnectedListener onConnectedListener) {
		channel = pusher.subscribePrivate(channelName,
				new PrivateChannelEventListener() {

					@Override public void onAuthenticationFailure(String message, Exception e) {
						LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("disconnected"));
						System.out.println("Failed! " + message + " " + e.getLocalizedMessage());
					}

					@Override public void onEvent(String channelName, String eventName, String data) {
						System.out.println("Event! " + channelName + " " + eventName + " " + data);

						try {
							JSONObject jsonData = new JSONObject(data);
							Intent intent = new Intent(INTENT_ACTION_EVENT);
							intent.putExtra(NAME, jsonData.optString(NAME));
							intent.putExtra(MESSAGE, jsonData.optString(MESSAGE));
							LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
						} catch (JSONException e) {

						}
					}

					@Override public void onSubscriptionSucceeded(String channelName) {
						System.out.println("Subscribed! " + channelName);
						LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent("connected"));
						onConnectedListener.onConnected();
					}

					// Other ChannelEventListener methods
				}, EVENT);

	}

}
