package ca.ualberta.cs.hissclu;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import ssExtraction.SemiWeight;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.Neighbor;

public class HISSCLU implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static ArrayList<Instance> seedList = new ArrayList<Instance>();

	/** Implementation of hissclu algorithm 
	 *  described by the authors in their paper.
	 *  
	 * @param  A Dataset dataset
	 * @param  An ArrayList<Instance> labeledObjects
	 * @param  A integer minPts
	 * @return An ArrayList<Instance> of ordered objects.
	 * @throws CloneNotSupportedException 
	 */

	public static void expansion(Dataset dataset, int minPts,
			double kValue, int minClSize,
			DistanceCalculator distance,
			HashMap<Integer, Integer> labeledIds,
			SemiWeight semi, boolean plotLog, String logFolder) throws IOException
	{

		Instance[] db = dataset.getObjects();
		String toPlot="";

		long overallStartTime = System.currentTimeMillis();

		/** First phase of the HISSCLU: Simultaneous expansion of all the labeled objects*/
		for(Map.Entry<Integer, Integer> entries : labeledIds.entrySet())
		{
			Instance inst = db[entries.getKey()]; 
			inst.setWeight(Double.MAX_VALUE);
			inst.setProcessed(true);
			//			System.out.println("Expanding object " + inst.getIndex() + " label: " + inst.getLabel() + " True label: " + inst.getTrueLabel());

			ArrayList<Neighbor> neighbors = dataset.getNeighborsHissclu(inst, distance, semi, true);

			inst.setCoreDistance2(neighbors, minPts);
			updateSeedList(inst, neighbors);
		}

		while(!seedList.isEmpty())
		{
			Instance currentObject = getMin(seedList);

			if(plotLog)
				toPlot += (currentObject.getIndex()+1) + "," + (currentObject.preceeding+1) + "," + currentObject.getWeight() + "\n";

			currentObject.setProcessed(true);
			seedList.remove(currentObject);
			ArrayList<Neighbor> neighbors= dataset.getNeighborsHissclu(currentObject, distance, semi, true);

			currentObject.setCoreDistance2(neighbors, minPts);
			updateSeedList(currentObject, neighbors);			
		}

		if(plotLog)
		{
			BufferedWriter writer = new BufferedWriter(new FileWriter(logFolder));
			writer.write(toPlot.substring(0, toPlot.length()-2));
			writer.close();
		}


		//		System.err.println("Time to run the first phrase of HISSCLU: (min): " + (((System.currentTimeMillis() - overallStartTime)/1000)/60));

		/** Second phase of the HISSCLU: Perform a single OPTICS expansion in order to create the reachability plot*/
		ArrayList<Instance> orderedObjects = new ArrayList<Instance>();
		seedList = new ArrayList<Instance>();

		for(Instance obj: db)
		{
			if(!obj.isPostProcessed())
			{

				ArrayList<Neighbor> neighbors= dataset.getNeighborsHissclu(obj, distance, semi, true);
				obj.setPostProcessed(true);
				obj.setWeight(Double.MAX_VALUE);
				obj.setCoreDistance2(neighbors, minPts);

				orderedObjects.add(obj);

				unsupervisedUpdateSeedList(obj, neighbors);

				while(!seedList.isEmpty())
				{
					Instance currentObject = getMin(seedList);

					currentObject.setPostProcessed(true);
					seedList.remove(currentObject);
					orderedObjects.add(currentObject);
					neighbors= dataset.getNeighborsHissclu(currentObject, distance, semi, true);

					currentObject.setCoreDistance2(neighbors, minPts);
					unsupervisedUpdateSeedList(currentObject, neighbors);
				}

			}
		}
		//		System.err.println("Time to run the second phrase of HISSCLU: (min): " + (((System.currentTimeMillis() - overallStartTime)/1000)/60));

		//		System.out.println("Ordered objects: ");
		//		for(Instance inst: orderedObjects)
		//			System.out.print(inst.getWeight() + ", ");

		extractCluster(orderedObjects, kValue, minClSize);

		//		System.out.println("\nCluster assignment: ");
		//		for(Instance inst: orderedObjects)
		//			System.out.print(inst.getClusterAssignment() + ", ");

		//		System.err.println("Time to run the third phrase of HISSCLU: (min): " + (((System.currentTimeMillis() - overallStartTime)/1000)/60));

	}

	private static void updateSeedList(Instance inst, ArrayList<Neighbor> neighbors) 
	{	
		double coreDistance= inst.getCoreDistance();

		for(Neighbor n: neighbors)
		{
			double rDist = 0.0;

			rDist= Math.max(coreDistance, n.getDistance());

			if(!seedList.contains(n.getInstance()) && 
					!n.getInstance().isProcessed() &&
					!n.getInstance().isPreLabeled())
			{
				n.getInstance().setWeight(rDist);
				n.getInstance().setLabel(inst.getLabel());
				seedList.add(n.getInstance());

				n.getInstance().preceeding = inst.getIndex();

			}else
			{
				if(rDist < n.getInstance().getWeight() && !n.getInstance().isPreLabeled())
				{
					//					System.out.println("Setting label of object: " + n.getInstance().getIndex() + " Instance seed: " + inst.getIndex());

					n.getInstance().setWeight(rDist);
					n.getInstance().setLabel(inst.getLabel());
					n.getInstance().preceeding = inst.getIndex();
				}
			}
		}
	}

	private static void unsupervisedUpdateSeedList(Instance inst, ArrayList<Neighbor> neighbors) 
	{	
		double coreDistance= inst.getCoreDistance();

		for(Neighbor n: neighbors)
		{
			double rDist = 0.0;
			rDist= Math.max(coreDistance, n.getDistance());

			if(!seedList.contains(n.getInstance()) && !n.getInstance().isPostProcessed())
			{
				n.getInstance().setWeight(rDist);
				seedList.add(n.getInstance());

			}else
			{
				if(rDist < n.getInstance().getWeight() && !n.getInstance().isPostProcessed())
				{
					n.getInstance().setWeight(rDist);
				}
			}
		}
	}

	private static Instance getMin(ArrayList<Instance> seed)
	{
		double minValue= Double.MAX_VALUE;
		Instance response= new Instance();

		for(Instance i: seed)
		{
			if(i.getWeight() <  minValue)
			{
				minValue=i.getWeight();
				response= i;
			}
		}
		return response;
	}

	private static void extractCluster(ArrayList<Instance> orderedObjects, double kValue, int minClSize)
	{
		/** To specify the eps value to perform a horizontal cut in HISSCLU, Bohm propose
		 * to multiply a factor (kValue) by the maximum reachability distance present in 
		 * the ordered objects. They also describe in their paper that a cluster should have
		 * at least minClSize objects.
		 */

		double maxReachability = Double.MIN_VALUE;

		for(Instance inst: orderedObjects)
			if(inst.getWeight() > maxReachability && inst.getWeight() < Double.MAX_VALUE)
				maxReachability = inst.getWeight();

		double eps    = kValue*maxReachability;
		int counter   = -1;
		int clusterID = 1;
		int i = 0;
		boolean belowEps = true;

		while(i < orderedObjects.size())
		{
			Instance inst = orderedObjects.get(i);

			if(belowEps && inst.getWeight() <= eps)
				inst.setClusterAssignment(clusterID);

			if(inst.getWeight() != Double.MAX_VALUE && inst.getWeight() > eps)
			{
				counter = 1;
				inst.setClusterAssignment(clusterID);

				int j = i+1;
				while(j < orderedObjects.size() && orderedObjects.get(j).getWeight() <= eps)
				{
					counter ++;
					j++;
				}

				if (counter > minClSize)
				{
					if(belowEps)
					{
						belowEps= false;
						clusterID++;
					}

					for(int l = i; l < j; l++)
						orderedObjects.get(l).setClusterAssignment(clusterID);

					i = j-1;

					if(i < (orderedObjects.size() - minClSize))
						clusterID++;

				}
				else
				{
					for(int l = i; l < j; l++)
						orderedObjects.get(l).setClusterAssignment(-1);

					i = j-1;
				}
			}

			i++;

		}

	}
}