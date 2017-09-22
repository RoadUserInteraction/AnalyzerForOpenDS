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

package eu.opends.main;

import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.math.ColorRGBA;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.font.BitmapText;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Curve;
import com.jme3.scene.shape.Cylinder;
import com.jme3.system.AppSettings;
import com.sun.javafx.application.PlatformImpl;

import de.lessvoid.nifty.Nifty;
import eu.opends.analyzer.DataUnit;
import eu.opends.analyzer.DeviationComputer;
import eu.opends.analyzer.DataReader;
import eu.opends.analyzer.IdealLine;
import eu.opends.analyzer.IdealLine.IdealLineStatus;
import eu.opends.basics.InternalMapProcessing;
import eu.opends.basics.SimulationBasics;
import eu.opends.camera.AnalyzerCam;
import eu.opends.car.SteeringCar;
import eu.opends.drivingTask.DrivingTask;
import eu.opends.environment.TrafficLightCenter;
import eu.opends.input.KeyBindingCenter;
import eu.opends.knowledgeBase.KnowledgeBase;
import eu.opends.niftyGui.AnalyzerFileSelectionGUIController;
import eu.opends.profiler.BasicProfilerState;
import eu.opends.taskDescription.tvpTask.MotorwayTask;
import eu.opends.tools.PanelCenter;
import eu.opends.traffic.Pedestrian;
import eu.opends.traffic.PhysicalTraffic;
import eu.opends.traffic.TrafficObject;
import eu.opends.trigger.TriggerCenter;

/**
 * 
 * @author Saied Tehrani, Rafael Math
 */
public class DriveAnalyzer extends SimulationBasics 
{	
	private boolean showRelativeTime = true;
	private boolean pointsEnabled = false;
	private boolean lineEnabled = true;
	private boolean coneEnabled = true;
	
	private boolean autorun = false;
	private String KB_ip_addr = "127.0.0.1";
	private int KB_port = 55432;
	private int maxFramerate = 300;

	private Nifty nifty;
    private boolean analyzerFileGiven = false;
    public String analyzerFilePath = "";
    private boolean initializationFinished = false;
    private boolean updateMessageBox = true;
    
    private boolean replayIsRunning = false;
    private long offset = 0;

	private Node pointNode = new Node();
	private Node lineNode = new Node();
	private Node coneNode = new Node();
	private Node target = new Node();
	private int targetIndex = 0;
	
	private double totalDistance = 0;

	private BitmapText markerText, speedText, timeText;
	
	private ArrayList<Vector3f> carPositionList = new ArrayList<Vector3f>();
	
	// MOD: Steering car & Other traffic
	private SteeringCar car;
    public SteeringCar getCar()
    {
    	return car;
    }
	private PhysicalTraffic physicalTraffic;
	public PhysicalTraffic getPhysicalTraffic()
	{
		return physicalTraffic;
		
	}
	private MotorwayTask motorwayTask;
	
	LinkedList<ArrayList<Vector3f>> trafficCarPositionLists = new LinkedList<ArrayList<Vector3f>>();
	
	private LinkedList<DataUnit> carDataUnitList = new LinkedList<DataUnit>();
	
	// MOD: Added trigger
	private TriggerCenter triggerCenter = new TriggerCenter(this);
	public TriggerCenter getTriggerCenter()
	{
		return triggerCenter;
	}
	
	// MOD: Other traffic
	private LinkedList<String> trafficCarNames = new LinkedList<String>();
	private LinkedList<String> pedestrianNames = new LinkedList<String>();
	private LinkedList<LinkedList<DataUnit>> trafficCarDataUnitList = new LinkedList<LinkedList<DataUnit>>();
	private LinkedList<LinkedList<DataUnit>> pedestrianDataUnitList = new LinkedList<LinkedList<DataUnit>>();

	
	private DataReader dataReader = new DataReader();
	private Long initialTimeStamp = 0l;

	public enum VisualizationMode
	{
		POINT, LINE, CONE;
	}

	private DataUnit currentCarDataUnit;
	public DataUnit getCurrentCarDataUnit() 
	{
		return currentCarDataUnit;
	}
	
