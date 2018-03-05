package ca.ualberta.cs.validity;

import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;


public abstract class Validity {
	
	 public abstract double getIndex(Instance[] dataset);
	 
	 public abstract double getIndexClassification(Dataset dataset);

}
