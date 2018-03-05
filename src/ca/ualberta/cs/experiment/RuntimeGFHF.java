package ca.ualberta.cs.experiment;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javafx.util.Pair;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.DescriptionAlgorithms;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.Util;
import ca.ualberta.cs.ssClassification.ExpGRF;
import ca.ualberta.cs.ssClassification.Graph;

public class RuntimeGFHF
{
	private static final DescriptionAlgorithms parameters = new DescriptionAlgorithms();


	public static void performIt(String fileName, String fileFolder, String outputFolder,
			DistanceCalculator distance, String delimiter) throws Exception
	{

		// Time (String) -> Algorithm (String) -> % labeled (Integer) -> Time (ArrayList<Double>)
		Map<String, Map<String, Map<Integer, ArrayList<Double>>>> report = new HashMap<String, Map<String, Map<Integer, ArrayList<Double>>>>();
		Map<Integer, Pair<Double, Graph>> mKNN= null; // MST files


		// Reading data set file
		Dataset datasetExperiments = new Dataset();
		datasetExperiments  = Util.readInDataSet(fileFolder+fileName+".data", delimiter);
		int numPoints       = datasetExperiments.getObjects().length;
		double[][] database = new double[numPoints][];

		for(Instance inst: datasetExperiments.getObjects())
			database[inst.getIndex()] = inst.getCoordinates();


		// Read file with MSTS
		try(FileInputStream fileIn = new FileInputStream(outputFolder + fileName + ".mknn");
				ObjectInputStream in   = new ObjectInputStream(fileIn);)
		{
			mKNN= (Map<Integer,Pair<Double, Graph>>) in.readObject();
		}
		catch(ClassNotFoundException | IOException ec)
		{
			System.err.println("[GFHF]Error while reading MST for dataset " + fileName);
			System.exit(-1);
		}

		//Defining the array of minPoints
		ArrayList<Integer> arrayMinPts = new ArrayList<Integer>(mKNN.keySet());
		double timeToComputeGraph = mKNN.get(arrayMinPts.get(0)).getKey();

		String[] algorithmsDesc = 
			{
					// New configurations for the label propagation step
					parameters.harMKNN,
			};

		// Initialization of the report for the algorithms
		report.put(parameters.wholeProcess, 	 new HashMap<String, Map<Integer, ArrayList<Double>>>());
		report.put(parameters.graphConstruction, new HashMap<String, Map<Integer, ArrayList<Double>>>());
		report.put(parameters.timeToPropagate, 	 new HashMap<String, Map<Integer, ArrayList<Double>>>());
		report.put(parameters.weightFunction, 	 new HashMap<String, Map<Integer, ArrayList<Double>>>());

		for(String s: algorithmsDesc)
		{
			report.get(parameters.wholeProcess).put(s, 		new HashMap<Integer, ArrayList<Double>>());
			report.get(parameters.graphConstruction).put(s, new HashMap<Integer, ArrayList<Double>>());
			report.get(parameters.timeToPropagate).put(s, 	new HashMap<Integer, ArrayList<Double>>());
			report.get(parameters.weightFunction).put(s, 	new HashMap<Integer, ArrayList<Double>>());

		}

		long overallStartTime = System.currentTimeMillis();

		String fileLabeled = outputFolder+fileName+"-labeled-objects-time.csv";

		BufferedReader reader = new BufferedReader(new FileReader(fileLabeled));
		String line = reader.readLine();

		int lineIndex = 0;
		while (line != null) 
		{
			long timeTrial = System.currentTimeMillis();
			Util.resetDataset(datasetExperiments, true);

			HashMap<Integer, Integer> preLabeledDenBased = new HashMap<Integer, Integer>();
			TreeMap<Integer, Pair<Integer, double[]>> classifiedHissclu = new TreeMap<Integer, Pair<Integer,double[]>>();


			lineIndex++;
			line.replaceAll("\\s+",""); // Remove spaces that might exist in the line
			String[] lineContents = line.split(":");

			int plab  = Integer.parseInt(lineContents[0]);
			int trial = Integer.parseInt(lineContents[1]);

			for(String alg: algorithmsDesc)
			{
				if(!report.get(parameters.wholeProcess).get(alg).containsKey(plab))
					report.get(parameters.wholeProcess).get(alg).put(plab, new ArrayList<Double>());

				if(!report.get(parameters.graphConstruction).get(alg).containsKey(plab))
					report.get(parameters.graphConstruction).get(alg).put(plab, new ArrayList<Double>());

				if(!report.get(parameters.timeToPropagate).get(alg).containsKey(plab))
					report.get(parameters.timeToPropagate).get(alg).put(plab, new ArrayList<Double>());

				if(!report.get(parameters.weightFunction).get(alg).containsKey(plab))
					report.get(parameters.weightFunction).get(alg).put(plab, new ArrayList<Double>());

			}

			lineContents[2] = lineContents[2].replaceAll("\\s+","");
			String[] stringLabeled = lineContents[2].split(delimiter);

			// Store the set of labeled objects
			for (int i = 0; i < stringLabeled.length; i++) 
			{
				try
				{
					int idLabeled = Integer.parseInt(stringLabeled[i]);

					datasetExperiments.getObjects()[idLabeled].setPreLabeled(true);
					datasetExperiments.getObjects()[idLabeled].setLabel(datasetExperiments.getObjects()[idLabeled].getTrueLabel());
					preLabeledDenBased.put(idLabeled, datasetExperiments.getObjects()[idLabeled].getTrueLabel());
					classifiedHissclu.put(idLabeled, new Pair<Integer, double[]>(datasetExperiments.getObjects()[idLabeled].getTrueLabel(), datasetExperiments.getObjects()[idLabeled].getCoordinates()));
				}
				catch (NumberFormatException nfe) 
				{
					System.err.println("[GFHF]Illegal value on line " + lineIndex + ":" + stringLabeled[i] + " Dataset: " + fileName);
				}
			}


			for(Integer minPts: arrayMinPts)
			{
				// Apply: GFHF
				Util.resetDataset(datasetExperiments, false);
				double timeHarmonic = ExpGRF.propagateLabels(datasetExperiments, mKNN.get(minPts).getValue(), preLabeledDenBased);

				report.get(parameters.wholeProcess).get(parameters.harMKNN).get(plab).add(timeHarmonic + timeToComputeGraph);
				report.get(parameters.graphConstruction).get(parameters.harMKNN).get(plab).add(timeToComputeGraph);
				report.get(parameters.timeToPropagate).get(parameters.harMKNN).get(plab).add(timeHarmonic);
				report.get(parameters.weightFunction).get(parameters.harMKNN).get(plab).add(0.0);

			}

			// Read the next trial of the algorithm
			line = reader.readLine();

			//-------------------------------- Save report of the data set ------------------------------------------------------
			String stringFolder 		    = outputFolder + fileName + ".reportTimeGFHF";

			try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
					ObjectOutputStream out = new ObjectOutputStream(outFile))
			{
				out.writeObject(report);
			}
			catch(IOException e)
			{
				System.out.println("[GFHF]An error ocurred while writing the report for data set " + fileName);
				e.printStackTrace();
				System.exit(-1);
			}

			System.out.println("[GFHF]Dataset:  " + fileName + ". plab: " + plab + ". Trial: " + trial + ". time(min): " + (((System.currentTimeMillis() - timeTrial)/1000)/60));

		}// End for while

		reader.close();

		System.out.println("[GFHF]Dataset:  " + fileName + ". Overall time(min): " + (((System.currentTimeMillis() - overallStartTime)/1000)/60));
	}
}