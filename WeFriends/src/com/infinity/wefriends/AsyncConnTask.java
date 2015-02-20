package com.infinity.wefriends;

import com.infinity.wefriends.apis.Users;

import android.content.Context;
import android.content.Intent;

public class AsyncConnTask {
	Users users = null;
	Context m_context = null;
	
	public AsyncConnTask(Context context) {
		m_context = context;
		users = new Users(context);
	}
	
	public void initCheckUserInfo() {
		Thread thread = new initUserCheckThread();
		thread.start();
	}
	
	protected class initUserCheckThread extends Thread {
		
		@Override
		public void run() {
			if (users.authenticateCachedAccessToken()==Users.TOKEN_INVALID) {
				Intent intent = new Intent();
				intent.setClass(AsyncConnTask.this.m_context,LoginActivity.class);
				AsyncConnTask.this.m_context.startActivity(intent);
				((MainActivity)AsyncConnTask.this.m_context).finish();
			} else {
				((MainActivity)AsyncConnTask.this.m_context).loadAllData();
			}
			super.run();
		}
		
	}

}
