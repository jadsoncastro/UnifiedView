package ca.ualberta.cs.experimentSemiSupClustering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import SHM.HMatrix.HMatrix;
import ca.ualberta.cs.hdbscanApts.HDBSCANApts;
import ca.ualberta.cs.hdbscanstar.Cluster;
import ca.ualberta.cs.hdbscanstar.Constraint;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.DescriptionAlgorithms;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.validity.AdjustedRandStatistic;
import ca.ualberta.cs.validity.WeightedARI;
import ssExtraction.SSExtraction;

public class RuntimeHDBSCANVariations 
{
	private static final DescriptionAlgorithms parameters = new DescriptionAlgorithms();


	//------------------------------- Unsupervised / constarint methods -----------------------------------
	public static void HdbscanUnsupervisedAndConstraints(ArrayList<Cluster> clusters,
			HMatrix matrix, HashMap<Integer, Integer> labeledObjects,
			Dataset dataset, Integer minPts,
			Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report,
			Map<Integer, Double> timeRegister,
			String algorithm, Integer pmc,
			Integer pLab) throws IOException
	{

		ArrayList<Constraint> constraints = null;
		HDBSCANApts.releaseAllClusters(clusters);

		long startTime = new Long(System.currentTimeMillis());
		double timeToRegister;

		if(labeledObjects != null)
		{
			constraints = new ArrayList<Constraint>();
			ArrayList<Map.Entry<Integer, Integer>> setEntries = new ArrayList(labeledObjects.entrySet());

			for(int i=0; i< setEntries.size()-1; i++)
			{
				Map.Entry<Integer, Integer> entryI = setEntries.get(i);
				for(int j= i+1; j< setEntries.size(); j++)
				{
					Map.Entry<Integer, Integer> entryJ = setEntries.get(j);

					if(entryI.getValue().equals(entryJ.getValue()))
					{
						constraints.add(new Constraint(entryI.getKey(), entryJ.getKey(), Constraint.CONSTRAINT_TYPE.MUST_LINK));
					}
					else
					{
						constraints.add(new Constraint(entryI.getKey(), entryJ.getKey(), Constraint.CONSTRAINT_TYPE.CANNOT_LINK));
					}
				}
			}
			HDBSCANApts.calculateAllNumContraintsSatisfied(matrix, clusters, constraints);
		}

		HDBSCANApts.propagateTree(clusters, "unsupervised");
		int[] tmp = HDBSCANApts.findProminentClustersSHM(clusters, matrix);
		timeToRegister = ((System.currentTimeMillis() - startTime)/1000.00);
		
		report.get(parameters.wholeProcess).get(algorithm).get(pmc).get(pLab).get(minPts).add(timeToRegister+timeRegister.get(minPts));
		report.get(parameters.timeToPropagate).get(algorithm).get(pmc).get(pLab).get(minPts).add(timeToRegister);

	}




	//------------------------------- BCubed based  methods --------------------------------------------------------------------------

	public static void BCubedBased(ArrayList<Cluster> clusters,
			UndirectedGraph mst,
			HMatrix matrix,
			HashMap<Integer, Integer> labeledObjects,
			Dataset dataset,
			Integer minClSize,
			Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report,
			Map<Integer, Double> timeRegister,
			String algorithm,
			Integer nmc,
			Integer pLab) throws IOException
	{

		HDBSCANApts.releaseAllClusters(clusters);

		long startTime = new Long(System.currentTimeMillis());
		double timeToRegister;

		HDBSCANApts.calculateBCubedIndex(matrix, clusters, labeledObjects);
		HDBSCANApts.propagateTree(clusters, "bcubed");

		int[] tmp = HDBSCANApts.findProminentClustersSHM(clusters, matrix);
		timeToRegister = ((System.currentTimeMillis() - startTime)/1000.00);
		
		report.get(parameters.wholeProcess).get(algorithm).get(nmc).get(pLab).get(minClSize).add(timeToRegister + timeRegister.get(minClSize));
		report.get(parameters.timeToPropagate).get(algorithm).get(nmc).get(pLab).get(minClSize).add(timeToRegister);
		
	}



	//------------------------------- Mixed approach --------------------------------------------------------------------------

