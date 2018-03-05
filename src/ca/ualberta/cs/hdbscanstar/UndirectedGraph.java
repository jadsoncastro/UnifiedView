package ca.ualberta.cs.hdbscanstar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * An undirected graph, with weights assigned to each edge.  Vertices in the graph are 0 indexed.
 * @author zjullion
 */
public class UndirectedGraph implements java.io.Serializable
{

	// ------------------------------ PRIVATE VARIABLES ------------------------------

	private int numVertices;
	private int[] verticesA;
	private int[] verticesB;
	private double[] edgeWeights;
	private Object[] edges;		//Each Object in this array in an ArrayList<Integer>

        /** 24/02/2016
         * @author fernando
         * Turning class serializable        
         */
        
	/** 16/02/2016
	 * 
	 * @author jadson
	 * Saving a copy of the MST to use for the label expansion of semi supervised clustering algorithms
	 * HashMap<Integer, Map<Integer, Double>>(); */
	private Map<Integer, Map<Integer, Double>> hashMST;


//	// ------------------------------ CONSTANTS ------------------------------
//
        private static final long serialVersionUID = 1L;

        // ------------------------------ CONSTRUCTORS ------------------------------

	/**
	 * Constructs a new UndirectedGraph, including creating an edge list for each vertex from the 
	 * vertex arrays.  For an index i, verticesA[i] and verticesB[i] share an edge with weight
	 * edgeWeights[i].
	 * @param numVertices The number of vertices in the graph (indexed 0 to numVertices-1)
	 * @param verticesA An array of vertices corresponding to the array of edges
	 * @param verticesB An array of vertices corresponding to the array of edges
	 * @param edgeWeights An array of edges corresponding to the arrays of vertices
	 */
	public UndirectedGraph(int numVertices, int[] verticesA, int[] verticesB, double[] edgeWeights) {
		this.numVertices = numVertices;
		this.verticesA = verticesA;
		this.verticesB = verticesB;
		this.edgeWeights = edgeWeights;
		this.hashMST = new HashMap<Integer, Map<Integer, Double>>();

		this.edges = new Object[numVertices];
		for (int i = 0; i < this.edges.length; i++) {
			this.edges[i] = new ArrayList<Integer>(1 + edgeWeights.length/numVertices);
			this.hashMST.put(i, new HashMap<Integer, Double>());
		}

		for (int i = 0; i < edgeWeights.length; i++) {
			int vertexOne = this.verticesA[i];
			int vertexTwo = this.verticesB[i];

			this.hashMST.get((Integer)vertexOne).put(vertexTwo, edgeWeights[i]);
			this.hashMST.get((Integer)vertexTwo).put(vertexOne, edgeWeights[i]);

			((ArrayList<Integer>)(this.edges[vertexOne])).add(vertexTwo);
			if (vertexOne != vertexTwo)
				((ArrayList<Integer>)(this.edges[vertexTwo])).add(vertexOne);
		}
	}

	/**
	 * @author jadson
	 * Update edges weights of the minimum spanning tree to after execute HISSCLU algorithm
	 * Mantain the same values on both structures: Arrays and HashMap
	 * @param vertexOne
	 * @param vertexTwo
	 * @param weight
	 */
	public UndirectedGraph(Map<Integer, Map<Integer, Double>> newEdgesWeights, int nVert, int[] vertA, int[] vertB, double[] edgW)
	{

		this.numVertices = nVert;
		this.verticesA   = vertA;
		this.verticesB   = vertB;
		this.edgeWeights = edgW;
		this.edges= new Object[this.numVertices];
		this.hashMST = new HashMap<Integer, Map<Integer, Double>>();

		for (int i = 0; i < this.edges.length; i++) 
		{
			this.edges[i] = new ArrayList<Integer>(1 + edgeWeights.length/numVertices);
			this.hashMST.put(i, new HashMap<Integer, Double>());
		}	


		for(int i=0; i < edgeWeights.length; i++)
		{

			Integer vertexOne = verticesA[i];
			Integer vertexTwo = verticesB[i];
			double  newWeight = newEdgesWeights.get(vertexOne).get(vertexTwo);

			edgeWeights[i]= newWeight;

			this.hashMST.get(vertexOne).put(vertexTwo, newWeight);
			this.hashMST.get(vertexTwo).put(vertexOne, newWeight);
			
			((ArrayList<Integer>)(this.edges[vertexOne])).add(vertexTwo);
			if (vertexOne != vertexTwo)
				((ArrayList<Integer>)(this.edges[vertexTwo])).add(vertexOne);

		}
	}

