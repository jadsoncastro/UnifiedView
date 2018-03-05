package ca.ualberta.cs.model;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Map;
import java.util.Stack;

import ca.ualberta.cs.distance.DistanceCalculator;

/**
 * Implementation of the SSDBSCAN algorithm, that uses one minimum Spanning tree.
 * @author jadsoncastro
 */
public class Util {

	// ------------------------------ PRIVATE VARIABLES ------------------------------

	// ------------------------------ CONSTANTS ------------------------------

	private static final int FILE_BUFFER_SIZE = 32678;

	// ------------------------------ CONSTRUCTORS ------------------------------

	// ------------------------------ PUBLIC METHODS ------------------------------

	/**
	 * Reads in the input data set from the file given, assuming the delimiter separates attributes
	 * for each data point, and each point is given on a separate line.  Error messages are printed
	 * if any part of the input is improperly formatted.
	 * @param fileName The path to the input file
	 * @param delimiter A regular expression that separates the attributes of each point
	 * @return A Dataset object
	 * @throws IOException If any errors occur opening or reading from the file
	 */
	public static Dataset readInDataSet(String fileName, String delimiter) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(fileName));
		Dataset dataSet = new Dataset();
		int numAttributes = -1;
		int lineIndex = 0;
		int idObj=0;
		String line = reader.readLine();

		while (line != null) {
			lineIndex++;
			String[] lineContents = line.split(delimiter);
			Instance inst = new Instance();

			if (numAttributes == -1)
				numAttributes = lineContents.length;
			else if (lineContents.length != numAttributes)
				System.err.println("Line " + lineIndex + " has incorrect number of attributes.");

			double[] attributes = new double[numAttributes-1];
			for (int i = 0; i < numAttributes-1; i++) 
			{
				try{
					//If an exception occurs, the attribute will remain 0:
					attributes[i] = Double.parseDouble(lineContents[i]);
				}
				catch (NumberFormatException nfe) {
					System.err.println("Illegal value on line " + lineIndex + ": " + lineContents[i]);
				}
				inst.addCoordinates(attributes);
			}

			inst.setTrueLabel(Integer.parseInt(lineContents[numAttributes-1])); // Last column of the file contains the label of the dataset
			inst.setIndex(idObj); // Set the label id

			dataSet.addObject(inst);
			idObj++;
			line = reader.readLine();
		}
		reader.close();
		return dataSet;
	}


	public static void resetDataset(Dataset d, boolean clearPreLabeled)
	{
		for(Instance inst: d.getObjects())
		{
			inst.setCoreDistance(0.0);
			inst.setWeight(0.0);

			if(clearPreLabeled)
			{
				inst.setLabel(-1);
				inst.setProcessed(false);
				inst.setPreLabeled(false);
				inst.setClusterAssignment(-1);
				inst.setPostProcessed(false);
			}else
			{
				if(!inst.isPreLabeled())
				{
					inst.setLabel(-1);
					inst.setProcessed(false);
					inst.setClusterAssignment(-1);
					inst.setPostProcessed(false);
				}else
				{
					inst.setLabel(inst.getTrueLabel());
					inst.setPreLabeled(true);
					inst.setProcessed(false);
					inst.setClusterAssignment(-1);
					inst.setPostProcessed(false);
				}
			}
		}
	}


}