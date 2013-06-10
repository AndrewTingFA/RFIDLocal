/**
 * 
 */
package parser;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;



import com.ibm.mqtt.MqttException;




/**
 * @author s4116591
 *
 */
public class parserMain{	
	
	private  HashMap<String, Point> readers;
	private volatile HashMap<String, Tag> tags;
	private ArrayList<String> referenceTags;
	private int maxNeighbours;
	
	
	/**
	 * @param args
	 * @throws MqttException 
	 */
	public parserMain(String[] args)  {

		readers = new HashMap<String,Point>();
		tags = new HashMap<String, Tag>();		
		referenceTags = new ArrayList<String>();
		maxNeighbours = 4;
		Path readerfile = Paths.get(args[2]);
		Charset charset = Charset.forName("US-ASCII");
		try {
			List<String> readerList = Files.readAllLines(readerfile, charset);
			Iterator<String> it = readerList.iterator();
			while (it.hasNext()){
					addReader(it.next());			
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.printf("Error reading reader file %s\r\n Expected format for line: readerIP xCoord yCoord\r\n", args[2]);
			e.printStackTrace();			
		}
		Path referencefile = Paths.get(args[3]);
		try {
			List<String> referenceList = Files.readAllLines(referencefile, charset);
			Iterator<String> it = referenceList.iterator();
			while (it.hasNext()){
					addReference(it.next());			
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.err.printf("Error reading reference file %s\r\n Expected format for line: refenceID xCoord yCoord\r\n", args[3]);
			e.printStackTrace();			
		}
		//Check if we parsed correctly
		
	}
	
	
	
	private void receivedPacket(String tagID, String reader, String power){
		//System.out.printf("Reader %s read tag  %s with power %s\r\n", reader, tagID, power);
		int transmitPower = Integer.parseInt(power, 16);
		if (tags.containsKey(tagID)){
			Tag tag = tags.get(tagID);
			tag.addReading(reader,transmitPower);			
		} else {
			Tag tag = new Tag(tagID);
			tag.addReading(reader,transmitPower);
			tags.put(tagID, tag);				
		}
		if ((tagID.equals("03BD")) || (tagID.equals("03B8")) || (tagID.equals("03D3") || (tagID.equals("03BB")))){
			calcLocation(tagID);
		}
		
	}
	
	private synchronized void log(String message, String filename){
		
	    try {
	        FileWriter fstream = new FileWriter(filename,true);
	        BufferedWriter fbw = new BufferedWriter(fstream);
			fbw.write(message);
		    fbw.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
	   
	}
	
	//Should I just use a priority queue here?
	private Point calcLocation(String tagID){			
		Date now = new Date();
		class referenceDist implements Comparable<referenceDist>{
			String referenceID;
			Double inverseDistance;
			
			public referenceDist(String id, double distance){
				this.referenceID = id;
				this.inverseDistance = new Double(distance);
			}

			@Override
			public int compareTo(referenceDist o) {
				
				return o.inverseDistance.compareTo(this.inverseDistance);
			}		
			
		}
		
		ArrayList<referenceDist> referenceList = new ArrayList<referenceDist>();		
		for (int i = 0 ; i < (referenceTags.size()); i++){
			String referenceID = referenceTags.get(i);
			//System.err.printf("Comparing tag %s with readings: %s\r\n", tagID, tags.get(tagID).toString());
			int distance = tags.get(tagID).calcDiff(tags.get(referenceID));
			//System.err.printf("Reference tag %s with readings: %s\r\n", referenceID, tags.get(referenceID).toString());
			double power = 0;
			if (distance == 0){
				power = 10;
			} else {
				power = 1.0/distance;
			}
			
			referenceList.add(new referenceDist(referenceID, power));
		}
		
		Collections.sort(referenceList);
//		Iterator<referenceDist> iter = referenceList.iterator();
//		System.out.printf("START\r\n");
//		while(iter.hasNext()){
//			referenceDist dist = iter.next();
//		   System.out.printf("'%s : %s'", dist.referenceID, dist.inverseDistance.toString());
//		}
//		System.out.printf("\r\nEND\r\n");
		int neighbours = Math.min(maxNeighbours, referenceList.size());
		double total = 0;
		double x = 0;
		double y = 0;
		double diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff)+ "\r\n", tagID+ "_4.log");
		
		neighbours = Math.min(3, referenceList.size());
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff)+ "\r\n", tagID+ "_3.log");
		
