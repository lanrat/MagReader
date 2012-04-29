package com.vorsk.magreader;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;

import android.util.Log;


/**
 * Class to assist in the decoding of Magstripe data
 * @author Ian Foster
 */
public class Card{
	// hard coded constants
	private static final String TAG = "Card";
	private static boolean DEBUG = true;
	private static boolean REVERSE = true;
	
	
	//class vars
	private boolean goodRead = false;
	private LinkedList<Character> binaryList;
	private String ascii;
	private CardType format;

	
	private enum CardType {
		ALPHA(7,32,'?','%'){},
		BCD(5,48,'?',';'){};
		
		final public int bitSize;
		final public int charOffset;
		final public char end_char;
		final public char start_char;
		final public static char badChar = '|';
		private CardType(int bitSize, int charOffset, char end_char, char start_char){
			this.bitSize = bitSize;
			this.charOffset = charOffset;
			this.start_char = start_char;
			this.end_char = end_char;
		}
	}
	
	
	/**
	 * Ctor for card
	 * @param binaryList
	 */
	public Card(LinkedList<Character> binaryList){
		/* error checking */
		if (binaryList == null){
			return;
		}
		this.binaryList = binaryList;
		

		//convert the binary to a string
		ascii = decodeBinary(binaryList.listIterator());

		//do a quick check on the validly of the String
		//TODO uncomment this out
		
		if (Card.REVERSE && !this.goodRead) {
			if (DEBUG) Log.i(TAG, "decode invalid; attempting reverse");
			//reverse!
			ascii = decodeBinary(new ReversedListIterator<Character>(binaryList));
			if (!this.goodRead){
				if (DEBUG) Log.i(TAG, "reverse did not help");
			}
		}//*/
		
		//fix returning null reads
		if (ascii == null){
			this.goodRead = false;
		}
	}
	
	/**
	 * Accessor method for the Card's string
	 * @return the data
	 */
	public String getASCII(){
		return this.ascii;
	}

	/**
	 * Returns the validly of the processed string
	 * @return the result
	 */
	public boolean isValid(){
		return this.goodRead;
	}
	
	/**
	 * Accessor method for the Binary List
	 * @return the list
	 */
	public LinkedList<Character> getBinaryList(){
		return this.binaryList;
	}

	/**
	 * Decodes the card binary to an ASCII String
	 * @param binaryList the binary to decode
	 * @return the ASCII string resulting from the binary
	 */
	private String decodeBinary(ListIterator<Character> binaryList) {
		this.goodRead = true; //reset to be nice

		//variable to hold the current char
		char current;

		// skip over prepended 0s and find where the data starts
		while (binaryList.hasNext() && binaryList.next().equals('0')){
			//nothing to see here, move along
		}
		backupIterator(binaryList,1); //move back just 1 spot!
		
		//output variable to build our ASCII string
		StringBuilder out = new StringBuilder();
		
		//Determine format
		if (binToChar(charSequenceWithoutInc(binaryList,CardType.ALPHA.bitSize),CardType.ALPHA.charOffset,null) == CardType.ALPHA.start_char){
			this.format = CardType.ALPHA;
			//backupIterator(binaryList, CardType.ALPHA.bitSize);
		}else{
			//backupIterator(binaryList, CardType.ALPHA.bitSize);
			if (binToChar(charSequenceWithoutInc(binaryList,CardType.BCD.bitSize),CardType.BCD.charOffset,null) == CardType.BCD.start_char){
				//backupIterator(binaryList, CardType.ALPHA.bitSize);
				this.format =  CardType.BCD;
				//backupIterator(binaryList, CardType.BCD.bitSize);
			}else{
				//backupIterator(binaryList, CardType.BCD.bitSize);
				this.goodRead = false;
				return "Unknown Card Format";
			}
		}
		
		//this.format = CardType.BCD;
		
		
		//variable to hold our calculated LRC
		boolean[] calcLRC = new boolean[this.format.bitSize];
		//variable to hold read LRC
		char[] readLRC = new char[this.format.bitSize];
		
		//while we still have binary data to convert
		while (binaryList.hasNext()){
			//get the next char
			//Log.v(TAG,"BIT_Size: "+this.format.bitSize);
			char[] binary = charSequence(binaryList, this.format.bitSize);
			current = binToChar(binary, this.format.charOffset,calcLRC);
			//Log.e(TAG,"new char: "+current); //TODO RM
			

			//TODO comment out
			if (binary != null){
				//System.out.println(new String(binary)+" => "+current);
				Log.v(TAG,new String(binary)+" => "+current);
				
			}else{
				Log.v(TAG,"nulls");
				//System.out.println("nulls!");
			}
			
			
			//add the char to our string builder
			out.append(current);
			//check for the end sentinel
			if (current == this.format.end_char && out.length() > 3) { //fixes the leading 0s problem
				// we have reached the end; read the LRC and break!
				readLRC = charSequence(binaryList, this.format.bitSize);
				break;
				//offset = binaryList.size(); // breaks the while loop
			}
		}

		// LRC checking
		if (!Arrays.equals(readLRC, finalizeLRC(calcLRC))) {
			// LRC is invalid
			this.goodRead = false;
		}

		return out.toString();
	}
	
