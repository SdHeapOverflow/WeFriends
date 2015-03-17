package com.infinity.wefriends;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.infinity.utils.HttpRequest;
import com.infinity.wefriends.apis.Messages;
import com.infinity.wefriends.apis.Users;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

public class NotifierService extends Service {
	
	public static final String NEW_MESSAGE_ACTION = "WEFRIENDS_NEW_MESSAGES";
	
	static public final int SEND_NOTIFICATION = 100;
	static public final int SHOW_TOAST = 102;
	static public final int QUIT_SERVICE = 103;
	
	
	protected boolean isBound = false;
	protected RunningThread serviceThread = null;
	protected Socket socket = null;
	protected Context m_context = null;
	protected String wefriendsId = "";
	protected String accessToken = "";
	protected boolean exitSignal = false;
	protected boolean errorMsgSent = false;
	protected Users users = null;
	protected Messages messagesAPI = null;
	protected NotificationManager notificationManager = null;
	protected int notificationId = 0;
	
	protected NotifierServiceReceiver receiver = null;
	
	protected boolean isReceiverRegistered = false;
	
	public ServiceHandler handler = new ServiceHandler();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//Prevent service being killed.
		flags = Service.START_REDELIVER_INTENT;
		
		m_context = getApplicationContext();
		notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
		users = new Users(m_context);
		messagesAPI = new Messages(m_context);
		ContentValues userInfo = users.getCachedUserInfo();
		String token = users.getCachedAccessToken();
		if (userInfo==null || token==null)
			return super.onStartCommand(intent, flags, startId);
		accessToken = token;
		wefriendsId = userInfo.getAsString("wefriendsid");
		if (serviceThread == null) {
			serviceThread = new RunningThread();
			serviceThread.start();
		}
		receiver = new NotifierServiceReceiver(this);
		registerReceiver(receiver, new IntentFilter("WEFRIENDS_RELOGIN"));
		isReceiverRegistered = true;
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		isBound = true;
		this.startService(intent);
		return null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		isBound = false;
		return super.onUnbind(intent);
	}
	
	class RunningThread extends Thread {
		@Override
		public void run() {
			Log.d("WeFriends","Notifier Service running");
			boolean connError;
			byte[] buffer = new byte[10];
			int byteReceived = 0;
			String receivedStr = "";
			List<ContentValues> newMessages = null;
			do {
				connError = false;
				try {
					socket = new Socket(m_context.getString(R.string.server_host),Integer.parseInt(m_context.getString(R.string.server_notifier_port)));
					OutputStream output = socket.getOutputStream();
					InputStream input = socket.getInputStream();
					output.write(wefriendsId.getBytes());
					while (true) {
						if (exitSignal)
							break;
						if ((byteReceived = input.read(buffer)) <0) {
							throw new IOException();
						}
						errorMsgSent = false;
						receivedStr = new String(buffer, 0, byteReceived);
						output.write("0".getBytes());
						
						if (receivedStr.equals("1")) {
							Log.d("WeFriends","New Message Found");
							newMessages = messagesAPI.getAndSaveNewMessages();
							if (newMessages != null)
								if (newMessages.size() > 0) {
									Bundle bundle = new Bundle();
									ArrayList list = new ArrayList();
									list.add(newMessages);
									bundle.putParcelableArrayList("messages", list);
									Message msg = new Message();
									msg.setData(bundle);
									msg.what = SEND_NOTIFICATION;
									NotifierService.this.handler.sendMessage(msg);
								}
						}
					}
					
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				} catch (IOException e) {
					/*Network Error OR exit signal is sent.*/
					connError = true;
					if ((!errorMsgSent) && (!exitSignal)) {
						Bundle msgBundle = new Bundle();
						msgBundle.putString("text", "NotifierService:" + m_context.getString(R.string.connection_error));
						Message msg = new Message();
						msg.setData(msgBundle);
						msg.what=NotifierService.SHOW_TOAST;
						handler.sendMessage(msg);
						errorMsgSent = true;
					}
					e.printStackTrace();
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			} while (connError && (!exitSignal));
			serviceThread = null;
			super.run();
		}
	}
	
	protected void sendNotification(List<ContentValues> newMessages) {

		int messageCount = newMessages.size();
		for (int i=0;i<messageCount;i++) {
			ContentValues message = newMessages.get(i);
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.ic_launcher);
			if (message.getAsString("chatgroup").equals("")) {
				builder.setContentTitle(message.getAsString("sendernickname") + m_context.getString(R.string.notification_new_im));
				if (message.getAsString("messagetype").equals(Messages.MESSAGE_TEXT))
					builder.setContentText(Html.fromHtml(message.getAsString("message")).toString());
			} else {
				builder.setContentTitle(message.getAsString("chatgroup"));
				if (message.getAsString("messagetype").equals(Messages.MESSAGE_TEXT))
					builder.setContentText(message.getAsString("sendernickname") + " : " + Html.fromHtml(message.getAsString("message")).toString());
			}
			Intent intent = new Intent();
			intent.setClass(NotifierService.this, MainActivity.class);
			PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
			builder.setContentIntent(pendingIntent);
			Notification notification = builder.build();
			notification.defaults |= Notification.DEFAULT_SOUND;
			notification.defaults |= Notification.DEFAULT_VIBRATE;
			
			notificationId = new Random().nextInt(9999999);
			notificationManager.notify(notificationId,notification);
			
			messagesAPI.bindNotification(message.getAsString("messageid"), notificationId);
		}

		Intent intent = new Intent();
		intent.setAction(NEW_MESSAGE_ACTION);
		sendBroadcast(intent);
		
	}
	
	class ServiceHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOW_TOAST:
				Toast.makeText(m_context, msg.getData().getString("text"), Toast.LENGTH_SHORT).show();
				break;
			case SEND_NOTIFICATION:
				sendNotification((List<ContentValues>)(msg.getData().getParcelableArrayList("messages").get(0)));
				break;
			case QUIT_SERVICE:
				if (isReceiverRegistered) {
					//Must use try case otherwise throws exception noting receiver
					//not registered. Reason unknown.
					try {
						unregisterReceiver(receiver);
					} catch (IllegalArgumentException e) {
						
					}
				}
				exitSignal = true;
				Log.d("WeFriends","Notifier service exiting.");
				NotifierService.this.stopSelf();
				break;
			}
			super.handleMessage(msg);
		}
		
	}

}
