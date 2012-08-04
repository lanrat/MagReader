package com.vorsk.magreader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class HeadsetStateReceiver extends BroadcastReceiver {
	
	private MagStriperActivity activity;
	
	public HeadsetStateReceiver(MagStriperActivity activity){
		this.activity = activity;
	}

	@Override
	public void onReceive(Context context, Intent intent) {

		Bundle extras = intent.getExtras();
		int state = extras.getInt("state");
		int mic = extras.getInt("microphone");
		
		//we have a headset with mic
		//start the thread!
		if (state == 1 && mic == 1){
			//start a new instance
			activity.runThread();
			//update status message
			activity.addToGUI("Got reader");
			
		}else if (state == 1 && mic == 0){
			//headphones are plugged in, not a reader (or mic)
			//update status to insert reader.
			activity.addToGUI("Non-Reader plugged in");
			
		}else{
			//stop the thread
			activity.stopThread();
			//update status message
			activity.addToGUI("Reader Unpluged");
		}
	}

}