	/**
	 * @author jadson
	 * Copy of the mst
	 */
	public UndirectedGraph(UndirectedGraph mst) 
	{
		this.numVertices = mst.getNumVertices();
		this.verticesA 	 = new int[mst.getVertexA().length];
		this.verticesB 	 = new int[mst.getVertexB().length];
		this.edgeWeights = new double[mst.getEdgesWeights().length];
		this.edges 		 = new Object[numVertices];
		this.hashMST     = new HashMap<Integer, Map<Integer, Double>>();
		
		for (int i = 0; i < this.edges.length; i++) 
		{
			this.edges[i] = new ArrayList<Integer>(1 + edgeWeights.length/numVertices);
			this.hashMST.put(i, new HashMap<Integer, Double>());
		}
		
		for(int i=0; i< mst.getVertexA().length; i++)
		{
			this.verticesA[i]= mst.getVertexA()[i];
			this.verticesB[i]= mst.getVertexB()[i];
			this.edgeWeights[i]= mst.getEdgeWeightAtIndex(i);
		}

		for (int i = 0; i < edgeWeights.length; i++) 
		{
			int vertexOne = this.verticesA[i];
			int vertexTwo = this.verticesB[i];

			this.hashMST.get(vertexOne).put(vertexTwo, edgeWeights[i]);
			this.hashMST.get(vertexTwo).put(vertexOne, edgeWeights[i]);

			((ArrayList<Integer>)(this.edges[vertexOne])).add(vertexTwo);
			if (vertexOne != vertexTwo)
				((ArrayList<Integer>)(this.edges[vertexTwo])).add(vertexOne);
		}
	}

	
	
	// ------------------------------ PUBLIC METHODS ------------------------------

	/**
	 * Quicksorts the graph by edge weight in descending order.  This quicksort implementation is 
	 * iterative and in-place.
	 */
	public void quicksortByEdgeWeight() 
	{
		
		if (this.edgeWeights.length <= 1)
			return;

		int[] startIndexStack = new int[this.edgeWeights.length/2];
		int[] endIndexStack = new int[this.edgeWeights.length/2];

		startIndexStack[0] = 0;
		endIndexStack[0] = this.edgeWeights.length-1;
		int stackTop = 0;

		while (stackTop >= 0) {
			int startIndex = startIndexStack[stackTop];
			int endIndex = endIndexStack[stackTop];
			stackTop--;

			int pivotIndex = this.selectPivotIndex(startIndex, endIndex);
			pivotIndex = this.partition(startIndex, endIndex, pivotIndex);

			if (pivotIndex > startIndex+1) {
				startIndexStack[stackTop+1] = startIndex;
				endIndexStack[stackTop+1] = pivotIndex-1;
				stackTop++;
			}

			if (pivotIndex < endIndex-1) {
				startIndexStack[stackTop+1] = pivotIndex+1;
				endIndexStack[stackTop+1] = endIndex;
				stackTop++;
			}
		}
	}
	
	public String toString()
	{
		String s = "";
		
		for(int i=0; i < this.verticesA.length; i++)
		{
			if(this.verticesA[i] != this.verticesB[i])
//				s+= (this.verticesA[i]+1) + "," + (this.verticesB[i]+1) + "," + String.format("%.2f", this.edgeWeights[i]) + "\n";
				s+= (this.verticesA[i]) + "," + (this.verticesB[i]) + "," + String.format(Locale.ENGLISH, "%.2f", this.edgeWeights[i]) + "\n";

		}
		
		return s;
	}

	// ------------------------------ PRIVATE METHODS ------------------------------

