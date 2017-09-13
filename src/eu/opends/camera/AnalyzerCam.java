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

package eu.opends.camera;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.control.CameraControl;

import eu.opends.camera.CameraFactory.CameraMode;
import eu.opends.camera.CameraFactory.MirrorMode;
import eu.opends.car.Car;
import eu.opends.main.DriveAnalyzer;
import eu.opends.tools.PanelCenter;
import eu.opends.tools.Util;

/**
 * 
 * @author Rafael Math
 */
public class AnalyzerCam extends CameraFactory 
{
	private Car car;
	private Node carNode; //copy/pasting from SimulatorCam
	
	public AnalyzerCam(DriveAnalyzer analyzer, Node targetNode, Car car) 
	{
		this.car = car;
		carNode = car.getCarNode();
		initCamera(analyzer, targetNode);
		setCamMode(CameraMode.CHASE);
	}
	
	
	public void setCamMode(CameraMode mode)
	{		
		switch (mode) 
		{			
			case EGO: //from SimulatorCam.java
				camMode = CameraMode.EGO;
				chaseCam.setEnabled(false);
				updateCamera();
				break;
			case TOP:
				camMode = CameraMode.TOP;
				chaseCam.setEnabled(false);
				updateCamera();
				break;	
			case CHASE:
				camMode = CameraMode.CHASE;
				chaseCam.setEnabled(true);
				updateCamera();
				break;
				
			default: break;	
		}
	}
	
	
	public void changeCamera() 
	{
		switch (camMode) 
		{
			// CHASE --> EGO --> TOP --> CHASE --> ...
			case CHASE: setCamMode(CameraMode.EGO); break;
			case EGO: setCamMode(CameraMode.TOP); break;
			case TOP:setCamMode(CameraMode.CHASE); break;
			default: break;
		}
	}
	
	
	public void updateCamera()
	{
//		if(camMode == CameraMode.EGO)
//		{
//			if(mirrorMode == MirrorMode.ALL)
//			{
//				backViewPort.setEnabled(true);
//				leftBackViewPort.setEnabled(true);
//				rightBackViewPort.setEnabled(true);
//				backMirrorFrame.setCullHint(CullHint.Dynamic);
//				leftMirrorFrame.setCullHint(CullHint.Dynamic);
//				rightMirrorFrame.setCullHint(CullHint.Dynamic);
//			}
//			else if(mirrorMode == MirrorMode.BACK_ONLY)
//			{
//				backViewPort.setEnabled(true);
//				leftBackViewPort.setEnabled(false);
//				rightBackViewPort.setEnabled(false);
//				backMirrorFrame.setCullHint(CullHint.Dynamic);
//				leftMirrorFrame.setCullHint(CullHint.Always);
//				rightMirrorFrame.setCullHint(CullHint.Always);
//			}
//			else if(mirrorMode == MirrorMode.SIDE_ONLY)
//			{
//				backViewPort.setEnabled(false);
//				leftBackViewPort.setEnabled(true);
//				rightBackViewPort.setEnabled(true);
//				backMirrorFrame.setCullHint(CullHint.Always);
//				leftMirrorFrame.setCullHint(CullHint.Dynamic);
//				rightMirrorFrame.setCullHint(CullHint.Dynamic);
//			}
//			else
//			{
//				backViewPort.setEnabled(false);
//				leftBackViewPort.setEnabled(false);
//				rightBackViewPort.setEnabled(false);
//				backMirrorFrame.setCullHint(CullHint.Always);
//				leftMirrorFrame.setCullHint(CullHint.Always);
//				rightMirrorFrame.setCullHint(CullHint.Always);
//			}			
//		}
//		else
//		{
//			backViewPort.setEnabled(false);
//			leftBackViewPort.setEnabled(false);
//			rightBackViewPort.setEnabled(false);
//			
//			backMirrorFrame.setCullHint(CullHint.Always);
//			leftMirrorFrame.setCullHint(CullHint.Always);
//			rightMirrorFrame.setCullHint(CullHint.Always);
//		}
		
		if(camMode == CameraMode.EGO)
		{
			setCarVisible(false);
			// set camera position
			Vector3f targetPosition = targetNode.localToWorld(new Vector3f(0, 0, 0), null);
			Vector3f camPos = new Vector3f(targetPosition.x-0.5f, targetPosition.y+0.8f , targetPosition.z);
			cam.setLocation(camPos);
			
		
			// get rotation of target node
			Quaternion targetRotation = targetNode.getLocalRotation();
			
			// rotate cam direction by 180 degrees, since car is actually driving backwards
			Quaternion YAW180 = new Quaternion().fromAngleAxis(FastMath.PI, new Vector3f(0,1,0));
			targetRotation.multLocal(YAW180);
			
			// set camera rotation
			cam.setRotation(targetRotation);
		}
		
		if(camMode == CameraMode.CHASE)
		{
			setCarVisible(true);
			// set camera position
			Vector3f targetPosition = targetNode.localToWorld(new Vector3f(0, 0, 0), null);
			Vector3f camPos = new Vector3f(targetPosition.x, targetPosition.y + 2, targetPosition.z);
			cam.setLocation(camPos);
			
		
			// get rotation of target node
			Quaternion targetRotation = targetNode.getLocalRotation();
			
			// rotate cam direction by 180 degrees, since car is actually driving backwards
			Quaternion YAW180 = new Quaternion().fromAngleAxis(FastMath.PI, new Vector3f(0,1,0));
			targetRotation.multLocal(YAW180);
			
			// set camera rotation
			cam.setRotation(targetRotation);
		}
		
		else if(camMode == CameraMode.TOP)
		{
			setCarVisible(true);
			// set camera position
			Vector3f targetPosition = targetNode.localToWorld(new Vector3f(0, 0, 0), null);
			Vector3f camPos = new Vector3f(targetPosition.x, targetPosition.y + 30, targetPosition.z);
			cam.setLocation(camPos);

			// set camera direction
			Vector3f left = new Vector3f(-1, 0, 0);
			Vector3f up = new Vector3f(0, 0, -1);
			Vector3f direction = new Vector3f(0, -1f, 0);
			cam.setAxes(left, up, direction);
		}
	}

	public void setCarVisible(boolean setVisible) 
	{
		if(setVisible)
		{
			// e.g. outside car

			//carNode.setCullHint(CullHint.Never);

			// show everything except sub-geometries of node interior
			Node interior = Util.findNode(carNode, "interior");
			for(Geometry g : Util.getAllGeometries(carNode))
				if(g.hasAncestor(interior))
					g.setCullHint(CullHint.Always);  //interior
				else
					g.setCullHint(CullHint.Dynamic); //rest (or interior == null)
					
		}
		else
		{
			// e.g. inside car
			
			//carNode.setCullHint(CullHint.Always);

			// cull everything except sub-geometries of node interior
			Node interior = Util.findNode(carNode, "interior");
			for(Geometry g : Util.getAllGeometries(carNode))
				if(g.hasAncestor(interior))
					g.setCullHint(CullHint.Dynamic); //interior
				else
					g.setCullHint(CullHint.Always);  //rest (or interior == null)
					
		}
		
//		PanelCenter.showHood(!setVisible);
	}
}
