package ca.ualberta.cs.validity;

import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;


public class WeightedARI extends Validity 
{

	private static final int NoiseLabel = -1;
	
	public double getIndex(Instance[] dataset) 
	{
		int f00 = 0; //number of pairs of objects having a different class and a different cluster (d)
		int f01 = 0; //number of pairs of objects having a different class and the same cluster (c)
		int f10 = 0; //number of pairs of objects having the same class and a different cluster (b)
		int f11 = 0; //number of pairs of objects having the same class and the same cluster (a)
						
		for (int i = 0; i < dataset.length; i++) 
		{
			if(dataset[i].isPreLabeled() || dataset[i].getLabel() == NoiseLabel)
				continue;
			
			for (int j = i + 1; j < dataset.length; j++) 
			{

				if(dataset[j].isPreLabeled() || dataset[j].getLabel() == NoiseLabel)
					continue;

				if(dataset[i].getLabel() != dataset[j].getLabel() && dataset[i].getTrueLabel() != dataset[j].getTrueLabel())
					f00++;
				
				else if(dataset[i].getLabel() == dataset[j].getLabel() && dataset[i].getTrueLabel() != dataset[j].getTrueLabel())
					f01++;
				
				else if(dataset[i].getLabel() != dataset[j].getLabel() && dataset[i].getTrueLabel() == dataset[j].getTrueLabel())
					f10++;
				
				else if(dataset[i].getLabel() == dataset[j].getLabel() && dataset[i].getTrueLabel() == dataset[j].getTrueLabel())
					f11++;
			}
		}
		
		// Compute the number of noise objects present into the final solution.
		
		int numberOfNoise = 0;
		int numberOfObjects = dataset.length;
		
		for(Instance inst: dataset)
			if(inst.getLabel()== NoiseLabel)
				numberOfNoise++;
		
		double weight= ((double)numberOfObjects-numberOfNoise)/numberOfObjects;
		
		
		double aTotal       = (double)f00+f01+f10+f11;
		double anAdjustment = (double)(f11+f01)*(f11+f10)/aTotal;
		double anAverage    = ((double)f11+f01+f11+f10)/2;
		
		double index = weight*(( (double)f11-anAdjustment ) / (anAverage-anAdjustment));
		
		if(Double.isNaN(index))
			return 0.0;
		
		return index;
	}
	
	
	public double getIndexClassification(Dataset data) // Not used for this validation index
	{
		return 0.0;
	}
}