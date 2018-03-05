package ca.ualberta.cs.ssClassification;

import Jama.Matrix;

// Robust Multi-class Graph Transduction (RMGT)
public class RMGT
{
	public int[] classify(double[][] weightedMatrix, Matrix Y, int l)
	{
		Matrix Yl = GRF.computeYl(Y, l);
		int n = weightedMatrix.length;
		Matrix DeltaUU = GRF.computeDeltaUU(weightedMatrix, l);
		Matrix inverseDeltaUU = DeltaUU.solve(Matrix.identity(n - l, n - l));
		Matrix WUL = GRF.computeWUL(weightedMatrix, l);
		Matrix Fu0 = inverseDeltaUU.times(WUL).times(Yl);
		Matrix A = A(Fu0, Yl, n);
		Matrix B = B(inverseDeltaUU);
		Matrix Fu = Fu0.plus(B.times(A));
		return Utils.getOutputVector(Fu, 0);
	}

	public Matrix A(Matrix Fu0, Matrix Yl, int n) 
	{
		int c = Fu0.getColumnDimension();
		int l = Yl.getRowDimension();
		Matrix A = new Matrix(new double[1][c]);
		double[] values = new double[c];

		for (int i = 0; i < c; i++)
			values[i] = n * 1.0 / c;

		for (int j = 0; j < c; j++)
		{
			double value = values[j];
			
			for (int i = 0; i < l; i++) 
				value -= Yl.get(i, j);
			
			for (int i = 0; i < n - l; i++)
				value -= Fu0.get(i, j);
			
			A.set(0, j, value);
		}
		return A;
	}

	public Matrix B(Matrix inverseDeltaUU) 
	{
		int u = inverseDeltaUU.getRowDimension();
		Matrix B = new Matrix(new double[u][1]);
		double totalSum = 0;
		for (int i = 0; i < u; i++) {
			double lineSum = 0;
			for (int j = 0; j < u; j++) {
				lineSum += inverseDeltaUU.get(i, j);
			}
			totalSum += lineSum;
			B.set(i, 0, lineSum);
		}
		for (int i = 0; i < u; i++) {
			B.set(i, 0, B.get(i, 0) / totalSum);
		}
		return B;
	}
}
