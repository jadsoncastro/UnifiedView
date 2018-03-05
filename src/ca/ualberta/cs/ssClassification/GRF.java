package ca.ualberta.cs.ssClassification;

import Jama.Matrix;
import javafx.util.Pair;

// Gaussian Random Fields (GRF)
public class GRF
{
	private double[] priors;

	public GRF(double[] priors) 
	{
		this.priors = priors;
	}

	
	public Pair<Double, int[]> classify(double[][] weightedMatrix, Matrix Y, int l) 
	{
		Long startTime = System.currentTimeMillis();
		double[] D = Utils.computeDiagonalValues(weightedMatrix);
		Matrix Luu = computeLuu(weightedMatrix, D, l);
		Matrix Lul = computeLul(weightedMatrix, D, l);
		Matrix Yl = computeYl(Y, l);
		Matrix Fu = Luu.solve(Lul.times(-1).times(Yl));
		Double totalTime = (System.currentTimeMillis() - startTime) / 1000.00;
		
		return new Pair<Double, int[]>(totalTime, Utils.ClassMassNormalization(Fu, priors, 0));
	}

	public static Matrix computeDeltaUU(double[][] weightedMatrix, int l) 
	{
		int n = weightedMatrix.length;
		double eps = 1.01;
		Matrix DeltaUU = new Matrix(new double[n - l][n - l]);
		for (int i = l; i < n; i++) {
			double sum = 0;
			for (int j = 0; j < l; j++) {
				sum += weightedMatrix[i][j];
			}
			for (int j = l; j < n; j++) {
				DeltaUU.set(i - l, j - l, -weightedMatrix[i][j]);
				sum += weightedMatrix[i][j];
			}
			DeltaUU.set(i - l, i - l, eps * sum);
		}
		return DeltaUU;
	}

	public static Matrix computeLuu(double[][] weightedMatrix, double[] D, int l){
		int n = weightedMatrix.length;
		Matrix Luu = new Matrix(new double[n - l][n - l]);
		double[] M = new double[n - l];
		for(int i = l; i < n; i++){
			M[i - l] = 1.0 / Math.sqrt(D[i]);
		}
		for(int i = 0; i < n - l; i++){
			Luu.set(i, i, 1.01);
			for(int j = 0; j < n - l; j++){
				if(i != j && weightedMatrix[i + l][j + l] != 0){
					double value = - weightedMatrix[i + l][j + l] * M[i] * M[j];
					Luu.set(i, j, value);
				}
			}                
		}        
		return Luu;
	}

	public static Matrix computeLul(double[][] weightedMatrix, double[] D, int l){
		int n = weightedMatrix.length;
		Matrix Lul = new Matrix(new double[n - l][l]);
		double[] M = new double[n];
		for(int i = 0; i < n; i++){
			M[i] = 1.0 / Math.sqrt(D[i]);
		}
		for(int i = 0; i < n - l; i++){
			for(int j = 0; j < l; j++){
				if(weightedMatrix[i + l][j] != 0){
					double value = - weightedMatrix[i + l][j] * M[i + l] * M[j];
					Lul.set(i, j, value);                    
				}
			}
		}
		return Lul;
	}

	public static Matrix computeWUL(double[][] weightedMatrix, int l) {
		int n = weightedMatrix.length;
		Matrix WUL = new Matrix(new double[n - l][l]);
		for (int i = l; i < n; i++) {
			for (int j = 0; j < l; j++) {
				WUL.set(i - l, j, weightedMatrix[i][j]);
			}
		}
		return WUL;
	}

	public static Matrix computeYl(Matrix Y, int l) {
		int c = Y.getColumnDimension();
		Matrix Yl = new Matrix(new double[l][c]);
		for (int i = 0; i < l; i++) {
			for (int j = 0; j < c; j++) {
				Yl.set(i, j, Y.get(i, j));
			}
		}
		return Yl;
	}
}