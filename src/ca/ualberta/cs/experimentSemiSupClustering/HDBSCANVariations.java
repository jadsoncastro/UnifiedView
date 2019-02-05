package ca.ualberta.cs.experimentSemiSupClustering;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
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

public class HDBSCANVariations 
{
	private static final DescriptionAlgorithms parameters = new DescriptionAlgorithms();


	//------------------------------- Unsupervised / constarint methods -----------------------------------
	public static void HdbscanUnsupervisedAndConstraints(ArrayList<Cluster> clusters,
			HMatrix matrix, HashMap<Integer, Integer> labeledObjects,
			Dataset dataset, Integer minPts,
			Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report,
			String algorithm, Integer pmc,
			Integer pLab) throws IOException
	{

		ArrayList<Constraint> constraints = null;
		HDBSCANApts.releaseAllClusters(clusters);
		
//		System.out.println("Algorithm: " + algorithm + "cluster hierarchy: " + clusters);
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


//		System.out.println("Cluster hierarchy with values computed for algorithm " + algorithm + "\n" + clusters);
		HDBSCANApts.propagateTree(clusters, "unsupervised");
//		System.out.println("Solution extracted from the algorithm " + algorithm + "\n" + clusters.get(1).getPropagatedDescendants());
		
		int[] tmp = HDBSCANApts.findProminentClustersSHM(clusters, matrix);
		HashMap<Integer, Integer> flatPartitioningSHM = new HashMap<Integer, Integer>();

		if(tmp.length > 0)
			for(int i=0; i< tmp.length; i++)
				if(tmp[i] > 0)
					flatPartitioningSHM.put(i, tmp[i]);

		Instance[] objects = dataset.getObjects();
		for(Map.Entry<Integer, Integer> entry: flatPartitioningSHM.entrySet())
			objects[entry.getKey()].setLabel(entry.getValue());

		WeightedARI weightedAri= new WeightedARI();
		double wAri = weightedAri.getIndex(objects);
		report.get(parameters.weightedARI).get(algorithm).get(pmc).get(pLab).get(minPts).add(wAri);

		AdjustedRandStatistic adjRandStatistic = new AdjustedRandStatistic();
		double ari = adjRandStatistic.getIndex(objects);
		report.get(parameters.ARI).get(algorithm).get(pmc).get(pLab).get(minPts).add(ari);
//		System.err.println(String.format(Locale.CANADA, "ARI for algorithm %s: %.2f", algorithm, ari));
	}




	//------------------------------- BCubed based  methods --------------------------------------------------------------------------

	public static void BCubedBased(ArrayList<Cluster> clusters,
			UndirectedGraph mst,
			HMatrix matrix,
			HashMap<Integer, Integer> labeledObjects,
			Dataset dataset,
			Integer minClSize,
			Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report,
			String algorithm,
			Integer nmc,
			Integer pLab) throws IOException
	{

		HDBSCANApts.releaseAllClusters(clusters);
		
		HDBSCANApts.calculateBCubedIndex(matrix, clusters, labeledObjects);
//		System.out.println("Cluster hierarchy with values computed for algorithm " + algorithm + "\n" + clusters);
		
		HDBSCANApts.propagateTree(clusters, "bcubed");
//		System.out.println("Solution extracted from the algorithm " + algorithm + "\n" + clusters.get(1).getPropagatedDescendants());

		int[] tmp = HDBSCANApts.findProminentClustersSHM(clusters, matrix);
		Instance[] objects = dataset.getObjects();

		if(tmp.length > 0)
			for(int i=0; i< tmp.length; i++)
				if(tmp[i] > 0)
					objects[i].setLabel(tmp[i]);

		WeightedARI weightedAri= new WeightedARI();
		double wAri = weightedAri.getIndex(objects);
		report.get(parameters.weightedARI).get(algorithm).get(nmc).get(pLab).get(minClSize).add(wAri);

		AdjustedRandStatistic adjRandStatistic = new AdjustedRandStatistic();
		double ari = adjRandStatistic.getIndex(objects);
		report.get(parameters.ARI).get(algorithm).get(nmc).get(pLab).get(minClSize).add(ari);
//		System.err.println(String.format(Locale.CANADA, "ARI for algorithm %s: %.2f", algorithm, ari));
	}



	//------------------------------- Mixed approach --------------------------------------------------------------------------

	public static void MixedBCubed(ArrayList<Cluster> clusters,
			UndirectedGraph mst,
			HMatrix matrix,
			HashMap<Integer, Integer> labeledObjects,
			Dataset dataset,
			Integer minClSize,
			Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report,
			String algorithm,
			Integer nmc,
			Integer pLab) throws IOException
	{

		double alpha=0.5;
		HDBSCANApts.releaseAllClusters(clusters);
//		System.out.println("Algorithm: " + algorithm + "cluster hierarchy: " + clusters);

		
		HDBSCANApts.propagateTree(clusters, "unsupervised");
		double maxStability = clusters.get(1).getPropagatedStability();
		HDBSCANApts.releaseAllClusters(clusters);

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

		Instance[] objects = dataset.getObjects();
		if(tmp.length > 0)
			for(int i=0; i< tmp.length; i++)
				if(tmp[i] > 0)
					objects[i].setLabel(tmp[i]);

		WeightedARI weightedAri= new WeightedARI();
		double wAri = weightedAri.getIndex(objects);
		report.get(parameters.weightedARI).get(algorithm).get(nmc).get(pLab).get(minClSize).add(wAri);

		AdjustedRandStatistic adjRandStatistic = new AdjustedRandStatistic();
		double ari = adjRandStatistic.getIndex(objects);
		report.get(parameters.ARI).get(algorithm).get(nmc).get(pLab).get(minClSize).add(ari);
//		System.out.println(String.format(Locale.CANADA, "ARI for alorithm %s: %.3f", algorithm, ari));
	}


	public static void MixedForConstraints(ArrayList<Cluster> clusters,
			HMatrix matrix,
			HashMap<Integer, Integer> labeledObjects,
			Dataset dataset,
			Integer minClSize,
			Map<String, Map<String, Map<Integer, Map<Integer, Map<Integer, ArrayList<Double>>>>>> report,
			String algorithm,
			Integer nmc,
			Integer pLab) throws IOException
	{

		double alpha=0.5;
		HDBSCANApts.releaseAllClusters(clusters);
//		System.out.println("Algorithm: " + algorithm + "cluster hierarchy: " + clusters);
		
		
		HDBSCANApts.propagateTree(clusters, "unsupervised");
		double maxStability = clusters.get(1).getPropagatedStability();
		HDBSCANApts.releaseAllClusters(clusters);

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

		Instance[] objects = dataset.getObjects();

		if(tmp.length > 0)
			for(int i=0; i< tmp.length; i++)
				if(tmp[i] > 0)
					objects[i].setLabel(tmp[i]);


		WeightedARI weightedAri= new WeightedARI();
		double wAri = weightedAri.getIndex(objects);
		report.get(parameters.weightedARI).get(algorithm).get(nmc).get(pLab).get(minClSize).add(wAri);

		AdjustedRandStatistic adjRandStatistic = new AdjustedRandStatistic();
		double ari = adjRandStatistic.getIndex(objects);
		report.get(parameters.ARI).get(algorithm).get(nmc).get(pLab).get(minClSize).add(ari);
//		System.out.println(String.format(Locale.CANADA, "ARI for alorithm %s: %.3f", algorithm, ari));
	}
}