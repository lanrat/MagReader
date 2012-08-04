package com.vorsk.magreader;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * A class to create a reversed list iterator given a list iterator
 * This class used the adapter pattern
 */
public class ReversedListIterator<E> implements ListIterator<E>{
	
	private ListIterator<E> iterator;
	
	/**
	 * Constructor given an existing list iterator
	 * @param forwardIterator the forward iterator
	 */
	public ReversedListIterator(ListIterator<E> forwardIterator){
		this.iterator = forwardIterator;
	}
	
	/**
	 * Constructor given a LinkedList
	 * @param myList the list to reversely iterate over
	 */
	public ReversedListIterator(LinkedList<E> myList){
		this.iterator = myList.listIterator(myList.size());
	}

	//Everything below here used the adapter pattern, nothing fancy
	
	//because this is backwards using the adapter pattern
	//this will add in the previous spot instead of next
	public void add(E object) {
		iterator.add(object);
		
	}

	public boolean hasNext() {
		return iterator.hasPrevious();
	}

	public boolean hasPrevious() {
		return iterator.hasNext();
	}

	public E next() {
		return iterator.previous();
	}

	public int nextIndex() {
		return iterator.previousIndex();
	}

	public E previous() {
		return iterator.next();
	}

	public int previousIndex() {
		return iterator.nextIndex();
	}

	public void remove() {
		iterator.remove();
		
	}

	public void set(E object) {
		iterator.set(object);
		
	}

}