	// MOD: Other traffic data units
	private LinkedList<DataUnit> currentTrafficCarDataUnit;
	public LinkedList<DataUnit> getCurrentTrafficCarDataUnit() 
	{
		return currentTrafficCarDataUnit;
	}
	private LinkedList<DataUnit> currentPedestrianDataUnit;
	public LinkedList<DataUnit> getCurrentPedestrianDataUnit() 
	{
		return currentPedestrianDataUnit;
	}
	
	@Override
	public void simpleInitApp() 
	{
		setDisplayFps(false);
		setDisplayStatView(false);
		
		assetManager.registerLocator("assets", FileLocator.class);
		
    	if(analyzerFileGiven)
    		simpleInitAnalyzerFile();
    	else
    		initAnalyzerFileSelectionGUI();
	}	
		
	
	private void initAnalyzerFileSelectionGUI()
	{
		NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
    	
    	// Create a new NiftyGUI object
    	nifty = niftyDisplay.getNifty();
    		
    	String xmlPath = "Interface/AnalyzerFileSelectionGUI.xml";
    	
    	// Read XML and initialize custom ScreenController
    	nifty.fromXml(xmlPath, "start", new AnalyzerFileSelectionGUIController(this, nifty));
    		
    	// attach the Nifty display to the gui view port as a processor
    	guiViewPort.addProcessor(niftyDisplay);
    	
    	// disable fly cam
    	flyCam.setEnabled(false);
	}
	
	
	public void closeAnalyzerFileSelectionGUI() 
	{
		nifty.exit();
        inputManager.setCursorVisible(false);
        flyCam.setEnabled(true);
	}

	
	public boolean isValidAnalyzerFile(File analyzerFile)
	{
		return dataReader.isValidAnalyzerFile(analyzerFile);
	}
	

	private ArrayList<IdealLine> idealLineList = new ArrayList<IdealLine>();
	
	public void simpleInitAnalyzerFile()
	{
		stateManager.attach(new BasicProfilerState(false));
		
		loadDrivingTask();
		
		PanelCenter.init(this);
		
		loadData();
		
		super.simpleInitApp();

		loadMap();
		
		// MOD: Init traffic light center
		trafficLightCenter = new TrafficLightCenter(this);
		
		// MOD: create and place steering car
		car = new SteeringCar(this);
		
		
		motorwayTask = new MotorwayTask(this);
		
		// MOD: Init other traffic
		physicalTraffic = new PhysicalTraffic(this);
		
		// setup key binding
		keyBindingCenter = new KeyBindingCenter(this);
     
		DeviationComputer devComp = new DeviationComputer(carPositionList);
		//devComp.showAllWayPoints();
		
		idealLineList = devComp.getIdealLines();

		for(IdealLine idealLine : idealLineList)
		{
			if(idealLine.getStatus() != IdealLineStatus.Unavailable)
			{
				String id = idealLine.getId();
				float area = idealLine.getArea();
				float length = idealLine.getLength();
				System.out.println("Area between ideal line (" + id + ") and driven line: " + area);
				System.out.println("Length of ideal line: " + length);
				System.out.println("Mean deviation: " + (float)area/length);
				System.out.println("Status: " + idealLine.getStatus() + "\n");
			}
		}
		
		createText();
		
        // setup camera settings
		cameraFactory = new AnalyzerCam(this, target, car);
		target.attachChild(cameraFactory.getMainCameraNode()); // TODO
		
		// MOD: Init trigger center
		triggerCenter.setup();
		
		
        visualizeData();
        
		// open TCP connection to KAPcom (knowledge component)
		KnowledgeBase.KB.setCulture("en-US");
		KnowledgeBase.KB.Initialize(this, KB_ip_addr, KB_port);
		KnowledgeBase.KB.start();
        
		if(autorun)
			startReplay();
		
        initializationFinished = true;
	}



