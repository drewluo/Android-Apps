package com.example.timeout;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class DebugView extends TextView {

	public DebugView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
	}

	public DebugView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
	}

	public DebugView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
	}


	public void print(String msg) {
		this.setText(msg);
	}

}
