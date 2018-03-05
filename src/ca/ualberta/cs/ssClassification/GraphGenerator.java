package ca.ualberta.cs.ssClassification;
import java.util.ArrayList;

import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;

public class GraphGenerator 
{

    public static Graph generateGraph(double[][] data, DistanceCalculator distance, int k) throws Exception 
    {
        // computing the distance matrix
        double[][] distanceMatrix = Utils.getDistanceMatrix(data, distance);

        // computing the adjacency matrix
        KNN knn = new KNN();
        int[][] adjacencyMatrix = null;
        knn.computeKNNGraph(distanceMatrix, k);
        adjacencyMatrix = knn.getMutualKNN(distanceMatrix);
        
        // computing the weighted matrix
        double[][] weightedMatrix = null;
        RBFKernel rbf = new RBFKernel();
        rbf.computeSigma(adjacencyMatrix, knn.getKNNDistances());
        weightedMatrix = rbf.getWeightedMatrix(adjacencyMatrix, data, distance);

        // computing the value of sigma
//        double sigma = 0;
//        rbf.computeSigma(adjacencyMatrix, knn.getKNNDistances());
        double sigma = rbf.getSigma();
        
        // returning the weighted graph
        return new Graph(data, distanceMatrix, weightedMatrix, adjacencyMatrix, distance, k, sigma);
    }
    
    public static Graph generateAptsGraph(double[][] data, double[] coreDistances, UndirectedGraph mst, DistanceCalculator distance)
    {
    	int n = data.length;
    	
    	int[][] adjacencyMatrix   = new int[n][n];    	
    	int[] vertexA = mst.getVertexA();
    	int[] vertexB = mst.getVertexB();
    	
    	for(int i=0; i < vertexA.length; i++)
    	{
    		if(vertexA[i]==vertexB[i])
    			continue;
    		
    		adjacencyMatrix[vertexA[i]][vertexB[i]]=1;
    		adjacencyMatrix[vertexB[i]][vertexA[i]]=1;	
    	}
    	
        double[][] weightedMatrix = null;
        RBFKernel rbf = new RBFKernel();
        rbf.computeSigma(adjacencyMatrix, coreDistances);
        weightedMatrix = rbf.getWeightedMatrix(adjacencyMatrix, data, distance);
        
        for(int i=0; i < vertexA.length; i++)
    	{
    		if(vertexA[i]==vertexB[i])
    			continue;
    		
//    		System.out.println("va " + vertexA[i] + "\tvb " + vertexB[i] + ":\t" + String.format("%.2f", weightedMatrix[vertexA[i]][vertexB[i]]));	
    	}
        
    	return new Graph(data, null, weightedMatrix, adjacencyMatrix, distance, -1, 0);
    }

    
    public static Graph generateAptsGraphDB(double[][] data, double[] coreDistances, UndirectedGraph mst, DistanceCalculator distance)
    {
    	int n = data.length;
    	
    	int[][] adjacencyMatrix   = new int[n][n];    	
    	int[] vertexA = mst.getVertexA();
    	int[] vertexB = mst.getVertexB();
    	double[] edges= mst.getEdgesWeights();
    	
    	double[][] weightedMatrix = new double[n][n];
    	
    	for(int i=0; i < vertexA.length; i++)
    	{
    		if(vertexA[i]==vertexB[i])
    			continue;
    		
    		adjacencyMatrix[vertexA[i]][vertexB[i]]=1;
    		adjacencyMatrix[vertexB[i]][vertexA[i]]=1;
    		
    		weightedMatrix[vertexA[i]][vertexB[i]]=1/edges[i];
    		weightedMatrix[vertexB[i]][vertexA[i]]=1/edges[i];
    	}
    	
    	return new Graph(data, null, weightedMatrix, adjacencyMatrix, distance, -1, 0);
    }

    
    public static UndirectedGraph adaptGraph(double[][] data, double[] coreDistances, UndirectedGraph mst, DistanceCalculator distance)
    {
    	int n = data.length;
    	
    	int[][] adjacencyMatrix   = new int[n][n];    	
    	int[] vertexA = mst.getVertexA();
    	int[] vertexB = mst.getVertexB();
    	
    	for(int i=0; i < vertexA.length; i++)
    	{
    		if(vertexA[i]==vertexB[i])
    			continue;
    		
    		adjacencyMatrix[vertexA[i]][vertexB[i]]=1;
    		adjacencyMatrix[vertexB[i]][vertexA[i]]=1;	
    	}
    	
        double[][] weightedMatrix = null;
        RBFKernel rbf = new RBFKernel();
        rbf.computeSigma(adjacencyMatrix, coreDistances);
        weightedMatrix = rbf.getWeightedMatrix(adjacencyMatrix, data, distance);
        
        ArrayList<Integer> arrayA = new ArrayList<Integer>();
        ArrayList<Integer> arrayB = new ArrayList<Integer>();
        ArrayList<Double> edges = new ArrayList<Double>();
        
        for(int i=0; i < vertexA.length; i++)
    	{
    		if(vertexA[i]==vertexB[i])
    			continue;
    		
    		int j= vertexA[i];
    		int k= vertexB[i];
    		double w = weightedMatrix[j][k];
    		
    		arrayA.add(j);
    		arrayB.add(k);
    		edges.add(w);
    	}
        
    	return new UndirectedGraph(n, Utils.convertVertices(arrayA), Utils.convertVertices(arrayB), Utils.convertEdges(edges));
    }
}
