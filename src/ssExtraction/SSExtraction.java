package ssExtraction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Stack;
import java.util.TreeSet;

import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;

public class SSExtraction implements Serializable
{

	private static final long serialVersionUID = 1L;
	private static final Integer  idNoise	   = -1;



	//-------------------------------------------------------- PUBLIC METHODS ----------------------------------------------------

	public static Map<Integer, Integer> expandLabels(UndirectedGraph G, Map<Integer, Integer> labeledObjects, boolean detectNoise)
	{
		if(detectNoise)
			return expandSSDBSCAN(G, labeledObjects);
		else
			return expandToAllObjects(G, labeledObjects);
	}

	public static Map<Integer, Integer> expandWeighted(double[][] dataSet,
			UndirectedGraph G, Map<Integer, Integer> labeledObjects,
			SemiWeight semi,
			DistanceCalculator distance,
			boolean detectNoise)
	{
		long time = System.currentTimeMillis();
		UndirectedGraph mst= updateEdges(G, dataSet, semi);
		semi.setTime(((System.currentTimeMillis() - time)/1000.00));
		
		if(detectNoise)
			return expandSSDBSCAN(G, labeledObjects);
		else
			return expandToAllObjects(mst, labeledObjects);
	}


	/** 
	 * Given a MST with real-valued edge costs, search to the largest edge that splits a labeled
	 * object O_i from a object with different Label O_j
	 * @param graph. The MST graph.
	 * @return label of each object
	 */

	public static Map<Integer, Integer> expandSSDBSCAN(UndirectedGraph G, Map<Integer, Integer> labeledObjects)
	{
		Map<Integer, Integer> response = new HashMap<Integer, Integer>();
		ArrayList<Integer> labeledIds  = new ArrayList<Integer>(labeledObjects.keySet());

		while(!labeledIds.isEmpty())
		{

			double[] distances    = new double[G.getNumVertices()]; // shortest known distance to MST
			int[]    preceeding   = new int[G.getNumVertices()];
			BitSet   visited      = new BitSet(G.getNumVertices());
			Stack<Integer> stack  = new Stack<Integer>();
			int idHighestEdge	  = -1,  vertex          = -1;
			double nearestDist    = 0.0, maxReachability = 0.0;
			boolean flag		  = false;

			for(int i=0; i<distances.length; i++)
			{
				distances[i]=Double.MAX_VALUE;
			}

			Integer startPoint= labeledIds.get(0);
			response.put(startPoint, labeledObjects.get(startPoint));
			distances[startPoint] = 0;
			
			for (int i=0; i < distances.length; i++)
			{
				Integer next = minVertex(distances, visited);

				if(next.equals(idNoise))
					break;

				visited.set(next);
				stack.push(next);

				if(!next.equals(startPoint) && G.getDistance(next, preceeding[next]) > maxReachability)
				{
					idHighestEdge  = next;
					maxReachability= G.getDistance(next, preceeding[next]);
				}				

				if( labeledObjects.containsKey(next) && labeledObjects.get(next) != labeledObjects.get(startPoint))
				{
					flag=true;
					break;
				}

				// The edge from pred[next] to next is in the MST (if next!=s)
				Map<Integer, Double> neighbors = G.getNeighbors(next);

				for(Map.Entry<Integer, Double> entries: neighbors.entrySet())
				{
					vertex= entries.getKey();
					nearestDist= entries.getValue();

					if(distances[vertex] > nearestDist)
					{
						distances [vertex] = nearestDist;
						preceeding[vertex] = next;
					}
				}
			}

			setFinalLabels(stack, response, startPoint, labeledObjects, labeledIds, idHighestEdge, flag);
		}
		
		return response;
	}


	//------------------------------ PRIVATEe METHODS --------------------------------

