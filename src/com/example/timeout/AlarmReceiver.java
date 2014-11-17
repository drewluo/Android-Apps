package com.example.timeout;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.view.WindowManager;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

	public AlarmReceiver() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction()!= null &&
			intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
			Toast toast = Toast.makeText(context, new String("Got shutdown"), Toast.LENGTH_SHORT);
			toast.show();
			return;
		}
		Intent wakeUpTOActivity = new Intent();
		wakeUpTOActivity.setClass(context, TOActivity.class);
		wakeUpTOActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_USER_ACTION);
		//wakeUpTOActivity.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		makeSureScreenIsOn(context);
		context.startActivity(wakeUpTOActivity);
	}
	
	
	@SuppressLint("Wakelock")
	private void makeSureScreenIsOn(Context context) {
	    PowerManager.WakeLock wl = ((PowerManager)context.getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.FULL_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP|PowerManager.ON_AFTER_RELEASE, "asdf");
	    wl.acquire(5000);
	    //wl.release();
	}

}
