package com.vorsk.magreader;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import android.util.Log;

/**
 * Class to assist in the decoding of Magstripe data
 * @author Ian Foster
 */
public class Swype{
	// hard coded constants
	private static final String TAG = "Swype";
	private static final boolean DEBUG = false;
	private static final short NOISE = 600; //was 900 //TODO dynamic? 600 seems better... need to run some tests on this
	private static final boolean CALC_STATS = false; //std deviation, etc...
	private LinkedList<int[]> oneCalc, zeroCalc; //used for the calc_stat
	
	
	//class vars
	private LinkedList<Short> pcmList;
	private LinkedList<int[]> peakList;
	private LinkedList<Character> binaryList;
	private boolean valid = true; //hopeful
	
	/**
	 * Ctor for Swype class
	 * @param pcmList list of pcm shorts for the audio wave
	 */
	public Swype(LinkedList<Short> pcmList){
		this.pcmList = pcmList;
		if (DEBUG) Log.v(TAG,"analyzing swype with len: "+pcmList.size());
		if (CALC_STATS){
			oneCalc = new LinkedList<int[]>();
			zeroCalc = new LinkedList<int[]>();
		}
		//decode the list on object creation
		getBinaryList();
	}
	
	/**
	 * accessor method for the PCM List
	 * @return the list
	 */
	public LinkedList<Short> getPCMList(){
		return this.pcmList;
	}
	
	/**
	 * accessor method for the Peak List
	 * @return the list
	 */
	public LinkedList<int[]> getPeakList(){
		if (peakList == null){
			this.peakList = findPeaks(pcmList);
			//the first peak is always useless
			try{
				peakList.removeFirst();	
				peakList.removeFirst();
			}catch (NoSuchElementException e) {
				//List is empty, do something
				this.valid = false;
			}
		}
			
		return this.peakList;
	}
	
	/**
	 * Returns a string of binary charectors
	 * @param binaryList the linked list of chars
	 * @return the string
	 */
	public static String binaryString(LinkedList<Character> binaryList){
		StringBuilder strb = new StringBuilder();
		Iterator<Character> it = binaryList.iterator();
		while (it.hasNext()){
			strb.append(it.next());
		}
		return strb.toString();
	}
	
	/**
	 * accessor method for the Binary List
	 * @return the list
	 */
	public LinkedList<Character> getBinaryList(){
		if (binaryList == null){
			this.binaryList = peaksToBinary(this.getPeakList());
		}
		return this.binaryList;
	}
	
	/**
	 * Returns a string of 1s and 0s
	 * @return the binary string of the swype
	 */
	public String getBinaryString(){
		StringBuilder out = new StringBuilder();
		Iterator<Character> it = getBinaryList().iterator();
		while (it.hasNext()){
			out.append(it.next());
		}
		return out.toString();
	}
	
	/**
	 * Returns the state of the swype
	 * @return true if the peaks look good
	 */
	public boolean isvalid(){
		return this.valid;
	}

	/**
	 * Decode peaks to binary
	 * @param peakList the peaks to decode
	 * @return linked list of 1s and 0s
	 */
	private LinkedList<Character> peaksToBinary(LinkedList<int[]> peakList) {
		//lets fix some errors with some other errors
		if (peakList == null || peakList.size() < 10){
			this.valid = false;
			return null;
		}
		
		//TODO currently just use index 0, the last index, try playing with the over packaged vars
		
		//create the output variable
		LinkedList<Character> binaryList = new LinkedList<Character>();
		//Initialize the oneClock variable for this swipe 
		oneClock clock = new oneClock((peakList.get(2)[0] - peakList.get(1)[0]) / 2); // (3rd - 2nd) / 2
		
		int lastPeakIdx = peakList.getFirst()[0];
		//variable to hold the current bit we are going to examine
		char currentBit;

		char lastBit = '\0'; //null char

		Iterator<int[]> it = peakList.iterator();
		while (it.hasNext()){
			int[] currentPeak = it.next();
		
			// ignoring the MAX_BITSTREAM_LEN (=1024), cuz I don't think I need it
			//get the current bit
			currentBit = peakDiffToBin(clock, currentPeak[0] - lastPeakIdx);
			if (currentBit == '0') {
				//if the current bit is a 0; all is good; add it
				binaryList.add(currentBit);
				if (CALC_STATS) zeroCalc.add(currentPeak);
			} else if (currentBit == lastBit) {
				//if not; then if we got two if the same bits, just add one
				//we do this because there will be double the amount of 1 bits because they are 1/2 the size of a 0
				// logic is a little fuzzy
				if (CALC_STATS) oneCalc.add(currentPeak);
				binaryList.add(currentBit);
				currentBit = '\0'; //reset
			}
			//update my last bit
			lastBit = currentBit;
			lastPeakIdx = currentPeak[0];
		}
		
		//TODO testing removing the first char
		binaryList.removeFirst();
		
		//return the finished product
		return binaryList;
	}
	
