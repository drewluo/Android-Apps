package com.example.timeout;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import com.example.timeout.TimerView.TimerChangedListener;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class TOActivity extends Activity implements OnClickListener, TimerChangedListener {

	//TODO remove me
	private String debugString = new String();
	public final String DRIVER_KEY = "DRIVER_KEY";
	public final String RING_KEY = "RING_KEY";
	
	private static final long DEFAULT_WORK_MS = 30*60*1000;
	private static final long DEFAULT_BREAK_MS = 5*60*1000;
	private static final long DEFAULT_SNOOZE_MS = 5*60*1000;

	Ringtone ringTone;
	
	private DriverButton doWork, doBreak;
	private DriverButton doSnooze;

	private TimerView nextWorkTimer;
	private TimerView nextBreakTimer;
	
	Button extendWork, extendBreak, startActivity2;

	StatusView status;

	TextView driverInput;

	
	long elapsedTicks;
	
	TODriver driver;
	
	PendingIntent startMe;
	
	AlarmManager am;


	private TimerView snoozeTimer;

	private TimerView waitingTimer;

	private Stats stats;
		 
	int resumeCount=0;

	private ImageButton powerButton;
	private boolean powerOn;
	private boolean keepRinging = false;


	private final static String POWER_KEY = "POWER_STATE";
	private final static String SNOOZE_KEY = "SNOOZE_DURATION";
	private final static String DEBUG_KEY = "DEBUG_KEY";
	
	private long snoozeTime;
	private com.example.timeout.StatsGraph statsGraph;
	
	public void startRinging() {
		if (!ringTone.isPlaying()) {
			ringTone.play();
		}
	}
	public void stopRinging() {
		ringTone.stop();
	}
	
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		initialize();
		
		performLayout();
		
		findViews();
	
		connectViews();
		
		connectDriver();
	}
	
	@Override
	protected void onPause() {
		
		driver.stall();
		saveTODriverInPrefs();
		//stats.saveStats();
		driver.schedulePendingIntentForNextEventTime(am, startMe);
		//don't use don't keep ringing here because we 
		stopRinging(); 
		//status.stopFlash();
		saveAppState();
		//TODO remove me, for debug only
		//doSomeDebug("ct: " + SystemClock.elapsedRealtime() + "next event time: " + driver.getNextEventTime());
		checkPointStats();
		doSomeDebug("OP:");
		super.onPause();

	}

	private void doSomeDebug (String msg) {
		DebugView dv = (DebugView) findViewById(R.id.debugView);
		if (dv == null)
			return;
		debugString = new String(debugString + "*\n" + msg);
		dv.print(debugString + "kR:" + keepRinging + " DS:b=" + driver.isUserBreaking() + "w=" + driver.isUserWorking());
	}

	@Override
	protected void onResume() {
		super.onResume();
		am.cancel(startMe);
		if (resumeCount == 0) {
			restoreAppState();
			restoreTODriverStateFromPrefs();
		    //driver.unStall();
		}
		
	    resumeCount++;	    
		doSomeDebug("OR:" + resumeCount + "SW:" + stats.getSecondsWorked(System.currentTimeMillis()/Stats.DAY_MS*Stats.DAY_MS) + " SB:" + stats.getSecondsBreaked(System.currentTimeMillis()/Stats.DAY_MS*Stats.DAY_MS));
		driver.unStall();
		checkPointStats();
		updateUI();
	}

	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO Auto-generated method stub
		super.onWindowFocusChanged(hasFocus);
		if (hasFocus) {
			//TODO
			//am.cancel(startMe);
		}

	}

	public void dontKeepRinging () {
		keepRinging = false;
		stopRinging();
	}
	private void saveTODriverInPrefs () {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();  // remember must use same ed object for put and commit, since put values are saved in the instance for commit purposes.
		ed.putString(DRIVER_KEY, driver.save());
		
		ed.commit();
	}

	private void saveAppState () {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor ed = prefs.edit();  // remember must use same ed object for put and commit, since put values are saved in the instance for commit purposes.
		ed.putBoolean(POWER_KEY, powerOn);
		ed.putLong(SNOOZE_KEY, snoozeTime);
		ed.putBoolean(RING_KEY, keepRinging );
		
		ed.putString(DEBUG_KEY, debugString);

		
		ed.commit();
	}
	
	private boolean restoreAppState () {
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		snoozeTime = prefs.getLong(SNOOZE_KEY, DEFAULT_SNOOZE_MS);
		if (snoozeTimer != null) {
			snoozeTimer.setTimeSilently(snoozeTime);
		}
		powerOn = prefs.getBoolean(POWER_KEY, false);

		keepRinging = prefs.getBoolean(RING_KEY, keepRinging);

		debugString = prefs.getString(DEBUG_KEY, "");
		return powerOn;
	}
	private void restoreTODriverStateFromPrefs () {
		String driver_state = getPreferences(MODE_PRIVATE).getString(DRIVER_KEY, "");
		if (!driver_state.equals("")) {
			driver.restore(driver_state);			
		}
	}



	private void initialize() {
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED, WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		elapsedTicks = 0;
		Intent i = new Intent(this, AlarmReceiver.class);
		startMe = PendingIntent.getBroadcast(this,0,i, PendingIntent.FLAG_UPDATE_CURRENT);
		am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		
		
		Uri alertTone;
		alertTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
		if (alertTone == null) {
		    alertTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		}
		if (alertTone == null) {
			return;
		}
		ringTone = RingtoneManager.getRingtone(this, alertTone);
		
		stats = new Stats(this);

	}


	private void performLayout() {
		setContentView(R.layout.activity_to2);
	}


	private void connectDriver() {
		driver = new TODriver() {

			@Override
			protected void onWork(long workDuration) {
				//status.setWorking();
				status.setTimeRemaining(workDuration);
				status.stopFlash();
				stopRinging();
				
				keepRinging = false;
				doSomeDebug("OW");
				updateUI();

			}

			@Override
			protected void onWorkDurationUp(long workedForTime) {
				//status.setWorksUp();
				status.startFlash();
				startRinging();
				keepRinging = true;
				doSomeDebug("Owu");
				updateUI();



			}

			@Override
			protected void onBreak(long breakDuration) {
				//status.setBreaking();
				status.setTimeRemaining(breakDuration);
				status.stopFlash();
				stopRinging();
				keepRinging = false;
				doSomeDebug("OB");
				updateUI();



			}

			@Override
			protected void onBreakDurationUp(long breakedForTime) {
				//status.setBreaksUp();
				status.startFlash();
				startRinging();
				keepRinging = true;
				doSomeDebug("Obu");
				updateUI();

			}

			@Override
			protected void onWorkExtension(long extension) {
				status.setWorking();
				status.stopFlash();
				stopRinging();
				keepRinging = false;
				updateUI();

			}

			@Override
			protected void onBreakExtension(long extension) {
				status.setBreaking();
				status.stopFlash();
				stopRinging();
				keepRinging = false;
				updateUI();

			}

			@Override
			protected void onDurationTick(long durationSoFar, long durationLeft) {
				status.setTimeRemaining(durationLeft);
			}
		};
		
		driver.setNextBreakDuration(DEFAULT_BREAK_MS);
		driver.setNextWorkDuration(DEFAULT_WORK_MS);
	}
	
	private void connectViews() {
		if (doWork != null) {
			doWork.setOnClickListener(this);
		}
		if (doBreak != null) {
			doBreak.setOnClickListener(this);
		}
		if (doSnooze != null) {
			doSnooze.setOnClickListener(this);
		}
		if (extendWork != null) {
			extendWork.setOnClickListener(this);
		}
		if (extendBreak != null) {
			extendBreak.setOnClickListener(this);
		}
		if (startActivity2 != null) {
			startActivity2.setOnClickListener(this);
		}
		if (nextWorkTimer != null) {
			nextWorkTimer.setOnTimerChangedListener(this);
		}
		
		if (nextBreakTimer != null) {
			nextBreakTimer.setOnTimerChangedListener(this);
		}
		
		if (snoozeTimer != null) {
			snoozeTimer.setOnTimerChangedListener(this);
			snoozeTimer.setTimeSilently(DEFAULT_SNOOZE_MS);
		}
		
		if (powerButton != null) {
			powerButton.setOnClickListener(this);
		}
		
		if (status != null) {
			status.setOnClickListener(this);
		}
		
		if (statsGraph != null) {
			statsGraph.setStats(stats);
			//TODO debug only
			long ct = System.currentTimeMillis();
			stats.checkPoint(ct-Stats.DAY_MS*2, 3600000, true);
			stats.checkPoint(ct-Stats.DAY_MS*1, 1800000, true);
			stats.checkPoint(ct, 2400000, true);
			statsGraph.changeGraphParams(ct-Stats.DAY_MS*2, 1, 3);
		}
		nextWorkTimer.setTimeSilently(DEFAULT_WORK_MS);
		nextBreakTimer.setTimeSilently(DEFAULT_BREAK_MS);
		snoozeTime = DEFAULT_SNOOZE_MS;
		status.setTimeRemaining(0);		
	}

	private void updateUI() {
		if (powerOn) {
			powerButton.setImageResource(R.drawable.power_button_on);
		} else {
			//saveTODriverInPrefs();
			powerButton.setImageResource(R.drawable.power_button_off);
			debugString = "";
		}
		
		if (!powerOn) {
			doWork.setEnabled(false);
			doBreak.setEnabled(false);
			doSnooze.setEnabled(false);
			status.turnOff();
		} else if (driver.isUserWorking()) {
			doWork.setEnabled(false);
			doBreak.setEnabled(true);
			doSnooze.setEnabled(true);
			if (driver.overWorkedDuration() > 0) {
				status.setWorksUp();
			} else {
				status.setWorking();
			}
		} else if (driver.isUserBreaking()) {
			doWork.setEnabled(true);
			doBreak.setEnabled(false);
			doSnooze.setEnabled(true);
			if (driver.overBreakedDuration() > 0) {
				status.setBreaksUp();
			} else {
				status.setBreaking();
			}
		} else {
			doWork.setEnabled(true); 
			doBreak.setEnabled(true);
			doSnooze.setEnabled(false);
			status.turnOn();
		}
		nextWorkTimer.setTimeSilently(driver.getNextWorkDuration());
		nextBreakTimer.setTimeSilently(driver.getNextBreakDuration());
		snoozeTimer.setTimeSilently(snoozeTime);
		
		if (keepRinging) {
			startRinging();
			status.startFlash();
		} else {
			stopRinging();
			status.stopFlash();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		super.onActivityResult(requestCode, resultCode, data);

		if (waitingTimer != null) {
			waitingTimer.onActivityResult(requestCode, resultCode, data);
			keepRinging = false;
		}
		
	}

	private void findViews() {
		doWork = (DriverButton)findViewById(R.id.doWork);
		nextWorkTimer = (TimerView)findViewById(R.id.nextWorkTimer);
		doBreak = (DriverButton)findViewById(R.id.doBreak);
		nextBreakTimer = (TimerView)findViewById(R.id.nextBreakTimer);
		doSnooze = (DriverButton)findViewById(R.id.doSnooze1);
		snoozeTimer = (TimerView)findViewById(R.id.snoozeTimer);
		
		status = (StatusView)findViewById(R.id.status);

		powerButton = (ImageButton)findViewById(R.id.power_button);
		
		statsGraph = (StatsGraph)findViewById(R.id.statsView);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.to, menu);
		return true;
	}

	@Override
	public void onClick(View arg0) {
		
		checkPointStats();
		if (arg0 == doWork) {
			
			driver.doWork();
		}
		if (arg0 == doBreak) {
			driver.doBreak();
		}
		
		if (arg0 == doSnooze) {
			if (driver.isUserWorking()) {
				driver.extendWork(snoozeTimer.getTime());
			} else if (driver.isUserBreaking()) {
				driver.extendBreak(snoozeTimer.getTime());
			}
		}
		
		if (arg0 == powerButton) {
			if (powerOn) {
				powerOff();
			} else {
				powerOn();
			}
		}
		
		if (arg0 == status) {
			 // TODO connect the status onclick to turn off alarm 
			keepRinging = false;
			stopRinging();
		}
	}

	private void checkPointStats() {
		//todo debug only
		return;
		/*
		if (driver.isUserBreaking()) {
			stats.checkPoint(System.currentTimeMillis(), TODriver.getCurrentTime()-driver.getStartTime(), false);
		} else if (driver.isUserWorking()) {
			stats.checkPoint(System.currentTimeMillis(), TODriver.getCurrentTime()-driver.getStartTime(), true);
		}
		*/
		
	}
	private void powerOn() {
		powerOn=true;
		updateUI();
	}

	private void powerOff() {
		driver.stop();
		powerOn=false;
		keepRinging = false;
		updateUI();
	}


	@Override
	public void onTimerChanged(TimerView view, long hours, long minutes, long seconds) {
		if (driver == null)
			return;
		
		long ms = seconds*1000+minutes*60000+hours*3600000;
		if (view == nextWorkTimer) {
			driver.setNextWorkDuration(ms);
		}
		
		if (view == nextBreakTimer) {
			driver.setNextBreakDuration(ms);
		}
		
		if (view == snoozeTimer) {
			snoozeTime = ms;
		}
	}

	public void startActivityForResult(TimerView timerView,
			Intent start_picker, int pickTime) {
		waitingTimer = timerView;
		startActivityForResult(start_picker, pickTime);
		
	}
	
	


}
