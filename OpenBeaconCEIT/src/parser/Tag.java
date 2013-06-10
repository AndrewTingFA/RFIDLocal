/**
 * 
 */
package parser;

import java.awt.Point;


import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author s4116591
 *
 */
public class Tag {
	private class Reading{
		public int value;
		public long time;
		
		public Reading(int value, long time){
			this.value = value;
			this.time = time;
		}
	}
	
	private volatile HashMap<String, Reading> readers;
	private boolean isReferenceTag;
	private Point location;
	private String tagID;
	
	public Tag(String tagID){
		readers = new HashMap<String,Reading>();
		isReferenceTag = false;
		this.tagID= tagID;
	}
	
	private synchronized void log(String message){
		
   
	}
	
	public void addReading(String reader, int power){
		Reading reading = readers.get(reader);
		Date now = new Date();
		if (reading==null){			
			reading = new Reading(power, now.getTime());
			readers.put(reader,reading);
			log(String.valueOf(now.getTime()) + ": Reader="+ reader + "Tag=" + tagID + "Power="+String.valueOf(power)+"\r\n");
		} else {
			if ((power <= reading.value) || (now.getTime() - reading.time > 2000)){
				reading = new Reading(power, now.getTime());
				readers.put(reader,reading);
				log(String.valueOf(now.getTime()) + ": Reader="+ reader + "Tag=" + tagID + "Power="+String.valueOf(power)+"\r\n");
			}		
		}
	}
	
	public HashMap<String, Reading> getReadings(){
		return this.readers;
	}
	
	public String toString(){
		String returnvalue = "";
		Set<Map.Entry<String,Reading>> readingSet = readers.entrySet();
		Iterator<Map.Entry<String,Reading>> it = readingSet.iterator();
		
		while (it.hasNext()){
			Map.Entry<String,Reading> entry = it.next();
			returnvalue+="Reader : ";
			returnvalue+=entry.getKey();
			returnvalue+=" Value: ";
			returnvalue+=String.valueOf(entry.getValue().value);
			returnvalue+=" Timestamp: ";
			returnvalue+=String.valueOf(entry.getValue().time);
			returnvalue+="\r\n";		
		}
		
		return returnvalue;
	}
	
	public void setPoint(Point location){	
		isReferenceTag=true;
		this.location = location;		
	}
	
	public boolean isRef(){
		return isReferenceTag;
	}
	public Point getLoc(){
		return location;
	}
	
	public int calcDiff(Tag other){
		int diff = 0;
		String key;
		int myStrength;
		int otherStrength;
		HashMap<String, Reading> myReadings = this.getReadings();
		HashMap<String,Reading> otherReadings = other.getReadings();
		Set<String> otherKeys = otherReadings.keySet();
		Set<String>  myKeys = new TreeSet<String>(myReadings.keySet());		
		
		myKeys.addAll(otherKeys);
		Iterator<String> it = myKeys.iterator();
		
		while (it.hasNext()){
			Date now = new Date();
			key = it.next();
			if ((myReadings.containsKey(key)) && ((now.getTime() - myReadings.get(key).time) > 5000) ){
				myStrength = myReadings.get(key).value;
			} else {
				myStrength = 5;
			}
			if ((otherReadings.containsKey(key)) && ((now.getTime() - otherReadings.get(key).time) > 5000)){
				otherStrength = otherReadings.get(key).value;
			} else {
				otherStrength = 5;
			}
			diff +=  Math.abs(myStrength-otherStrength);			
		}
		
		return diff;
	}
	

	
}
