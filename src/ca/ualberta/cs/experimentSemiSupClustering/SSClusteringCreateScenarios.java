package ca.ualberta.cs.experimentSemiSupClustering;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import SHM.HMatrix.HMatrix;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.hdbscanApts.HDBSCANApts;
import ca.ualberta.cs.hdbscanstar.Cluster;
import ca.ualberta.cs.hdbscanstar.HDBSCANStar;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;
import ca.ualberta.cs.hdbscanstar.HDBSCANStarRunner.WrapInt;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.LabelSelection;
import ca.ualberta.cs.model.Util;

public class SSClusteringCreateScenarios
{

	public static void generateInformation(String fileName, String fileFolder, String outputFolder,
			LabelSelection labelSelection, DistanceCalculator distance,
			int numberTrials, ArrayList<Integer> arrayPlab,
			ArrayList<Integer> arrayMinPts,
			ArrayList<Integer> numberOfMissedClasses,
			String delimiter) throws Exception
	{	

		//  Reading data set file
		Dataset datasetExperiments = new Dataset();
		datasetExperiments  = Util.readInDataSet(fileFolder+fileName+".data", delimiter);
		int numPoints       = datasetExperiments.getObjects().length;
		double[][] database = new double[numPoints][];

		for(Instance inst: datasetExperiments.getObjects())
			database[inst.getIndex()] = inst.getCoordinates();

		HashMap<Integer, UndirectedGraph> mstReach 	  	 = new HashMap<Integer, UndirectedGraph>();
		HashMap<Integer, UndirectedGraph> mstAptsStore	 = new HashMap<Integer, UndirectedGraph>();

		HashMap<Integer, ArrayList<Cluster>> clustersCoreDistance = new HashMap<Integer, ArrayList<Cluster>>();
		HashMap<Integer, ArrayList<Cluster>> clustersAllPoints = new HashMap<Integer, ArrayList<Cluster>>();

		HashMap<Integer, HMatrix> matrixCoreDistance = new HashMap<Integer, HMatrix>();
		HashMap<Integer, HMatrix> matrixAllPoints = new HashMap<Integer, HMatrix>();
		
		long startTime;
		HashMap<Integer, Double> timeCoreDistance = new HashMap<Integer, Double>();
		HashMap<Integer, Double> timeMSTCoreDistance = new HashMap<Integer, Double>();
		HashMap<Integer, Double> timeAllPoints = new HashMap<Integer, Double>();
		

		
		for(Integer minPts: arrayMinPts)
		{
			startTime = new Long(System.currentTimeMillis());
			double[] coreDistances = HDBSCANStar.calculateCoreDistances(database, minPts, distance);
			UndirectedGraph mst = HDBSCANStar.constructMST(database, coreDistances, true, distance);
			
			double timeMST = ((System.currentTimeMillis() - startTime)/1000.00);
			timeMSTCoreDistance.put(minPts, timeMST);
			
			mst.quicksortByEdgeWeight();
			mstReach.put(minPts, new UndirectedGraph(mst));

			ArrayList<Cluster> clusters	= null;
			double[] pointNoiseLevels 	= new double[numPoints];
			int[] pointLastClusters 	= new int[numPoints];

			//Read hierarchy matrix
			HMatrix matrix			    = new HMatrix();
			WrapInt lineCount 			= new WrapInt(0);

			clusters = HDBSCANApts.computeHierarchyAndClusterTree(mst, minPts, false, null, " ", " ", ",", pointNoiseLevels, pointLastClusters, "shm", matrix, lineCount);
			double timeToConstructTree = ((System.currentTimeMillis() - startTime)/1000.00);

			timeCoreDistance.put(minPts, timeToConstructTree);
			clustersCoreDistance.put(minPts, clusters);
			matrixCoreDistance.put(minPts, matrix);
			mst=null;
		}

		/** Construction of the MST using the apts core distance and new mutual reachability distance*/
		startTime = new Long(System.currentTimeMillis());
		double[] coreDistances   = HDBSCANApts.calculateCoreDistances(database, distance);
		UndirectedGraph mstApts  = HDBSCANApts.constructMST(database, coreDistances, true, distance);
		mstApts.quicksortByEdgeWeight();
		double timeToConstructTree = ((System.currentTimeMillis() - startTime)/1000.00);

		for(Integer minPts: arrayMinPts)
		{
			timeAllPoints.put(minPts, timeToConstructTree);
			mstAptsStore.put(minPts, mstApts);
		}
		
		
		for(Integer minPts: arrayMinPts)
		{
			startTime = new Long(System.currentTimeMillis());
			ArrayList<Cluster> clusters	= null;
			double[] pointNoiseLevels 	= new double[numPoints];
			int[] pointLastClusters 	= new int[numPoints];

			//Read hierarchy matrix
			HMatrix matrix			    = new HMatrix();
			WrapInt lineCount 			= new WrapInt(0);
			
			clusters = HDBSCANApts.computeHierarchyAndClusterTree(new UndirectedGraph(mstApts), minPts, false, null, " ", " ", ",", pointNoiseLevels, pointLastClusters, "shm", matrix, lineCount);
			timeToConstructTree = ((System.currentTimeMillis() - startTime)/1000.00);
			
			timeAllPoints.put(minPts, timeAllPoints.get(minPts)+timeToConstructTree);
			clustersAllPoints.put(minPts, clusters);
			matrixAllPoints.put(minPts, matrix);
		}

		
		// ------------------------------ Saving the graphs and MSTS ---------------------------------------------
		// First: Save all the core distance MST
		String stringFolder = outputFolder + fileName + ".mstCoreDistance";

		try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream out = new ObjectOutputStream(outFile))
		{
			out.writeObject(mstReach);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the MST (Core distance): " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}
		

		// Second: Save all the ArrayList<Cluster> core distance
		stringFolder = outputFolder + fileName + ".clustersCoreDistance";

		try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream out = new ObjectOutputStream(outFile))
		{
			out.writeObject(clustersCoreDistance);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the clusters (Core distance): " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}


		// Third: Save all the HMatrix core distance
		stringFolder = outputFolder + fileName + ".matrixCoreDistance";

		try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream out = new ObjectOutputStream(outFile))
		{
			out.writeObject(matrixCoreDistance);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the matrices (Core distance): " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}


		// Fourth: Save the time spent to compute the hierarchy
		stringFolder = outputFolder + fileName + ".timeCoreDistance";

		try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream out = new ObjectOutputStream(outFile))
		{
			out.writeObject(timeCoreDistance);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the time results (Core distance): " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}

		// Fifth: Save the time spent to compute the MST
		stringFolder = outputFolder + fileName + ".timeMSTCoreDistance";

		try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream out = new ObjectOutputStream(outFile))
		{
			out.writeObject(timeMSTCoreDistance);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the result time for MSTs (Core distance): " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}
		
		

		
		
		
		
		// ------------------------------ Saving the graphs and MSTS (All-points core distance) --------------
		// First: Save all the all-points core distance MST
		stringFolder = outputFolder + fileName + ".mstAllPointsCoreDistance";

		try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream out = new ObjectOutputStream(outFile))
		{
			out.writeObject(mstAptsStore);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the MST (All-points Core distance): " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}
		

		// Second: Save all the ArrayList<Cluster> all points core distance
		stringFolder = outputFolder + fileName + ".clustersAllPointsCoreDistance";

		try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream out = new ObjectOutputStream(outFile))
		{
			out.writeObject(clustersAllPoints);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the clusters (All points Core distance): " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}


		// Third: Save all the HMatrix All points core distance
		stringFolder = outputFolder + fileName + ".matrixAllPointsCoreDistance";

		try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream out = new ObjectOutputStream(outFile))
		{
			out.writeObject(matrixAllPoints);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the matrices (All-points Core distance): " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}

		
		// Fourth: Save the time spent to create the hierarchy
		stringFolder = outputFolder + fileName + ".timeAllPointsCoreDistance";

		try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
				ObjectOutputStream out = new ObjectOutputStream(outFile))
		{
			out.writeObject(timeAllPoints);
		}
		catch(IOException e)
		{
			System.out.println("An error ocurred while writing the time registers (All-points Core distance): " + fileName);
			e.printStackTrace();
			System.exit(-1);
		}

				
		
		
		
		
		
