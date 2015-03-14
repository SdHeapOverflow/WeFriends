package com.infinity.wefriends;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotifierServiceReceiver extends BroadcastReceiver {
	
	protected NotifierService notifierService = null;
	
	public NotifierServiceReceiver(NotifierService service) {
		notifierService = service;
	}

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		notifierService.handler.sendEmptyMessage(NotifierService.QUIT_SERVICE);
	}

}