	/**
	 * Loading the data from <code>path</code> and storing them in the
	 * appropriate data-structures.
	 * 
	 * @param analyzerFilePath
	 */
	private void loadData()
	{
		dataReader.initReader(analyzerFilePath, true);
		dataReader.loadDriveData();
				
		carPositionList = dataReader.getCarPositionList();
		
		// MOD: TrafficCar position list
		trafficCarPositionLists = dataReader.getTrafficCarPositionLists();
		
		totalDistance = dataReader.getTotalDistance();
		carDataUnitList = dataReader.getCarDataUnitList();
		
		
		// MOD: For other traffic check folder 'trafficData'
		String trackStr = "_track";
		try
		{
			// Track available
			int trackIndex = Integer.parseInt(analyzerFilePath.substring(analyzerFilePath.lastIndexOf("track") + 5, analyzerFilePath.length() - 4));	
			trackStr += trackIndex;
		}
		catch(Exception ex)
		{
			trackStr = "";
		}
		try
		{
			File trafficCarFolder = new File((new File(analyzerFilePath)).getParent() + "/trafficData" + trackStr + "/trafficCar/");
		    for (final File fileEntry : trafficCarFolder.listFiles())
		    {
		        if (fileEntry.isFile())
		        {
		        	trafficCarNames.add(fileEntry.getName().replaceFirst("[.][^.]+$", ""));
		        	dataReader.initReader(fileEntry.getAbsolutePath(), false);
		        	dataReader.loadTrafficCarData();
		        }
		    }
		    trafficCarDataUnitList = dataReader.getTrafficCarDataUnitList();
		}
		catch(Exception ex)
		{}
	    
		try
		{
		    File pedestrianFolder = new File((new File(analyzerFilePath)).getParent() + "/trafficData" + trackStr + "/pedestrian/");
		    for (final File fileEntry : pedestrianFolder.listFiles())
		    {
		        if (fileEntry.isFile())
		        {
		        	pedestrianNames.add(fileEntry.getName().replaceFirst("[.][^.]+$", ""));
		        	dataReader.initReader(fileEntry.getAbsolutePath(), false);
		        	dataReader.loadPedestrianData();
		        }
		    }
		    pedestrianDataUnitList = dataReader.getPedestrianDataUnitList();
		}
		catch(Exception ex)
		{}
		
		if(carDataUnitList.size() > 0)
			initialTimeStamp = carDataUnitList.get(0).getDate().getTime();
	}
	
	
    public void toggleReplay()
    {    	
    	if(!replayIsRunning)
    		startReplay();
    	else
    		stopReplay();
    }
    
    
    public void startReplay()
    {
    	replayIsRunning = true;
    	    	
		// end has been reached
		if((targetIndex + 1) >= carDataUnitList.size())
		{
			// make cone at last position invisible (if exists)
			Spatial currentCone = coneNode.getChild("cone_" + targetIndex);
			if(currentCone != null)
				currentCone.setCullHint(CullHint.Always);
			
			// reset camera to first position 
			targetIndex = 0;
			updateView(carDataUnitList.get(targetIndex), getDataUnitRow(trafficCarDataUnitList, targetIndex), getDataUnitRow(pedestrianDataUnitList, targetIndex));
		}
		
		// offset between current time and time in replay (at current position)
		offset = System.currentTimeMillis() - carDataUnitList.get(targetIndex).getDate().getTime();
    }
    
    
    public void stopReplay()
    {
    	replayIsRunning = false;
    }
	
    
	private void loadDrivingTask()
	{
		String drivingTaskName = dataReader.getNameOfDrivingTaskFile();
		File drivingTaskFile = new File(drivingTaskName);
		drivingTask = new DrivingTask(this,drivingTaskFile);
		
		sceneLoader = drivingTask.getSceneLoader();
		scenarioLoader = drivingTask.getScenarioLoader();
		interactionLoader = drivingTask.getInteractionLoader();
		settingsLoader = drivingTask.getSettingsLoader();
	}
	
	
	/**
	 * This method is used to generate the additional Text-elements.
	 */
	private void createText() 
	{
	    guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        
        markerText = new BitmapText(guiFont, false);
        markerText.setName("markerText");
        markerText.setText("");
        markerText.setCullHint(CullHint.Dynamic);
        markerText.setSize(guiFont.getCharSet().getRenderedSize());
        markerText.setColor(ColorRGBA.LightGray);
        markerText.setLocalTranslation(0, 20, 0);
        guiNode.attachChild(markerText);

        timeText = new BitmapText(guiFont, false);
        timeText.setName("timeText");
        timeText.setText("");
        timeText.setCullHint(CullHint.Dynamic);
        timeText.setSize(guiFont.getCharSet().getRenderedSize());
        timeText.setColor(ColorRGBA.LightGray);
        timeText.setLocalTranslation(settings.getWidth() / 2 - 125, 20,	0);
        guiNode.attachChild(timeText);
        
        speedText = new BitmapText(guiFont, false);
        speedText.setName("speedText");
        speedText.setText("");
        speedText.setCullHint(CullHint.Dynamic);
        speedText.setSize(guiFont.getCharSet().getRenderedSize());
        speedText.setColor(ColorRGBA.LightGray);
        speedText.setLocalTranslation(settings.getWidth() - 125, 20, 0);
        guiNode.attachChild(speedText);
	}

	
	private void visualizeData() 
	{
		for(IdealLine idealLine : idealLineList)
		{
			if(idealLine.getIdealPoints().size() >= 2)
			{
				/*
				 * Visualizing the distance between the car and the ideal line
				 */
				Material deviationMaterial = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
				deviationMaterial.setColor("Color", ColorRGBA.Red);
				
				Curve deviationLineCurve = new Curve(idealLine.getDeviationPoints().toArray(new Vector3f[0]), 1);
				deviationLineCurve.setMode(Mode.Lines);
				deviationLineCurve.setLineWidth(4f);
				Geometry geoDeviationLine = new Geometry("deviationLine_" + idealLine.getId(), deviationLineCurve);
				geoDeviationLine.setMaterial(deviationMaterial);
				sceneNode.attachChild(geoDeviationLine);
				
				
				/*
				 * Drawing the ideal Line
				 */
				Material idealMaterial = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
				idealMaterial.setColor("Color", ColorRGBA.Blue);
				
				Curve idealLineCurve = new Curve(idealLine.getIdealPoints().toArray(new Vector3f[0]), 1);
				idealLineCurve.setMode(Mode.Lines);
				idealLineCurve.setLineWidth(4f);
				Geometry geoIdealLine = new Geometry("idealLine_" + idealLine.getId(), idealLineCurve);
				geoIdealLine.setMaterial(idealMaterial);
				sceneNode.attachChild(geoIdealLine);
			}
		}
		
		/*
		 * Drawing the driven Line
		 */
		Material drivenMaterial = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
		drivenMaterial.setColor("Color", ColorRGBA.Yellow);
		
		// visualize points
		Curve points = new Curve(carPositionList.toArray(new Vector3f[0]), 1);
		points.setMode(Mode.Points);
		points.setPointSize(4f);
		Geometry geoPoints = new Geometry("drivenPoints", points);
		geoPoints.setMaterial(drivenMaterial);
		pointNode.attachChild(geoPoints);

		// visualize line
		Curve line = new Curve(carPositionList.toArray(new Vector3f[0]), 1);
		line.setMode(Mode.Lines);
		line.setLineWidth(2f);	// originally 4f
		Geometry geoLine = new Geometry("drivenLine", line);
	    geoLine.setMaterial(drivenMaterial);
	    lineNode.attachChild(geoLine);
	
	    // visualize cones
	    Material coneMaterial = new Material(assetManager,"Common/MatDefs/Misc/Unshaded.j3md");
	    coneMaterial.setColor("Color", ColorRGBA.Black);
		
		for (int i=0; i<carDataUnitList.size(); i++) 
		{
			Cylinder cone = new Cylinder(10, 10, 0.3f, 0.01f, 0.9f, true, false);
			cone.setLineWidth(4f);
			Geometry geoCone = new Geometry("cone_"+i, cone);
			geoCone.setLocalTranslation(carDataUnitList.get(i).getCarPosition());
			geoCone.setLocalRotation(carDataUnitList.get(i).getCarRotation());
			geoCone.setMaterial(coneMaterial);
			geoCone.setCullHint(CullHint.Always);
			coneNode.attachChild(geoCone);
		}

		if (pointsEnabled)
			sceneNode.attachChild(pointNode);
		
		if (lineEnabled)
			sceneNode.attachChild(lineNode);
		
		if (coneEnabled)
			sceneNode.attachChild(coneNode);
		
		// set camera view and time/speed texts
		updateView(carDataUnitList.get(targetIndex), getDataUnitRow(trafficCarDataUnitList, targetIndex), getDataUnitRow(pedestrianDataUnitList, targetIndex));
	}


