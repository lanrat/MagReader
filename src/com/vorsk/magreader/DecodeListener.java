package com.vorsk.magreader;

import java.util.LinkedList;

//for testing, delete
/*
import com.vorsk.magstripereader.old.MagDecoder;
import com.vorsk.magstripereader.old.MagDecoder_old;
import com.vorsk.magstripereader.old.PCMLinkedList;
*/

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Thread which listens on the MIC input and passes captured audio to the decoder
 * @author Ian Foster
 */
class DecodeListener extends AsyncTask<Integer, String, Void> {
	private static final String TAG = "Decode Listener";
	private static final boolean DEBUG = true;
	private final int FREQUENCY = 44100;
	//used to determine is a sound sample was long enough to bother parsing
	private static final int minListLen = 5000;
	//offset to 0, this is a good default value, but I update it so that it is dynamic
	private int offset = 460;
	
	//Gui Var
	private MagStriperActivity activity;

	//boolean value used to stop the recording if the activity is closed
	private boolean isRecording = true;
	
	/** Ctor for the listener
	 * @param callingActivity  the activity to update
	 */
	public DecodeListener(MagStriperActivity callingActivity){
		this.activity = callingActivity;
		this.offset = findZero();
		if (DEBUG) Log.v(TAG,"new zero offset: " + offset);
	}

	/**
	 * Main thread method
	 * Listens to the audio input and passes audio data to the Decoder
	 * when it finds any 
	 * @param i [unused]
	 */
	protected Void doInBackground(Integer... i) {
		// configure stuff
		int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
		int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
		int bufferSize = AudioRecord.getMinBufferSize(FREQUENCY, channelConfiguration, audioEncoding);
		AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, FREQUENCY, channelConfiguration,audioEncoding, bufferSize);

		short[] buffer = new short[bufferSize];
		audioRecord.startRecording();

		// A linked-list object to hold our pcm data
		LinkedList<Short> pcmList = new LinkedList<Short>();

		// variable used to individual PCM data
		short pcmShort;

		// for auto detect; used to determine time-stamp of last audio
		long lastFound = 0;
		

		while (isRecording) {
			// process new audio data
			int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
			for (int b = 0; b < bufferReadResult; b++) {
				//add the incoming short to the offset
				pcmShort = (short) (buffer[b] + offset);
				
				//if we detect some non-noise let the timer know
				if (Swype.isOutsideThreshold(pcmShort)) {
					lastFound = System.currentTimeMillis();
				}
	
				//if the timer is counting, add the data to the list
				if (lastFound != 0) {
					pcmList.add(pcmShort);
					//pcmList_sand.add(pcmShort);
				}
			}

			//if the timer is running and it has been > 1/10th of a second since the last peak
			if (lastFound != 0 && (System.currentTimeMillis() - lastFound) > 100) {
				// make sure that the PCM data is actually of a meaningful length
				if (pcmList.size() > minListLen) {
					// looks like we may have found a card
					audioRecord.stop();
					 if (DEBUG) Log.v(TAG,"processing audio segment");
					
					// reset timer
					lastFound = 0;
										
					//process the PCM data and hope for a card

					//decode the swype
					Swype swype = new Swype(pcmList);
					
					//publishProgress(swype.getBinaryString());
					
					//publishProgress("Size:" +swype.getBinaryList().size());
					
					
					if (swype.isvalid()){ //TODO this is backwards
						if (DEBUG) Log.d(TAG, "Swype was valid");
				
						// get the card
						Card card = new Card(swype.getBinaryList());
						
						if (card.isValid()){ //possibly check that the string is != null
							if (DEBUG) Log.d(TAG, "card is valid");
							publishProgress("Valid Swipe!");
						}else{
							publishProgress("Invalid Swipe, outputting regardless");
						}
						publishProgress("---------------------------------------------------------");
						publishProgress(card.getASCII());
						publishProgress(" ------- binary ------------");
						publishProgress(Swype.binaryString(swype.getBinaryList()));
						
					}else{
						publishProgress("Invalid Swipe!");
					}
					
					audioRecord.startRecording();
				}
				//reset the PCMlist for the next card
				//pcmList.clear(); //TODO check this
				pcmList.clear();
				//pcmList_sand.clear();
			}

		}

		//we are done recording
		audioRecord.stop();
		//due to the Void (note the capital V) return type
		//the following line must be here.
		return null;
	}
	
	
	/** listens for a short while and finds what should be the zero offset for the class
	 * @return the new zero offset
	 */
	private int findZero(){
		// configure stuff
		int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
		int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
		int bufferSize = AudioRecord.getMinBufferSize(FREQUENCY, channelConfiguration, audioEncoding);
		AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, FREQUENCY, channelConfiguration,audioEncoding, bufferSize);

		short[] buffer = new short[bufferSize];
		audioRecord.startRecording();
		int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
		audioRecord.stop();
		int sum = 0;
		for (int b = bufferReadResult * (2/3); b < bufferReadResult; b++) {
			sum += buffer[b];
		}
		if (bufferReadResult == 0){
			return this.offset;
		}
		
		return -1*(sum/bufferReadResult);
	}


	/**
	 * Push a string to the GUI to display
	 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
	 */
	@Override
	protected void onProgressUpdate(String... s) {
		activity.addToGUI(s[0]);
	}
	
	/**
	 * Run when the thread completes
	 */
	protected void onPostExecute() {
	}

	
	/**
	 *  Stops the recording, to re-start re-create the object
	 */
	public void stop() {
		isRecording = false;
	}

}