package ca.ualberta.cs.experiment;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
import javafx.util.Pair;

public class RuntimeCreateScenarios
{

	public static void generateInformation(String fileName, String fileFolder, String outputFolder,
			LabelSelection labelSelection, DistanceCalculator distance,
			int numberTrials,
			ArrayList<Integer> arrayMinPts,
			String delimiter) throws Exception
	{	

		long startTime;
		
		// Reading data set file
		Dataset datasetExperiments = new Dataset();
		datasetExperiments  = Util.readInDataSet(fileFolder+fileName+".data", delimiter);
		int numPoints       = datasetExperiments.getObjects().length;
		double[][] database = new double[numPoints][];

		for(Instance inst: datasetExperiments.getObjects())
			database[inst.getIndex()] = inst.getCoordinates();


		HashMap<Integer, Pair<Double,Graph>> mKNN 			   		 = new HashMap<Integer, Pair<Double,Graph>>();
		HashMap<Integer, Pair<Double,UndirectedGraph>> mstReach   	 = new HashMap<Integer, Pair<Double,UndirectedGraph>>();
		HashMap<Integer, Pair<Double,UndirectedGraph>> mstAptsStore	 = new HashMap<Integer, Pair<Double,UndirectedGraph>>();

		for(Integer minPts: arrayMinPts)
		{
			startTime = new Long(System.currentTimeMillis());
			double[] coreDistances = HDBSCANStar.calculateCoreDistances(database, minPts, distance);
			UndirectedGraph mst    = HDBSCANStar.constructMST(database, coreDistances, true, distance);
			double timeToComputeMST = ((System.currentTimeMillis() - startTime)/1000.00);

			mstReach.put(minPts, new Pair<Double, UndirectedGraph>(timeToComputeMST, mst));

			startTime = new Long(System.currentTimeMillis());
			Graph graph 		= GraphGenerator.generateGraph(database, distance, minPts);
			double timeToComputeGraph = ((System.currentTimeMillis() - startTime)/1000.00);
			mKNN.put(minPts, new Pair<Double, Graph>(timeToComputeGraph,graph));
		}

		/** Construction of the MST using the apts core distance and new mutual reachability distance*/
		startTime = new Long(System.currentTimeMillis());
		double[] coreDistances   = HDBSCANApts.calculateCoreDistances(database, distance);
		UndirectedGraph mstApts  = HDBSCANApts.constructMST(database, coreDistances, true, distance);
		double timeToComputeMST = ((System.currentTimeMillis() - startTime)/1000.00);
		

		for(Integer minPts: arrayMinPts)
			mstAptsStore.put(minPts, new Pair<Double, UndirectedGraph>(timeToComputeMST, mstApts));


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


		String folderLabeledObjects = outputFolder+fileName+"-labeled-objects-time.csv";
		BufferedWriter responseWriter = new BufferedWriter(new FileWriter(folderLabeledObjects));



		/**Perform trials for each quantity of pre labeled objects */
		
		int nLab = 30;
		for(int trial = 0; trial < numberTrials; trial++)
		{
			// Before each trial, clear the labels for all the data sets
			Util.resetDataset(datasetExperiments, true);
			ArrayList<Instance>   preLabeled = labelSelection.selectLabels(datasetExperiments, nLab, 0);

			String tmp = "";
			for(Instance inst: preLabeled)
				tmp = tmp + inst.getIndex() + ",";

			tmp = nLab + ":" + (trial+1) + ":" + tmp.substring(0, (tmp.length()-2)) + "\n";
			responseWriter.write(tmp);

		}// End for Trials
		responseWriter.close();
	}
}