		//* Commented to perform experiments with minClSize=1
		String folderLabeledObjects = outputFolder+fileName+"-labeled-objects.csv";
		BufferedWriter responseWriter = new BufferedWriter(new FileWriter(folderLabeledObjects));


		//Perform trials for each quantity of pre labeled objects
		for(Integer pmc: numberOfMissedClasses)
		{
			for(Integer pLab: arrayPlab)
			{
				for(int trial = 0; trial < numberTrials; trial++)
				{
					// Before each trial, clear the labels for all the data sets
					Util.resetDataset(datasetExperiments, true);
//					ArrayList<Instance>   preLabeled = labelSelection.selectLabels(datasetExperiments, (int)Math.ceil(pLab*numPoints/100.00), pmc);
					ArrayList<Instance>   preLabeled = labelSelection.selectLabels(datasetExperiments, 30, pmc); // To use in the semi-supervised clustering runtime

					String tmp = "";

					if(preLabeled.isEmpty())
						tmp = "-1,";

					for(Instance inst: preLabeled)
						tmp = tmp + inst.getIndex() + ",";

//					tmp = pmc + ":" + pLab + ":" + (trial+1) + ":" + tmp.substring(0, (tmp.length()-1)) + "\n";
					tmp = pmc + ":" + 30 + ":" + (trial+1) + ":" + tmp.substring(0, (tmp.length()-1)) + "\n";
					responseWriter.write(tmp);

				} // End for Trials
			} // End for pLab
		} //End for number of classes for which labels are missing

		responseWriter.close();
		//*/
	}
}