/** TODO: Code version that misses at least one class */

package ca.ualberta.cs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class SelectRandomObjectsGivenPercentage implements LabelSelection 
{
	
//	Implementation selecting label randomly (do not enforce labels from each class)
	
	public ArrayList<Instance> selectLabels(Dataset dataset, int numLabeledObjects, int percentageOfMissedInformation) 
	{
		Instance[] instances 		 = dataset.getObjects();
		ArrayList<Instance> response = new ArrayList<Instance>();

		Random generator 			   = new Random();
		//		numLabeledObjects= Integer.max(numLabeledObjects, 1); // Making sure that exist at least one labeled object

		for(int i = 0; i < numLabeledObjects; i++)
		{
			int randomIndex = generator.nextInt(instances.length);

			if(instances[randomIndex].isPreLabeled())
			{
				numLabeledObjects++;
				continue;
			}			

			Integer trueLabel = instances[randomIndex].getTrueLabel();
			instances[randomIndex].setLabel(trueLabel);
			instances[randomIndex].setPreLabeled(true);
			response.add(instances[randomIndex]);
		}

		// 		System.out.println("Objects selected for the trial: " + response);

		Collections.sort(response, new Comparator<Instance>() 
		{
			@Override
			public int compare(Instance i1, Instance i2)
			{

				return  i1.getTrueLabel()-i2.getTrueLabel();
			}
		});

		return response;
	}
}