package com.example.androidtest;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

public class MyService extends Service {
	
	protected MyBinder binder = new MyBinder();

	@Override
	public IBinder onBind(Intent intent) {
		Log.d("test","Service On Bind");
		Log.d("test","Data from caller:" + intent.getStringExtra("data"));
		return binder;
	}
	
	class MyBinder extends Binder {
		
		public MyService getService() {
			return MyService.this;
		}

		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply,
				int flags) throws RemoteException {
			
			Log.d("test","From MyService get data:" + data.readString());
			reply.writeString("ACK!ACK!");
			
			return super.onTransact(code, data, reply, flags);
		}
		
	}

}