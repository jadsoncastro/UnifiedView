package ca.ualberta.cs.experiment;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.hdbscanApts.HDBSCANApts;
import ca.ualberta.cs.hdbscanstar.HDBSCANStar;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.Util;
import ca.ualberta.cs.ssClassification.Graph;
import ca.ualberta.cs.ssClassification.GraphGenerator;

public class ExperimentsSSClassificationCreateScenarios
{

	public static void generateInformation(String fileName, String fileFolder, String outputFolder,
			LabelSelection labelSelection, DistanceCalculator distance,
			int numberTrials, ArrayList<Integer> arrayPlab,
			ArrayList<Integer> arrayMinPts,
			String delimiter) throws Exception
	{	

		// Reading data set file
		Dataset datasetExperiments = new Dataset();
		datasetExperiments  = Util.readInDataSet(fileFolder+fileName+".data", delimiter);
		int numPoints       = datasetExperiments.getObjects().length;
		double[][] database = new double[numPoints][];

		for(Instance inst: datasetExperiments.getObjects())
			database[inst.getIndex()] = inst.getCoordinates();


		HashMap<Integer, Graph> mKNN 			   		 = new HashMap<Integer, Graph>();
		HashMap<Integer, UndirectedGraph> mstReach 	  	 = new HashMap<Integer, UndirectedGraph>();
		HashMap<Integer, UndirectedGraph> mstAptsStore	 = new HashMap<Integer, UndirectedGraph>();

		for(Integer minPts: arrayMinPts)
		{
			double[] coreDistances = HDBSCANStar.calculateCoreDistances(database, minPts, distance);
			UndirectedGraph mst = HDBSCANStar.constructMST(database, coreDistances, true, distance);
			mstReach.put(minPts, mst);

			Graph graph 		= GraphGenerator.generateGraph(database, distance, minPts);
			mKNN.put(minPts, graph);
		}

		/** Construction of the MST using the apts core distance and new mutual reachability distance*/
		double[] coreDistances   = HDBSCANApts.calculateCoreDistances(database, distance);
		UndirectedGraph mstApts  = HDBSCANApts.constructMST(database, coreDistances, true, distance);

		for(Integer minPts: arrayMinPts)
			mstAptsStore.put(minPts, mstApts);


		// ------------------------------ Saving the graphs and MSTS ---------------------------------------------
		// First: Save all the core distance MST
		String stringFolder = outputFolder + fileName + ".mstCd";

		try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream out = new ObjectOutputStream(outFile))
		{
			out.writeObject(mstReach);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the mutual reachability distances graphs: " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}

		// Second: Save all the all-points Mst
		stringFolder = outputFolder + fileName + ".mstApts";

		try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream out = new ObjectOutputStream(outFile))
		{
			out.writeObject(mstAptsStore);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the mutual reachability distances (APTS) graphs: " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}


		// Third: Save all the MKNN graphs
		stringFolder = outputFolder + fileName + ".mknn";

		try(FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream outMknn = new ObjectOutputStream(outFile))
		{
			outMknn.writeObject(mKNN);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the mutual KNN graphs: " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}


		String folderLabeledObjects = outputFolder+fileName+"-labeled-objects.csv";
		BufferedWriter responseWriter = new BufferedWriter(new FileWriter(folderLabeledObjects));



		/**Perform trials for each quantity of pre labeled objects */
		for(Integer pLab: arrayPlab)
		{

			for(int trial = 0; trial < numberTrials; trial++)
			{
				// Before each trial, clear the labels for all the data sets
				Util.resetDataset(datasetExperiments, true);
				ArrayList<Instance>   preLabeled = labelSelection.selectLabels(datasetExperiments, Math.round(pLab*numPoints/100), 0);

				String tmp = "";
				for(Instance inst: preLabeled)
					tmp = tmp + inst.getIndex() + ",";

				tmp = pLab + ":" + (trial+1) + ":" + tmp.substring(0, (tmp.length()-2)) + "\n";
				responseWriter.write(tmp);

			}// End for Trials

		} // End for pLab
		responseWriter.close();
	}
}