	public void toggleVisualization(VisualizationMode vizMode) 
	{
		if(!isPause())
		{
			switch (vizMode) {
			case POINT:
	
				if (pointsEnabled) {
					sceneNode.detachChild(pointNode);
					pointsEnabled = false;
				} else {
					sceneNode.attachChild(pointNode);
					pointsEnabled = true;
				}
	
				break;
	
			case LINE:
	
				if (lineEnabled) {
					sceneNode.detachChild(lineNode);
					lineEnabled = false;
				} else {
					sceneNode.attachChild(lineNode);
					lineEnabled = true;
				}
	
				break;
	
			case CONE:
	
				if (coneEnabled) {
					sceneNode.detachChild(coneNode);
					coneEnabled = false;
				} else {
					sceneNode.attachChild(coneNode);
					coneEnabled = true;
				}
	
				break;
	
			default:
				break;
			}
		}

	}


	/**
	 * Does load a physics map. Here the elements of the map are also further
	 * processed by using the class <code>InternalMapProcessing</code>, e.g.
	 * replacing symbolic elements by a simulated counterpart.
	 */
	private void loadMap() 
	{
    	//load map model and setup car
		new InternalMapProcessing(this);
	}

	
	/**
	 * <code>moveFocus()</code> sets the position of the target. The target's
	 * position is equal to one of the data-points, whereas the direction
	 * specifies which of the neighbors in the data-point list should be taken.
	 * 
	 * @param direction
	 * 			Specifies which of the neighbors (1 or -1) in the data-point list should be taken.
	 */
	public void moveFocus(int direction) 
	{
		if(!replayIsRunning)
		{
			if (!isPause() && direction == 1 && (targetIndex + 1) < carDataUnitList.size()) 
			{
				targetIndex++;
				updateView(carDataUnitList.get(targetIndex), getDataUnitRow(trafficCarDataUnitList, targetIndex), getDataUnitRow(pedestrianDataUnitList, targetIndex));
			}
	
			if (!isPause() && direction == -1 && (targetIndex - 1) >= 0)
			{
				targetIndex--;
				updateView(carDataUnitList.get(targetIndex), getDataUnitRow(trafficCarDataUnitList, targetIndex), getDataUnitRow(pedestrianDataUnitList, targetIndex));
			}
		}
	}

