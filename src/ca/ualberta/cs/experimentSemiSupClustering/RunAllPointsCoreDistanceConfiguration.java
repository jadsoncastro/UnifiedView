package ca.ualberta.cs.experimentSemiSupClustering;

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

import SHM.HMatrix.HMatrix;
import javafx.util.Pair;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.hdbscanstar.Cluster;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.DescriptionAlgorithms;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.Util;

public class RunAllPointsCoreDistanceConfiguration
{
	private static final DescriptionAlgorithms parameters = new DescriptionAlgorithms();

	public static void performIt(String fileName, String fileFolder, String outputFolder,
			DistanceCalculator distance, String delimiter)
					throws Exception
	{
		//  Validation Method (String) -> Algorithm (String) -> Number of missed classes (Integer) -> % labeled (Integer) -> mPts (Integer) -> Result (ArrayList<Double>)
		Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report = new HashMap<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>>();

		Map<Integer, UndirectedGraph> mstStore= null; // MST files
		Map<Integer, ArrayList<Cluster>> clustersStore = null; // Clusters structures
		Map<Integer, HMatrix> matrixStore = null; // HMatrix structures

		// Reading data set file
		Dataset datasetExperiments, datasetUnsupervised = new Dataset();
		datasetExperiments  = Util.readInDataSet(fileFolder+fileName+".data", delimiter);
		datasetUnsupervised = Util.readInDataSet(fileFolder+fileName+".data", delimiter);

		int numPoints       = datasetExperiments.getObjects().length;
		double[][] database = new double[numPoints][];

		for(Instance inst: datasetExperiments.getObjects())
			database[inst.getIndex()] = inst.getCoordinates();



		// Read file with MSTS
		try(FileInputStream fileIn = new FileInputStream(outputFolder + fileName + ".mstAllPointsCoreDistance");
				ObjectInputStream in = new ObjectInputStream(fileIn);)
		{
			mstStore = (Map<Integer,UndirectedGraph>) in.readObject();
		}
		catch(ClassNotFoundException | IOException ec)
		{
			System.err.println("[AP] Error while reading MST for dataset " + fileName);
			System.exit(-1);
		}


		// Read file with Cluster structures
		try(FileInputStream fileIn = new FileInputStream(outputFolder + fileName + ".clustersAllPointsCoreDistance");
				ObjectInputStream in = new ObjectInputStream(fileIn);)
		{
			clustersStore = (Map<Integer, ArrayList<Cluster>>) in.readObject();
		}
		catch(ClassNotFoundException | IOException ex)
		{
			System.err.println("[AP] Error while reading cluster structure for dataset " + fileName);
			System.exit(-1);
		}

		//Defining the array of minPoints
		ArrayList<Integer> arrayMinPts = new ArrayList<Integer>(clustersStore.keySet());

		// Read file with Matrix structures
		try(FileInputStream fileIn = new FileInputStream(outputFolder + fileName + ".matrixAllPointsCoreDistance");
				ObjectInputStream in = new ObjectInputStream(fileIn);)
		{
			matrixStore = (Map<Integer, HMatrix>) in.readObject();
		}
		catch(ClassNotFoundException | IOException ex)
		{
			System.err.println("[AP] Error while reading matrix structure for dataset " + fileName);
			System.exit(-1);
		}


		String[] algorithmsDesc = 
			{
					parameters.hdbscanStarAp,
					parameters.hdbscanStarBCubedAp,
					parameters.hdbscanStarMixedAp,
					parameters.hdbscanConstraintsAp,
					parameters.hdbscanStarMixedForConstraintsAp
			};


		// Initialization of the report for the algorithms
		report.put(parameters.ARI, new HashMap<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>());
		report.put(parameters.weightedARI, new HashMap<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>());


		for(String alg: algorithmsDesc)
		{
			report.get(parameters.ARI).put(alg, new HashMap<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>());
			report.get(parameters.weightedARI).put(alg, new HashMap<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>());
		}

		long overallStartTime = System.currentTimeMillis();
		String fileLabeled = outputFolder+fileName+"-labeled-objects.csv";

		BufferedReader reader = new BufferedReader(new FileReader(fileLabeled));
		String line = reader.readLine();

		int lineIndex = 0;
		while (line != null) 
		{
			long timeTrial = System.currentTimeMillis();
			Util.resetDataset(datasetExperiments, true);

			HashMap<Integer, Integer> preLabeledDenBased = new HashMap<Integer, Integer>();

			lineIndex++;
			line.replaceAll("\\s+",""); // Remove spaces that might exist in the line
			String[] lineContents = line.split(":");

			int pmc   = Integer.parseInt(lineContents[0]); // Percentage of classes for which labels are missing
			int plab  = Integer.parseInt(lineContents[1]); // Percentage of labeled objects
			int trial = Integer.parseInt(lineContents[2]);


			for(String alg: algorithmsDesc)
			{
				// Update ARI information
				if(!report.get(parameters.ARI).get(alg).containsKey(pmc))
					report.get(parameters.ARI).get(alg).put(pmc, new HashMap<Integer, Map<Integer, ArrayList<Double>>>());

				if(!report.get(parameters.ARI).get(alg).get(pmc).containsKey(plab))
					report.get(parameters.ARI).get(alg).get(pmc).put(plab, new HashMap<Integer, ArrayList<Double>>());

				for(Integer mpts: arrayMinPts)
				{
					if(!report.get(parameters.ARI).get(alg).get(pmc).get(plab).containsKey(mpts))
						report.get(parameters.ARI).get(alg).get(pmc).get(plab).put(mpts, new ArrayList<Double>());
				}

				// Update Weighted-ARI information
				if(!report.get(parameters.weightedARI).get(alg).containsKey(pmc))
					report.get(parameters.weightedARI).get(alg).put(pmc, new HashMap<Integer, Map<Integer, ArrayList<Double>>>());

				if(!report.get(parameters.weightedARI).get(alg).get(pmc).containsKey(plab))
					report.get(parameters.weightedARI).get(alg).get(pmc).put(plab, new HashMap<Integer, ArrayList<Double>>());

				for(Integer mpts: arrayMinPts)
				{
					if(!report.get(parameters.weightedARI).get(alg).get(pmc).get(plab).containsKey(mpts))
						report.get(parameters.weightedARI).get(alg).get(pmc).get(plab).put(mpts, new ArrayList<Double>());
				}
			}

			lineContents[3] = lineContents[3].replaceAll("\\s+","");
			String[] stringLabeled = lineContents[3].split(delimiter);

			// Store the set of labeled objects
			int idLabeled = Integer.parseInt(stringLabeled[0]);
			if(idLabeled > -1)
			{
				for (int i = 0; i < stringLabeled.length; i++) 
				{
					try
					{
						idLabeled = Integer.parseInt(stringLabeled[i]);

						datasetExperiments.getObjects()[idLabeled].setPreLabeled(true);
						datasetExperiments.getObjects()[idLabeled].setLabel(datasetExperiments.getObjects()[idLabeled].getTrueLabel());
						preLabeledDenBased.put(idLabeled, datasetExperiments.getObjects()[idLabeled].getTrueLabel());
					}
					catch (NumberFormatException nfe) 
					{
						System.err.println("[AP] Illegal value on line " + lineIndex + ":" + stringLabeled[i] + " Dataset: " + fileName);
					}
				}
			}


			/**
			 * Run the algorithms
			 */

			for(Integer minPts: arrayMinPts)
			{
				if(idLabeled > -1)
				{
					Util.resetDataset(datasetExperiments, false);
					HDBSCANVariations.BCubedBased(clustersStore.get(minPts), mstStore.get(minPts), matrixStore.get(minPts), preLabeledDenBased, datasetExperiments, minPts, report, parameters.hdbscanStarBCubedAp, pmc, plab);

					Util.resetDataset(datasetExperiments, false);
					HDBSCANVariations.HdbscanUnsupervisedAndConstraints(clustersStore.get(minPts), matrixStore.get(minPts), preLabeledDenBased, datasetExperiments, minPts, report, parameters.hdbscanConstraintsAp, pmc, plab);
				}

				Util.resetDataset(datasetExperiments, false);
				HDBSCANVariations.MixedBCubed(clustersStore.get(minPts), mstStore.get(minPts), matrixStore.get(minPts), preLabeledDenBased, datasetExperiments, minPts, report, parameters.hdbscanStarMixedAp, pmc, plab);

				Util.resetDataset(datasetExperiments, false);
				HDBSCANVariations.MixedForConstraints(clustersStore.get(minPts), matrixStore.get(minPts), preLabeledDenBased, datasetExperiments, minPts, report, parameters.hdbscanStarMixedForConstraintsAp, pmc, plab);

				Util.resetDataset(datasetUnsupervised, true);
				HDBSCANVariations.HdbscanUnsupervisedAndConstraints(clustersStore.get(minPts), matrixStore.get(minPts), null, datasetUnsupervised, minPts, report, parameters.hdbscanStarAp, pmc, plab);

			}

			// Read the next trial of the algorithm
			line = reader.readLine();

			//-------------------------------- Save report of the data set ------------------------------------------------------
			String stringFolder    = outputFolder + fileName + ".reportAllPointsCoreDistanceConfiguration";

			if(trial%5==0)
			{
				//				System.out.println("Writing results in trial " + trial);
				try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
						ObjectOutputStream out = new ObjectOutputStream(outFile))
				{
					out.writeObject(report);
				}
				catch(IOException e)
				{
					System.out.println("[AP] An error ocurred while writing the report for data set " + fileName);
					e.printStackTrace();
					System.exit(-1);
				}
			}

			//			System.out.println("[All points Core distance configuration] Dataset:  " + fileName + " Perc of classes for which labels are missing: " + pmc + ". plab: " + plab + ". Trial: " + trial + ". time(min): " + (((System.currentTimeMillis() - timeTrial)/1000)/60));

		} // End for while

		reader.close();
		System.out.println("[AP] Dataset:  " + fileName + ". Overall time(min): " + (((System.currentTimeMillis() - overallStartTime)/1000)/60));
	}
}