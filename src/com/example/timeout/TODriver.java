package com.example.timeout;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.os.Handler;
import android.os.SystemClock;

public class TODriver {

	private static final long SCHEDULER_GRAIN = 100;
	private static final int BREAKING=1;
	private static final int WORKING=2;
	private static final int UNKNOWN=3;
	
	/* v restore-able data v */
	private int userState;
	
	private long startTime;
	
	private long workDuration;
	private long breakDuration;
	
	private long nextWorkDuration;
	private long nextBreakDuration;
	
	
	private long tickInterval;
	private boolean isDurationUpOutstanding;
	private final int version = 1;
    /* ^ restore-able data ^ */
	
	private Handler handler;
	Runnable workTick;
	
	private static final String KEY_USER_STATE_1="user_state";
	private static final String KEY_START_TIME_1="start_time";
	private static final String KEY_WORK_DURATION_1="work_duration";
	private static final String KEY_BREAK_DURATION_1="break_duration";
	private static final String KEY_NEXT_WORK_DURATION_1="next_work_duration";
	private static final String KEY_NEXT_BREAK_DURATION_1="next_break_duration";
	private static final String KEY_TICK_INTERVAL_1="tick_interval";
	private static final String KEY_IS_DURATION_UP_OUTSTANDING_1="is_duration_up_out";
	

	private final String KEY_VERSION = "0";
	
	public TODriver () {
		userState = UNKNOWN;
		workDuration = 0;		
		isDurationUpOutstanding = false;
		tickInterval = 1000;
	}
	
	public void setNextWorkDuration (long workDuration) {
		this.nextWorkDuration = workDuration;
	}
	public void setNextBreakDuration (long breakDuration) {
		this.nextBreakDuration = breakDuration;
	}

	public long getNextWorkDuration () {
		return nextWorkDuration;
	}
	public long getNextBreakDuration () {
		return nextBreakDuration;
	}

	public long getWorkDuration () {
		return workDuration;
	}
	
	public long getBreakDuration () {
		return breakDuration;
	}
	
	
	
	public void doWork () {
		if (userState != WORKING) {
			workDuration = nextWorkDuration;
			userState = WORKING;
			startTime = getCurrentTime();
			isDurationUpOutstanding = true;
			onWork(workDuration);
			scheduleWorkCallbacks ();
		}
	}



	public void doBreak () {
		if (userState != BREAKING) {
			breakDuration = nextBreakDuration;
			userState = BREAKING;
			startTime = getCurrentTime();
			isDurationUpOutstanding = true;
			onBreak(breakDuration);
			scheduleBreakCallbacks ();
			
		}		
	}

	public void extendWork (long extension) {
		if (!isUserWorking())
			return;
		workDuration += overWorkedDuration() + extension;
		if (!isDurationUpOutstanding) {
			isDurationUpOutstanding = true;
		}
		onWorkExtension(extension);
	}

	public void extendBreak (long extension) {
		if (!isUserBreaking())
			return;
		breakDuration += overBreakedDuration() + extension;
		if (!isDurationUpOutstanding) {
			isDurationUpOutstanding = true;
		}
		onBreakExtension(extension);
	}

	public boolean isUserWorking() {
		return (userState == WORKING);
	}

	public boolean isUserBreaking() {
		return (userState == BREAKING);
	}

	public long overWorkedDuration() {
		if (!isUserWorking())
			return 0;
		long overDuration = getDurationSinceStart() - workDuration;
		if (overDuration < 0)
			overDuration = 0;
		if (overDuration == 0 && isDurationUpOutstanding)
			return 1;
		return overDuration;
	}

	public long overBreakedDuration() {
		if (!isUserBreaking())
			return 0;
		long overDuration = getDurationSinceStart() - breakDuration;
		if (overDuration < 0)
			overDuration = 0;
		if (overDuration == 0 && isDurationUpOutstanding)
			overDuration = 1;
		return overDuration;
	}

	public void stall () {
		cancelCallbacks();
	}

	public void unStall () {
		if (isUserWorking()) {
			//workDuration+=overWorkedDuration();
			scheduleWorkCallbacks();
		} else if (isUserBreaking()) {
			//breakDuration+=overBreakedDuration();
			scheduleBreakCallbacks();
		}
	}

	public String save () throws Error {
		JSONObject jsonWriter = new JSONObject();
		
		try {
			jsonWriter.put(KEY_VERSION, version);

			jsonWriter.put(KEY_START_TIME_1, startTime);

			jsonWriter.put(KEY_USER_STATE_1, userState);

			jsonWriter.put(KEY_NEXT_WORK_DURATION_1, nextWorkDuration);
			jsonWriter.put(KEY_NEXT_BREAK_DURATION_1, nextBreakDuration);

			jsonWriter.put(KEY_WORK_DURATION_1, workDuration);
			jsonWriter.put(KEY_BREAK_DURATION_1, breakDuration);
			
			jsonWriter.put(KEY_TICK_INTERVAL_1, tickInterval);
			
			jsonWriter.put(KEY_IS_DURATION_UP_OUTSTANDING_1, isDurationUpOutstanding);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw(new Error(e));
		}
		return jsonWriter.toString();
	}
	