	// MOD: Add parameters for traffic data units
	private void updateView(DataUnit carDataUnit, LinkedList<DataUnit> trafficCarDataUnit, LinkedList<DataUnit> pedestrianDataUnit) 
	{
		// Store previous data units
		LinkedList<DataUnit> previousPedestrianDataUnit = currentPedestrianDataUnit;
		
		currentCarDataUnit = carDataUnit;
		currentTrafficCarDataUnit = trafficCarDataUnit;
		currentPedestrianDataUnit = pedestrianDataUnit;
		
		target.setLocalTranslation(currentCarDataUnit.getCarPosition());
		target.setLocalRotation(currentCarDataUnit.getCarRotation());
		cameraFactory.updateCamera();
		
		// update speed text
		DecimalFormat decimalFormat = new DecimalFormat("#0.00");
		speedText.setText(decimalFormat.format(currentCarDataUnit.getSpeed()) + " km/h");
		
		// update timestamp
		updateTimestamp();

		// make previous cone invisible (if exists)
		Spatial previousCone = coneNode.getChild("cone_" + (targetIndex-1));		
		if(previousCone != null)
			previousCone.setCullHint(CullHint.Always);
		
		// make current cone visible (if exists)
		Spatial currentCone = coneNode.getChild("cone_" + targetIndex);
		if(currentCone != null)
			currentCone.setCullHint(CullHint.Dynamic);
		
		// make next cone invisible (if exists)
		Spatial nextCone = coneNode.getChild("cone_" + (targetIndex+1));
		if(nextCone != null)
			nextCone.setCullHint(CullHint.Always);
		
		// MOD: Update steering car position and rotation - Why negative Y-rotation necessary?
		car.setPosition(currentCarDataUnit.getCarPosition());
		car.setRotation(currentCarDataUnit.getCarRotation().getX(), -currentCarDataUnit.getCarRotation().getY(), currentCarDataUnit.getCarRotation().getZ(), currentCarDataUnit.getCarRotation().getW());;
				
		// MOD: Update other traffic position and rotation
		for(int i = 0; i < trafficCarNames.size(); i++)
		{
			physicalTraffic.getTrafficObject(trafficCarNames.get(i)).setPosition(currentTrafficCarDataUnit.get(i).getCarPosition());
			physicalTraffic.getTrafficObject(trafficCarNames.get(i)).setRotation(currentTrafficCarDataUnit.get(i).getCarRotation());
		}
		for(int i = 0; i < pedestrianNames.size(); i++)
		{
			float walkingSpeed = 0.0f;

			if((float)currentPedestrianDataUnit.get(i).getSpeed() > 0.0)
			{
				// In case there is already a speed available in the data
				walkingSpeed = (float)currentPedestrianDataUnit.get(i).getSpeed();
			}
			else if(previousPedestrianDataUnit != null)
			{
				// If no speed available, calculate from current and previous position
				float ds = currentPedestrianDataUnit.get(i).getCarPosition().subtract(previousPedestrianDataUnit.get(i).getCarPosition()).length();
				int dt = (int) (currentPedestrianDataUnit.get(i).getDate().getTime() - previousPedestrianDataUnit.get(i).getDate().getTime());
				
				walkingSpeed = ds / (float)dt * 3600f;
			}
			
			physicalTraffic.getTrafficObject(pedestrianNames.get(i)).setWalkingSpeedKmh(walkingSpeed);
			physicalTraffic.getTrafficObject(pedestrianNames.get(i)).setPosition(currentPedestrianDataUnit.get(i).getCarPosition());
			physicalTraffic.getTrafficObject(pedestrianNames.get(i)).setRotation(currentPedestrianDataUnit.get(i).getCarRotation());
		}
				
		updateMessageBox();
	}


