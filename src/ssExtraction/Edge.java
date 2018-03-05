package ssExtraction;

import java.util.Locale;

class Edge implements Comparable<Edge>
{
	private int vertexOne;
	private int vertexTwo;
	private double weight;


	public Edge(int v1, int v2, double ew)
	{
		this.vertexOne = v1;
		this.vertexTwo = v2;
		this.weight = ew;
	}

	public int getVertexOne()
	{
		return this.vertexOne;
	}

	public int getVertexTwo()
	{
		return this.vertexTwo;
	}

	public double getWeight()
	{
		return this.weight;
	}

	@Override
	public int compareTo(Edge e2)
	{
		return Double.compare(this.getWeight(), e2.getWeight());
	}
	
	@Override
	public String toString()
	{
		return "(" + this.vertexOne + "," + this.vertexTwo + ": " + String.format(Locale.CANADA, "%.2f", this.weight) + ")";
	}
}
