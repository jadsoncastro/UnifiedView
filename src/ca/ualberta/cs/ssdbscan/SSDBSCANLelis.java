package ca.ualberta.cs.ssdbscan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;

import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.Neighbor;

public class SSDBSCANLelis 
{

	private PriorityQueue<Instance> Q = new PriorityQueue<Instance>();
	private ArrayList<Instance> orderedObjects = new ArrayList<Instance>();
	private DistanceCalculator distance;
	private int indexHeighestEdgeValue = 0;
	private int minPts;
	private Dataset dataset;
	
	public SSDBSCANLelis(DistanceCalculator d)
	{
		this.distance = d;
	}
	
	public Map<Integer, Integer> getForest(Dataset dataset, int minPts)
	{
		this.dataset = dataset;
		this.minPts  = minPts;
		
		Map<Integer, Integer> response = new HashMap<Integer, Integer>();
		
		Instance [] objects = dataset.getObjects();
		double maxWeightValue = 0.0;
		
		HashSet<Instance> labeledDataset = new HashSet<Instance>();
		
		for (int i = 0; i < objects.length; i++) 
		{
			if(objects[i].getLabel() >= 0 && objects[i].isPreLabeled())
			{
				labeledDataset.add(objects[i]);
			}
		}
		
		while(!labeledDataset.isEmpty())
		{
			Instance labeledInstance = labeledDataset.toArray(new Instance[0])[0];
			
			labeledDataset.remove(labeledInstance);
			
			ArrayList<Neighbor> neighbors = dataset.getNeighbors(labeledInstance, distance);
			if(labeledInstance.wasNotCoreDistanceSet())
			{
				labeledInstance.setCoreDistance2(neighbors, minPts);
			}
			
			labeledInstance.setProcessed(true);
			updateQ(neighbors, labeledInstance);
			
			orderedObjects.add(labeledInstance);
		
			while(!Q.isEmpty())
			{
				Instance currentInstance = Q.poll();
				currentInstance.setProcessed(true);
				
				orderedObjects.add(currentInstance);
				if(currentInstance.getWeight() > maxWeightValue)
				{
					maxWeightValue = currentInstance.getWeight();
					indexHeighestEdgeValue = orderedObjects.size() - 1;
				}
				
				if(currentInstance.isPreLabeled() && currentInstance.getLabel() == labeledInstance.getLabel())
				{
					labeledDataset.remove(currentInstance);
				}
				
				if(currentInstance.isPreLabeled() && currentInstance.getLabel() != labeledInstance.getLabel())
				{	
					for (int j = 0; j < indexHeighestEdgeValue; j++)
					{
						response.put(orderedObjects.get(j).getIndex(), orderedObjects.get(j).getLabel());
					}
					
					for(int j = indexHeighestEdgeValue; j < orderedObjects.size(); j++)
					{
						if(!orderedObjects.get(j).isPreLabeled())
						{
							orderedObjects.get(j).setLabel(-1);
						}
					}
					
					orderedObjects.clear();
					Q.clear();
					indexHeighestEdgeValue = 0;
					maxWeightValue = 0.0;
					
					resetDataset(dataset);
					
					break;
				}
				
				ArrayList<Neighbor> currentNeighbors = dataset.getNeighbors(currentInstance, distance);
				if(currentInstance.wasNotCoreDistanceSet())
				{
					currentInstance.setCoreDistance2(currentNeighbors, minPts);
				}
				updateQ(currentNeighbors, currentInstance);
			}
		
		}
		
		return response;
	}

	private void updateQ(ArrayList<Neighbor> neighbors, Instance instance) 
	{		
		double core = instance.getCoreDistance();
		
		for (Neighbor neighbor : neighbors)
		{	
			if(neighbor.getInstance().wasNotCoreDistanceSet())
			{
				ArrayList<Neighbor> currentNeighbors = dataset.getNeighbors(neighbor.getInstance(), distance);
				neighbor.getInstance().setCoreDistance2(currentNeighbors, minPts);
			}
			
			double newDist = Math.max(Math.max(core, neighbor.getDistance()), neighbor.getInstance().getCoreDistance());
			
			if(!neighbor.getInstance().isProcessed() && neighbor.getInstance().getWeight() < 0)
			{
				neighbor.getInstance().setWeight(newDist);
				Q.add(neighbor.getInstance());
				
				if(!neighbor.getInstance().isPreLabeled())
				{
					neighbor.getInstance().setLabel(instance.getLabel());
				}
			}
			else
			{
				if(!neighbor.getInstance().isProcessed() && neighbor.getInstance().getWeight() > newDist)
				{
					Q.remove(neighbor.getInstance());
					neighbor.getInstance().setWeight(newDist);
					Q.add(neighbor.getInstance());
					
					if(!neighbor.getInstance().isPreLabeled())
					{
						neighbor.getInstance().setLabel(instance.getLabel());
					}
				}
			}
		}
	}
	
	private void resetDataset(Dataset dataset) 
	{
		Instance [] objects = dataset.getObjects();
		
		for (Instance instance : objects) 
		{
			instance.setWeight(-1.0);
			instance.setProcessed(false);
			
			if(!instance.isPreLabeled())
			{
				instance.setLabel(-1);
			}
		}
	}

	private void copyLabel(Instance instance, Instance[] objects) 
	{
		for (int i = 0; i < objects.length; i++) 
		{			
			if(objects[i].equals(instance))
			{				
				objects[i].setLabel(instance.getLabel());
			}
		}
		
	}

}
