package ca.ualberta.cs.ssClassification;

import java.util.Arrays;

public class ActualState {
    
    private double[][] actualData;
    private double[][] actualWeightedMatrix;
    private int[][] actualAdjacencyMatrix;
    private int[] actualNumbers;
    private int[] actualSplit;
    private int[] actualLabels;
    private double[][] actualDistanceMatrix;
    private double[][] actualGramMatrix;
    private double[][] actualNormalizedLaplacian;
    
    public ActualState(double[][] data, double[][] distanceMatrix, double[][] weightedMatrix, int[][] adjacencyMatrix, int[] numbers, int[] split, int[] labels)
    {
        actualData = Utils.copyMatrix(data);
        actualWeightedMatrix = Utils.copyMatrix(weightedMatrix);
        actualAdjacencyMatrix = Utils.copyMatrix(adjacencyMatrix);
        actualNumbers = Utils.copyVector(numbers);
        actualSplit = Utils.copyVector(split);
        actualLabels = Utils.copyVector(labels);
        actualDistanceMatrix = Utils.copyMatrix(distanceMatrix);
    }
    
    public ActualState(double[][] data, double[][] distanceMatrix, double[][] weightedMatrix, int[][] adjacencyMatrix, int[] numbers, int[] split, int[] labels, 
            double[][] gramMatrix, double[][] normalizedLaplacian)
    {
        actualData = Utils.copyMatrix(data);
        actualWeightedMatrix = Utils.copyMatrix(weightedMatrix);
        actualAdjacencyMatrix = Utils.copyMatrix(adjacencyMatrix);
        actualNumbers = Utils.copyVector(numbers);
        actualSplit = Utils.copyVector(split);
        actualLabels = Utils.copyVector(labels);
        actualDistanceMatrix = Utils.copyMatrix(distanceMatrix);
        actualGramMatrix = Utils.copyMatrix(gramMatrix);
        actualNormalizedLaplacian = Utils.copyMatrix(normalizedLaplacian);
    }
    
    public double[][] getActualData()
    {
        return actualData;
    }
    
    public double[][] getActualWeightedMatrix()
    {
        return actualWeightedMatrix;
    }
    
    public int[][] getActualAdjacencyMatrix()
    {
        return actualAdjacencyMatrix;
    }
    
    public int[] getActualNumbers()
    {
        return actualNumbers;
    }
    
    public int[] getActualSplit()
    {
        return actualSplit;
    }
    
    public int[] getActualLabels()
    {
        return actualLabels;
    }
    
//    public double[][] getActualDistanceMatrix()
//    {
//        return actualDistanceMatrix;
//    }
    
    public double[][] getActualGramMatrix()
    {
        return actualGramMatrix;
    }
    
    public double[][] getActualNormalizedLaplacian()
    {
        return actualNormalizedLaplacian;
    }
        
    public void reorder(){
        int l = actualSplit.length;
        Arrays.sort(actualSplit);
        int index = 0;
        for (int j = 0; j < l; j++) 
        {
            if (actualNumbers[index] != actualSplit[j]) 
            {
                Utils.changeLine(actualData, 		index, actualSplit[j]);
                Utils.change(actualNumbers, 		index, actualSplit[j]);
                Utils.change(actualWeightedMatrix,  index, actualSplit[j]);
                Utils.change(actualLabels, 			index, actualSplit[j]);
//                Utils.change(actualDistanceMatrix,  index, actualSplit[j]);
                Utils.change(actualAdjacencyMatrix, index, actualSplit[j]);
                
                if(actualGramMatrix != null)
                {
                    Utils.change(actualGramMatrix, index, actualSplit[j]);
                    Utils.change(actualNormalizedLaplacian, index, actualSplit[j]);
                }
            }
            index++;
        }
    }
}
