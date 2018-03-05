package ca.ualberta.cs.ssClassification;

import Jama.Matrix;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;

public class ExpRMGT 
{
	public static double propagateLabels(Graph graph, Dataset dataset, HashMap<Integer, Integer> preLabeled)
	{
		long startTime;
		int numObjects = dataset.getObjects().length;
		int numClasses = dataset.getNumOfClass();
		int l		   = preLabeled.size();
		int[] split    = new int[l];
		Instance[] objects = dataset.getObjects();

		int[] labels 		 = new int[numObjects];
		int[] orderedNumbers = new int[numObjects];

		int ix = 0;
		for(Map.Entry<Integer, Integer> entries: preLabeled.entrySet())
		{
			split[ix] = entries.getKey();
			ix++;
		}

		ix = 0;
		for(Instance inst: objects)
		{
			labels[ix] 		   = inst.getTrueLabel();
			orderedNumbers[ix] = inst.getIndex();
			ix++;
		}

		Arrays.sort(split);
		ActualState actualState = new ActualState(graph.getData(), graph.getDistanceMatrix(), graph.getWeightedMatrix(),
				graph.getAdjacencyMatrix(), orderedNumbers, split, labels);
		actualState.reorder();

		int[] inputLabels = Utils.copyVector(actualState.getActualLabels());
		for (ix = l; ix < numObjects; ix++) 
		{
			inputLabels[ix] = -1;
		}

		// compute the label matrix
		Matrix Y = Utils.computeLabelMatrix(inputLabels, l, numClasses, numObjects);

		// running the Robust Multiclass Graph Transduction (RMGT) algorithm
		startTime = System.currentTimeMillis();
		RMGT rmgt          = new RMGT();
		int[] output       = rmgt.classify(actualState.getActualWeightedMatrix(), Y, l);
		double timeToRegister= ((System.currentTimeMillis() - startTime)/1000.00);


		//		System.out.println("Input labels: "+ Arrays.toString(inputLabels)  + "\n Output size: " + output.length + "\n" + Arrays.toString(output) + "\n Split size: " + Arrays.toString(split) +
		//				"\n Labels: " + Arrays.toString(actualState.getActualLabels()) + "\n Actual numbers: " + Arrays.toString(actualState.getActualNumbers()));


		// Comment to perform experiments comparing the time
		for(int i=l; i < numObjects; i++)
		{
			int idObj = actualState.getActualNumbers()[i];	
			if(output[i-l]==-1)
				objects[idObj].setLabel(output[i-l]);
			else
				objects[idObj].setLabel((output[i-l]+1));
			//			System.out.println("ID: " + idObj + " label: " + (output[i-l]+1));
		}

		return timeToRegister;
	}
}