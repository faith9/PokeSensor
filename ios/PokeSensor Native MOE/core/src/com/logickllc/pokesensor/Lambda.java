package com.logickllc.pokesensor;

import java.util.Hashtable;

/** This is just an abstract class useful for a Lambda expression. Just make a method
 * that takes this class as an argument and pass a Labmda expression in its place. Then
 * the method can call {@link #execute()} to run the expression. 
 * 
 * @author Patrick Ballard
 */
public abstract class Lambda {
	public Hashtable<String, Object> params = new Hashtable<String, Object>();
	
	public abstract void execute();
}
