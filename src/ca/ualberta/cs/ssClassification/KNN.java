package ca.ualberta.cs.ssClassification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

// k-nearest neighbors (kNN)
public class KNN {

    private int[][] kNNGraph;
    private double[] kNNDistances;

    public int[][] getkNNGraph() 
    {
        return kNNGraph;
    }

    public double[] getKNNDistances() 
    {
        return kNNDistances;
    }

    public static void getSymmetricKNN(int[][] adjacencyMatrix) {
        int n = adjacencyMatrix.length;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (adjacencyMatrix[i][j] == 1 || adjacencyMatrix[j][i] == 1) {
                    adjacencyMatrix[j][i] = 1;
                    adjacencyMatrix[i][j] = 1;
                }
            }
        }
    }

    public int[][] getSymmetryFavoredKNN() {
        int n = kNNGraph.length;
        int[][] symFKNN = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                int temp = kNNGraph[i][j] + kNNGraph[j][i];
                symFKNN[i][j] = temp;
                symFKNN[j][i] = temp;
            }
        }
        return symFKNN;
    }

    public int[][] getMutualKNN(double[][] distanceMatrix) 
    {
        int n = kNNGraph.length;
        int[][] mutKNN = new int[n][n];
        for (int i = 0; i < n; i++)
        {
            for (int j = i + 1; j < n; j++) 
            {
                if (kNNGraph[i][j] == 1 && kNNGraph[j][i] == 0) {
                    mutKNN[i][j] = 0;
                } else if (kNNGraph[i][j] == 0 && kNNGraph[j][i] == 1) {
                    mutKNN[j][i] = 0;
                } else if (kNNGraph[i][j] == 1 && kNNGraph[j][i] == 1) {
                    mutKNN[i][j] = 1;
                    mutKNN[j][i] = 1;
                }
            }
        }
        for (int i = 0; i < n; i++) {
            boolean none = true;
            for (int j = 0; j < n; j++) {
                if (mutKNN[i][j] != 0) {
                    none = false;
                }
            }
            if (none) 
            {
                double minDist = Double.MAX_VALUE;
                int minJ = 0;
                for (int j = 0; j < n; j++) 
                {
                    if (i != j) 
                    {
                        if (distanceMatrix[i][j] < minDist)
                        {
                            minDist = distanceMatrix[i][j];
                            minJ = j;
                        }
                    }
                }
                mutKNN[i][minJ] = 1;
                mutKNN[minJ][i] = 1;
            }
        }
        return mutKNN;
    }

    public int[][] getSymmetricKNN() {
        int n = kNNGraph.length;
        int[][] symKNN = new int[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (kNNGraph[i][j] == 1 || kNNGraph[j][i] == 1) {
                    symKNN[j][i] = 1;
                    symKNN[i][j] = 1;
                }
            }
        }
        return symKNN;
    }

    public void computeKNNGraph(double[][] distanceMatrix, int k) {
        int n = distanceMatrix.length;
        kNNGraph = new int[n][n];
        kNNDistances = new double[n];
        for (int i = 0; i < n; i++) {
            
            List<Integer> adjacencyList = computeNearestNeighbors(distanceMatrix, i, k);
            double maxDistance = 0;
            for(int vertex : adjacencyList){
                kNNGraph[i][vertex] = 1;
                
                if(distanceMatrix[i][vertex] > maxDistance){
                    maxDistance = distanceMatrix[i][vertex];
                }
                kNNDistances[i] = maxDistance;
            }
        }
    }

    public static List<Integer> computeNearestNeighbors(double[][] distanceMatrix, int pointIndex, int k) {
        
        List<Integer> adjacencyList = new ArrayList<Integer>();
        Map<Integer, Double> maps = new HashMap<Integer, Double>();
        PriorityQueue<Double> queue = new PriorityQueue<Double>(k, Collections.reverseOrder());
        double epsilon = 1E-10;
        int n = distanceMatrix[0].length;
        int count = 0;
        int actualIndex = pointIndex;
                
        for (int j = 0; j < n; j++) {
            if (j != pointIndex) {
                
                double distance = distanceMatrix[actualIndex][j];
                if (count < k) {
                    queue.add(distance);

                    maps.put(j, distance);
                    count++;
                } else {
                    double max = queue.peek();
                    if (distance < max) {
                        queue.poll();
                        for (Integer x : maps.keySet()) {
                            double actualValue = maps.get(x);
                            if (Math.abs(actualValue - max) <= epsilon) {
                                maps.remove(x);
                                break;
                            }
                        }
                        queue.add(distance);
                        maps.put(j, distance);
                    }
                }
            }
        }

        double max = queue.peek();
        for (int j = 0; j < n; j++) {
            if (j != pointIndex) {
                double dist = distanceMatrix[actualIndex][j];
                if (!maps.containsKey(j) && Math.abs(dist - max) <= epsilon) {
                    maps.put(j, max);
                }
            }
        }

        for (int index : maps.keySet()) {
            adjacencyList.add(index);
        }
        queue.clear();
        maps.clear();

        return adjacencyList;
    }
    
}
