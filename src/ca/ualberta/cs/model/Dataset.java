package ca.ualberta.cs.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import javafx.util.Pair;
import ssExtraction.SemiWeight;
import ca.ualberta.cs.distance.DistanceCalculator;

/**
 * @author unknown
 *
 * Stores a collection of objects - Instance - that can be
 * either labeled or unlabeled.
 *
 */
public class Dataset
{

	private ArrayList<Instance> objects = new ArrayList<Instance>();
	private Map<Integer, Integer> classDistribution = new HashMap<Integer, Integer>();

	public void addObject(Instance instance)
	{
		this.objects.add(instance);
		
		Integer key = instance.getTrueLabel();
		
		if(this.classDistribution.containsKey(key))
			this.classDistribution.put(key, (this.classDistribution.get(key)+1));
		else
			this.classDistribution.put(key, 1);		
	}

	public Instance[] getObjects()
	{
		return objects.toArray(new Instance[0]);
	}

	public int getNumOfClass()
	{
		return this.classDistribution.keySet().size();
	}

	public TreeSet<Integer> getClassIds()
	{
		return new TreeSet<Integer>(this.classDistribution.keySet());
	}
	
	public Map<Integer, Integer> getClassDistribution()
	{
		return this.classDistribution;
	}
	

	public ArrayList<Neighbor> getNeighbors(Instance instance, DistanceCalculator distance) 
	{		
		ArrayList<Neighbor> neighbors = new ArrayList<Neighbor>();

		for (Instance i : objects) 
		{
			double distanceValue = distance.computeDistance(i.getCoordinates(), instance.getCoordinates());
			Neighbor neighbor = new Neighbor(i, distanceValue);
			neighbors.add(neighbor);
		}

		Collections.sort(neighbors);

		return neighbors;
	}
	

	public ArrayList<Neighbor> getNeighborsHissclu(Instance instance, DistanceCalculator distance, SemiWeight semi, boolean isWeighted) 
	{
		ArrayList<Neighbor> neighbors = new ArrayList<Neighbor>();

		for (Instance i : this.objects) 
		{

			if(i.getIndex()==instance.getIndex())
				continue;

			double weight = 1.0;

			if(isWeighted)
				weight = semi.computeWeight(instance.getCoordinates(), i.getCoordinates(), instance.getIndex(), i.getIndex());

			double distanceValue = distance.computeDistance(i.getCoordinates(), instance.getCoordinates())*weight;

			Neighbor neighbor = new Neighbor(i, distanceValue);

			neighbors.add(neighbor);
		}

		Collections.sort(neighbors);		
		return neighbors;
	}


	// TODO: To use with the distance matrix
	public ArrayList<Neighbor> getNeighborsHisscluDistanceMatrix(Instance instance, double[][] distanceMatrix) 
	{
		ArrayList<Neighbor> neighbors = new ArrayList<Neighbor>();

		for (Instance i : this.objects) 
		{

			if(i.getIndex()==instance.getIndex())
				continue;

			double distanceValue = distanceMatrix[instance.getIndex()][i.getIndex()];

			Neighbor neighbor = new Neighbor(i, distanceValue);
			neighbors.add(neighbor);
		}

		Collections.sort(neighbors);
		return neighbors;
	}

}