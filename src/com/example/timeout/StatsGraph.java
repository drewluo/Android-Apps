package com.example.timeout;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.view.View;

public class StatsGraph extends View {

	Canvas mCanvas;
	long[] mDates;
	float[] mXvalues;
	private Stats mStats;
	private long mStartDate;
	private long mDaysPerPoint;
	private long mNumPoints;
	private Bitmap mBackBuffer;
	private Paint workPaint;
	boolean init_done = false;
	
	public StatsGraph(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public StatsGraph(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}

	public StatsGraph(Context context) {
		super(context);
		init();
	}

	void init() {
		if (init_done)
			return;
		workPaint = new Paint();
		workPaint.setColor(Color.BLACK);
		workPaint.setStrokeWidth(0);
		workPaint.setStyle(Style.STROKE);
		init_done=true;
	}
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		super.onDraw(canvas);
		canvas.drawColor(Color.RED);
		mCanvas = canvas;
		plotGraph();
	}


	void setStats (Stats s) {
		mStats = s;
	}
	void changeGraphParams (long startDate, long daysPerPoint, long numPoints) {
		
		mStartDate = startDate/Stats.DAY_MS*Stats.DAY_MS;
		mDaysPerPoint = daysPerPoint;
		mNumPoints = numPoints;
		//plotGraph();
	}
	class UpdateGraphTask extends AsyncTask<Void,Void,Void> {

		long startDate;
		
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			
		}

		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		}
		
		
		
	}
	private void plotGraph () {
		//query stats
		//calculate point to canvas mapping
		//draw x axis
		//draw y axis
		//draw graph
		if (mStats == null)
			return;
		if (mNumPoints < 1) {
			return;
		}
		int w = mCanvas.getWidth();
		int h = mCanvas.getHeight();
		Cursor c = mStats.queryDateRange(mStartDate, mStartDate+mNumPoints*mDaysPerPoint*Stats.DAY_MS);
		c.moveToFirst();
		Long nextDate = mStartDate;
		float[] workPoints = new float[(int) ((mNumPoints-1)*4)];
		int pointsInd=0;
		for (int pInd=0;pInd<mNumPoints;pInd++) {
		
			int sumWorked = 0;
			nextDate += mDaysPerPoint*Stats.DAY_MS;
			
			while (!c.isAfterLast()) {
				long day = c.getInt(0);
				if (day >= nextDate) {
					break;
				}
				int secs = c.getInt(1);
				
				sumWorked += secs;
				c.move(1);
			}
			sumWorked/=mDaysPerPoint;
			workPoints[pointsInd] = pInd*(w/(mNumPoints-1));
			workPoints[pointsInd+1] = h - sumWorked/60/5*h/(288-1);
			pointsInd+=2;
			if (pInd != 0 && pInd != mNumPoints-1) {
				workPoints[pointsInd] = workPoints[pointsInd-2];
				workPoints[pointsInd+1] = workPoints[pointsInd-1];
				pointsInd+=2;
			}
		}

		mBackBuffer = Bitmap.createBitmap(mCanvas.getWidth(), mCanvas.getHeight(), Bitmap.Config.RGB_565);
		Canvas canv = new Canvas(mBackBuffer);
		//TODO debug only
		/*
		workPoints[0] = 0;
		workPoints[1] = 100;
		workPoints[2] = 100;
		workPoints[3] = 100;
		workPoints[4] = 100;
		workPoints[5] = 100;
		workPoints[6] = 200;
		workPoints[7] = 0;
		*/
		canv.drawColor(Color.WHITE);
		canv.drawLines(workPoints, workPaint);
		//mCanvas.drawBitmap(mBackBuffer, null, mCanvas.getClipBounds(), null);

		mCanvas.drawBitmap(mBackBuffer, getMatrix(), null);
		//mCanvas.drawBitmap(mBackBuffer, null, null, null);

	}
/*
	private void doDrawGraphSegment(Canvas canvas, Stats stats, long startDay,
			float startX, long endDay, float endX) {
		long daysToAvg = 1;
		Cursor cursor;
		
		long d1 = startDay;
		long sum_worked = 0;
		cursor.moveToFirst();
		while (cursor.getLong(0) < startDay+daysToAvg) {
			sum_worked += cursor.getLong(1);
			cursor.moveToNext();
		}
		
	}
	*/
}
