package ca.ualberta.cs.validity;

import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;

public class RandStatistic extends Validity {

	public double getIndex(Instance[] dataset) 
	{
		int f00 = 0; //number of pairs of objects having a different class and a different cluster
		int f01 = 0; //number of pairs of objects having a different class and the same cluster
		int f10 = 0; //number of pairs of objects having the same class and a different cluster
		int f11 = 0; //number of pairs of objects having the same class and the same cluster
		
		for (int i = 0; i < dataset.length; i++) 
		{
			if(dataset[i].isPreLabeled() == true)
			{
				continue;
			}
			
			for (int j = i + 1; j < dataset.length; j++) 
			{

				if(dataset[j].isPreLabeled() == true)
				{
					continue;
				}

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
		
		return (double)(f00 + f11) / (f00 + f01 + f10 + f11);
	}
	
	
	public double getIndexClassification(Dataset dataset) // Not applied in this method
	{
		return 0.0;
	}
}