	private void updateMessageBox() 
	{
		DecimalFormat decimalFormat = new DecimalFormat("#0.00");
		String speedString = " speed: " + decimalFormat.format(currentCarDataUnit.getSpeed()) + " km/h";
		
		Long currentTimeStamp = currentCarDataUnit.getDate().getTime();
		Long elapsedTime = currentTimeStamp - initialTimeStamp;
		SimpleDateFormat relativeDateFormat = new SimpleDateFormat("mm:ss.S");
		String relativeTimeString = "elapsed time: " + relativeDateFormat.format(elapsedTime);
		
		SimpleDateFormat absoluteDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
		String absoluteTimeString = " (" + absoluteDateFormat.format(new Date(currentTimeStamp)) + ")";
		
		String timeString = relativeTimeString + absoluteTimeString;
		
		String deviationString = "";
		for(IdealLine idealLine : idealLineList)
		{
			if(idealLine.getStatus() != IdealLineStatus.Unavailable)
			{
				String id = idealLine.getId();
				float area = idealLine.getArea();
				float length = idealLine.getLength();
				String status = idealLine.getStatus() == IdealLineStatus.Complete ? "complete" : "incomplete";
				
				String textString = " mean deviation '" + id + "': " + decimalFormat.format((float)area/length)
						+ " m (a: " + decimalFormat.format(area) + " m^3, l: " + decimalFormat.format(length) 
						+ " m, " + status +	")";
				
				String textBuffer = "";
				for(int i = 80; i>textString.length();i--)
					textBuffer += " ";
				
				deviationString += textString + textBuffer;
			}
		}
		
		String distanceString = " traveled: " + decimalFormat.format(currentCarDataUnit.getTraveledDistance()) + " m (total: " + 
				decimalFormat.format(totalDistance) + " m)";
		
		String steeringWheelString = " steering wheel: " + decimalFormat.format(-100*currentCarDataUnit.getSteeringWheelPos()) + "%";
		
		String acceleratorString = " acceleration: " + decimalFormat.format(100*currentCarDataUnit.getAcceleratorPedalPos()) + "%";
		
		String brakeString = " brake: " + decimalFormat.format(100*currentCarDataUnit.getBrakePedalPos()) + "%";
		
		String timeBuffer = "";
		for(int i = 130; i>timeString.length(); i--)
			timeBuffer += " ";
		
		String distanceBuffer = "";
		String distSpeedString = distanceString + speedString;
		for(int i = 130; i>distSpeedString.length();i--)
			distanceBuffer += " ";
		
		String total = timeString + timeBuffer +
				distanceString + speedString + distanceBuffer +
				deviationString +
				steeringWheelString + acceleratorString + brakeString;
		
		
		PanelCenter.getMessageBox().addMessage(total, 0);
	}


