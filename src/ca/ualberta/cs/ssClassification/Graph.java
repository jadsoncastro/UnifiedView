package ca.ualberta.cs.ssClassification;

import java.io.Serializable;

import ca.ualberta.cs.distance.DistanceCalculator;

public class Graph implements Serializable
{
 
	private static final long serialVersionUID = 1L;
	
    private double[][] 		   data;
    private double[][] 		   weightedMatrix;
    private int[][]    		   adjacencyMatrix;
    private double[][] 		   distanceMatrix;
    private int 	   		   k;
//    private DistanceCalculator distanceOption;
    private double 			   sigma;
    

    
    public Graph(double[][] data, double[][] D, double[][] W, int[][] A, DistanceCalculator distance, int k, double sigma)
    {
        if(data != null)
            this.data = Utils.copyMatrix(data);
            
        weightedMatrix  	= Utils.copyMatrix(W);
        adjacencyMatrix 	= Utils.copyMatrix(A);
        distanceMatrix      = Utils.copyMatrix(D);
//        this.distanceOption = distance;
        this.k = k;  
        this.sigma = sigma;
    }
    
    public double[][] getData()
    {
        return data;
    }
    
    public double[][] getWeightedMatrix(){
        return weightedMatrix;
    }
    
    public int[][] getAdjacencyMatrix(){
        return adjacencyMatrix;
    }
    
    public double[][] getDistanceMatrix(){
        return distanceMatrix;
    }
        
    public int getK(){
        return k;
    }
    
    public double getSigma(){
        return sigma;
    }
        
//    public DistanceCalculator getDistanceOption(){
//        return distanceOption;
//    }    
}
