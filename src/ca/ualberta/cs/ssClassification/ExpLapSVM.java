package ca.ualberta.cs.ssClassification;

import Jama.Matrix;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import matlabcontrol.MatlabConnectionException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxyFactory;
import matlabcontrol.MatlabProxyFactoryOptions;
import matlabcontrol.extensions.MatlabNumericArray;
import matlabcontrol.extensions.MatlabTypeConverter;

public class ExpLapSVM 
{
	private static final int positiveLabel = 1;
	private static final int negativeLabel = 0;


	public static void propagateLabels(Dataset dataset, Graph graph, Map<Integer, Integer> pl) throws Exception
	{
		TreeSet<Integer> classIds = dataset.getClassIds();

		Instance[] objects   = dataset.getObjects();
		int[] originalLabels = new int[objects.length];
		int[] originalOrder = new int[objects.length];

		int numberOfClass = classIds.size();
		int numberOfPrelabeled = pl.size();
		int numberOfObjects = objects.length;


		if(classIds.size()==2) //Binary classification
		{
			Map<Integer, Integer> tmpResponse = new HashMap<Integer, Integer>();

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

			tmpResponse = computeLapSVM(graph, numberOfClass, numberOfObjects, numberOfPrelabeled, originalOrder, originalLabels, prelabeledIds);

			// Comment to perform experiments with time
			for(Map.Entry<Integer, Integer> entries: tmpResponse.entrySet())
			{
				if(entries.getValue().equals(positiveLabel))
					objects[entries.getKey()].setLabel(classIdOne);
				else
					objects[entries.getKey()].setLabel(classIdTwo);
			}
		}


		for(Integer i: classIds)
		{
			Map<Integer, Integer> tmpResponse = new HashMap<Integer, Integer>();

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

			tmpResponse = computeLapSVM(graph, numberOfClass, numberOfObjects, numberOfPrelabeled, originalOrder, originalLabels, prelabeledIds);


			// Comment to compare time between algorithms
			for(Map.Entry<Integer, Integer> entries: tmpResponse.entrySet())
			{
				if(entries.getValue().equals(positiveLabel))
					objects[entries.getKey()].setLabel(i);
			}
		}

	}

	private static Map<Integer, Integer> computeLapSVM(Graph graph, int c, int n, int l, int[] originalOrder, int[] originalLabels, int[] split) throws MatlabConnectionException, Exception 
	{
		MatlabProxyFactoryOptions options = new MatlabProxyFactoryOptions.Builder()
				.setProxyTimeout(900000000L)
				.build();
		MatlabProxyFactory factory = new MatlabProxyFactory(options);
		MatlabProxy proxy = factory.getProxy();

		// computing the normalized Laplacian
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

		// getting the actual state
		Arrays.sort(split);
		ActualState actualState = new ActualState(graph.getData(), graph.getDistanceMatrix(),
				graph.getWeightedMatrix(), graph.getAdjacencyMatrix(),
				orderedNumbers, split, labels, gramMatrix, normLaplacian);
		actualState.reorder();

		// generate the input labels (labeled \in [0 : c - 1]; unlabeled = -1)
		int[] inputLabels = Utils.copyVector(actualState.getActualLabels());
		for (ix = l; ix < n; ix++) 
		{
			inputLabels[ix] = -1;
		}

		// compute the class priors
		double[] classPriors = Utils.getClassPriors(inputLabels, l, c);

		// compute the label vector
		double[][] Y = Utils.getLabelVector(inputLabels, l);

		// running matlab scripts
		MatlabTypeConverter processor = new MatlabTypeConverter(proxy);
		proxy.eval("clear;");
		processor.setNumericArray("data.L", new MatlabNumericArray(actualState.getActualNormalizedLaplacian(), null));
		processor.setNumericArray("data.K", new MatlabNumericArray(actualState.getActualGramMatrix(), null));
		processor.setNumericArray("data.Y", new MatlabNumericArray(Y, null));
		processor.setNumericArray("data.X", new MatlabNumericArray(actualState.getActualData(), null));

		// setting options for LapSVM
		proxy.eval("options.Hinge = 1;");
		//proxy.eval("options.UseBias = 1;");
		//proxy.eval("options.LaplacianNormalize=0;");
		//proxy.eval("options.NewtonLineSearch=0;");
		proxy.setVariable("gamma_A", 1E-5);
		proxy.setVariable("gamma_I", 1);
		proxy.eval("options.gamma_A = gamma_A;");
		proxy.eval("options.gamma_I = gamma_I;");

		// running the LapSVM classifier
		proxy.eval("classifier = lapsvmp(options,data)");

		// getting the output vector
		double[][] alpha = processor.getNumericArray("classifier.alpha").getRealArray2D();
		int[] output = getOutputVector(actualState.getActualGramMatrix(), alpha, classPriors, l, actualState.getActualLabels());

		proxy.exit();
		proxy.disconnect();

		Map<Integer, Integer> finalResult = new HashMap<Integer, Integer>();
		for(int i=l; i < n; i++)
		{
			int idObj = actualState.getActualNumbers()[i];	
			finalResult.put(idObj, output[i-l]);
		}
		return finalResult;
	}


	public static int[] getOutputVector(double[][] gramMatrix, double[][] alpha, double[] classPriors, int l, int[] labels) {
		int n = alpha.length;
		int[] output = new int[n - l];
		double[] f = new double[n];        
		for(int i = 0; i < n; i++){
			double value = 0;
			for (int j = 0; j < n; j++) {
				value += gramMatrix[i][j] * alpha[j][0];
			}            
			f[i] = value;
		}
		for(int i = l; i < n; i++){
			if(f[i] >= 0){
				output[i - l] = 1;
			}
			else{
				output[i - l] = 0;
			}
		}
		return output;
	}
}