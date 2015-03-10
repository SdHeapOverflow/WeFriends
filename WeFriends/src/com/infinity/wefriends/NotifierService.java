package com.infinity.wefriends;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

import com.infinity.utils.HttpRequest;
import com.infinity.wefriends.apis.Messages;
import com.infinity.wefriends.apis.Users;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class NotifierService extends Service {
	
	public static String newMessagesAction = "WEFRIENDS_NEW_MESSAGES";
	
	protected boolean isBound = false;
	protected boolean isFirstlyCreated = true;
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
	
	public ServiceHandler handler = new ServiceHandler();

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (serviceThread == null)
		{
			serviceThread = new RunningThread();
			serviceThread.start();
		}
		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public IBinder onBind(Intent intent) {
		m_context = getApplicationContext();
		notificationManager = (NotificationManager)m_context.getSystemService(Context.NOTIFICATION_SERVICE);
		users = new Users(m_context);
		messagesAPI = new Messages(m_context);
		ContentValues userInfo = users.getCachedUserInfo();
		String token = users.getCachedAccessToken();
		if (userInfo==null || token==null)
			return null;
		accessToken = token;
		wefriendsId = userInfo.getAsString("wefriendsid");
		isBound = true;
		if (isFirstlyCreated)
			this.startService(intent);
		isFirstlyCreated = false;
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
								if (newMessages.size() > 0)
									sendNotification(newMessages);
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
						handler.sendMessage(msg);
						errorMsgSent = true;
					}
					e.printStackTrace();
				}
			} while (connError && (!exitSignal));
			super.run();
		}
	}
	
	protected void sendNotification(List<ContentValues> newMessages) {

		int messageCount = newMessages.size();
		for (int i=0;i<messageCount;i++) {
			ContentValues message = newMessages.get(i);
			Notification notification = new Notification(R.drawable.ic_launcher, message.getAsString("message"), System.currentTimeMillis());
			notificationManager.notify(notificationId, notification);
			messagesAPI.bindNotification(message.getAsString("messageid"), notificationId);
			notificationId++;
		}
		if (isBound) {
			Intent intent = new Intent();
			intent.setAction(newMessagesAction);
			sendBroadcast(intent);
		}
	}
	
	class ServiceHandler extends Handler {
		static public final int SHOWTOAST = 102;
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case SHOWTOAST:
				Toast.makeText(m_context, msg.getData().getString("text"), Toast.LENGTH_SHORT).show();
				break;
			}
			super.handleMessage(msg);
		}
		
	}

}
