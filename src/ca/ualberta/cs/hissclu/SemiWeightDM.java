/*
 * Method for semi-supervised OPTICS
 *
 * Created on January 25, 2005, 8:00 AM (Claudia Plant)
 * Adapted by Jadson Castro in October, 26, 2015
 */

package ca.ualberta.cs.hissclu;

import java.util.Map;
import java.util.TreeMap;
import javafx.util.Pair;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.model.Instance;

public class SemiWeightDM 
{
	DistanceCalculator distance;
	TreeMap<Integer, Pair<Integer, double[]>> classified;
	double rho;
	double expo;

	/** Creates a new instance of SemiWeight */

	//-------------------------------------- PUBLIC FUNCTIONS --------------------------------------------------------
	public SemiWeightDM(TreeMap<Integer, Pair<Integer, double[]>> cl, double r, double exp, DistanceCalculator dist)
	{
		this.classified = cl;
		this.rho  		= r;
		this.expo 		= exp;
		this.distance   = dist;
	}

	public double computeWeight(Instance p, Instance q)
	{
		return(weightDistance(p, q));
	}

	// ------------------------------------- PRIVATE FUNCTIONS --------------------------------------------------------

	private double weightDistance(Instance p, Instance q) 
	{
		double rho    = this.rho;
		double expo   = this.expo;
		double result = 1.0;

		Integer[] nextCl  = classKnnQuery(p);
		Integer[] otherCl = classKnnQuery(q);

		if (classified.get(nextCl[0]).getKey() != classified.get(otherCl[0]).getKey())
		{
			return rho;
		}

		if (nextCl[1] != null)
		{
			double d1 = Math.abs(skalarprod(classified.get(nextCl[0]).getValue(), p.getCoordinates(), classified.get(nextCl[1]).getValue()));
			double d2 = Math.abs(skalarprod(classified.get(nextCl[1]).getValue(), p.getCoordinates(), classified.get(nextCl[0]).getValue()));

			double hValue  = (d1*d2)/(0.25*((d1+d2)*(d1+d2))); 

			result = Math.pow(hValue, expo) * (rho - 1) + 1;
		}


		if (otherCl[1] != null)
		{
			double d1 = Math.abs(skalarprod(classified.get(otherCl[0]).getValue(), p.getCoordinates(), classified.get(otherCl[1]).getValue()));
			double d2 = Math.abs(skalarprod(classified.get(otherCl[1]).getValue(), p.getCoordinates(), classified.get(otherCl[0]).getValue()));

			double hValue  = (d1*d2)/(0.25*((d1+d2)*(d1+d2))); 

			if(result < Math.pow(hValue, expo) * (rho - 1) + 1)
				result = Math.pow(hValue, expo) * (rho - 1) + 1;
		}

		return result;
	}

	private Integer[] classKnnQuery(Instance center) 
	{
		Integer[] result   = new Integer[2];
		double    minDist  = Double.MAX_VALUE;
		int       minIndex = 0;


//		for (Map.Entry<Integer, Pair<Integer, double[]>> entries: classified.entrySet())
//		{
//			double aktDist = this.distance.computeDistance(center, entries.getValue().getValue());
//
//			if (aktDist < minDist)
//			{
//				minDist = aktDist;
//				minIndex = entries.getKey();
//			}
//		}

		Integer nn1 = center.getNearestLabeled();

		minDist  = -1.0;
		minIndex = -1;

		Integer nn2=null;


		for (Map.Entry<Integer, Pair<Integer, double[]>> entries: classified.entrySet())
		{
			// Labels not equals and the "next labeled object is beyond the object under consult"
			if(entries.getValue().getKey() != classified.get(nn1).getKey() &&
					skalarprod(classified.get(nn1).getValue(), center.getCoordinates(), entries.getValue().getValue()) >= 0)
			{
				double d1 = Math.abs(skalarprod(classified.get(nn1).getValue(), center.getCoordinates(), entries.getValue().getValue()));
				double d2 = Math.abs(skalarprod(entries.getValue().getValue(),  center.getCoordinates(), classified.get(nn1).getValue()));

				double hValue  = (d1*d2)/(0.25*((d1+d2)*(d1+d2)));

				if(hValue > minDist)
				{
					minDist  = hValue;
					minIndex = entries.getKey();
				}
			}
		}

		if(minIndex > -1)
			nn2=minIndex;

		result[0] = nn1;
		result[1] = nn2;

		return result;
	}

	public double skalarprod(double[] a, double[] b, double[] m)
	{
		double sum = 0.0;

		for(int i = 0; i < b.length; i++)
		{
			sum+= (a[i]- b[i])*(a[i] - m[i]);
		}
		return sum;
	}
}