package ca.ualberta.cs.experiment;

import java.util.ArrayList;

import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;



public interface LabelSelection 
{
	
	public abstract ArrayList<Instance> selectLabels(Dataset dataset, int numLabeledObjects, int numberOfMissingClass);

}
