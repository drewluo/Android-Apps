package com.example.timeout;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.text.InputType;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;

public class TimerView extends RelativeLayout {

	private TextView label;
	private TextView timeDisplay;
	private EditText timeHours, timeMinutes, timeSeconds;
	private NumberPicker picker;
	private TimerChangedListener timerChangedListener;
	private long hours, minutes, seconds;
	private OnEditorActionListener timeChangedHandler;
	private final static int PICK_TIME = 1;
	static public interface TimerChangedListener {
		public void onTimerChanged (TimerView view, long hours, long minutes, long seconds);
	}
	
	public TimerView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}

	public TimerView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TimerView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		RelativeLayout.inflate(context, R.layout.timer_view_layout, this);

		//label = new TextView(context, attrs);
		

		/*
		label = new TextView(context);
		time = new TextView(context);
		*/
		

		layoutLabel(context, attrs);
		
		layoutTime(context);
		connectTime();
		
		//lp = new RelativeLayout.LayoutParams(context, attrs);
		//this.setLayoutParams(lp);	
	}

	private void layoutLabel(Context context, AttributeSet attrs) {
		String label_text;
		label = (TextView)findViewById(R.id.timer_view_label);
		TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimerView, 0, 0);
		label_text = a.getString(R.styleable.TimerView_timerLabel);
		label.setText(label_text);
		a.recycle();
	}

	private void connectTime() {
		/*
		timeChangedHandler = new OnEditorActionListener () {

			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				sampleTimer();
				onTimerChanged();
				return true;	
			}
			
		};
		
		OnFocusChangeListener focusChangedHandler = new OnFocusChangeListener() {

			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus) {
					sampleTimer();
					onTimerChanged();
				}
			}
			
		};

		timeMinutes.setOnEditorActionListener(timeChangedHandler);
		timeHours.setOnEditorActionListener(timeChangedHandler);
		timeSeconds.setOnEditorActionListener(timeChangedHandler);
		
		timeMinutes.setOnFocusChangeListener(focusChangedHandler);
		timeHours.setOnFocusChangeListener(focusChangedHandler);
		timeSeconds.setOnFocusChangeListener(focusChangedHandler);
		*/
		setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Context ctx = TimerView.this.getContext();
				Intent start_picker = new Intent(ctx, TimePickerActivity.class);
				start_picker.putExtra(TimePickerActivity.HOURS, TimerView.this.hours);
				start_picker.putExtra(TimePickerActivity.MINUTES, TimerView.this.minutes);
				start_picker.putExtra(TimePickerActivity.SECONDS, TimerView.this.seconds);
				((TOActivity) ctx).startActivityForResult(TimerView.this,start_picker,PICK_TIME);
			}
			
		});
		
		
		
	}

	private void normalizeTimer () {
		
		
		long total_seconds = seconds;
		total_seconds += minutes*60;
		total_seconds += hours*3600;
		
		seconds = total_seconds % 60;
		total_seconds -= seconds;
		minutes = total_seconds % 3600;
		total_seconds-=minutes;
		minutes/=60;
		hours = total_seconds/3600;
	}
	private void layoutTime(Context context) {
		/*
		timeHours = (EditText) findViewById(R.id.timer_view_hour);
		timeMinutes = (EditText) findViewById(R.id.timer_view_minute);
		timeSeconds = (EditText) findViewById(R.id.timer_view_second);
		
		
		timeSeconds.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_NORMAL);
		timeHours.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_NORMAL);
		timeMinutes.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_VARIATION_NORMAL);
		*/
		
		timeDisplay = (TextView) findViewById(R.id.timer_view_display);

		
		minutes=0;
		hours=0;
		seconds=0;
		

		syncTimerDisplay();
	}


	public void setTimerLabel (String name) {
		label.setText(name);
	}
	
	private void onTimerChanged () {
		if (timerChangedListener != null)
			timerChangedListener.onTimerChanged(this, hours, minutes, seconds);
	}
	
	public void setOnTimerChangedListener (TimerChangedListener listener) {
		this.timerChangedListener = listener;
	}

	public void setTimeSilently (long ms) {
		seconds = ms /1000;
		minutes = 0;
		hours = 0;
		normalizeTimer();
		syncTimerDisplay();
	}
	private void syncTimerDisplay () {
		if (timeDisplay == null)
			return;
		timeDisplay.setText(String.format("%02d:%02d:%02d",  hours, minutes, seconds));
	}

	public long getTime () {
		
		return timeInMs();
	}
	
	private long timeInMs () {
		long ms=hours*60;
		ms+=minutes;
		ms*=60;
		ms+=seconds;
		ms*=1000;
		return ms;
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PICK_TIME) {
			if (resultCode == Activity.RESULT_OK) {
				hours = data.getLongExtra(TimePickerActivity.HOURS, hours);
				minutes = data.getLongExtra(TimePickerActivity.MINUTES, minutes);
				seconds = data.getLongExtra(TimePickerActivity.SECONDS, seconds);
				normalizeTimer();
				syncTimerDisplay();
				onTimerChanged();

			}
		}
		
	}
}
