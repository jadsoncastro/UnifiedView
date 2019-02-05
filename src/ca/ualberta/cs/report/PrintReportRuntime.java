package ca.ualberta.cs.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.rmi.registry.LocateRegistry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import com.sun.xml.internal.fastinfoset.tools.PrintTable;

import javafx.util.Pair;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.distance.EuclideanDistance;
import ca.ualberta.cs.experimentSemiSupClassification.SimpleStatistics;
import ca.ualberta.cs.model.DescriptionAlgorithms;

/** Statistics analsys (Mean and SD) for each algorithm over all data sets.
 * @author jadson
 */

public class PrintReportRuntime
{

	private static final DescriptionAlgorithms param = new DescriptionAlgorithms();

	public static void main(String args[]) throws IOException
	{

		String stringFolder = args[0];
		String validationMethod = args[1];
		String descriptionComparison = args[2];

		File folder         = new File(stringFolder);
		String tableStatistics = "Dataset\tAlgorithm\tPlab\tTrial\tPerformance\n";


		System.out.println("----Writing head of the table results " + "----");

		String tmp = stringFolder + "results-" + validationMethod + "-" + descriptionComparison + ".csv";
		BufferedWriter responseWriter = new BufferedWriter(new FileWriter(tmp, true));		
		responseWriter.write(tableStatistics);


		ArrayList<String> algorithmsToCompare = new ArrayList<String>();


		/**
		 * TODO: Semi-supervised clustering approaches
		 */
		if(args.length < 4)
		{
			System.err.println("It is necessary to inform at least two algorithms that you wanto to compare!");
			System.exit(-1);
		}else
		{
			for(int i=3; i< args.length; i++)
			{
				algorithmsToCompare.add(args[i]);
			}
		}


		for(File fileEntry: folder.listFiles())
		{
			String dataFile   = fileEntry.getName();
			if(!fileEntry.isHidden() && !fileEntry.isDirectory())
			{

				if(dataFile.endsWith(".reportTime"))
				{
					try(FileInputStream fileIn = new FileInputStream(stringFolder + dataFile);
							ObjectInputStream in   = new ObjectInputStream(fileIn);)
					{
						Map<String, Map<String, Map<Integer, ArrayList<Double>>>> reportForDataset = 
								(Map<String, Map<String, Map<Integer, ArrayList<Double>>>>) in.readObject();

						String dataName = dataFile.substring(0, dataFile.lastIndexOf("."));
						System.out.println("Processing data: " + dataName );

						tableStatistics = printTableStatistics(dataName, reportForDataset.get(validationMethod), algorithmsToCompare);
						responseWriter.write(tableStatistics);

					}
					catch(ClassNotFoundException | IOException ec)
					{
						System.err.println("Error while reading " + dataFile);
						System.exit(-1);
					}
				}
			}
		}

		responseWriter.close();
		System.out.println("Finished");

	}

	private static String printTableStatistics(String stringData, Map<String, Map<Integer, ArrayList<Double>>> report, ArrayList<String> algorithmsList)
	{
		String response = "";

		for(Map.Entry<String, Map<Integer, ArrayList<Double>>> firstEntry: report.entrySet())
		{
			String algorithm = firstEntry.getKey();

			if(!algorithmsList.contains(algorithm))
				continue;

			for(Map.Entry<Integer, ArrayList<Double>> secondEntry: firstEntry.getValue().entrySet())
			{
				Integer plab = secondEntry.getKey();
				ArrayList<Double> arrayResults = secondEntry.getValue();

				for(int trial = 0; trial < arrayResults.size(); trial++)
				{
					//		String tableStatistics = "Dataset\tAlgorithm\tPlab\tTrial\tPerformance\n";
					response += stringData.toUpperCase() + "\t" + algorithm + "\t" + plab + "\t" + trial + "\t" + arrayResults.get(trial) + "\n";
				}
			}
		}
		
		return response;
	}
}