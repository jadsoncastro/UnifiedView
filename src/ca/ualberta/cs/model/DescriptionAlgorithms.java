package ca.ualberta.cs.model;

public class DescriptionAlgorithms 
{
	//Algorithm to compare with our winner algorithm
	public String  rmgtMKNN			   		   = "RMGT";
	public String  harMKNN					   = "GFHF";
	public String  lapSVM					   = "LapSVM";

	// Main algorithms
	public String  hisscluClassBased   		   = "HISSCLU"; // To plot the clustering results
	public String  hisscluClusterBased     	   = "K-Cluster";
	public String  ssdbscan					   = "SSDBSCAN";
	public String  ssdbscanLelis			   = "SSDBSCANLelis";
//	public String  ssdbscanCandy			   = "SSDBSCANCandy";


	// Semi-supervised classification algorithms
	public String  hdbscanCd	    = "HDBSCAN*(cd,-)";
	public String  hdbscanAp		= "HDBSCAN*(ap,-)";
	public String  hdbscanCdWMST    = "HDBSCAN*(cd,wMST)";
	public String  hdbscanApWMST    = "HDBSCAN*(ap,wMST)";
	public String  hdbscanCdWPWD    = "HDBSCAN*(cd,wPWD)"; // New definition for HISSCLU
	public String  hdbscanApWPWD    = "HDBSCAN*(ap,wPWD)";


	// Unsupervised algorithms
	public String  hdbscanStarAp	= "HDBSCAN*(UN,AP)";
	public String  hdbscanStarCd	= "HDBSCAN*(UN,CD)";

	// Algorithms that apply the Precision BCubed function
	public String  hdbscanStarBCubedAp    = "HDBSCAN*(BC,AP)";
	public String  hdbscanStarBCubedCd    = "HDBSCAN*(BC,CD)";

	public String  hdbscanStarMixedAp     = "HDBSCAN*(MixBC,AP)";
	public String  hdbscanStarMixedCd     = "HDBSCAN*(MixBC,CD)";

	// HDBSCAN* with constraints
	public String  hdbscanConstraintsAp   = "HDBSCAN*(CON,AP)";
	public String  hdbscanConstraintsCd   = "HDBSCAN*(CON,CD)";

	public String  hdbscanStarMixedForConstraintsAp   = "HDBSCAN*(MixCON-AP)";
	public String  hdbscanStarMixedForConstraintsCd   = "HDBSCAN*(MixCON-CD)";


	// Validation indexes
	public String  ARI      	 	 = "ARI";
	public String  weightedARI		 = "Weighted-ARI";
	public String  FMClassification  = "F-Measure";

	// Time registers
	public String  wholeProcess 	 = "wholeProcess"; // Take the whole time to run the algorithm.
	public String  graphConstruction = "graphConstruction"; // Take into account the time to construct the graphs
	public String  timeToPropagate   = "timeToPropagate"; // Take into account just the time to propagate the labels
	public String  weightFunction    = "weightFunction"; // Take into account the time to apply the weighting funciton
}