	public void restore (String representation) {
		try {

			JSONObject jsonReader = new JSONObject(representation);
			startTime = jsonReader.optLong(KEY_START_TIME_1);

			userState = jsonReader.optInt(KEY_USER_STATE_1);

			nextWorkDuration = jsonReader.optLong(KEY_NEXT_WORK_DURATION_1);
			nextBreakDuration = jsonReader.optLong(KEY_NEXT_BREAK_DURATION_1);

			workDuration = jsonReader.optLong(KEY_WORK_DURATION_1);
			breakDuration = jsonReader.optLong(KEY_BREAK_DURATION_1);
			
			//tickInterval = jsonReader.optLong(KEY_TICK_INTERVAL_1);
			
			isDurationUpOutstanding = jsonReader.optBoolean(KEY_IS_DURATION_UP_OUTSTANDING_1);
		} catch (JSONException e) {
			throw new Error(e);
		}


	}
	
	public void stop () {
		cancelCallbacks();
		userState=UNKNOWN;
	}
	public long getNextEventTime() {
		if (!isDurationUpOutstanding) {
			return -1;
		}
		if (userState == WORKING) {
			return startTime + workDuration;
		} else if (userState == BREAKING) {
			return startTime + breakDuration;
		} else {
			return -1;
		}
		
	}

	public void schedulePendingIntentForNextEventTime(AlarmManager am, PendingIntent pi) {
		long nextEventTime = getNextEventTime();
		if (nextEventTime != -1) {
			am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextEventTime, pi);
		}
	}
	private void scheduleWorkCallbacks() {
		cancelCallbacks();
		handler = new Handler();
		workTick = new Runnable () {
			long nextTickTime = getCurrentTime();
			@Override
			public void run() {
				
				onDurationTick(getDurationSinceStart());
				onDurationTick(getDurationSinceStart(), getDurationLeft());
				nextTickTime += TODriver.this.tickInterval;

				if (isTimeToCallDurationUp()) {

				    onWorkDurationUp(getDurationSinceStart());
				    isDurationUpOutstanding = false;
				}
				
				long orig_delay = nextTickTime - TODriver.this.getCurrentTime();
				long delay = orig_delay;
				if (isDurationUpOutstanding && (TODriver.this.getCurrentTime() + delay > TODriver.this
						.getStartTime() + getWorkDuration())) {


					delay = getStartTime() + getWorkDuration()
							- TODriver.this.getCurrentTime();
					if (delay < 0) {
						delay = 0;
					}
					nextTickTime -= orig_delay - delay;
				}


				handler.postDelayed(workTick, delay);
			}
		}; 
		handler.post(workTick);
	}

	private void scheduleBreakCallbacks() {
		cancelCallbacks();
		handler = new Handler();
		workTick = new Runnable () {
	
			long nextTickTime = getCurrentTime();
			@Override
			public void run() {
				
				onDurationTick(getDurationSinceStart());
				onDurationTick(getDurationSinceStart(), getDurationLeft());
				nextTickTime += TODriver.this.tickInterval;


				if (isTimeToCallDurationUp()) {

				    onBreakDurationUp(getDurationSinceStart());
					isDurationUpOutstanding=false;
				}
				long orig_delay = nextTickTime - TODriver.this.getCurrentTime();
				long delay = orig_delay;
				if (isDurationUpOutstanding && TODriver.this.getCurrentTime() + delay > TODriver.this
						.getStartTime() + getBreakDuration()) {

					delay = getStartTime() + getBreakDuration()
							- TODriver.this.getCurrentTime();
					if (delay < 0 ) {
						delay = 0;
					}
					nextTickTime -= orig_delay - delay;
				}

				handler.postDelayed(workTick, delay);
			}
		};
		
		handler.post(workTick);
	}

	private long getDurationLeft() {
		long duration_left =0;
		if (isUserWorking()) {
			duration_left = getWorkDuration() - getDurationSinceStart();
		} else if (isUserBreaking()) {
			duration_left = getBreakDuration() - getDurationSinceStart();
		}
		if (duration_left < 0) {
			duration_left = 0;
		}
		return duration_left;
	}
	
	private void cancelCallbacks() {
		if (handler == null) {
			return;
		}
		handler.removeCallbacks(workTick);
		handler=null;
	}

	private boolean isTimeToCallDurationUp () {
		long duration;
		if (!isDurationUpOutstanding) {
			return false;
		}
		if (userState == WORKING) {
			duration = workDuration;
		} else if (userState == BREAKING) {
			duration = breakDuration;
		} else {
			throw new Error("isDurationUpOustanding but userState is:" + userState);
		}
		
		return (getDurationSinceStart() + SCHEDULER_GRAIN>= duration);
	}
	long getStartTime () {
		return startTime;
		
	}
	public long getStartTimeUTC () {
		//System.currentTimeMillis() - ()
		return 0;

	}
	static public long getCurrentTime() {
		return SystemClock.elapsedRealtime();
	}

	private long getDurationSinceStart () {
		return getCurrentTime() - startTime;
	}
	
	protected void onWork(long workDuration) {}
	protected void onWorkDurationUp (long workedForTime) {}
	protected void onBreak (long breakDuration) {}
	protected void onBreakDurationUp (long breakedForTime) {}
	protected void onWorkExtension(long extension) {}
	protected void onBreakExtension(long extension) {}
	protected void onDurationTick(long durationSoFar) {}
	protected void onDurationTick(long durationSoFar, long durationRemaining) {}

}
