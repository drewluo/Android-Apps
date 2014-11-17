package com.example.timeout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class TimePickerActivity extends Activity {

	final static String HOURS = "1";
	final static String MINUTES = "2";
	final static String SECONDS = "3";
	
	EditText hours, minutes, seconds;
	Button ok, cancel;
	
	long h,m,s;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.timer_picker_layout);
		
		hours = (EditText) findViewById(R.id.timer_picker_layout_hours_input);
		minutes = (EditText) findViewById(R.id.timer_picker_layout_minutes_input);
		seconds = (EditText) findViewById(R.id.timer_picker_layout_seconds_input);

		ok = (Button) findViewById(R.id.timer_picker_layout_ok);
		cancel = (Button) findViewById(R.id.timer_picker_layout_cancel);
		
		Intent my_intent = this.getIntent();
		h=my_intent.getLongExtra(HOURS, 0);
		m=my_intent.getLongExtra(MINUTES, 0);
		s=my_intent.getLongExtra(SECONDS, 0);
		
		hours.setText(h+"");
		minutes.setText(m+"");
		seconds.setText(s+"");
		
		ok.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				doOK();
			}
			
		});
		
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				doCancel();
			}
		});
	}
	
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		doCancel();

	}

	private void doOK() {
		try {
			h = Long.parseLong(hours.getText().toString());
		} catch (Exception e) {
			h=0;
		}
		try {

		m = Long.parseLong(minutes.getText().toString());
		} catch (Exception e) {
			m=0;
		}
		try {

		s = Long.parseLong(seconds.getText().toString());
		} catch (Exception e) {
			s=0;
		}
		Intent timeIntent = new Intent();
		
		timeIntent.putExtra(HOURS, h);
		timeIntent.putExtra(MINUTES, m);
		timeIntent.putExtra(SECONDS, s);
		
		setResult(RESULT_OK, timeIntent);
		finish();
	}
	
	private void doCancel() {
		Intent timeIntent = new Intent();
		setResult(RESULT_CANCELED, timeIntent);
		finish();
	}


}
