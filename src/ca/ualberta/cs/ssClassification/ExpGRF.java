package ca.ualberta.cs.ssClassification;

import Jama.Matrix;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.ssClassification.GRF;

import javafx.util.Pair;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class ExpGRF {

	private static final int positiveLabel = 1;
	private static final int negativeLabel = 0;


	public static double propagateLabels(Dataset dataset, Graph graph, Map<Integer, Integer> pl) throws Exception
	{
		TreeSet<Integer> classIds = dataset.getClassIds();
		double totalTime = 0;

		Instance[] objects   = dataset.getObjects();
		int[] originalLabels = new int[objects.length];
		int[] originalOrder = new int[objects.length];

		int numberOfClass = classIds.size();
		int numberOfPrelabeled = pl.size();
		int numberOfObjects = objects.length;


		if(classIds.size()==2) //Binary classification
		{
			Pair<Double, Map<Integer, Integer>> tmpResponse = new Pair<Double, Map<Integer, Integer>>(0.0, new HashMap<Integer, Integer>());

			Integer classIdOne = classIds.pollFirst();
			Integer classIdTwo = classIds.pollFirst();

			int prelabeledIds[] = new int[pl.size()];
			int ix=0;

			for(Instance inst: objects)
			{
				if(inst.getTrueLabel()==classIdOne)
				{
					originalLabels[inst.getIndex()] = positiveLabel;
					originalOrder[inst.getIndex()]  = inst.getIndex();
				}
				else
				{
					originalLabels[inst.getIndex()] = negativeLabel;
					originalOrder[inst.getIndex()]  = inst.getIndex();
				}

				if(inst.isPreLabeled())
				{
					prelabeledIds[ix] = inst.getIndex();
					ix++;
				}
			}

			tmpResponse = computeGFHF(graph, numberOfClass, numberOfObjects, numberOfPrelabeled, originalOrder, originalLabels, prelabeledIds);
			totalTime = tmpResponse.getKey(); 
			
			// Comment to perform experiments with time
			for(Map.Entry<Integer, Integer> entries: tmpResponse.getValue().entrySet())
			{
				if(entries.getValue().equals(positiveLabel))
					objects[entries.getKey()].setLabel(classIdOne);
				else
					objects[entries.getKey()].setLabel(classIdTwo);
			}
			return totalTime;
		}


		for(Integer i: classIds)
		{
			Pair<Double, Map<Integer, Integer>> tmpResponse = new Pair<Double, Map<Integer, Integer>>(0.0, new HashMap<Integer, Integer>());

			int prelabeledIds[] = new int[pl.size()];
			int ix=0;

			for(Instance inst: objects)
			{
				if(i.equals(inst.getTrueLabel()))
				{
					originalLabels[inst.getIndex()] = positiveLabel;
					originalOrder[inst.getIndex()]  = inst.getIndex();
				}
				else
				{
					originalLabels[inst.getIndex()] = negativeLabel;
					originalOrder[inst.getIndex()]  = inst.getIndex();
				}

				if(inst.isPreLabeled())
				{
					prelabeledIds[ix] = inst.getIndex();
					ix++;
				}
			}

			tmpResponse = computeGFHF(graph, numberOfClass, numberOfObjects, numberOfPrelabeled, originalOrder, originalLabels, prelabeledIds);
			totalTime+= tmpResponse.getKey();


			// Comment to compare time between algorithms
			for(Map.Entry<Integer, Integer> entries: tmpResponse.getValue().entrySet())
			{
				if(entries.getValue().equals(positiveLabel))
					objects[entries.getKey()].setLabel(i);
			}
		}
		return totalTime;
	}


	private static Pair<Double, Map<Integer, Integer>> computeGFHF(Graph graph, int c, int n, int l, int[] originalOrder, int[] originalLabels, int[] split) throws Exception 
	{

		Long startTime = System.currentTimeMillis();
		//computing the normalized Laplacian
		double[] D = Utils.computeDiagonalValues(graph.getWeightedMatrix());
		Matrix L = Utils.normalizedLaplacian(graph.getWeightedMatrix(), D);
		double[][] normLaplacian = L.getArray();


		int[] orderedNumbers = Utils.orderedNumbers(n);
		int[] labels= new int[n];

		int ix=0;
		for(int i: originalOrder)
		{
			labels[ix]= originalLabels[ix];
			orderedNumbers[ix]= i;
			ix++;
		}

		double[][] gramMatrix = Utils.computeGramMatrix(graph.getWeightedMatrix(), graph.getDistanceMatrix(), graph.getSigma());

		Double timeLaplacian = (System.currentTimeMillis() - startTime) /1000.00;

		// getting the actual state
		Arrays.sort(split);
		ActualState actualState = new ActualState(graph.getData(), graph.getDistanceMatrix(), graph.getWeightedMatrix(),
				graph.getAdjacencyMatrix(), orderedNumbers, split, labels, gramMatrix, normLaplacian);
		actualState.reorder();

		// generate the input labels (labeled \in [0 : c - 1]; unlabeled = -1)
		int[] inputLabels = Utils.copyVector(actualState.getActualLabels());
		for (ix = l; ix < n; ix++) 
		{
			inputLabels[ix] = -1;
		}

		// compute the class priors
		double[] classPriors = Utils.getClassPriors(inputLabels, l, c);

		// compute the label matrix
		Matrix Y = Utils.computeLabelMatrixGFHF(inputLabels, l, c, n);
        

		GRF grf = new GRF(classPriors);
		Pair<Double, int[]> pairResult = grf.classify(actualState.getActualWeightedMatrix(), Y, l);

		int[] output = pairResult.getValue();
		Double timeGRF = pairResult.getKey();

		Map<Integer, Integer> finalResult = new HashMap<Integer, Integer>();
		for(int i=l; i < n; i++)
		{
			int idObj = actualState.getActualNumbers()[i];	
			finalResult.put(idObj, output[i-l]);
		}
		return new Pair<Double, Map<Integer, Integer>>((timeLaplacian+timeGRF), finalResult);
	}
}