package ca.lizon.fingerdraw;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;

/**
 * FingerDrawActivity is an activity showcases using motion
 * events to create a simple finger painting experience. 
 * 
 * @author Chris Lizon
 *
 */

public class FingerDrawActivity extends Activity {
	DrawView drawView;


	/**
	 * onCreate is called when the system has loaded
	 * the application. Alot of initialization is done here, 
	 * but it can be done later in the application lifecycle.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//load the view layout from the xml
		this.setContentView(R.layout.draw_layout);
		
		drawView = (DrawView)this.findViewById(R.id.drawing_screen_drawview);
		drawView.requestFocus();

		RadioButton drawButton = (RadioButton)this.findViewById(R.id.drawing_screen_drawbutton);
		RadioButton eraseButton = (RadioButton)this.findViewById(R.id.drawing_screen_erasebutton);

		drawButton.setChecked(true);
		drawButton.setOnClickListener(
				new OnClickListener(){

					@Override
					public void onClick(View v) {
						drawView.setMode(DrawView.MODE_DRAW);
					}
				});

		eraseButton.setOnClickListener(
				new OnClickListener(){

					@Override
					public void onClick(View v) {
						drawView.setMode(DrawView.MODE_ERASE);
					}
				});

		Button undoButton = (Button)this.findViewById(R.id.drawing_screen_undobutton);
		drawView.registerUndoButton(undoButton);

	}

}