package ca.ualberta.cs.distance;

/**
 * Computes the tanimoto similarity between two points,
 * 
 * t = 1-(c/(r+d-c)), where
 * 
 * c is the number of bit one shared by both objects
 * r is the amount of bit one in object 1
 * d is the amount of bit one in object 2
 * 
 * @author jadson
 */
public class TanimotoSimilarity implements DistanceCalculator {

	// ------------------------------ PRIVATE VARIABLES ------------------------------

	// ------------------------------ CONSTANTS ------------------------------

	// ------------------------------ CONSTRUCTORS ------------------------------

	public TanimotoSimilarity() {
	}

	// ------------------------------ PUBLIC METHODS ------------------------------

	public double computeDistance(double[] attributesOne, double[] attributesTwo)
	{
		double distance = 0;
		double r =0;
		double d =0;
		double c =0;


		for (int i = 0; i < attributesOne.length && i < attributesTwo.length; i++) 
		{
			r+= attributesOne[i]*attributesOne[i];
			d+= attributesTwo[i]*attributesTwo[i];
			c+= attributesOne[i]*attributesTwo[i];
		}

		distance = 1.0-(c/(r+d-c));
		return distance;
	}


	public String getName() {
		return "tanimoto";
	}

	// ------------------------------ PRIVATE METHODS ------------------------------

	// ------------------------------ GETTERS & SETTERS ------------------------------

}