	/**
	 * Given a MST with real-valued edge costs,
	 * propagate the labels through the MST, respecting the 
	 * minimum path reachability distance, and also the order for which
	 * the objects are reached in the minimum spanning tree
	 * @param G. The MST graph.
	 * @param labeledObjects. The set of Labeled objects
	 * to define the noise objects after the label propagation
	 * @return label of each object
	 */
	private static Map<Integer, Integer> expandToAllObjects(UndirectedGraph G, Map<Integer, Integer> labeledObjects)
	{

		int numVertices = G.getNumVertices();

		Map<Integer, double[]> storedPaths = new HashMap<Integer, double[]>();
		Map<Integer, Integer> response = new HashMap<Integer, Integer>();


		// Initially, store in the paths: for the labeled objects, include the double zero;
		// for the response, include the labeled object and its respective label;
		for(Map.Entry<Integer, Integer> entry: labeledObjects.entrySet())
			response.put(entry.getKey(), entry.getValue());

		for(int i=0; i< numVertices; i++)
		{
			storedPaths.put(i, new double[5]);
			Arrays.fill(storedPaths.get(i), 0.0);
		}

		//		System.out.println("Stored paths at the begining: " + storedPaths);

		PriorityQueue<Edge> edgeList = new PriorityQueue<Edge>(); // List to store the outgoing edge from each component

		for(Map.Entry<Integer, Integer> entry: labeledObjects.entrySet())
		{
			int idLabeled = entry.getKey();
			for(Map.Entry<Integer, Double> neighbor: G.getNeighbors(idLabeled).entrySet())
			{
				if(idLabeled != neighbor.getKey())
					edgeList.add(new Edge(idLabeled, neighbor.getKey(), neighbor.getValue()));
			}
		}


		while(!edgeList.isEmpty())
		{
			ArrayList<Edge> edgesWithSameWeight = new ArrayList<Edge>();
			Edge currentEdge = edgeList.poll();
			edgesWithSameWeight.add(currentEdge);

			// Dequeue the edges of the same weight at the same time
			while(!edgeList.isEmpty() && (currentEdge.getWeight() == edgeList.peek().getWeight()))
			{
				Edge nextEdge = edgeList.poll();
				edgesWithSameWeight.add(nextEdge);
			}

			ArrayList<Edge> affectedEdges = new ArrayList<Edge>();

			while(!edgesWithSameWeight.isEmpty())
			{

				/* Separate the edges by classes:
				 * 1) Edges where both components are labeled // separated already during the first verification
				 * 2) Edges where at least one side is labeled
				 * 3) Edges connecting unlabeled components
				 */
				Edge tmp = edgesWithSameWeight.remove(0);

				int v1 = tmp.getVertexOne();
				int v2 = tmp.getVertexTwo();

				if(response.containsKey(v1) && response.containsKey(v2))
				{
					if(response.get(v1) == response.get(v2)) // If the components are from the same label, join the sets. Otherwise, ignore
					{

					}
				}
				else if (response.containsKey(v1) || response.containsKey(v2))
				{
					affectedEdges.add(tmp);
				}
			}

			//			System.out.println("\nAffected edges" + affectedEdges);
			while(!affectedEdges.isEmpty())
			{
				Edge tmp = affectedEdges.remove(0);

				if(response.containsKey(tmp.getVertexOne()) && response.containsKey(tmp.getVertexTwo()))
					continue;

				int unlabeledVertex = -1;
				ArrayList<Integer> labeledNeighbors = new ArrayList<Integer>();

				if(response.containsKey(tmp.getVertexOne()))
				{
					unlabeledVertex = tmp.getVertexTwo();
					labeledNeighbors.add(tmp.getVertexOne());
				}
				else
				{
					unlabeledVertex = tmp.getVertexOne();
					labeledNeighbors.add(tmp.getVertexTwo());
				}

				// Check if the edge is affecting the connected component which the unlabeled object is contained.
				for(Edge e: affectedEdges)
				{
					if(unlabeledVertex == e.getVertexOne())
						labeledNeighbors.add(e.getVertexTwo());

					else if(unlabeledVertex == e.getVertexTwo())
						labeledNeighbors.add(e.getVertexOne());
				}
				//				System.out.println("Going to update paths " + affectedEdges + "\nUnlabeled: " + unlabeledVertex);
				updatePaths(G, labeledNeighbors, unlabeledVertex, storedPaths, response);

				for(Map.Entry<Integer, Double> neighbor: G.getNeighbors(unlabeledVertex).entrySet())
				{
					if(neighbor.getKey() != unlabeledVertex && !response.containsKey(neighbor.getKey()))
						edgeList.add(new Edge(unlabeledVertex, neighbor.getKey(), neighbor.getValue()));
				}
			}
		}
		//		System.out.println("Stored paths at the end: " + storedPaths);

		return response;
	}


