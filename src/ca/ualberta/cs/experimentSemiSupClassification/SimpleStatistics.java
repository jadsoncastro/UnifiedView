package ca.ualberta.cs.experimentSemiSupClassification;

import java.util.ArrayList;


import ca.ualberta.cs.model.Instance;

public class SimpleStatistics 
{
	
	public static double getMean(ArrayList<Double> values)
	{
		double total = 0.0;
		
		for (Double value : values) 
		{
			if(!value.isNaN())
				total += value;
		}
		
		return total / values.size();
	}
	
	public static double getStandardDeviation(ArrayList<Double> values, double mean)
	{
		double total = 0.0;
		
		for (Double value : values) 
		{
			if(!value.isNaN())
				total += (value - mean) * (value - mean);
		}
		
		return Math.sqrt(total / values.size());
	}
}