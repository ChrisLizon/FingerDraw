package ca.lizon.fingerdraw;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

/**
 * The drawview is the view that intercepts the MotionEvents
 * and does the actual drawing of line segments on the display.
 * 
 * @author Chris Lizon
 */

public class DrawView extends View{
	private static final String TAG = "DrawView";

	public static final int MODE_DRAW = 0;
	public static final int MODE_ERASE = 1;

	public static final int ACTION_ADDSEGMENT = 0;
	public static final int ACTION_DELETESEGMENT = 1;

	Button undoButton = null;

	int mode = MODE_DRAW;
	
	/** the paint holds the draw options like colors */
	Paint paint = new Paint();
	
	/** hold a list of segments that are on the screen */
	List<Segment> segments = new ArrayList<Segment>();

	/** hold a list of segments removed from the screen */
	List<Segment> deletedSegments = new ArrayList<Segment>();

	/** keep track to determine if we last added or deleted */
	List<Integer> actions = new ArrayList<Integer>();

	/** an extra segment to be used for erasing */
	Segment eraser;

	
	//listers to process the correct events
	//when we change modes, we swap out the listeners
	OnTouchListener drawListener = new DrawListener();
	OnTouchListener eraseListener = new EraseListener();

	/** this constructor allows us to use this in an xml layout */
	public DrawView(Context context, AttributeSet attribs){
		super(context, attribs);
		setup();
	}
	
	/** default constructor */
	public DrawView(Context context) {
		super(context);
		setup();
		
	}

	private void setup(){
		setFocusable(true);
		setFocusableInTouchMode(true);

		this.setOnTouchListener(drawListener);

		paint.setColor(Color.WHITE);
		paint.setAntiAlias(true);

	}
	
	/**Called when the system is painting the view to the screen
	 * in our case we need to draw all the lines we've stored 
	 */
	@Override
	public void onDraw(Canvas canvas) {

		//draw the line segments
		for(Segment segment: segments){

			List<Point> points = segment.points;

			for(int i = 0; i < points.size() -1; i++){
				Point start = points.get(i);
				Point end = points.get(i+1);
				
				//simply draw lines between all points on a segment
				canvas.drawLine(start.x, start.y, end.x, end.y, paint);
			}
		}

		//if we're in the eraser mode, we need to draw the red eraser line
		if(mode == MODE_ERASE && eraser != null){
			List<Point> points = eraser.points;

			Paint erasePaint = new Paint(paint);
			erasePaint.setColor(Color.RED);

			for(int i = 0; i < points.size() -1; i++){
				Point start = points.get(i);
				Point end = points.get(i+1);

				canvas.drawLine(start.x, start.y, end.x, end.y, erasePaint);


			}
		}

		//either enable or disable the undo button
		if(undoButton != null)
			//if there are actions recorded enable the undo button
			undoButton.setEnabled(actions.size() > 0);
	}

	///////////////////////////////////////////////////////
	//ACCESSORS

	/**Set the mode of the DrawView, Use this when switching the radio buttons
	 * on the activity. use either DrawView.MODE_DRAW or DrawView.MODE_ERASE
	 */
	public void setMode(int mode){
		this.mode = mode;
		switch(mode){
		case MODE_ERASE:
			this.setOnTouchListener(eraseListener);
			break;
		default:
			this.setOnTouchListener(drawListener);
			break;
		}
	}

