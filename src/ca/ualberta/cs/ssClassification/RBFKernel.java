package ca.ualberta.cs.ssClassification;

import ca.ualberta.cs.distance.DistanceCalculator;

public class RBFKernel {
    
    private double sigma;
    
    
    public double getSigma(){
        return sigma;
    }
    
    public void computeSigma(int[][] adjacencyMatrix, double[] kNNDistances) {
        int n = adjacencyMatrix.length;
        sigma = 0;
        for(int i = 0; i < kNNDistances.length; i++){
            sigma += kNNDistances[i];
        }
        sigma /= (3.0 * n);
//        System.out.println("Sigma: " + sigma);
    }

    public double[][] getWeightedMatrix(int[][] adjacencyMatrix, double[][] data, DistanceCalculator distance) {
        
    	int n = adjacencyMatrix.length;
        double[][] weightedMatrix = new double[n][n];
        
        double den = 2 * sigma * sigma;
        
        for (int i = 0; i < n; i++)
        {
            for (int j = 0; j < n; j++)
            {
                if (adjacencyMatrix[i][j] != 0)
                {
                    double dist = distance.computeDistance(data[i], data[j]);
                    weightedMatrix[i][j] = adjacencyMatrix[i][j] * Math.exp( -(dist * dist) / den );
//                    System.out.println("i: " + i + " j: " + j + " w: " + String.format("%.2f", weightedMatrix[i][j]) + " dist: " + String.format("%.2f", dist)+ " den: " + String.format("%."+ "2f", den));
                }
            }
        }
        return weightedMatrix;
    }
}
