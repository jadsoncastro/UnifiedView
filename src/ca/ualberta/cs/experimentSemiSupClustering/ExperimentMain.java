package ca.ualberta.cs.experimentSemiSupClustering;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Serializable;
import java.util.ArrayList;

import ca.ualberta.cs.distance.CosineSimilarity;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.distance.EuclideanDistance;
import ca.ualberta.cs.distance.ManhattanDistance;
import ca.ualberta.cs.distance.PearsonCorrelation;
import ca.ualberta.cs.distance.TanimotoSimilarity;
import ca.ualberta.cs.model.LabelSelection;
import ca.ualberta.cs.model.SelectRandomObjectsGivenPercentage;
import ca.ualberta.cs.model.SelectRandomObjectsGivenPercentageExtratified;

public class ExperimentMain implements Serializable
{

	private static final long serialVersionUID = 1L;

	private static final String MESSAGE_ERROR= "How to call the experiment code: \n Java -jar Experiments.jar [folder with data] [output folder] [listOfFiles]";

	public static void main(String [] args) throws Exception
	{
		if(args.length < 3)
		{
			System.err.println("Error in the input parameters.");
			System.err.println(MESSAGE_ERROR);
			System.exit(-1);
		}

		String dataFolder   = args[0];
		String outputFolder = args[1];

		int numberTrials	= 20;
		String delimiter	= ",";

		//Parameters for HISSCLU
		double rho			= 10.0;
		double expoent		= 5.0;


		ArrayList<Integer> arrayPlab   = new ArrayList<Integer>();
//		arrayPlab.add(0);		
		arrayPlab.add(1);
//		arrayPlab.add(2);
//		arrayPlab.add(5);
//		arrayPlab.add(10);

		ArrayList<Integer> arrayMinPts = new ArrayList<Integer>();
		arrayMinPts.add(4);
//		arrayMinPts.add(8);
//		arrayMinPts.add(12);
//		arrayMinPts.add(16);
//		arrayMinPts.add(20);

		ArrayList<Integer> arrayMissed = new ArrayList<Integer>();
		arrayMissed.add(0);
//		arrayMissed.add(50);
//		arrayMissed.add(100);

		LabelSelection methodLabelSelection = new SelectRandomObjectsGivenPercentageExtratified();
//		LabelSelection methodLabelSelection = new SelectRandomObjectsGivenPercentage();

		String listOfFiles = args[2];

		BufferedReader reader = new BufferedReader(new FileReader(listOfFiles));
		String line = reader.readLine();

		while (line != null) 
		{
			line.replaceAll("\\s+",""); // Remove spaces that might exist in the line
			String[] lineContents = line.split(delimiter);

			String dataFile      = lineContents[0];
			String inputName     = dataFile.substring(0, dataFile.lastIndexOf("."));
			String distanceParam = lineContents[1];

			DistanceCalculator distance = null;

			if(distanceParam.equals("euclidean"))
				distance	= new EuclideanDistance();

			else if (distanceParam.equals("cosine"))
				distance	= new CosineSimilarity();

			else if (distanceParam.equals("tanimoto"))
				distance	= new TanimotoSimilarity();

			else if (distanceParam.equals("pearson"))
				distance	= new PearsonCorrelation();

			else if (distanceParam.equals("manhattan"))
				distance	= new ManhattanDistance();
			else
			{
				System.err.println(MESSAGE_ERROR);
				System.exit(-1);
			}

			System.out.println("Dataset: " + inputName);
			
			// Create scenarios
			SSClusteringCreateScenarios.generateInformation(inputName, dataFolder, outputFolder, methodLabelSelection, distance, numberTrials, arrayPlab, arrayMinPts, arrayMissed, delimiter);

			// Performance experiments
//			RunHISSCLUAndSSDBSCAN.performIt(inputName, dataFolder, outputFolder, distance, rho, expoent, delimiter);
//			RunCoreDistanceConfiguration.performIt(inputName, dataFolder, outputFolder, distance, delimiter);
//			RunAllPointsCoreDistanceConfiguration.performIt(inputName, dataFolder, outputFolder, distance, delimiter);

			
			// Runtime experiments.
			RuntimeHISSCLUAndSSDBSCAN.performIt(inputName, dataFolder, outputFolder, distance, rho, expoent, delimiter);
			RuntimeCoreDistance.performIt(inputName, dataFolder, outputFolder, distance, delimiter);
			RuntimeAllPointsCoreDistance.performIt(inputName, dataFolder, outputFolder, distance, delimiter);
			
			//  Read the next trial of the algorithm
			line = reader.readLine();
			
		}// End for while
		reader.close();
	}
}