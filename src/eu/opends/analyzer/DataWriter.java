/*
*  This file is part of OpenDS (Open Source Driving Simulator).
*  Copyright (C) 2016 Rafael Math
*
*  OpenDS is free software: you can redistribute it and/or modify
*  it under the terms of the GNU General Public License as published by
*  the Free Software Foundation, either version 3 of the License, or
*  (at your option) any later version.
*
*  OpenDS is distributed in the hope that it will be useful,
*  but WITHOUT ANY WARRANTY; without even the implied warranty of
*  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*  GNU General Public License for more details.
*
*  You should have received a copy of the GNU General Public License
*  along with OpenDS. If not, see <http://www.gnu.org/licenses/>.
*/

package eu.opends.analyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import eu.opends.car.Car;
import eu.opends.tools.Util;
import eu.opends.traffic.Pedestrian;
import eu.opends.traffic.PhysicalTraffic;
import eu.opends.traffic.TrafficCar;
import eu.opends.traffic.TrafficObject;

/**
 * 
 * That class is responsible for writing drive-data. At the moment it is a
 * ripped down version of similar classes used in CARS.
 * 
 * @author Saied
 * 
 */
public class DataWriter
{
	private Calendar startTime = new GregorianCalendar();

	/**
	 * An array list for not having to write every row directly to file.
	 */
	private ArrayList<DataUnit> arrayDataList;
	
	private BufferedWriter out;
	private File outFile;	
	private String newLine = System.getProperty("line.separator");
	private long lastAnalyzerDataSave;
	private Car car;	
	private File analyzerDataFile;
	
	// MOD: Trigger data
	/*private File triggerDataFile;
	private File outFileTrigger;
	private ArrayList<String[]> arrayTriggerDataList;*/
	
	private boolean dataWriterEnabled = false;
	private String relativeDrivingTaskPath;

	// MOD: ArrayList for other traffic files
	private ArrayList<ArrayList<DataUnit>> trafficCarDataList, pedestrianDataList;
	private ArrayList<File> arrayTrafficCarDataFiles, arrayPedestrianDataFiles;

	/*
	public DataWriter(String outputFolder, Car car, String driverName, String absoluteDrivingTaskPath, int trackNumber)  
	{
		this.car = car;
		
		this.relativeDrivingTaskPath = getRelativePath(absoluteDrivingTaskPath);
		
		Util.makeDirectory(outputFolder);		

		if(trackNumber >= 0)
		{
			analyzerDataFile = new File(outputFolder + "/carData_track" + trackNumber + ".txt");
		}
		else
		{
			analyzerDataFile = new File(outputFolder + "/carData.txt");
		}

		
		if (analyzerDataFile.getAbsolutePath() == null) 
		{
			System.err.println("Parameter not accepted at method initWriter.");
			return;
		}
		
		outFile = new File(analyzerDataFile.getAbsolutePath());
		
		int i = 2;
		while(outFile.exists()) 
		{
			if(trackNumber >= 0)
				analyzerDataFile = new File(outputFolder + "/carData_track" + trackNumber + "(" + i + ").txt");
			else
				analyzerDataFile = new File(outputFolder + "/carData(" + i + ").txt");
			
			outFile = new File(analyzerDataFile.getAbsolutePath());
			i++;
		}
		
		
		try {
			out = new BufferedWriter(new FileWriter(outFile));
			out.write("Driving Task: " + relativeDrivingTaskPath + newLine);
			out.write("Date-Time: "
					+ new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss")
							.format(new Date()) + newLine);
			out.write("Driver: " + driverName + newLine);
			out.write("Used Format = Time (ms): Position (x,y,z) : Rotation (x,y,z,w) :"
					+ " Speed (km/h) : Steering Wheel Position [-1,1] : Gas Pedal Position :"
					+ " Brake Pedal Position : Engine Running" + newLine);

		} catch (IOException e) {
			e.printStackTrace();
		}
		arrayDataList = new ArrayList<DataUnit>();
		lastAnalyzerDataSave = (new Date()).getTime();
	}
	*/
	
