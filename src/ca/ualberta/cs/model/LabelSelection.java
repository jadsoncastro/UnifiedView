package ca.ualberta.cs.model;

import java.util.ArrayList;



public interface LabelSelection 
{
	
	public abstract ArrayList<Instance> selectLabels(Dataset dataset, int numLabeledObjects, int numberOfMissingClass);

}
