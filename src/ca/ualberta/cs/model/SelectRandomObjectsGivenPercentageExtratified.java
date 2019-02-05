/** TODO: Code version that misses at least one class */

package ca.ualberta.cs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class SelectRandomObjectsGivenPercentageExtratified implements LabelSelection 
{

	public ArrayList<Instance> selectLabels(Dataset dataset, int numLabeledObjects, int percentageOfMissedInformation) 
	{

		Instance[] instances 		 = dataset.getObjects();
		ArrayList<Instance> response = new ArrayList<Instance>();
		ArrayList<Integer>  classIds = new ArrayList<Integer>(dataset.getClassIds());

		// Check the class distribution in the dataset
		Map<Integer, Integer> classesDistribution = dataset.getClassDistribution();
		int numberOfMissingClusters = (int)Math.ceil(((double)(percentageOfMissedInformation*classIds.size())/100));
		
		
		// Instead of return a empty list, we select the objects from a single class
		if(numberOfMissingClusters >= classIds.size())
		{
			numberOfMissingClusters=(classIds.size()-1);
		}
		Collections.shuffle(classIds);

		
		Random generator 			   = new Random();
		ArrayList<Integer> missedClass = new ArrayList<Integer>();
		ArrayList<Integer> shouldSelect = new ArrayList<Integer>();
		missedClass.add(-1);
		

		if(numberOfMissingClusters > 0)
		{
			missedClass = new ArrayList<Integer>();

			for(int i=0; i < numberOfMissingClusters; i++)
				missedClass.add(classIds.get(i));


			for(int i= numberOfMissingClusters; i < classIds.size(); i++)
				shouldSelect.add(classIds.get(i));
		}

		// Try to respect at least one object per class, EXCEPTED FOR THE MISSED CLASS(ES)
		HashMap<Integer, ArrayList<Instance>> objectsPerClass = new HashMap<Integer, ArrayList<Instance>>();
		for(Integer id: classIds)
			objectsPerClass.put(id, new ArrayList<Instance>());

		for(Instance inst: instances)
			objectsPerClass.get((Integer)inst.getTrueLabel()).add(inst);

		for(Map.Entry<Integer, ArrayList<Instance>> entries: objectsPerClass.entrySet())
		{
			if(missedClass.contains(entries.getKey()))
				continue;

			ArrayList<Instance> tmp = entries.getValue();			

			int randomIndex = generator.nextInt(tmp.size());

			Instance inst = tmp.get(randomIndex);
			inst.setLabel(inst.getTrueLabel());
			inst.setPreLabeled(true);
			response.add(inst);
			numLabeledObjects--;
		}
		
		
		int shouldSelectTotalSize= 0;
		for(Integer id: shouldSelect)
			shouldSelectTotalSize+= classesDistribution.get(id);

		
		for(int i = 0; i < numLabeledObjects; i++)
		{
			// Avoid cases where the percentage of labeled objects is higher than the number of objects in a specific class.
			if(response.size() >= shouldSelectTotalSize)
				break;

			int randomIndex = generator.nextInt(instances.length);

			if(instances[randomIndex].getLabel() > 0)
			{
				numLabeledObjects++;
				continue;
			}			

			Integer trueLabel = instances[randomIndex].getTrueLabel();
			if(!missedClass.contains(trueLabel))
			{
				instances[randomIndex].setLabel(trueLabel);
				instances[randomIndex].setPreLabeled(true);
				response.add(instances[randomIndex]);
			}else
			{
				numLabeledObjects++;
				continue;
			}
		}

		Collections.sort(response, new Comparator<Instance>()
		{
			@Override
			public int compare(Instance i1, Instance i2)
			{

				return  i1.getTrueLabel()-i2.getTrueLabel();
			}
		});

		if(response.isEmpty())
		{
			System.err.println("Random object selection is returning a empty solution!!");
			System.exit(0);
		}

//		System.out.println(" Class distribution " + classesDistribution + " class ids: " + classIds + " misssed: " + missedClass + " class that should select: " + shouldSelect + " total shoud select size " + shouldSelectTotalSize + " response size: " + response.size());		
		return response;
	}
}