	/**
	 * Quicksorts the graph in the interval [startIndex, endIndex] by edge weight.
	 * @param startIndex The lowest index to be included in the sort
	 * @param endIndex The highest index to be included in the sort
	 */
	private void quicksort(int startIndex, int endIndex) {

		if (startIndex < endIndex) 
		{
			int pivotIndex = this.selectPivotIndex(startIndex, endIndex);
			pivotIndex = this.partition(startIndex, endIndex, pivotIndex);
			this.quicksort(startIndex, pivotIndex-1);
			this.quicksort(pivotIndex+1, endIndex);
		}
	}


	/**
	 * Returns a pivot index by finding the median of edge weights between the startIndex, endIndex,
	 * and middle.
	 * @param startIndex The lowest index from which the pivot index should come
	 * @param endIndex The highest index from which the pivot index should come
	 * @return A pivot index
	 */
	private int selectPivotIndex(int startIndex, int endIndex) {
		if (startIndex - endIndex <= 1)
			return startIndex;

		double first = this.edgeWeights[startIndex];
		double middle = this.edgeWeights[startIndex + (endIndex-startIndex)/2];
		double last = this.edgeWeights[endIndex];

		if (first <= middle) {
			if (middle <= last)
				return startIndex + (endIndex-startIndex)/2;
			else if (last >= first)
				return endIndex;
			else
				return startIndex;
		}
		else {
			if (first <= last)
				return startIndex;
			else if (last >= middle)
				return endIndex;
			else
				return startIndex + (endIndex-startIndex)/2;
		}
	}


	/**
	 * Partitions the array in the interval [startIndex, endIndex] around the value at pivotIndex.
	 * @param startIndex The lowest index to  partition
	 * @param endIndex The highest index to partition
	 * @param pivotIndex The index of the edge weight to partition around
	 * @return The index position of the pivot edge weight after the partition
	 */
	private int partition(int startIndex, int endIndex, int pivotIndex) {
		double pivotValue = this.edgeWeights[pivotIndex];
		this.swapEdges(pivotIndex, endIndex);
		int lowIndex = startIndex;

		for (int i = startIndex; i < endIndex; i++) {
			if (this.edgeWeights[i] < pivotValue) {
				this.swapEdges(i, lowIndex);
				lowIndex++;
			}
		}

		this.swapEdges(lowIndex, endIndex);
		return lowIndex;
	}


	/**
	 * Swaps the vertices and edge weights between two index locations in the graph.
	 * @param indexOne The first index location
	 * @param indexTwo The second index location
	 */
	private void swapEdges(int indexOne, int indexTwo) {
		if (indexOne == indexTwo)
			return;

		int tempVertexA = this.verticesA[indexOne];
		int tempVertexB = this.verticesB[indexOne];
		double tempEdgeDistance = this.edgeWeights[indexOne];

		this.verticesA[indexOne] = this.verticesA[indexTwo];
		this.verticesB[indexOne] = this.verticesB[indexTwo];
		this.edgeWeights[indexOne] = this.edgeWeights[indexTwo];

		this.verticesA[indexTwo] = tempVertexA;
		this.verticesB[indexTwo] = tempVertexB;
		this.edgeWeights[indexTwo] = tempEdgeDistance;
	}


	// ------------------------------ GETTERS & SETTERS ------------------------------

	public int getNumVertices() {
		return this.numVertices;
	}

	public int getNumEdges() {
		return this.edgeWeights.length;
	}

	public int getFirstVertexAtIndex(int index) {
		return this.verticesA[index];
	}

	public int getSecondVertexAtIndex(int index) {
		return this.verticesB[index];
	}

	public double getEdgeWeightAtIndex(int index) {
		return this.edgeWeights[index];
	}

	public ArrayList<Integer> getEdgeListForVertex(int vertex) {
		return (ArrayList<Integer>)this.edges[vertex];
	}

	public Map<Integer, Double> getNeighbors(int vertex)
	{
		return this.hashMST.get((Integer)vertex);
	}

	public double getDistance(int vertexOne, int vertexTwo)
	{
		return this.hashMST.get((Integer)vertexOne).get((Integer)vertexTwo);
	}

	public int[] getVertexA()
	{
		return this.verticesA;
	}

	public int[] getVertexB()
	{
		return this.verticesB;
	}

	public double[] getEdgesWeights()
	{
		return this.edgeWeights;
	}
	
}