	// MOD: new Constructor
	public DataWriter(String outputFolder, Car car, String driverName, String absoluteDrivingTaskPath, int trackNumber) 
	{
		this.car = car;
		this.relativeDrivingTaskPath = getRelativePath(absoluteDrivingTaskPath);
		
		arrayTrafficCarDataFiles = new ArrayList<File>();
		arrayPedestrianDataFiles = new ArrayList<File>();
		
		//MOD: init other traffic data arrays
		trafficCarDataList = new ArrayList<ArrayList<DataUnit>>();
		pedestrianDataList = new ArrayList<ArrayList<DataUnit>>();
		
		Util.makeDirectory(outputFolder);		
		
		System.out.println("Track number: " + trackNumber);

		if(trackNumber >= 0)
		{
			analyzerDataFile = new File(outputFolder + "/carData_track" + trackNumber + ".txt");
			
			// MOD: Trigger file
			//triggerDataFile = new File(outputFolder + "/triggerData_track" + trackNumber + ".txt");
			
			// MOD: Create directory for other traffic
			Util.makeDirectory(outputFolder + "/trafficData_track" + trackNumber);
			Util.makeDirectory(outputFolder + "/trafficData_track" + trackNumber + "/trafficCar");
			Util.makeDirectory(outputFolder + "/trafficData_track" + trackNumber + "/pedestrian");
			
			for (eu.opends.traffic.TrafficCarData trafficCarData : PhysicalTraffic.getVehicleDataList())
			{
				arrayTrafficCarDataFiles.add(new File(outputFolder + "/trafficData_track" + trackNumber + "/trafficCar/" + trafficCarData.getName() + ".txt"));
			}
			for (eu.opends.traffic.PedestrianData pedestrianData : PhysicalTraffic.getPedestrianDataList())
			{
				arrayPedestrianDataFiles.add(new File(outputFolder + "/trafficData_track" + trackNumber + "/pedestrian/" + pedestrianData.getName() + ".txt"));
			}
		}
		else
		{
			analyzerDataFile = new File(outputFolder + "/carData.txt");
			
			// MOD: Trigger file
			//triggerDataFile = new File(outputFolder + "/triggerData.txt");
			
			// MOD: Create directory for other traffic
			Util.makeDirectory(outputFolder + "/trafficData");
			Util.makeDirectory(outputFolder + "/trafficData/trafficCar");
			Util.makeDirectory(outputFolder + "/trafficData/pedestrian");
			
			for (eu.opends.traffic.TrafficCarData trafficCarData : PhysicalTraffic.getVehicleDataList())
			{
				arrayTrafficCarDataFiles.add(new File(outputFolder + "/trafficData/trafficCar/" + trafficCarData.getName() + ".txt"));
			}
			for (eu.opends.traffic.PedestrianData pedestrianData : PhysicalTraffic.getPedestrianDataList())
			{
				arrayPedestrianDataFiles.add(new File(outputFolder + "/trafficData/pedestrian/" + pedestrianData.getName() + ".txt"));
			}
		}

		
		if (analyzerDataFile.getAbsolutePath() == null)
		{
			System.err.println("Parameter not accepted at method initWriter.");
			return;
		}
		
		// MOD: Check for trigger data file
		/*if (triggerDataFile.getAbsolutePath() == null)
		{
			System.err.println("Parameter not accepted at method initWriter.");
			return;
		}*/
		
		// MOD: Check the same for other traffic
		for(File fTrafficCar : arrayTrafficCarDataFiles)
		{
			if (fTrafficCar.getAbsolutePath() == null) 
			{
				System.err.println("Parameter not accepted at method initWriter.");
				return;
			}
		}
		for(File fPedestrianCar : arrayPedestrianDataFiles)
		{
			if (fPedestrianCar.getAbsolutePath() == null) 
			{
				System.err.println("Parameter not accepted at method initWriter.");
				return;
			}
		}
		
		outFile = new File(analyzerDataFile.getAbsolutePath());
		
		int i = 2;
		while(outFile.exists())
		{
			if(trackNumber >= 0)
				analyzerDataFile = new File(outputFolder + "/carData_track" + trackNumber + "(" + i + ").txt");
			else
				analyzerDataFile = new File(outputFolder + "/carData(" + i + ").txt");
			
			outFile = new File(analyzerDataFile.getAbsolutePath());
			i++;
		}
		
		
		try {
			out = new BufferedWriter(new FileWriter(outFile, true));
			out.write("Driving Task: " + relativeDrivingTaskPath + newLine);
			out.write("Date-Time: "
					+ new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss")
							.format(new Date()) + newLine);
			out.write("Driver: " + driverName + newLine);
			out.write("Used Format = Time (ms): Position (x,y,z) : Rotation (x,y,z,w) :"
					+ " Speed (km/h) : Steering Wheel Position [-1,1] : Gas Pedal Position :"
					+ " Brake Pedal Position : Engine Running" + newLine);
			// MOD: close out file first
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		arrayDataList = new ArrayList<DataUnit>();
		
		lastAnalyzerDataSave = (new Date()).getTime();
		
		// MOD: Init trigger data file
		/*outFileTrigger = new File(triggerDataFile.getAbsolutePath());
		
		i = 2;
		while(outFileTrigger.exists())
		{
			if(trackNumber >= 0)
				triggerDataFile = new File(outputFolder + "/triggerData_track" + trackNumber + "(" + i + ").txt");
			else
				triggerDataFile = new File(outputFolder + "/triggerData(" + i + ").txt");
			
			outFileTrigger = new File(triggerDataFile.getAbsolutePath());
			i++;
		}
		
		try {
			out = new BufferedWriter(new FileWriter(outFileTrigger, true));
			out.write("Driving Task: " + relativeDrivingTaskPath + newLine);
			out.write("Date-Time: "
					+ new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss")
							.format(new Date()) + newLine);
			out.write("Driver: " + driverName + newLine);
			out.write("Used Format = Time (ms): Triggers" + newLine);
			// MOD: close out file first
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		arrayTriggerDataList = new ArrayList<String[]>();*/
		
		// MOD: Init traffic data files
		for(File file : arrayTrafficCarDataFiles)
		{
			// TODO: Case if more than one record of same task
			i = 2;
			while(file.exists())
			{
				if(trackNumber >= 0)
					file = new File(outputFolder + "/carData_track" + trackNumber + "(" + i + ").txt");
				else
					file = new File(outputFolder + "/carData(" + i + ").txt");
				
				file = new File(file.getAbsolutePath());
				i++;
			}
			
			
			try {
				out = new BufferedWriter(new FileWriter(new File(file.getAbsolutePath()), true));
				out.write("Driving Task: " + relativeDrivingTaskPath + newLine);
				out.write("Date-Time: "
						+ new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss")
								.format(new Date()) + newLine);
				out.write("Driver: " + driverName + newLine);
				out.write("Used Format = Time (ms): Position (x,y,z) : Rotation (x,y,z,w) :"
						+ " Speed (km/h) : Steering Wheel Position [-1,1] : Gas Pedal Position :"
						+ " Brake Pedal Position : Engine Running" + newLine);
				// MOD: close out file first
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for(File file : arrayPedestrianDataFiles)
		{
			// TODO: Case if more than one record of same task
			i = 2;
			while(file.exists())
			{
				if(trackNumber >= 0)
					file = new File(outputFolder + "/carData_track" + trackNumber + "(" + i + ").txt");
				else
				file = new File(outputFolder + "/carData(" + i + ").txt");
				
				file = new File(file.getAbsolutePath());
				i++;
			}
			
			try {
				out = new BufferedWriter(new FileWriter(new File(file.getAbsolutePath()), true));
				out.write("Driving Task: " + relativeDrivingTaskPath + newLine);
				out.write("Date-Time: "
						+ new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss")
								.format(new Date()) + newLine);
				out.write("Driver: " + driverName + newLine);
				out.write("Used Format = Time (ms): Position (x,y,z) : Rotation (x,y,z,w) :"
						+ " Speed (km/h) : Steering Wheel Position [-1,1] : Gas Pedal Position :"
						+ " Brake Pedal Position : Engine Running" + newLine);
				// MOD: close out file first
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	
	private String getRelativePath(String absolutePath)
	{
		URI baseURI = new File("./").toURI();
		URI absoluteURI = new File(absolutePath).toURI();
		URI relativeURI = baseURI.relativize(absoluteURI);
		
		return relativeURI.getPath();
	}


	/**
	 * Save the car data at a frequency of 20Hz. That class should be called in
	 * the update-method <code>Simulator.java</code>.
	 */
	public void saveAnalyzerData()
	{
		int updateInterval = 50; // = 1000/20
		
		Date curDate = new Date();

		if (curDate.getTime() - lastAnalyzerDataSave/*.getTime()*/ >= 2*updateInterval) 
		{
			lastAnalyzerDataSave = curDate.getTime() - 2*updateInterval;
		}
		
		
		if (curDate.getTime() - lastAnalyzerDataSave/*.getTime()*/ >= updateInterval) 
		{
			//System.err.println("diff: " + (curDate.getTime() - lastAnalyzerDataSave));
			
			DataUnit rowCar = new DataUnit(
					curDate,
					Math.round(car.getPosition().x * 1000) / 1000.0f,
					Math.round(car.getPosition().y * 1000) / 1000.0f,
					Math.round(car.getPosition().z * 1000) / 1000.0f,
					Math.round(car.getRotation().getX() * 10000) / 10000.0f,
					Math.round(car.getRotation().getY() * 10000) / 10000.0f,
					Math.round(car.getRotation().getZ() * 10000) / 10000.0f,
					Math.round(car.getRotation().getW() * 10000) / 10000.0f,
					car.getCurrentSpeedKmhRounded(), Math.round(car
							.getSteeringWheelState() * 100000) / 100000.0f, car
							.getAcceleratorPedalIntensity(), car.getBrakePedalIntensity(), 
							car.isEngineOn());
			
			// MOD: Save triggers
			/*if(!triggerList.isEmpty())
			{
				String[] triggers = new String[2];
				triggers[0] = curDate.getTime() + "";
				// Construct trigger string
				triggers[1] = "";
				for(String trigger : triggerList)
				{
					triggers[1] += trigger + ",";
				}
				triggers[1] = triggers[1].substring(0, triggers[1].length() - 1);
				// Check if triggers are new
				if(arrayTriggerDataList.isEmpty() ||
					!arrayTriggerDataList.get(arrayTriggerDataList.size() - 1)[1].equals(triggers[1]))
				{
					arrayTriggerDataList.add(triggers);
				}
			}*/
			
			// MOD: Save other traffic
			DataUnit[] rowsTrafficCar = new DataUnit[arrayTrafficCarDataFiles.size()];
			DataUnit[] rowsPedestrian = new DataUnit[arrayPedestrianDataFiles.size()];
									
			int i = 0;
			int j = 0;
			for(TrafficObject trafficObj : PhysicalTraffic.getTrafficObjectList())
			{
				if(trafficObj.getClass().equals(TrafficCar.class))
				{
					// Traffic Car
					rowsTrafficCar[i] = new DataUnit(curDate,
							Math.round(trafficObj.getPosition().x * 1000) / 1000.0f,
							Math.round(trafficObj.getPosition().y * 1000) / 1000.0f,
							Math.round(trafficObj.getPosition().z * 1000) / 1000.0f,
							Math.round(trafficObj.getRotation().getX() * 10000) / 10000.0f,
							Math.round(trafficObj.getRotation().getY() * 10000) / 10000.0f,
							Math.round(trafficObj.getRotation().getZ() * 10000) / 10000.0f,
							Math.round(trafficObj.getRotation().getW() * 10000) / 10000.0f,
							0, 0, 0, 0, true);
					
					i++;
				}
				else if(trafficObj.getClass().equals(Pedestrian.class))
				{
					// Pedestrian
					rowsPedestrian[j] = new DataUnit(curDate,
							Math.round(trafficObj.getPosition().x * 1000) / 1000.0f,
							Math.round(trafficObj.getPosition().y * 1000) / 1000.0f,
							Math.round(trafficObj.getPosition().z * 1000) / 1000.0f,
							Math.round(trafficObj.getRotation().getX() * 10000) / 10000.0f,
							Math.round(trafficObj.getRotation().getY() * 10000) / 10000.0f,
							Math.round(trafficObj.getRotation().getZ() * 10000) / 10000.0f,
							Math.round(trafficObj.getRotation().getW() * 10000) / 10000.0f,
							0, 0, 0, 0, true);
					j++;
				}
			}
			
			arrayDataList.add(rowCar);
			trafficCarDataList.add(new ArrayList<DataUnit>(Arrays.asList(rowsTrafficCar)));
			pedestrianDataList.add(new ArrayList<DataUnit>(Arrays.asList(rowsPedestrian)));
			
			if (arrayDataList.size() > 50)
				flush();
			
			//lastAnalyzerDataSave = curDate;
			lastAnalyzerDataSave += updateInterval;
		}

	}

	
	/*
	 * MAYBE NOT NECESSARY
	 * 
	// see eu.opends.analyzer.IAnalyzationDataWriter#write(float,
	//      float, float, float, java.util.Date, float, float, boolean, float)
	public void writeCar(Date curDate, float x, float y, float z, float xRot,
			float yRot, float zRot, float wRot, float linearSpeed,
			float steeringWheelState, float gasPedalState, float brakePedalState,
			boolean isEngineOn)
	{
		DataUnit rowCar = new DataUnit(curDate, x, y, z, xRot, yRot, zRot, wRot,
				linearSpeed, steeringWheelState, gasPedalState, brakePedalState,
				isEngineOn);
		
		// MOD: Other traffic
		DataUnit[] rowsTrafficCar = new DataUnit[arrayTrafficCarDataFiles.size()];
		DataUnit[] rowsPedestrian = new DataUnit[arrayPedestrianDataFiles.size()];

		//this.write(rowCar, new ArrayList<DataUnit>(Arrays.asList(rowsTrafficCar)), new ArrayList<DataUnit>(Arrays.asList(rowsPedestrian)));
	}

	
	/**
	 * Write data to the data pool. After 50 data sets, the pool is flushed to
	 * the file.
	 * 
	 * @param row
	 * 			Datarow to write
	 */
	public void write(DataUnit rowCar, ArrayList<DataUnit> rowTrafficCars, ArrayList<DataUnit> rowPedestrians, String[] rowTrigger)
	{
		arrayDataList.add(rowCar);
		
		//arrayTriggerDataList.add(rowTrigger);
		
		trafficCarDataList.add(rowTrafficCars);
		pedestrianDataList.add(rowPedestrians);
		
		if (arrayDataList.size() > 50)
			flush();
	}
	

	public void flush() 
	{
		try {
			// MOD
			out = new BufferedWriter(new FileWriter(outFile, true));
			
			StringBuffer sb = new StringBuffer();
			for (DataUnit r : arrayDataList) {
				sb.append(r.getDate().getTime() + ":" + r.getXpos() + ":"
						+ r.getYpos() + ":" + r.getZpos() + ":" + r.getXrot()
						+ ":" + r.getYrot() + ":" + r.getZrot() + ":"
						+ r.getWrot() + ":" + r.getSpeed() + ":"
						+ r.getSteeringWheelPos() + ":" + r.getAcceleratorPedalPos() + ":"
						+ r.getBrakePedalPos() + ":" + r.isEngineOn() + newLine
						);
			}
			out.write(sb.toString());
			arrayDataList.clear();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
		
		// MOD: Flush traffic data
		for(File file : arrayTrafficCarDataFiles)
		{
			try {
				out = new BufferedWriter(new FileWriter(file, true));
				
				StringBuffer sb = new StringBuffer();
				for (ArrayList<DataUnit> rList : this.trafficCarDataList) {
					DataUnit r = rList.get(arrayTrafficCarDataFiles.indexOf(file));
						sb.append(r.getDate().getTime() + ":" + r.getXpos() + ":"
							+ r.getYpos() + ":" + r.getZpos() + ":" + r.getXrot()
							+ ":" + r.getYrot() + ":" + r.getZrot() + ":"
							+ r.getWrot() + ":" + r.getSpeed() + ":"
							+ r.getSteeringWheelPos() + ":" + r.getAcceleratorPedalPos() + ":"
							+ r.getBrakePedalPos() + ":" + r.isEngineOn() + newLine
							);
				}
				out.write(sb.toString());
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		for(File file : arrayPedestrianDataFiles)
		{
			try {
				out = new BufferedWriter(new FileWriter(file, true));
				
				StringBuffer sb = new StringBuffer();
				for (ArrayList<DataUnit> rList : this.pedestrianDataList) {
					DataUnit r = rList.get(arrayPedestrianDataFiles.indexOf(file));
						sb.append(r.getDate().getTime() + ":" + r.getXpos() + ":"
							+ r.getYpos() + ":" + r.getZpos() + ":" + r.getXrot()
							+ ":" + r.getYrot() + ":" + r.getZrot() + ":"
							+ r.getWrot() + ":" + r.getSpeed() + ":"
							+ r.getSteeringWheelPos() + ":" + r.getAcceleratorPedalPos() + ":"
							+ r.getBrakePedalPos() + ":" + r.isEngineOn() + newLine
							);
				}
				out.write(sb.toString());
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		trafficCarDataList.clear();
		pedestrianDataList.clear();
	}

	/*public void flushTriggerData()
	{
		// MOD: Flush trigger data
		try {
			out = new BufferedWriter(new FileWriter(outFileTrigger, true));
		
			StringBuffer sb = new StringBuffer();
			for (String[] r : arrayTriggerDataList) {
				sb.append(r[0] + ":[" + r[1] + "]" + newLine);
			}
			out.write(sb.toString());
			//arrayTriggerDataList.clear();
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}*/

	
	public void quit() 
	{
		dataWriterEnabled = false;
		flush();
		try {
			if (out != null)
				out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	

	public boolean isDataWriterEnabled() 
	{
		return dataWriterEnabled;
	}

	
	public void setDataWriterEnabled(boolean dataWriterEnabled) 
	{
		this.dataWriterEnabled = dataWriterEnabled;
	}

	
	public void setStartTime() 
	{
		this.startTime = new GregorianCalendar();
	}
	
	
	public String getElapsedTime()
	{
		Calendar now = new GregorianCalendar();
		
		long milliseconds1 = startTime.getTimeInMillis();
	    long milliseconds2 = now.getTimeInMillis();
	    
	    long elapsedMilliseconds = milliseconds2 - milliseconds1;
	    
	    return "Time elapsed: " + new SimpleDateFormat("mm:ss.SSS").format(elapsedMilliseconds);
	}

}