	/**
	 * Simple function to move an iterator back some paces
	 * @param it the iterator
	 * @param count the amount of steps to reverse
	 */
	private <E> void backupIterator(ListIterator<E> it, int count){
		for (int i = 0; i < count; i++){
			if (it.hasPrevious()){
				it.previous();
			}
		}
	}
	
	/**
	 * This method is the same as charSequence, bit it will reverse the iterator
	 * for every step it takes, so the iterator is left in the same position it 
	 * was left in
	 * @param it
	 * @param chunk
	 * @return
	 */
	private char[] charSequenceWithoutInc(ListIterator<Character> it, int chunk){
		char[] out = charSequence(it, chunk);
		backupIterator(it, chunk);
		return out;
	}
	
	/**
	 * Returns an array of the next X chars in the iterator
	 * @param it the iterator
	 * @param chunk the size of the resulting array
	 * @return the array or null if there is not enough chars left
	 */
	private static char[] charSequence(Iterator<Character> it, int chunk){
		char[] out = new char[chunk];
		for (int i = 0; i<chunk; i++){
			if (it.hasNext()){
				out[i] = it.next();
			}else{
				return null;
			}
		}
		return out;
	}

	/**
	 * Convert a boolean array to char
	 * @param boolArray boolean array
	 * @return char array
	 */
	private static char[] boolArrayToChar(boolean[] boolArray) {
		char[] charArray = new char[boolArray.length];

		for (int i = 0; i < boolArray.length; i++) {
			if (boolArray[i]) {
				charArray[i] = '1';
			} else {
				charArray[i] = '0';
			}
		}

		return charArray;
	}

	
	/**
	 * Add the parity bit to the LRC
	 * @param calcLRC the current LRC
	 * @return a char array of the finalized calculated LRC
	 */
	private static char[] finalizeLRC(boolean[] calcLRC) {
		int count = 0;
		// count the 1s
		for (int i = 0; i < calcLRC.length - 1; i++) {
			if (calcLRC[i]) {
				count++;
			}
		}
		//set the last parity element
		calcLRC[calcLRC.length - 1] = (count % 2 != 1);
		//convert from boolean values to char 
		return boolArrayToChar(calcLRC);
	}

	
	/**
	 * Determine if the binary sequence has a valid parity bit
	 * @param bin the binary sequence
	 * @return true if the parity matches
	 */
	private static boolean validChar(char[] bin) {
		if (bin == null){
			return false;
		}
		int count = 0;
		for (char ch : bin) {
			if (ch == '1') {
				count++;
			}
		}
		// is count odd?
		return (count % 2 == 1);
	}

	
	/**
	 * Determine the ASCII char represented by the binary
	 * @param binary the binary sequence to decode
	 * @param offset the ASCII offset to use for decoding
	 * @param lrc the LRC array to update with the given bit
	 * @return the ASCII char
	 */
	private char binToChar(char[] binary, int offset, boolean[] lrc) {
		//check for valid parity bit
		if (!validChar(binary)) {
			this.goodRead = false;
			return CardType.badChar;
		}
		
		//update the LRC
		if (lrc != null){
			for (int i = 0; i < binary.length; i++) {
				if (binary[i] == '1') { //found a 1
					lrc[i] = !lrc[i]; //flip the bit
				}
			}
		}
		
		//our binary is backwards
		binary = reverseArrayAndRemoveLast(binary);
		// Return the char from the parsed binary
		return (char) (Integer.parseInt(new String(binary), 2) + offset);
	}

	/**
	 * return a reversed array with the first->last value missing
	 * @param array the array to reverse
	 * @return the reversed array
	 */
	private static char[] reverseArrayAndRemoveLast(char[] array) {
		char[] out = new char[array.length-1];
		for (int i = 0; i < out.length; i++) {
			out[i] = array[array.length-2-i];
		}
		return out;
	}
	

	
}