package ca.ualberta.cs.ssClassification;

import Jama.Matrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import ca.ualberta.cs.distance.DistanceCalculator;

public class Utils {

    // getting the distance matrix
    public static double[][] getDistanceMatrix(double[][] data, DistanceCalculator function) {
        int n = data.length;
        double[][] distanceMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double dist = function.computeDistance(data[i], data[j]);
                distanceMatrix[i][j] = dist;
                distanceMatrix[j][i] = dist;
            }
        }
        return distanceMatrix;
    }

    // computing the normalized Laplacian
    public static Matrix normalizedLaplacian(double[][] weightedMatrix, double[] D) {
        int n = weightedMatrix.length;
        Matrix L = new Matrix(new double[n][n]);
        double[] M = new double[n];
        for (int i = 0; i < n; i++) {
            M[i] = Math.pow(D[i], -0.5);
        }
        for (int i = 0; i < n; i++) {
            L.set(i, i, 1.01);
            for (int j = 0; j < n; j++) {
                if (i != j && weightedMatrix[i][j] != 0) {
                    L.set(i, j, -weightedMatrix[i][j] * M[i] * M[j]);
                }
            }
        }
        return L;
    }

    // computing the gram (kernel) matrix
    public static double[][] computeGramMatrix(double[][] weightedMatrix, double[][] distanceMatrix, double sigma) {
        int n = weightedMatrix.length;
        double[][] K = new double[n][n];
        double den = 2 * sigma * sigma;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double dist = distanceMatrix[i][j];
                double weight = Math.exp(-(dist * dist) / den);
                K[i][j] = weight;
                K[j][i] = weight;
            }
            K[i][i] = 1;
        }
        return K;
    }

    // computing a vector that contains the sum of each line of a weighted matrix
    public static double[] computeDiagonalValues(double[][] weightedMatrix) {
        int n = weightedMatrix.length;
        double[] D = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                D[i] += weightedMatrix[i][j];
            }
        }
        return D;
    }

    // computing the maximum argument for the i-th line of a matrix
    public static int argmax(Matrix F, int i) {
        double epsilon = 1E-10;
        int c = F.getColumnDimension();
        double max = F.get(i, 0);
        int index = 0;
        for (int j = 1; j < c; j++) {
            if (Math.abs(F.get(i, j) - max) <= epsilon) {
                index = -1;
            } else if (F.get(i, j) > max) {
                index = j;
                max = F.get(i, j);
            }
        }
        return index;
    }

    // getting the output vector using the argmax operator
    public static int[] getOutputVector(Matrix F, int l) {
        int n = F.getRowDimension();
        int[] output = new int[n - l];
        for (int i = 0; i < n - l; i++) {
            output[i] = Utils.argmax(F, i + l);
        }
        return output;
    }

    // computing the class mass normalization (CMN) procedure
    public static int[] ClassMassNormalization(Matrix F, double[] priors, int l) {
        int c = priors.length;
        int n = F.getRowDimension();
        double[] columnSums = new double[c];
        for (int i = 0; i < c; i++) {
            double actualSum = 0;
            for (int j = 0; j < n; j++) {
                actualSum += F.get(j, i);
            }
            columnSums[i] = actualSum;
        }
        int[] output = new int[n - l];
        for (int j = l; j < n; j++) {
            double max = priors[0] * F.get(j, 0) / columnSums[0];
            int index = 0;
            for (int i = 1; i < c; i++) {
                double temp = priors[i] * F.get(j, i) / columnSums[i];
                if (temp > max) {
                    max = temp;
                    index = i;
                }
            }
            output[j - l] = index;
        }
        return output;
    }

    // getting the prior probability of each class
    public static double[] getClassPriors(int[] inputLabels, int l, int c)
    {
        int[] examplesPerClass = examplesPerClass(inputLabels, l, c);
        double[] priors = new double[c];
        for (int ix = 0; ix < c; ix++) {
            priors[ix] = (examplesPerClass[ix] * 1.0) / l;
        }
        return priors;
    }

    // computing the label matrix
    public static Matrix computeLabelMatrix(int[] labels, int l, int c, int n)
    {
        Matrix Y = new Matrix(new double[n][c]);        
        
        for (int i = 0; i < l; i++)
        	Y.set(i, (labels[i]-1), 1);
        
        return Y;
    }
    

    // computing the label matrix for the GFHF
    public static Matrix computeLabelMatrixGFHF(int[] labels, int l, int c, int n)
    {
        Matrix Y = new Matrix(new double[n][c]);        
        
        for (int i = 0; i < l; i++)
        	Y.set(i, labels[i], 1);
        
        return Y;
    }

    

    // getting the label vector (used for LapSVM algorithm)
    public static double[][] getLabelVector(int[] labels, int l) throws Exception 
    {
        int n = labels.length;
        double[][] y = new double[n][1];
        for (int i = 0; i < l; i++) {
            if (labels[i] == 0) {
                y[i][0] = -1;
            } else if (labels[i] == 1) {
                y[i][0] = 1;
            } else {
                throw new Exception("There are more than 2 classes!");
            }
        }
        return y;
    }

    // computing the number of examples per class
    public static int[] examplesPerClass(int[] labels, int l, int c) 
    {
        int[] examplesPerClass = new int[c];
        for (int i = 0; i < l; i++) {
            examplesPerClass[labels[i]]++;
        }
        return examplesPerClass;
    }

    // getting integer numbers from 0 to n-1
    public static int[] orderedNumbers(int n) {
        int[] orderedNumbers = new int[n];
        for (int i = 0; i < n; i++) {
            orderedNumbers[i] = i;
        }
        return orderedNumbers;
    }

    // changing the i-th and j-th positions of a vector
    public static void change(int[] v, int i, int j) {
        int temp = v[i];
        v[i] = v[j];
        v[j] = temp;
    }

    // changing the i-th and j-th lines and columns of a matrix of integers
    public static void change(double[][] matrix, int i, int j) {
        int n = matrix.length;
        for (int k = 0; k < n; k++) {
            double temp = matrix[i][k];
            matrix[i][k] = matrix[j][k];
            matrix[j][k] = temp;
        }
        for (int k = 0; k < n; k++) {
            double temp = matrix[k][i];
            matrix[k][i] = matrix[k][j];
            matrix[k][j] = temp;
        }
    }

    // changing the i-th and j-th lines and columns of a matrix of doubles
    public static void change(int[][] matrix, int i, int j) {
        int n = matrix.length;
        for (int k = 0; k < n; k++) {
            int temp = matrix[i][k];
            matrix[i][k] = matrix[j][k];
            matrix[j][k] = temp;
        }
        for (int k = 0; k < n; k++) {
            int temp = matrix[k][i];
            matrix[k][i] = matrix[k][j];
            matrix[k][j] = temp;
        }
    }

    // changing i-th and j-th lines of a matrix
    public static void changeLine(double[][] matrix, int i, int j) {
        double[] temp = matrix[i];
        matrix[i] = matrix[j];
        matrix[j] = temp;
    }

    // copying a matrix of doubles
    public static double[][] copyMatrix(double[][] M) {
        int n = M.length;
        int m = M[0].length;
        double[][] A = new double[n][m];
        for (int i = 0; i < n; i++) {
            System.arraycopy(M[i], 0, A[i], 0, m);
        }
        return A;
    }

    // copying a matrix of integers
    public static int[][] copyMatrix(int[][] M) {
        int n = M.length;
        int m = M[0].length;
        int[][] A = new int[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(M[i], 0, A[i], 0, m);
        }
        return A;
    }

    // copying a vector of integers
    public static int[] copyVector(int[] M) {
        int n = M.length;
        int[] A = new int[n];
        System.arraycopy(M, 0, A, 0, n);
        return A;
    }

    public static int[][] readDataSplits(String datasetName, int l) throws FileNotFoundException, IOException {

        String fileName = "";

        String[] tempName = datasetName.split("_");
        if (tempName.length == 1) {
            fileName = datasetName;
        } else {
            fileName = tempName[0];
        }

        BufferedReader myReader = new BufferedReader(new FileReader(new File("./datasets_splits/" + fileName + "_l=" + l + ".txt")));
        int[][] labeledExamples = new int[12][l];
        String line = myReader.readLine();
        int i = 0;
        while (line != null) {
            String[] temp = line.trim().split(";");
            for (int j = 0; j < temp.length; j++) {
                labeledExamples[i][j] = Integer.parseInt(temp[j]) - 1;
            }
            i++;
            line = myReader.readLine();
        }
        return labeledExamples;
    }

    public static double computeError(int[] output, int[] labels, int l) 
    {
        double error = 0;
        int n = labels.length;
        for (int i = l; i < n; i++)
        {
            if (output[i - l] != labels[i])
            {
                error++;
            }
        }
        return error / (n * 1.0 - l);
    }
    
	public static int[] convertVertices(ArrayList<Integer> list)
	{
		int[] newVertex = new int[list.size()];
		
		for(int i=0; i < list.size(); i++)
			newVertex[i]=list.get(i);
		
		return newVertex;
	}
	
	public static double[] convertEdges(ArrayList<Double> list)
	{
		double[] newVertex = new double[list.size()];
		
		for(int i=0; i < list.size(); i++)
			newVertex[i]=list.get(i);
		
		return newVertex;
	}
}
