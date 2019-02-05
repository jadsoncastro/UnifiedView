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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javafx.util.Pair;
import ssExtraction.SSExtraction;
import ssExtraction.SemiWeight;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;
import ca.ualberta.cs.hissclu.HISSCLU;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.DescriptionAlgorithms;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.Util;
import ca.ualberta.cs.ssdbscan.SSDBSCANLelis;

public class RuntimeHISSCLUAndSSDBSCAN
{
	private static final DescriptionAlgorithms parameters = new DescriptionAlgorithms();

	public static void performIt(String fileName, String fileFolder, String outputFolder,
			DistanceCalculator distance, double rho, double expo, String delimiter)
					throws Exception
	{

		//  Validation Method (String) -> Algorithm (String) -> Number of missed classes (Integer) -> % labeled (Integer) -> mPts (Integer) -> Result (ArrayList<Double>)
		Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report = new HashMap<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>>();
		Map<Integer, UndirectedGraph> mstCd= null; // MST files
		Map<Integer, Double> timeMST= null; // time registered to compute the MST files


		// Reading data set file
		Dataset datasetExperiments = new Dataset();
		datasetExperiments  = Util.readInDataSet(fileFolder+fileName+".data", delimiter);
		int numPoints       = datasetExperiments.getObjects().length;
		double[][] database = new double[numPoints][];

		for(Instance inst: datasetExperiments.getObjects())
			database[inst.getIndex()] = inst.getCoordinates();


		// Read file with MSTS
		try(FileInputStream fileIn = new FileInputStream(outputFolder + fileName + ".mstCoreDistance");
				ObjectInputStream in = new ObjectInputStream(fileIn);)
		{
			mstCd = (Map<Integer,UndirectedGraph>) in.readObject();
		}
		catch(ClassNotFoundException | IOException ec)
		{
			System.err.println("[HS] Error while reading MST for dataset " + fileName);
			System.exit(-1);
		}


		// Read file with time registered to compute the MSTS
		try(FileInputStream fileIn = new FileInputStream(outputFolder + fileName + ".timeMSTCoreDistance");
				ObjectInputStream in = new ObjectInputStream(fileIn);)
		{
			timeMST = (Map<Integer,Double>) in.readObject();
		}
		catch(ClassNotFoundException | IOException ec)
		{
			System.err.println("[HS] Error while reading time  registered to compute MSTs for dataset " + fileName);
			System.exit(-1);
		}






		//Defining the array of minPoints
		ArrayList<Integer> arrayMinPts = new ArrayList<Integer>(mstCd.keySet());

		String[] algorithmsDesc = 
			{
					parameters.hisscluClusterBased,
					parameters.ssdbscan,
					parameters.ssdbscanLelis
			};

		// Initialization of the report for the algorithms
		report.put(parameters.wholeProcess, new HashMap<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>());
		report.put(parameters.timeToPropagate, new HashMap<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>());


		for(String alg: algorithmsDesc)
		{
			report.get(parameters.wholeProcess).put(alg, new HashMap<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>());
			report.get(parameters.timeToPropagate).put(alg, new HashMap<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>());
		}
		
		long overallStartTime = System.currentTimeMillis();
		String fileLabeled = outputFolder+fileName+"-labeled-objects.csv";

		BufferedReader reader = new BufferedReader(new FileReader(fileLabeled));
		String line = reader.readLine();

		int lineIndex = 0;
		while (line != null) 
		{
			Util.resetDataset(datasetExperiments, true);
			HashMap<Integer, Integer> preLabeledDenBased = new HashMap<Integer, Integer>();
			TreeMap<Integer, Pair<Integer, double[]>> classifiedHissclu = new TreeMap<Integer, Pair<Integer,double[]>>();

			lineIndex++;
			line.replaceAll("\\s+",""); // Remove spaces that might exist in the line
			String[] lineContents = line.split(":");

			int pmc   = Integer.parseInt(lineContents[0]);
			int plab  = Integer.parseInt(lineContents[1]);
			int trial = Integer.parseInt(lineContents[2]);


			for(String alg: algorithmsDesc)
			{
				// Compute the complete time of the algorithm
				if(!report.get(parameters.wholeProcess).get(alg).containsKey(pmc))
					report.get(parameters.wholeProcess).get(alg).put(pmc, new HashMap<Integer, Map<Integer, ArrayList<Double>>>());

				if(!report.get(parameters.wholeProcess).get(alg).get(pmc).containsKey(plab))
					report.get(parameters.wholeProcess).get(alg).get(pmc).put(plab, new HashMap<Integer, ArrayList<Double>>());

				for(Integer mpts: arrayMinPts)
				{
					if(!report.get(parameters.wholeProcess).get(alg).get(pmc).get(plab).containsKey(mpts))
						report.get(parameters.wholeProcess).get(alg).get(pmc).get(plab).put(mpts, new ArrayList<Double>());
				}
				
				
				// Update the time to of the label propagation
				if(!report.get(parameters.timeToPropagate).get(alg).containsKey(pmc))
					report.get(parameters.timeToPropagate).get(alg).put(pmc, new HashMap<Integer, Map<Integer, ArrayList<Double>>>());

				if(!report.get(parameters.timeToPropagate).get(alg).get(pmc).containsKey(plab))
					report.get(parameters.timeToPropagate).get(alg).get(pmc).put(plab, new HashMap<Integer, ArrayList<Double>>());

				for(Integer mpts: arrayMinPts)
				{
					if(!report.get(parameters.timeToPropagate).get(alg).get(pmc).get(plab).containsKey(mpts))
						report.get(parameters.timeToPropagate).get(alg).get(pmc).get(plab).put(mpts, new ArrayList<Double>());
				}

			}

			lineContents[3] = lineContents[3].replaceAll("\\s+","");
			String[] stringLabeled = lineContents[3].split(delimiter);
			
			
			int idLabeled = Integer.parseInt(stringLabeled[0]);
			
			if(idLabeled <= -1) // For these algorithms, in cases where we do not have label, we cannot run the algorithm.
			{
				line = reader.readLine();
				continue;
			}
			
			// Store the set of labeled objects
			for (int i = 0; i < stringLabeled.length; i++) 
			{
				try
				{
					idLabeled = Integer.parseInt(stringLabeled[i]);

					datasetExperiments.getObjects()[idLabeled].setPreLabeled(true);
					datasetExperiments.getObjects()[idLabeled].setLabel(datasetExperiments.getObjects()[idLabeled].getTrueLabel());
					preLabeledDenBased.put(idLabeled, datasetExperiments.getObjects()[idLabeled].getTrueLabel());
					classifiedHissclu.put(idLabeled, new Pair<Integer, double[]>(datasetExperiments.getObjects()[idLabeled].getTrueLabel(), datasetExperiments.getObjects()[idLabeled].getCoordinates()));
				}
				catch (NumberFormatException nfe) 
				{
					System.err.println("[HS] Illegal value on line " + lineIndex + ":" + stringLabeled[i] + " Dataset: " + fileName);
				}
			}

			/**
			 * Run the algorithms
			 */
			Map<Integer, Integer> result = null;
			SemiWeight semi   	 = new SemiWeight(classifiedHissclu, rho, expo, distance);
			semi.performQuery(database);
			long startTime;
			double timeToRegister;
			
			for(Integer minPts: arrayMinPts)
			{
				// Apply: HISSCLU
				Util.resetDataset(datasetExperiments, false);

				startTime = new Long(System.currentTimeMillis());
				HISSCLU.expansion(datasetExperiments, minPts, 0.2, minPts, distance, preLabeledDenBased, semi, false, "");
				timeToRegister = ((System.currentTimeMillis() - startTime)/1000.00);

				report.get(parameters.wholeProcess).get(parameters.hisscluClusterBased).get(pmc).get(plab).get(minPts).add(timeToRegister);
				report.get(parameters.timeToPropagate).get(parameters.hisscluClusterBased).get(pmc).get(plab).get(minPts).add(timeToRegister);


				
				// Apply SSDBSCAN
				Util.resetDataset(datasetExperiments, false);

				startTime = new Long(System.currentTimeMillis());
				result = SSExtraction.expandSSDBSCAN(new UndirectedGraph(mstCd.get(minPts)),preLabeledDenBased);
				timeToRegister = ((System.currentTimeMillis() - startTime)/1000.00);

				report.get(parameters.wholeProcess).get(parameters.ssdbscan).get(pmc).get(plab).get(minPts).add(timeToRegister+timeMST.get(minPts));
				report.get(parameters.timeToPropagate).get(parameters.ssdbscan).get(pmc).get(plab).get(minPts).add(timeToRegister);

				
				
				// Apply SSDBSCAN (Lelis's code)
				Util.resetDataset(datasetExperiments, false);

				startTime = new Long(System.currentTimeMillis());
				SSDBSCANLelis amazon2 = new SSDBSCANLelis(distance);
				result = amazon2.getForest(datasetExperiments, minPts);
				
				timeToRegister = ((System.currentTimeMillis() - startTime)/1000.00);
				report.get(parameters.wholeProcess).get(parameters.ssdbscanLelis).get(pmc).get(plab).get(minPts).add(timeToRegister);
				report.get(parameters.timeToPropagate).get(parameters.ssdbscanLelis).get(pmc).get(plab).get(minPts).add(timeToRegister);
//				System.out.println(String.format(Locale.CANADA, "Result of the algorithm %s: %.2f seconds", parameters.ssdbscanLelis, timeToRegister));

			}

			// Read the next trial of the algorithm
			line = reader.readLine();


			//-------------------------------- Save report of the data set ------------------------------------------------------
			String stringFolder    = outputFolder + fileName + ".reportHisscluAndSsdbscan";

			if(trial%5==0)
			{
				//				System.out.println("Writing results in trial " + trial);
				try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
						ObjectOutputStream out  = new ObjectOutputStream(outFile))
				{
					out.writeObject(report);
				}
				catch(IOException e)
				{
					System.out.println("[HS] An error ocurred while writing the report for data set " + fileName);
					e.printStackTrace();
					System.exit(-1);
				}
			}

		} // End for while

		reader.close();
		System.out.println("[HS] Dataset:  " + fileName + ". Overall time(min): " + (((System.currentTimeMillis() - overallStartTime)/1000)/60));
	}
}