	/**
	 * returns the stats calculated if enabled
	 * @return a 2d array, if stats
	 */
	public double[][] getStats(){
		if (CALC_STATS){
			double[][] out = new double[2][3];
			out[0] = calcStats(zeroCalc);
			out[1] = calcStats(oneCalc);
			return out;
		}
		return null;
	}
	
	/**
	 * returns some stats in a double int
	 * order of stats is as follows
	 * ExpectedValue, Variance, StdDeviation
	 * @param list the list to loop over
	 * @return stats
	 */
	private double[] calcStats(LinkedList<int[]> list){
		//offset for which dataset we should use
		int dataSet = 2;
		
		//first calculate the expected value
		double e = 0;
		double e2 = 0;
		double current;
		Iterator<int[]> it = list.iterator();
		while (it.hasNext()) {
			current = Math.abs(it.next()[dataSet]);
			
			e+=current;
			e2+=(current*current);
		}
		e=e/list.size();
		e2=e2/list.size();
		
		//now calculate variance
		double var = e2 - (e*e);
		
		//now calculate the std deviation
		double stdDev = Math.sqrt(var);
		
		//done
		return new double[]{e,var,stdDev};
		
	}

	
	/**
	 * Determines if the difference between two peaks is a 1 or 0
	 * @param diff the difference between the two peaks
	 * @return char 1 or 0
	 */
	private static char peakDiffToBin(oneClock clock, int diff) {
		//the diff which represents a 1 or 0
		int oneDif = Math.abs(clock.current - diff);
		int zeroDif = Math.abs((clock.current * 2) - diff);

		//if valid; we have a 1
		if (oneDif < zeroDif) {
			//update oneClock for timing
			clock.setClock(diff);
			return '1';
		} else { // got a 0
			//update oneClock for timing
			clock.setClock(diff/2);
			return '0';
		}
	}
	
	/**
	 * Finds and returns the peaks in the passed PCM data
	 * @param pcmList a List of PCM peaks
	 * @return A linked list of peak values
	 */
	private LinkedList<int[]> findPeaks(LinkedList<Short> pcmList) {
		//check for bad data
		if (pcmList == null || pcmList.size() < 50){
			this.valid = false;
			return null;
		}
		//create output variable
		//the int[] stores [peakIndex, lastValue, frame-lastPeakframe]
		LinkedList<int[]> peakList = new LinkedList<int[]>();
		
		//start at the first peak
		short currentPeak;
		short largestPeak = 0;
		int largestIDX = 0;
		int lastIDX = 0;
		
		//are we starting on a positive bit
		boolean positive = (pcmList.getFirst() > 0);
		
		//create our iterator
		ListIterator<Short> it = pcmList.listIterator();

		while (it.hasNext()) {
			currentPeak = it.next().shortValue();
			//is the current PCM short not noise?
			if (isOutsideThreshold(currentPeak)) {
				//are we still working on the new peak?
				if ((currentPeak > 0) == positive) {
					//compare the peaks
					if (Math.abs(currentPeak) > Math.abs(largestPeak)) {
						largestPeak = currentPeak;
						largestIDX = it.nextIndex();
					}
				} else { //new peak, stop the old
					//the next peak must be on the other side of 0
					positive = !positive;
					//add the largest previous peak
					peakList.add(new int[] {largestIDX,largestPeak,largestIDX-lastIDX});
					lastIDX = largestIDX;
					//reset the current largest peak
					largestPeak = 0;
				}
			}
		}
		//done; return my list
		return peakList;
	}

	/**
	 * Method to determine if the PCM short is of any use
	 * @param s the short to check
	 * @return true if the short is outside the noise threshold; else false
	 */
	public static boolean isOutsideThreshold(short s) {
		if (Math.abs(s) > NOISE) {
			return true;
		}
		return false;
	}
	
	/**
	 * Class instance variable used to store the oneClock
	 * Has the ability to average the past few ones for greater accuracy
	 */
	protected class oneClock{
		public int current;
		private int previous;
		public oneClock(int clock){
			current = clock;
			previous = clock;
		}
		public void setClock(int clock){
			previous = current;
			current = (clock+current+previous)/3;
		}
	}

}