	private static void updatePaths(UndirectedGraph G, ArrayList<Integer> labeledNeighbors,
			int unlabeledVertex, Map<Integer, double[]> storedPaths, Map<Integer, Integer> response) 
	{

		TreeSet<Integer> connectingClasses = new TreeSet<Integer>();

		int closestNeighbor = labeledNeighbors.get(0);
		double[] closestNeighborPath = Arrays.copyOf(storedPaths.get(closestNeighbor), storedPaths.get(closestNeighbor).length);
		connectingClasses.add(response.get(closestNeighbor));


		if(labeledNeighbors.size() > 1) // If there is more than one labeled object connecting to the unlabeled component.
		{
			// Compare paths between the neighbors			
			for(Integer l: labeledNeighbors)
			{
				connectingClasses.add(response.get(l));
				double[] tmp = Arrays.copyOf(storedPaths.get(l), storedPaths.get(l).length);

				// Comparing the values on the path: Check if the first largest edge is smaller, than the second and so on.
				for(int i=0; i< closestNeighborPath.length; i++)
				{
					if(tmp[i] < closestNeighborPath[i])
					{
						closestNeighborPath=tmp;
						closestNeighbor = l;
						break;
					}else if(tmp[i] > closestNeighborPath[i])
						break;

				}
			}
			//			System.out.println("Closest neighbor: " + closestNeighbor + " path: " + closestNeighborPath);
		}

		double[] tmp = Arrays.copyOf(closestNeighborPath, closestNeighborPath.length);

		// Store the new distance in the "compressed" path. Check if this new weight will be higer than any one of 
		// the weights stored on the path. case true, we replace the value, and check the removed value with the following
		// values stored on the path.
		double newDistance = G.getDistance(unlabeledVertex, closestNeighbor);
		//		System.out.println("New distance to be included: " + String.format(Locale.CANADA, "%.2f", newDistance));
		for(int i=0; i< tmp.length; i++)
		{
			if(newDistance > tmp[i])
			{
				double aux = tmp[i];
				tmp[i] = newDistance;
				newDistance=aux;
			}
		}

		response.put(unlabeledVertex, response.get(closestNeighbor));
		storedPaths.put(unlabeledVertex, tmp);

		//		System.out.println("Labeled neighbors: " + labeledNeighbors);
		//		System.out.println("Unlabeled: " + unlabeledVertex + ". Closest neighbor: " + closestNeighbor + " path: " + Arrays.toString(closestNeighborPath));
		//		System.out.println("Set of edges for the new object: " + Arrays.toString(tmp));
		//		System.out.println("Labeled neighbors: " + labeledNeighbors + " connecting classes: " + connectingClasses);

		// Update Connected components. If the connecting edges are from the same classes,
		// update all the components to a single one.
	}

	/**
	 * Updates edges of the Minimum spanning tree, using the label based distance weighting proposed in HISSCLU.
	 * @param G
	 * @param objects
	 * @param weight
	 * @return
	 */
	private static UndirectedGraph updateEdges(UndirectedGraph G,
			double[][] objects, SemiWeight weight) 
	{

		Map<Integer, Map<Integer, Double>> newEdges           = new HashMap<Integer, Map<Integer, Double>>();
		BitSet visited           							  = new BitSet(G.getNumVertices());
		ArrayList<Integer> queue 							  = new ArrayList<Integer>();


		for(Integer inst=0; inst < objects.length; inst++)
			newEdges.put(inst, new HashMap<Integer, Double>());

		int startPoint = objects.length-1;
		visited.set(startPoint);
		queue.add(startPoint);

		while(!queue.isEmpty())
		{
			Integer currentNode = queue.remove((int)0);

			Map<Integer, Double> neighbors = G.getNeighbors(currentNode);

			for(Map.Entry<Integer, Double> entries: neighbors.entrySet())
			{
				int idNeighbor    = entries.getKey();

				double newCostEdges = entries.getValue() * weight.computeWeight(objects[currentNode], objects[idNeighbor], currentNode, idNeighbor);
				newEdges.get(currentNode).put(idNeighbor, newCostEdges);
				newEdges.get(idNeighbor).put(currentNode, newCostEdges);

				if(!visited.get(idNeighbor))
				{
					queue.add(idNeighbor);
					visited.set(idNeighbor);
				}

			}
		}
		return new UndirectedGraph(newEdges, G.getNumVertices(), G.getVertexA(), G.getVertexB(), G.getEdgesWeights());
	}


	private static void setFinalLabels(Stack<Integer> stack,
			Map<Integer, Integer> response,
			Integer labeledId,
			Map<Integer, Integer> labeledObjects,
			ArrayList<Integer> labeledIds,
			int highestEdge,
			boolean foundAnotherLabel)
	{

		while(!stack.isEmpty() && foundAnotherLabel)
		{
			Integer inst= stack.pop();
			if(inst == highestEdge)
				break;
		}

		//Adjust the label of the objects to the remaining dataset in the stack
		while(!stack.isEmpty())
		{
			Integer inst= stack.pop();
			response.put(inst, labeledObjects.get(labeledId));

			/** Check if the object is pre-labeled and if contains the same label as the object used
			 * to explore the minimum spanning tree
			 */
			if(labeledObjects.containsKey(inst))
			{
				labeledIds.remove((Integer)inst);
			}
		}
		/*
		 * Remove the labeled object already used during the label expansion.
		 */
		if(labeledIds.contains((Integer)labeledId))
		{
			labeledIds.remove((Integer)labeledId);
		}
	}

	private static int minVertex (double [] dist, BitSet v) 
	{
		double x = Double.MAX_VALUE;
		int y = -1;   // graph not connected, or no unvisited vertices

		for (int i=0; i<dist.length; i++) 
		{
			if (!v.get(i) && dist[i] < x) {y=i; x=dist[i];}
		}
		return y;
	}

}





//---------------------------------------- Auxiliary classes ----------------------------------------------
