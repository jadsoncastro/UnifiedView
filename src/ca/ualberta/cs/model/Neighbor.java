package ca.ualberta.cs.model;

public class Neighbor implements Comparable<Neighbor>
{
	private Instance instance;
	private double distance;
	
	public Neighbor(Instance instance, double distance)
	{
		this.instance = instance;
		this.distance = distance;
	}
	
	public Instance getInstance() 
	{
		return instance;
	}

	public void setInstance(Instance instance) 
	{
		this.instance = instance;
	}

	public double getDistance() 
	{
		return distance;
	}

	public void setDistance(double distance) 
	{
		this.distance = distance;
	}

	public int compareTo(Neighbor o) 
	{
		double difference = this.distance - o.distance;
		
		if(difference > 0)
		{
			return 1;

		}else if(difference == 0)
			return 0;
		else
			return -1;
	}
	
	public String toString()
	{
		return "(id: " + this.instance.getIndex() + ", dist: " + this.distance + ")";
	}
}