	private void updateTimestamp() 
	{
		Long currentTimeStamp = carDataUnitList.get(targetIndex).getDate().getTime();
		
		if(showRelativeTime)
		{
			Long elapsedTime = currentTimeStamp - initialTimeStamp;
			SimpleDateFormat dateFormat = new SimpleDateFormat("mm:ss.S");
			timeText.setText(dateFormat.format(elapsedTime));
		}
		else
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			timeText.setText(dateFormat.format(new Date(currentTimeStamp)));
		}
	}
	

    @Override
    public void simpleUpdate(float tpf)
    {
    	if(initializationFinished)
    	{
			// updates camera
			super.simpleUpdate(tpf);
			
			// MOD: Update trigger center
			triggerCenter.doTriggerChecks();
						
			// MOD: Update traffic
			for(TrafficObject trafficObject : PhysicalTraffic.getTrafficObjectList())
				if(trafficObject.getClass().equals(Pedestrian.class))
					trafficObject.update(tpf,  PhysicalTraffic.getTrafficObjectList());	

			
			// MOD: replayPlaying update
			for(int i = 0; i < pedestrianNames.size(); i++)
			{
				physicalTraffic.getTrafficObject(pedestrianNames.get(i)).setReplayRunning(replayIsRunning);
			}
			
			motorwayTask.update(tpf);
			
			if(updateMessageBox)
				PanelCenter.getMessageBox().update();
			
			if(replayIsRunning)
				updatePosition();
						
			try {
				Thread.sleep((long) (Math.max((1000/maxFramerate)-tpf,0)));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
    }
    
    
    // MOD: Additional method
    private LinkedList<DataUnit> getDataUnitRow(LinkedList<LinkedList<DataUnit>> table, int timeIndex)
    {
    	LinkedList<DataUnit> result = new LinkedList<DataUnit>();
    	
    	for(LinkedList<DataUnit> list : table)
    	{
    		result.add(list.get(timeIndex));
    	}
    	
    	return result;
    }
    

    private void updatePosition()
	{
		if((targetIndex + 1) < carDataUnitList.size())
		{
			// offset translates current time string to recording time
			long currentRecordingTime = System.currentTimeMillis() - offset;
			long timeAtNextTarget = carDataUnitList.get(targetIndex + 1).getDate().getTime();
			
			if(currentRecordingTime >= timeAtNextTarget)
			{				
				targetIndex++;
				updateView(carDataUnitList.get(targetIndex), getDataUnitRow(trafficCarDataUnitList, targetIndex), getDataUnitRow(pedestrianDataUnitList, targetIndex));
			}
			else
			{
				// provide previous and next data units
				DataUnit previous = carDataUnitList.get(targetIndex);
				DataUnit next = carDataUnitList.get(targetIndex+1);
								
				// MOD: previous and next data units for other traffic
				LinkedList<DataUnit> previousTrafficCar = getDataUnitRow(trafficCarDataUnitList, targetIndex);
				LinkedList<DataUnit> nextTrafficCar = getDataUnitRow(trafficCarDataUnitList, targetIndex+1);
				LinkedList<DataUnit> previousPedestrian = getDataUnitRow(pedestrianDataUnitList, targetIndex);
				LinkedList<DataUnit> nextPedestrian = getDataUnitRow(pedestrianDataUnitList, targetIndex+1);
								
				// interpolate between previous and next data unit
				DataUnit interpolatedDataUnit = DataUnit.interpolate(previous, next, currentRecordingTime);

				// MOD: Interpolation for other traffic
				LinkedList<DataUnit> interpolatedTrafficCarDataUnit = DataUnit.interpolate(previousTrafficCar, nextTrafficCar, currentRecordingTime);
				LinkedList<DataUnit> interpolatedPedestrianDataUnit = DataUnit.interpolate(previousPedestrian, nextPedestrian, currentRecordingTime);
				
				updateView(interpolatedDataUnit, interpolatedTrafficCarDataUnit, interpolatedPedestrianDataUnit);
			}
		}
		else
		{
			// reset replay when last position has been reached
			replayIsRunning = false;
		}
	}


	/**
	 * Cleanup after game loop was left 
	 */
    /*
	@Override
    public void stop() 
    {
		if(initializationFinished)
			super.stop();
  
    	System.exit(0);
    }
	*/


	/**
	 * Cleanup after game loop was left
	 * Will be called whenever application is closed.
	 */
	@Override
	public void destroy()
    {
		if(initializationFinished)
		{
			KnowledgeBase.KB.disconnect();
		}
		
		// MOD: Close other traffic
		//physicalTraffic.close();

		super.destroy();
		System.exit(0);
    }
	
	
	public static void main(String[] args)
	{
		Logger.getLogger("").setLevel(Level.SEVERE);
		DriveAnalyzer analyzer = new DriveAnalyzer();

    	if(args.length >= 1)
    	{
    		analyzer.analyzerFilePath = args[0];
    		analyzer.analyzerFileGiven = true;
    		
    		if(!analyzer.isValidAnalyzerFile(new File(args[0])))
    			return;
    	}
    	
    	if(args.length >= 2)
    	{
    		analyzer.autorun = Boolean.parseBoolean(args[1]);
    	}
    	
    	if(args.length >= 3)
    	{
    		analyzer.KB_ip_addr = args[2];
    	}
    	
    	if(args.length >= 4)
    	{
    		analyzer.KB_port = Integer.parseInt(args[3]);
    	}
    	
    	if(args.length >= 5)
    	{
    		analyzer.maxFramerate = Integer.parseInt(args[4]);
    	}    	
    	
    	
    	// MOD: TEST
    	PlatformImpl.startup(() -> {});
    	
    	
    	// CN: Modifications of Analyzer to shortcut the settings window
    	StartPropertiesReader startPropertiesReader = new StartPropertiesReader();
    	analyzer.setSettings(startPropertiesReader.getSettings());

		// show/hide settings screen
    	analyzer.setShowSettings(startPropertiesReader.showSettingsScreen());
    	
    	if (!analyzer.analyzerFileGiven){ //if no file path is given as an argument, then check the startProperties file
	    	if(startPropertiesReader.getDrivingTaskPath()!=null && !startPropertiesReader.getDrivingTaskPath().isEmpty()){
	    		analyzer.analyzerFilePath = startPropertiesReader.getDrivingTaskPath();
				analyzer.analyzerFileGiven = true;
				
				if(!analyzer.isValidAnalyzerFile(new File(startPropertiesReader.getDrivingTaskPath())))
					return;
	    	}
    	}
//    	AppSettings settings = new AppSettings(false);
//
//        settings.setUseJoysticks(true);
//        settings.setSettingsDialogImage("OpenDS.png");
//        settings.setTitle("OpenDS Analyzer");
//        settings.setWidth(768);
//        
//		analyzer.setSettings(settings);		

		analyzer.setPauseOnLostFocus(false);
		analyzer.start();
	}


	public void toggleMessageBoxUpdates() 
	{
		updateMessageBox = !updateMessageBox;
	}
}