		neighbours = Math.min(2, referenceList.size());
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff)+ "\r\n", tagID+ "_2.log");
		
		neighbours = Math.min(5, referenceList.size());
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_5.log");
		
		neighbours = Math.min(6, referenceList.size());
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_6.log");
		
		neighbours = Math.min(7, referenceList.size());
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_7.log");
		neighbours = Math.min(8, referenceList.size());
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_8.log");
		neighbours = Math.min(9, referenceList.size());
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_9.log");
		neighbours = Math.min(10, referenceList.size());
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_10.log");
		neighbours = Math.min(11, referenceList.size()-1);
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_11.log");
		neighbours = Math.min(12, referenceList.size()-1);
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_12.log");
		neighbours = Math.min(13, referenceList.size()-1);
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_13.log");
		
		neighbours = Math.min(14, referenceList.size()-1);
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_14.log");
		neighbours = Math.min(15, referenceList.size()-1);
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_15.log");
		neighbours = Math.min(16, referenceList.size()-1);
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_16.log");
		neighbours = Math.min(17, referenceList.size()-1);
		total = 0;
		x = 0;
		y = 0;
		diff = 0;
		for (int i = 1; i <= neighbours; i++){
			total += referenceList.get(i).inverseDistance;
		}
		for (int i = 1; i <= neighbours; i++){
			Point current = tags.get(referenceList.get(i).referenceID).getLoc();
			x += (referenceList.get(i).inverseDistance / total) * current.getX();
			y += (referenceList.get(i).inverseDistance / total) * current.getY();
		}
		if (tagID.equals("03BD")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-150),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-175),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-150),2) + Math.pow((y-100),2));
		} else if (tagID.equals("03BB")){
			diff = Math.sqrt(Math.pow((x-200),2) + Math.pow((y-100),2));
		}
		log(String.valueOf(diff) + "\r\n", tagID+ "_17.log");
		Point location = new Point((int )x, (int)y);
		log(String.valueOf(now.getTime()) + "\r\n", "time.log");			

	//	log(String.valueOf(now.getTime()) + ": Tag="+ tagID + " x=" + String.valueOf(x) + " y="+String.valueOf(y)+"\r\n", "tagLocationLogfile.log");
		
		return location;
	}
	

	
	private void addReader(String line){		
			try {
				String[] readerData = line.split(" ");
				Point point = new Point(Integer.parseInt(readerData[1]), Integer.parseInt(readerData[2]));
				readers.put(readerData[0], point);
			} catch (Exception e) {
				System.err.printf("Error while parseing reader file\r\n");
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}		
	}
		
	private void addReference(String line){		
		try {
			String[] referenceData = line.split(" ");
			Point point = new Point(Integer.parseInt(referenceData[1]), Integer.parseInt(referenceData[2]));
			Tag tag = new Tag(referenceData[0]);
			tag.setPoint(point);
			tags.put(referenceData[0], tag);
			referenceTags.add(referenceData[0]);
		} catch (Exception e) {
			System.err.printf("Error while parseing reference file, line %s\r\n", line);
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(1);
		}		
}



	/**
	 * @param args
	 * @throws MqttException 
	 */
	public static void main(String[] args) throws MqttException {
		
		//Check arg length
		if (args.length < 4){
			System.out.printf("Invalid arguments. \r\n Expected parserMain  <targetfilename> <lazy_nothing> <reader filename> <reference tag filename(optional)>\r\n Example: LocalizationMain  /openbeacon/LIB/raw/reader/# reader.txt\r\n");	
			System.exit(1);
		}
		
		parserMain me = new parserMain(args);
		
		Path file = Paths.get(args[0]);
		Charset charset = Charset.forName("latin1");
		try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
		    String line = null;
	
		    while ((line = reader.readLine()) != null) {

		       //1367758961914: Reader=130.102.86.52Tag=045EPower=2		    	
		    	try {
					String readerID = "";
					String tag = "";
					String power = "";
					String[] split = line.split("=");
					power = split[3];
					tag = split[2].substring(0,split[2].indexOf('P'));
					readerID = split[1].substring(0,split[1].indexOf('T'));
					//System.err.printf("Tag is %s, reader is %s, power is %s\r\n", tag, readerID, power);
					me.receivedPacket(tag, readerID, power);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
		} catch (IOException x) {
		    System.err.format("IOException: %s%n", x);
		    x.printStackTrace();
		}
		System.out.printf("Completed\r\n");
	}
	
	
	

}
