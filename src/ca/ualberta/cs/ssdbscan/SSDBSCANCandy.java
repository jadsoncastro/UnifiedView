package ca.ualberta.cs.ssdbscan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;
import ca.ualberta.cs.model.Neighbor;


public class SSDBSCANCandy {

	private PriorityQueue<Instance> Q = new PriorityQueue<Instance>();
	private ArrayList<Instance> orderedObjects = new ArrayList<Instance>();
	private int indexHeighestEdgeValue = 0;
	private int minPts;
	private Dataset dataset;
	DistanceCalculator distance;

	public class LabeledOrderedObject 
	{
		Instance object = null;
		boolean processed = false;
	}

	public SSDBSCANCandy(DistanceCalculator d)
	{
		this.distance = d;
	}

	//CANDY: theLabelIndices represent order of the labeled objects to be processed.
	public Map<Integer, Integer> getForest(Dataset dataset, Map<Integer, Integer> theLabelIndices, int minPts)
	{
		//CANDY: bad parameter name.
		this.dataset = dataset;
		this.minPts  = minPts;

		Instance [] objects = dataset.getObjects();
		double maxWeightValue = 0.0;
		Map<Integer, Integer> response = new HashMap<Integer, Integer>();

		ArrayList<LabeledOrderedObject> labeledDataset = new ArrayList<LabeledOrderedObject>();		

		for (Map.Entry<Integer, Integer> entry: theLabelIndices.entrySet()) 
		{
			LabeledOrderedObject anLOObject = new LabeledOrderedObject();
			anLOObject.object = objects[entry.getKey()];
			labeledDataset.add(anLOObject);
		}

		while(!labeledDataset.isEmpty())
		{
			Instance labeledInstance = labeledDataset.get(0).object;
			labeledDataset.remove(0);			
			//			System.out.println("Process pre-label object index: " + labeledInstance.getIndex());

			ArrayList<Neighbor> neighbors = dataset.getNeighbors(labeledInstance, distance);
			labeledInstance.setCoreDistance2(neighbors, minPts);	
			labeledInstance.setProcessed(true);

			//TODO: If Instance.isProcessed(), then it will not be added to the Q.
			//      Is the resetDataset() function conflict with this?
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
					for (int j = 0; j < labeledDataset.size(); j++ )
					{
						if ( labeledDataset.get(j).object.equals(currentInstance) )
						{
							labeledDataset.get(j).processed = true;
							break;
						}
					}
				}

				if(currentInstance.isPreLabeled() && currentInstance.getLabel() != labeledInstance.getLabel())
				{	

					for (int j = 0; j < indexHeighestEdgeValue; j++)
						response.put(orderedObjects.get(j).getIndex(), orderedObjects.get(j).getLabel());

					for(int j = indexHeighestEdgeValue; j < orderedObjects.size(); j++)
					{
						// The following fields are reset in resetDataset() for all objects.
						// Which caused the processed objects to be re-processed.
						//     instance.setWeight(-1.0);
						//     instance.setProcessed(false);
						//     if not pre-labeled then instance.setLabel(-1);
						// Should reset the objects in the "orderedObject" only.
						orderedObjects.get(j).setWeight(-1.0);
						orderedObjects.get(j).setProcessed(false);

						if(!orderedObjects.get(j).isPreLabeled())
						{
							orderedObjects.get(j).setLabel(-1);							
						} 
						else 
						{ 
							for ( int k = 0; k < labeledDataset.size(); k++ )
							{
								if ( labeledDataset.get(k).object.equals(orderedObjects.get(j)) ) 
								{
									labeledDataset.get(k).processed = false;
									break;
								}
							}
						}
					}

					// Finally remove labeled objects from the list.
					for ( int k = labeledDataset.size() - 1; k >= 0; k-- )
					{
						if ( labeledDataset.get(k).processed )
						{
							labeledDataset.remove(k);
						}
					}

					orderedObjects.clear();
					Q.clear();
					indexHeighestEdgeValue = 0;
					maxWeightValue = 0.0;
					//resetDataset(dataset);
					break;
				}

				ArrayList<Neighbor> currentNeighbors = dataset.getNeighbors(currentInstance, distance);

				//CANDY: setCoreDistance() is call for all instances before added to "Q". 
				//       Should re-consider where core-distance should be set.
				currentInstance.setCoreDistance2(currentNeighbors, minPts);
				updateQ(currentNeighbors, currentInstance);

			} //CANDY: while(!Q.isEmpty())

			// The queue is emptied out without seeing another labeled objects.
			// Should clear the "orderOBjects, and reset other variables.
			if (orderedObjects.size() > 0) 
			{
				for (int i = 0; i < orderedObjects.size(); i++)
					response.put(orderedObjects.get(i).getIndex(), orderedObjects.get(i).getLabel());

				orderedObjects.clear();
				indexHeighestEdgeValue = 0;
				maxWeightValue = 0.0;
			}

		} //CANDY: while(!labeledDataset.isEmpty())

		return response;
	}

	private void updateQ(ArrayList<Neighbor> neighbors, Instance instance) 
	{		
		double core = instance.getCoreDistance();

		for (Neighbor neighbor : neighbors)
		{	
			//TODO: The neighbor's core-distance is not used in this method, why calculate here?
			ArrayList<Neighbor> currentNeighbors = dataset.getNeighbors(neighbor.getInstance(), distance);
			neighbor.getInstance().setCoreDistance2(currentNeighbors, minPts);

			//TODO: "core" equals to "instance.getCoreDistance", why is the second Math.max() necessary?
			double newDist = Math.max(Math.max(core, neighbor.getDistance()), instance.getCoreDistance());

			//CANDY: bad if-then-else conditions below.
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

	//CANDY: Inconsistent parameter type. Got Dataset here, but Instance[] in the next method.
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
		//CANDY: linear search, bad performance. Should at least use Hashtable or break.  
		for (int i = 0; i < objects.length; i++) 
		{			
			if(objects[i].equals(instance))
			{				
				//TODO: should added checks about "objects[i].getLabel() >= 0". If so, algorithm breakdown? Bad labels?
				//TODO: what about border points that can be in two clusters? Random assignment?
				objects[i].setLabel(instance.getLabel());
				objects[i].setPreLabeled(instance.isPreLabeled());
			}
		}

	}

}
