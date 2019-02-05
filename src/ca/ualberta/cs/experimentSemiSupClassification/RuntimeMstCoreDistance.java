package ca.ualberta.cs.experimentSemiSupClassification;

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
import ssExtraction.UnifiedView;
import ssExtraction.SemiWeight;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.hdbscanstar.HDBSCANStar;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.DescriptionAlgorithms;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.Util;
import ca.ualberta.cs.validity.FMeasure;

public class RuntimeMstCoreDistance
{
	private static final DescriptionAlgorithms parameters = new DescriptionAlgorithms();


	public static void performIt(String fileName, String fileFolder, String outputFolder,
			DistanceCalculator distance,
			double rho, double expo,
			String delimiter) throws Exception
	{

		// Time (String) -> Algorithm (String) -> % labeled (Integer) -> Time (ArrayList<Double>)
		Map<String, Map<String, Map<Integer, ArrayList<Double>>>> report = new HashMap<String, Map<String, Map<Integer, ArrayList<Double>>>>();
		Map<Integer, Pair<Double, UndirectedGraph>> mstCd= null; // MST files


		// Reading data set file
		Dataset datasetExperiments = new Dataset();
		datasetExperiments  = Util.readInDataSet(fileFolder+fileName+".data", delimiter);
		int numPoints       = datasetExperiments.getObjects().length;
		double[][] database = new double[numPoints][];

		for(Instance inst: datasetExperiments.getObjects())
			database[inst.getIndex()] = inst.getCoordinates();


		// Read file with MSTS
		try(FileInputStream fileIn = new FileInputStream(outputFolder + fileName + ".mstCd");
				ObjectInputStream in   = new ObjectInputStream(fileIn);)
		{
			mstCd= (Map<Integer,Pair<Double, UndirectedGraph>>) in.readObject();
		}
		catch(ClassNotFoundException | IOException ec)
		{
			System.err.println("[MST-CD]Error while reading MST for dataset " + fileName);
			System.exit(-1);
		}

		//Defining the array of minPoints
		ArrayList<Integer> arrayMinPts = new ArrayList<Integer>(mstCd.keySet());
		double timeToComputeGraph = mstCd.get(arrayMinPts.get(0)).getKey();

		String[] algorithmsDesc = 
			{
					// New configurations for the label propagation step
					parameters.hdbscanCd,
					parameters.hdbscanCdWPWD,
					parameters.hdbscanCdWMST,
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
			long startTime;
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
					System.err.println("[MST-CD]Illegal value on line " + lineIndex + ":" + stringLabeled[i] + " Dataset: " + fileName);
				}
			}
			
			double wholeTime, tPropTime, weiFunTime;


			/**
			 * Run the algorithms
			 */
			Map<Integer, Integer> result = null;
			SemiWeight semi   	 = new SemiWeight(classifiedHissclu, rho, expo, distance);
			
			startTime= new Long(System.currentTimeMillis());
			semi.performQuery(database); // Perform the query define the optimal labeled objects
			weiFunTime = ((System.currentTimeMillis() - startTime)/1000.00);


			for(Integer minPts: arrayMinPts)
			{
				// Apply: HDBSCAN*(cd,-)
//				System.out.println("Running Algorithm " + parameters.jsCd+ " mPts: " + minPts);
				Util.resetDataset(datasetExperiments, false);
				result=null;
				
				startTime= new Long(System.currentTimeMillis());
				
				result = UnifiedView.expandLabels(new UndirectedGraph(mstCd.get(minPts).getValue()), preLabeledDenBased, false);
				
				tPropTime  = ((System.currentTimeMillis() - startTime)/1000.00);
				wholeTime  = timeToComputeGraph + tPropTime;

				report.get(parameters.wholeProcess).get(parameters.hdbscanCd).get(plab).add(wholeTime);
				report.get(parameters.graphConstruction).get(parameters.hdbscanCd).get(plab).add(timeToComputeGraph);
				report.get(parameters.timeToPropagate).get(parameters.hdbscanCd).get(plab).add(tPropTime);
				report.get(parameters.weightFunction).get(parameters.hdbscanCd).get(plab).add(0.0);



				// Apply: HDBSCAN*(cd,wMST)
//				System.out.println("Running Algorithm " + parameters.jsCdWMST+ " mPts: " + minPts);
				Util.resetDataset(datasetExperiments, false);
				result=null;

				startTime= new Long(System.currentTimeMillis());
				
				result = UnifiedView.expandWeighted(database, new UndirectedGraph(mstCd.get(minPts).getValue()), preLabeledDenBased, semi, distance, false);

				tPropTime  = ((System.currentTimeMillis() - startTime)/1000.00);
				wholeTime  = timeToComputeGraph + tPropTime;

				report.get(parameters.wholeProcess).get(parameters.hdbscanCdWMST).get(plab).add(wholeTime);
				report.get(parameters.graphConstruction).get(parameters.hdbscanCdWMST).get(plab).add(timeToComputeGraph);
				report.get(parameters.timeToPropagate).get(parameters.hdbscanCdWMST).get(plab).add(tPropTime);
				report.get(parameters.weightFunction).get(parameters.hdbscanCdWMST).get(plab).add(weiFunTime + semi.getTime());
				
				
				// Apply: HDBSCAN*(cd,wPWD)
//				System.out.println("Running Algorithm " + parameters.jsCdWPWD+ " mPts: " + minPts);
				Util.resetDataset(datasetExperiments, false);
				result=null;
				
				startTime= new Long(System.currentTimeMillis());
				double[] cd = HDBSCANStar.calculateWeightedCoreDistances(database, minPts, distance, semi);
				UndirectedGraph mstWdist = HDBSCANStar.constructWeightedMST(database, cd, true, distance, semi);
				
				semi.setTime(((System.currentTimeMillis() - startTime)/1000.00));
				
				result = UnifiedView.expandLabels(mstWdist, preLabeledDenBased, false);
				
				tPropTime  = ((System.currentTimeMillis() - startTime)/1000.00);
				wholeTime = tPropTime;

				report.get(parameters.wholeProcess).get(parameters.hdbscanCdWPWD).get(plab).add(wholeTime);
				report.get(parameters.graphConstruction).get(parameters.hdbscanCdWPWD).get(plab).add(semi.getTime());
				report.get(parameters.timeToPropagate).get(parameters.hdbscanCdWPWD).get(plab).add(tPropTime);
				report.get(parameters.weightFunction).get(parameters.hdbscanCdWPWD).get(plab).add(weiFunTime+semi.getTime());

			}


			// Read the next trial of the algorithm
			line = reader.readLine();

			//-------------------------------- Save report of the data set ------------------------------------------------------
			String stringFolder 		    = outputFolder + fileName + ".reportTimeAlgorithmsCd";

			try (FileOutputStream outFile   = new FileOutputStream(stringFolder);
					ObjectOutputStream out = new ObjectOutputStream(outFile))
			{
				out.writeObject(report);
			}
			catch(IOException e)
			{
				System.out.println("[MST-CD]An error ocurred while writing the report for data set " + fileName);
				e.printStackTrace();
				System.exit(-1);
			}

			System.out.println("[MST-CD]Dataset:  " + fileName + ". plab: " + plab + ". Trial: " + trial + ". time(min): " + (((System.currentTimeMillis() - timeTrial)/1000)/60));

		}// End for while

		reader.close();

		System.out.println("[MST-CD]Dataset:  " + fileName + ". Overall time(min): " + (((System.currentTimeMillis() - overallStartTime)/1000)/60));

	}
}