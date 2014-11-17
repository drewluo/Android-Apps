package com.example.timeout;

import java.util.Calendar;
import java.util.Date;

import android.content.Context;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View.OnClickListener;

public class StatusView extends RelativeLayout implements OnClickListener {

	TextView status;
	TextView timer;
	TextView clock;
	private Handler flashHandler;
	private Runnable flasher;
	private boolean stopFlash;
	private Handler timerHandler;
	private Runnable timerRunner;
	private long currentTime;
	private boolean isFlashing=false;
	
	OnClickListener onClick;
	public StatusView(Context context) {
		this(context, null);
		// TODO Auto-generated constructor stub
	}

	public StatusView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public StatusView(Context context, AttributeSet attrs, int defStyle) {
		
		super(context, attrs, defStyle);
		
		LayoutInflater.from(context).inflate(R.layout.layout_status_view, this);
		status = (TextView) findViewById(R.id.statusview_status);
		timer = (TextView) findViewById(R.id.statusview_time_remaining);
		clock = (TextView) findViewById(R.id.statusview_clock);
		status.setText("");
		timer.setText("");

		
		
		timerHandler = new Handler();
		timerRunner = new Runnable () {

			int hours;
			int minutes;
			Calendar cal;
			public void run() {
				cal = Calendar.getInstance();
				hours = cal.get(Calendar.HOUR);
				minutes = cal.get(Calendar.MINUTE);

				if (hours == 0) {
					hours = 12;
				}
				if (cal.get(Calendar.HOUR_OF_DAY)>=12) {
				    clock.setText(String.format("%02d:%02d PM", hours, minutes));
				} else {
				    clock.setText(String.format("%02d:%02d AM", hours, minutes));
				}
				timerHandler.postDelayed(timerRunner, 1000);
			}
				
		};
		
	    timerHandler.postDelayed(timerRunner, 1000);
	}
	
	public void setWorking () {
		status.setText(getResources().getText(R.string.status_work));
	}
	
	public void setBreaking () {
		status.setText(getResources().getText(R.string.status_break));
	}
	public void setWorksUp () {
		status.setText(getResources().getText(R.string.status_worksup));
	}
	public void setBreaksUp () {
		status.setText(getResources().getText(R.string.status_breaksup));
	}
	
	public void setTimeRemaining (long hours, long minutes, long seconds) {
		timer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
	}
	
	public void setTimeRemaining (long ms) {
		long seconds = ms /1000;
		long minutes = seconds / 60;
		long hours = seconds / 3600;
		minutes -= hours * 60;
		seconds -= (hours*3600) + (minutes*60);
		setTimeRemaining(hours, minutes, seconds);
	}
	public void startFlash () {
		if (isFlashing)
			return;
		
		isFlashing=true;
		
		if (flashHandler == null) {
			flashHandler = new Handler();
			flasher = new Runnable () {
				
				int on;
				@Override
				public void run() {
					if (!isFlashing) {
						return;
					}
					if (on == 1) {
						setAlert();
						on = 0;
					} else {
						clearAlert();
						on = 1;
					}		
					flashHandler.postDelayed(flasher, 500);
				}
				
			};
		}

		flasher.run();
	}
	
	public void stopFlash () {

		if (flashHandler != null) {
			flashHandler.removeCallbacks(flasher);
		}
		clearAlert();
		isFlashing = false;
	}
	
	public void turnOff () {
		setTimeRemaining(0);
		stopFlash();
		status.setText(getResources().getText(R.string.status_off));
	}
	public void turnOn () {
		status.setText(getResources().getText(R.string.status_ready));
	}

	private void setAlert() {

		timer.setBackgroundColor(0xffffafaf);
	}
	
	private void clearAlert() {
		timer.setBackgroundColor(Color.WHITE);
	}

	@Override
	public void onClick(View v) {
		if (onClick != null) {
			onClick.onClick(this);
		}
	}
	/*
	public void setOnClickListener(OnClickListener l) {
		onClick = l;
	}
*/
}