	public static void MixedBCubed(ArrayList<Cluster> clusters,
			UndirectedGraph mst,
			HMatrix matrix,
			HashMap<Integer, Integer> labeledObjects,
			Dataset dataset,
			Integer minClSize,
			Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report,
			Map<Integer, Double> timeRegister,
			String algorithm,
			Integer nmc,
			Integer pLab) throws IOException
	{

		long startTime;
		double timeToRegister;
		
		double alpha=0.5;
		HDBSCANApts.releaseAllClusters(clusters);
		
		startTime = new Long(System.currentTimeMillis());
		HDBSCANApts.propagateTree(clusters, "unsupervised");
		double maxStability = clusters.get(1).getPropagatedStability();
		timeToRegister = ((System.currentTimeMillis() - startTime)/1000.00);

		HDBSCANApts.releaseAllClusters(clusters);

		startTime = new Long(System.currentTimeMillis());
		double maxBcubed = 1.0;
		if(!labeledObjects.isEmpty())
		{
			HDBSCANApts.calculateBCubedIndex(matrix, clusters, labeledObjects);
			HDBSCANApts.propagateTree(clusters, "bcubed");
			maxBcubed = clusters.get(1).getPropagatedBCubed();
			
			HDBSCANApts.releaseAllClusters(clusters);
			HDBSCANApts.calculateBCubedIndex(matrix, clusters, labeledObjects);
		}
		
		HDBSCANApts.calculateAllMixedIndexes(clusters, maxStability, maxBcubed, alpha);
		HDBSCANApts.propagateTree(clusters, "mixStabilityBcubed");
		int[] tmp = HDBSCANApts.findProminentClustersSHM(clusters, matrix);
		timeToRegister += ((System.currentTimeMillis() - startTime)/1000.00);

		report.get(parameters.wholeProcess).get(algorithm).get(nmc).get(pLab).get(minClSize).add(timeToRegister + timeRegister.get(minClSize));
		report.get(parameters.timeToPropagate).get(algorithm).get(nmc).get(pLab).get(minClSize).add(timeToRegister);
		
	}


	public static void MixedForConstraints(ArrayList<Cluster> clusters,
			HMatrix matrix,
			HashMap<Integer, Integer> labeledObjects,
			Dataset dataset,
			Integer minClSize,
			Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report,
			Map<Integer, Double> timeRegister,
			String algorithm,
			Integer nmc,
			Integer pLab) throws IOException
	{

		long startTime;
		double timeToRegister;

		double alpha=0.5;
		HDBSCANApts.releaseAllClusters(clusters);

		startTime = new Long(System.currentTimeMillis());
		HDBSCANApts.propagateTree(clusters, "unsupervised");
		double maxStability = clusters.get(1).getPropagatedStability();
		timeToRegister = ((System.currentTimeMillis() - startTime)/1000.00);
		
		HDBSCANApts.releaseAllClusters(clusters);

		startTime = new Long(System.currentTimeMillis());
		ArrayList<Constraint> constraints = null;
		int numConstraints =1;

		if(!labeledObjects.isEmpty())
		{
			constraints = new ArrayList<Constraint>();
			ArrayList<Map.Entry<Integer, Integer>> setEntries = new ArrayList(labeledObjects.entrySet());

			for(int i=0; i< setEntries.size()-1; i++)
			{
				Map.Entry<Integer, Integer> entryI = setEntries.get(i);
				for(int j= i+1; j< setEntries.size(); j++)
				{
					Map.Entry<Integer, Integer> entryJ = setEntries.get(j);

					if(entryI.getValue().equals(entryJ.getValue()))
					{
						constraints.add(new Constraint(entryI.getKey(), entryJ.getKey(), Constraint.CONSTRAINT_TYPE.MUST_LINK));
					}
					else
					{
						constraints.add(new Constraint(entryI.getKey(), entryJ.getKey(), Constraint.CONSTRAINT_TYPE.CANNOT_LINK));
					}
				}
			}
			HDBSCANApts.calculateAllNumContraintsSatisfied(matrix, clusters, constraints);
			numConstraints = constraints.size();
		}

		HDBSCANApts.calculateMixedForConstraints(clusters, maxStability, numConstraints, alpha);
		HDBSCANApts.propagateTree(clusters, "mixStabilityConstraint");
		int[] tmp = HDBSCANApts.findProminentClustersSHM(clusters, matrix);
		timeToRegister += ((System.currentTimeMillis() - startTime)/1000.00);

		report.get(parameters.wholeProcess).get(algorithm).get(nmc).get(pLab).get(minClSize).add(timeToRegister+timeRegister.get(minClSize));
		report.get(parameters.timeToPropagate).get(algorithm).get(nmc).get(pLab).get(minClSize).add(timeToRegister);

	}
}