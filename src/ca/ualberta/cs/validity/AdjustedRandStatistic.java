package ca.ualberta.cs.validity;

import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;


public class AdjustedRandStatistic extends Validity 
{
	public double getIndex(Instance[] dataset) 
	{

		// Get the max cluster id of an object
		int maxClusterId = 0;

		for(Instance inst: dataset)
			if(inst.getLabel() > maxClusterId)
				maxClusterId=inst.getLabel();


		int numberOfPreLabeled = 0;
		// We assign a singleton cluster for each noise object.
		for(Instance inst: dataset)
		{
			if(inst.isPreLabeled())
				numberOfPreLabeled++;
			
			if(inst.getLabel() <= 0)
			{
				maxClusterId++;
				inst.setLabel(maxClusterId);
			}
		}

		int f00 = 0; //number of pairs of objects having a different class and a different cluster (d)
		int f01 = 0; //number of pairs of objects having a different class and the same cluster (c)
		int f10 = 0; //number of pairs of objects having the same class and a different cluster (b)
		int f11 = 0; //number of pairs of objects having the same class and the same cluster (a)

		for (int i = 0; i < dataset.length; i++) 
		{
			if(dataset[i].isPreLabeled())
				continue;

			for (int j = i + 1; j < dataset.length; j++) 
			{

				if(dataset[j].isPreLabeled())
					continue;

				if(dataset[i].getLabel() != dataset[j].getLabel() && dataset[i].getTrueLabel() != dataset[j].getTrueLabel())
				{
					f00++;
				}

				if(dataset[i].getLabel() == dataset[j].getLabel() && dataset[i].getTrueLabel() != dataset[j].getTrueLabel())
				{
					f01++;
				}

				if(dataset[i].getLabel() != dataset[j].getLabel() && dataset[i].getTrueLabel() == dataset[j].getTrueLabel())
				{
					f10++;
				}

				if(dataset[i].getLabel() == dataset[j].getLabel() && dataset[i].getTrueLabel() == dataset[j].getTrueLabel())
				{
					f11++;
				}
			}
		}


		double aTotal       = (double)f00+f01+f10+f11;
		double anAdjustment = (double)(f11+f01)*(f11+f10)/aTotal;
		double anAverage    = ((double)f11+f01+f11+f10)/2;

		return ( (double)f11-anAdjustment ) / (anAverage-anAdjustment); 
	}

	public double getIndexClassification(Dataset data) // Not used for this validation index
	{
		return 0.0;
	}
}