	/** register an undo button that will undo actions on the
	 * DrawView. if undo is not possible the button will be
	 * automatically disabled.
	 */
	public void registerUndoButton(Button undoButton){
		this.undoButton = undoButton;
		
		undoButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				undo();
			}
        	
        });
	}


	/** give some user control and freedom */
	public void undo(){
		
		if(actions.size() > 0){
			try{
				int action = actions.remove(actions.size()-1);
				//if the action is an add, we'll remove it
				if(action == ACTION_ADDSEGMENT){
					segments.remove(segments.size() -1);
					//if the action was a remove, we'll re-add the segment
				}else if(action == ACTION_DELETESEGMENT){
					segments.add(deletedSegments.remove(deletedSegments.size()-1));
				}
			}catch(NullPointerException e){
				
			}
			invalidate(); //and re-draw
		}
	}
	
	/** DrawListener will take touches and add segments */
	private class DrawListener implements OnTouchListener{
		public boolean onTouch(View view, MotionEvent event) {

			
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				//action down is fired when your finger hits the screen
				
				if(segments.size() == 0 || segments.get(segments.size()-1).points.size() != 0){
					//when you put your finger down, we'll start a new segment
					segments.add(new Segment());
					
					//and we'll indicate that the last action was a draw
					actions.add(ACTION_ADDSEGMENT);
				}

			}else if(event.getAction() == MotionEvent.ACTION_MOVE){
				//action move is fired over and over as your finger is
				//dragged across the screen
				
				//here we add a point to the last segment
				Point point = new Point(event.getX(), event.getY());
				segments.get(segments.size()-1).points.add(point);
				
				invalidate(); //time to redraw
		
			}else if(event.getAction() == MotionEvent.ACTION_UP){
				//we do nothing, but we could do something
				//if we wanted to.
				
			}

			return true; //let the OS know this touch event was handled
		}
	};


	/** EraseListener will delete lines that intersect with the one
	 * the user has drawn.
	 */
	private class EraseListener implements OnTouchListener{
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			if(event.getAction() == MotionEvent.ACTION_DOWN){
				eraser = new Segment();
			}else if(event.getAction() == MotionEvent.ACTION_MOVE){
				Point point = new Point();
				point.x = event.getX();
				point.y = event.getY();

				eraser.points.add(point);
				invalidate();
				Log.d(TAG, "ErasePoint: " + point);
			}else if(event.getAction() == MotionEvent.ACTION_UP){

				//check every segment against the segment
				//drawn by the eraser
				for(int i = 0; i < segments.size(); i++){
					Log.i(TAG, "Checking Segment " + i);
					if(segmentIntersects(eraser, segments.get(i))){
						//remove the segment from the array
						deletedSegments.add(segments.remove(i));
						//save the fact that we're removing a segment
						actions.add(ACTION_DELETESEGMENT);
					}
				}

				eraser = null; //we're done here
				
				invalidate(); //time for a redraw
			}

			return true;
		}

	}

	/** Determine if two segments intersect */
	private boolean segmentIntersects(Segment s1, Segment s2){

		//this is a nieve method to check each subsection of each
		//segment against each subsection of the eraser segment
		for(int i = 0; i < s1.points.size()-1; i++){
			for(int j = 0; j < s2.points.size()-1; j++){
				if(lineIntersects(s1.points.get(i), s1.points.get(i+1), s2.points.get(j), s2.points.get(j+1))){
					Log.i(TAG, "Intersection Found!");
					return true;
				}
			}
		}

		return false;
	}


	/** Determine if two lines intersect */
	private boolean lineIntersects(Point start1,
			Point end1, Point start2, Point end2) {

		//this code taken from 
		//http://groups.google.com/group/android-developers/browse_thread/thread/ea7c3b2cdcf917b4	

		//First find Ax+By=C values for the two lines
		float A1 = end1.y - start1.y;
		float B1 = start1.x - end1.x;
		float C1 = A1 * start1.x + B1 * start1.y;

		float A2 = end2.y - start2.y;
		float B2 = start2.x - end2.x;
		float C2 = A2 * start2.x + B2 * start2.y;

		float det = (A1 * B2) - (A2 * B1);

		if (Math.abs(det) < 0.1) {
			// Lines are either parallel, are collinear (the exact same
			// segment), or are overlapping partially, but not fully
			// To see what the case is, check if the endpoints of one line
			// correctly satisfy the equation of the other (meaning the two
			// lines have the same y-intercept).
			// If no endpoints on 2nd line can be found on 1st, they are
			// parallel.
			// If any can be found, they are either the same segment,
			// overlapping, or two segments of the same line, separated by some
			// distance.
			// Remember that we know they share a slope, so there are no other
			// possibilities

			// Check if the segments lie on the same line
			// (No need to check both points)
			if ((A1 * start2.x) + (B1 * start2.y) == C1) {
				// They are on the same line, check if they are in the same
				// space
				// We only need to check one axis - the other will follow
				if ((Math.min(start1.x, end1.x) < start2.x)
						&& (Math.max(start1.x, end1.x) > start2.x))
					return true;

				// One end point is ok, now check the other
				if ((Math.min(start1.x, end1.x) < end2.x)
						&& (Math.max(start1.x, end1.x) > end2.x))
					return true;

				// They are on the same line, but there is distance between them
				return false;
			}

			// They are simply parallel
			return false;
		} else {
			// Lines DO intersect somewhere, but do the line segments intersect?
			float x = (B2 * C1 - B1 * C2) / det;
			float y = (A1 * C2 - A2 * C1) / det;

			// Make sure that the intersection is within the bounding box of
			// both segments
			if ((x > Math.min(start1.x, end1.x) && 
					x < Math.max(start1.x, end1.x))	&&
					(y > Math.min(start1.y, end1.y) &&
							y < Math.max(start1.y, end1.y))) {
				// We are within the bounding box of the first line segment,
				// so now check second line segment
				if ((x > Math.min(start2.x, end2.x) &&
						x < Math.max(start2.x, end2.x))
						&& (y > Math.min(start2.y, end2.y) && y < Math.max(
								start2.y, end2.y))) {
					// The line segments do intersect
					return true;
				}
			}

			// The lines do intersect, but the line segments do not
			return false;
		}
	}


}

///////////////////////////////////////////////////////////
//HELPER CLASSES

class Point {
	float x, y;

	public Point(float x2, float y2) {
		this.x = x2; this.y = y2;
	}

	public Point() {
	}

	//overwrote toString to represent the points coordinates
	@Override
	public String toString() {
		return x + "," + y;
	}
}

class Segment {
	List<Point> points;
	public Segment(){
		points = new ArrayList<Point>();
	}

	//overwrote toString to printout a list of points for debugging
	@Override
	public String toString(){
		StringBuilder buffer = new StringBuilder();

		for(Point p: points){
			buffer.append("|");
			buffer.append(p.toString());		
		}

		return buffer.toString().replaceFirst("\\|", "");
	}


}


