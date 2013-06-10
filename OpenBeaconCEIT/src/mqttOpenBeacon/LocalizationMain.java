/**
 * 
 */
package mqttOpenBeacon;

import java.awt.Point;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.Map.Entry;



import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;




/**
 * @author s4116591
 *
 */
public class LocalizationMain extends MqttClient{	
	
	private  HashMap<String, Point> readers;
	private volatile HashMap<String, Tag> tags;
	private ArrayList<String> referenceTags;
	private int maxNeighbours;
	
	
	/**
	 * @param args
	 * @throws MqttException 
	 */
	public LocalizationMain(String[] args) throws MqttException {
		super(args[0]);
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
			System.err.printf("Error reading file %s\r\n Expected format for line: readerIP xCoord yCoord\r\n", args[2]);
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
			System.err.printf("Error reading file %s\r\n Expected format for line: refenceID xCoord yCoord\r\n", args[3]);
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
		if ((tagID.equals("03BD")) || (tagID.equals("03B8")) || (tagID.equals("03D3"))){
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
			diff = Math.sqrt(Math.pow((x-345),2) + Math.pow((y-200),2));
		} else if (tagID.equals("03B8")){
			diff = Math.sqrt(Math.pow((x-345),2) + Math.pow((y-300),2));
		} else if (tagID.equals("03D3")){
			diff = Math.sqrt(Math.pow((x-345),2) + Math.pow((y-400),2));
		}
		log(String.valueOf(diff)+ "\r\n", tagID+ "_4.log");		
			

		Point location = new Point((int )x, (int)y);
		log(String.valueOf(now.getTime()) + "\r\n", "time.log");			

	//	log(String.valueOf(now.getTime()) + ": Tag="+ tagID + " x=" + String.valueOf(x) + " y="+String.valueOf(y)+"\r\n", "tagLocationLogfile.log");
		
		return location;
	}
	
	//React to an incoming topic
	protected void publishArrived(java.lang.String topic, byte[] message, int QoS, boolean retained)
		     throws java.lang.Exception{
				
				String packet = new String(message);
				//System.out.printf("Packet : %s\r\n", packet);
				String protocol = packet.substring(0,2);
				String tagID = packet.substring(2,6);
				String power = packet.substring(9,10);
				if (protocol.equals("46")){
					power = "3";
				}
				String[] readerSplit = topic.split("/",0);
				String readerID = readerSplit[readerSplit.length-1];
				receivedPacket(tagID, readerID, power);
	}
	

	
	//Try to reconnect if we disconnect
	public void connectionLost(){
		try {
			connect("andrewTest", false, (short) 100);
	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {			
			}
			connectionLost();
			
		}
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
			System.out.printf("Invalid arguments. \r\n Expected LocalizationMain <MQTTBroker> <MQTTTopic> <reader filename> <reference tag filename(optional)>\r\n Example: LocalizationMain tcp://winter.ceit.uq.edu.au:1883 /openbeacon/LIB/raw/reader/# reader.txt\r\n");	
			System.exit(1);
		}
		
		LocalizationMain me = new LocalizationMain(args);
		
		
		// TODO Auto-generated method stub
		String[] subscriptions = new String[1];
		int[] QOS = new int[1];
		subscriptions[0] = args[1];
		QOS[0]=0;
		
		Set<Map.Entry<String,Point>> readerSet = me.readers.entrySet();
		Iterator<Map.Entry<String, Point>> it = readerSet.iterator();
		while (it.hasNext()){
			Map.Entry<String,Point> next =  it.next();
			System.out.printf("Key %s has value %s\r\n", next.getKey(), next.getValue().toString());
		}
		Set<Map.Entry<String,Tag>> tagSet = me.tags.entrySet();
		Iterator<Entry<String, Tag>> itTag = tagSet.iterator();
		while (itTag.hasNext()){
			Map.Entry<String,Tag> next =  itTag.next();
			if (next.getValue().isRef()){
				System.out.printf("Tag %s is a reference tag, with coordinates %s\r\n", next.getKey(), next.getValue().getLoc().toString());
			}
			System.out.printf("Tag %s has readerlist %s\r\n", next.getKey(), next.getValue().toString());
		}
		
		try {
			
			me.connect("andrewTest", false, (short) 10);	
			me.subscribe(subscriptions, QOS);				
			//me.unsubscribe(subscriptions);
			//me.disconnect();
			//me.terminate();

		} catch (MqttException e) {
	
			me.terminate();
			e.printStackTrace();
			System.exit(1);
		}	
		System.out.printf("Logging of tags has started\r\n");
		
		while(true){

			
		}
		
	}
	
	
	

}
