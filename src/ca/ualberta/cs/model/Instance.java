package ca.ualberta.cs.model;

import java.util.ArrayList;
/**
 * @author lelis
 * @author jadson
 * Represents an object in a dataset
 * Each object can contains its own trueLabel to use in a validation case (not necessary)
 * and a flag showing if it is pre-labeled or not.
 */

public class Instance implements Comparable <Instance>{

	// ------------------------------ PRIVATE VARIABLES ------------------------------

	private int label                = -1;
	private int trueLabel            = -1;
	private int index                = -1;
	private boolean isPreLabeled     = false;
	private boolean isProcessed      = false;
	private boolean isPostProcessed  = false;
	private double coreDistance      = -1.0;
	private double[] coordinates;
	private double weight            = -1.0;
	private int clusterAssignment	 = -1;
	public int     preceeding 		 = 0;
	public int     next              = 0;
	public int     idLabeledObject   = -1;
	private Integer nearestLabeled = -1;



	// ------------------------------ CONSTANTS ------------------------------

	// ------------------------------ CONSTRUCTORS ------------------------------
	public Instance()
	{

	}

	public Instance(int l, int tl, int idx, boolean isPre, boolean isPro, boolean isPost, double core, double[] coord, double weight)
	{
		this.label		  	 = l;
		this.trueLabel    	 = tl;
		this.index		 	 = idx;
		this.isPreLabeled	 = isPre;
		this.isProcessed 	 = isPro;
		this.isPostProcessed = isPost;
		this.coreDistance	 = core;
		this.coordinates	 = coord;
		this.weight			 = weight;
	}

	// ------------------------------ PUBLIC METHODS ------------------------------
	public void addCoordinates(double[] c)
	{
		this.coordinates= c;
	}

	public String toString()
	{
		//		String s="(id: " + this.index + ", lab: "+ this.label + ", rd: " +  this.weight + ")";
		String s= ""+ this.label;

		return s;
	}

	// ------------------------------ PRIVATE METHODS ------------------------------	

	// ------------------------------ GETTERS & SETTERS ------------------------------
	public boolean isPostProcessed() 
	{
		return isPostProcessed;
	}

	public void setPostProcessed(boolean isPostProcessed) 
	{
		this.isPostProcessed = isPostProcessed;
	}

	public boolean isProcessed() 
	{
		return isProcessed;
	}
	
	public boolean wasNotCoreDistanceSet() 
	{
		if(coreDistance == -1)
		{
			return true;
		}
		
		return false;
	}

	public void setProcessed(boolean isProcessed) 
	{
		this.isProcessed = isProcessed;
	}

	public void setCoreDistance2(ArrayList<Neighbor> neighbors, int minPts)
	{
		/*
		 * Core distance is undefined if
		 * the number of minPts is greater
		 * than the number of points we
		 * have in its neighbourhood
		 */
		if(neighbors.size() < minPts)
		{
			coreDistance = -1;
			return;
		}

		coreDistance = neighbors.get(minPts - 1).getDistance();
	}

	public double getWeight() 
	{
		return weight;
	}

	public void setWeight(double weight) 
	{
		this.weight = weight;
	}	

	public int compareTo(Instance o) 
	{
		double difference = this.weight - o.weight;

		if(difference >= 0)
		{
			return 1;
		}

		return -1;
	}

	public double skalarprod(double[] b, double[] m){
		double sum = 0.0;
		for(int i = 0; i < b.length; i++)
		{
			sum+= (this.coordinates[i]- b[i])*(this.coordinates[i] - m[i]);
		}
		return sum;
	}

	//--------------------------- right methods!! --------------------	
	public void setCoreDistance(double core)
	{
		this.coreDistance = core;
	}

	public double getCoreDistance() 
	{
		return coreDistance;
	}

	public boolean isPreLabeled() 
	{
		return isPreLabeled;
	}

	public void setPreLabeled(boolean isLabeled) 
	{
		this.isPreLabeled = isLabeled;
	}

	public int getTrueLabel() 
	{
		return trueLabel;
	}

	public void setTrueLabel(int trueLabel) 
	{
		this.trueLabel = trueLabel;
	}

	public int getLabel() 
	{
		return label;
	}

	public void setLabel(int label) 
	{
		this.label = label;
	}

	public int getIndex() 
	{
		return index;
	}

	public void setIndex(int index) 
	{
		this.index = index;
		this.preceeding= index;
		this.next= index;
	}

	public double[] getCoordinates() 
	{
		return coordinates;
	}

	public int getClusterAssignment()
	{
		return this.clusterAssignment;
	}

	public void setClusterAssignment(int clId)
	{
		this.clusterAssignment = clId;
	}

	public void setNearestLabeled(Integer id)
	{
		this.nearestLabeled = id;
	}

	public int getNearestLabeled()
	{
		return this.nearestLabeled;
	}

}