package ca.ualberta.cs.distance;

/**
 * Computes the euclidean distance between two points, d = (x1-y1)^2 + (x2-y2)^2 + ... + (xn-yn)^2.
 * @author zjullion
 */
public class SquaredEuclideanDistance implements DistanceCalculator {

	// ------------------------------ PRIVATE VARIABLES ------------------------------

	// ------------------------------ CONSTANTS ------------------------------

	// ------------------------------ CONSTRUCTORS ------------------------------
	
	public SquaredEuclideanDistance() {
	}

	// ------------------------------ PUBLIC METHODS ------------------------------
	
	public double computeDistance(double[] attributesOne, double[] attributesTwo) {
		double distance = 0;
		
		for (int i = 0; i < attributesOne.length && i < attributesTwo.length; i++) {
			distance+= ((attributesOne[i] - attributesTwo[i]) * (attributesOne[i] - attributesTwo[i]));
		}
		
		return distance;
	}
	
	
	public String getName() {
		return "sqdeuclidean";
	}

	// ------------------------------ PRIVATE METHODS ------------------------------

	// ------------------------------ GETTERS & SETTERS ------------------------------

}