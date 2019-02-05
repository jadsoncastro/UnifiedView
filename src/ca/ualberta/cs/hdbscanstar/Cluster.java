package ca.ualberta.cs.hdbscanstar;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;


/**
 * An HDBSCAN* cluster, which will have a birth level, death level, stability, and constraint 
 * satisfaction once fully constructed.
 * @author zjullion
 * @author jadson
 */
public class Cluster implements java.io.Serializable
{

	private static final long serialVersionUID = 2L;

	// ------------------------------ PRIVATE VARIABLES ------------------------------	
	private int label;
	private double birthLevel;
	private double deathLevel;
	private int numPoints;
	private long fileOffset;	//First level where points with this cluster's label appear

	private double stability;
	private double propagatedStability;

	private double propagatedLowestChildDeathLevel;

	private int numConstraintsSatisfied;
	private int propagatedNumConstraintsSatisfied;
	private TreeSet<Integer> virtualChildCluster;

	// Attribute included by @author jadson
	private TreeMap<Integer, Integer> classDistributionInNode;	// <Integer, Integer> == <class, count of objects of that class>
	private TreeMap<Integer, Integer> classDistributionInVirtualChild;

	// Attribute included by @author jadson
	private int numberOfLabeledInNode;

	/*Attribute included by @author jadson
	 * Define the variable to store the Bcubed of a cluster with respect to the labeled objects
	 */
	private double bCubed;
	private double propagatedBCubed;

	//Define the mixed stability function (stability and bcubed index) (Attribute included by @author jadson)
	private double mixStabilityBcubed;
	private double propagatedMixStabilityBcubed;	

	//Define the mix between stability and constraints
	private double mixStabilityConstraints;
	private double propagatedMixStabilityConstraints;	

	
	private Cluster parent;
	private boolean hasChildren;
	public ArrayList<Cluster> propagatedDescendants;

	private TreeSet<Integer> children;

	//The attribute below (objects) was created by Fernando S. de Aguiar Neto
	private TreeSet<Integer> objects; //Objects that belong to this cluster i.e. become noise before/at the death level of this cluster.


	// ------------------------------ CONSTANTS ------------------------------

	// ---------------------------- CONSTRUCTORS ------------------------------

	/**
	 * Creates a new Cluster.
	 * @param label The cluster label, which should be globally unique
	 * @param parent The cluster which split to create this cluster
	 * @param birthLevel The MST edge level at which this cluster first appeared
	 * @param numPoints The initial number of points in this cluster
	 */
	public Cluster(int label, Cluster parent, double birthLevel, int numPoints) 
	{
		this.label = label;
		this.birthLevel = birthLevel;
		this.deathLevel = 0;
		this.numPoints = numPoints;
		this.fileOffset = 0;

		this.stability = 0;
		this.propagatedStability = 0;

		this.propagatedLowestChildDeathLevel = Double.MAX_VALUE;

		this.numConstraintsSatisfied = 0;
		this.propagatedNumConstraintsSatisfied = 0;
		this.virtualChildCluster = new TreeSet<Integer>();

		this.bCubed= 0.0;
		this.propagatedBCubed = 0.0;

		this.classDistributionInNode = new TreeMap<Integer, Integer>();
		this.classDistributionInVirtualChild = new TreeMap<Integer, Integer>();

		this.numberOfLabeledInNode = 0;

		this.mixStabilityBcubed =0.0;
		this.propagatedMixStabilityBcubed = 0.0;

		this.mixStabilityConstraints = 0.0;
		this.propagatedMixStabilityConstraints = 0.0;

		this.objects = new TreeSet<Integer>();

		this.parent = parent;
		if (this.parent != null)
			this.parent.hasChildren = true;

		this.hasChildren = false;
		this.propagatedDescendants = new ArrayList<Cluster>(1);

		this.children = new TreeSet<Integer>();
	}


	// ------------------------------ PUBLIC METHODS ------------------------------

	/**
	 * Removes the specified number of points from this cluster at the given edge level, which will
	 * update the stability of this cluster and potentially cause cluster death.  If cluster death
	 * occurs, the number of constraints satisfied by the virtual child cluster will also be calculated.
	 * @param numPoints The number of points to remove from the cluster
	 * @param level The MST edge level at which to remove these points
	 */
	public void detachPoints(int numPoints, double level) 
	{
		this.numPoints-=numPoints;
		this.stability+=(numPoints * (1/level - 1/this.birthLevel));

		if (this.numPoints == 0)
			this.deathLevel = level;
		else if (this.numPoints < 0)
			throw new IllegalStateException("Cluster cannot have less than 0 points.");
	}

	/**
	 * This cluster will propagate itself to its parent if its number of satisfied constraints is
	 * higher than the number of propagated constraints.  Otherwise, this cluster propagates its
	 * propagated descendants.  In the case of ties, stability is examined.
	 * Additionally, this cluster propagates the lowest death level of any of its descendants to its
	 * parent.
	 */

	public void propagate()
	{
		if (this.parent != null)
		{

			//Propagate lowest death level of any descendants:
			if (this.propagatedLowestChildDeathLevel == Double.MAX_VALUE)
				this.propagatedLowestChildDeathLevel = this.deathLevel;

			if (this.propagatedLowestChildDeathLevel < this.parent.propagatedLowestChildDeathLevel)
				this.parent.propagatedLowestChildDeathLevel = this.propagatedLowestChildDeathLevel;

			//If this cluster has no children, it must propagate itself:
			if (!this.hasChildren)
			{
				this.parent.propagatedNumConstraintsSatisfied+= this.numConstraintsSatisfied;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.numConstraintsSatisfied > this.propagatedNumConstraintsSatisfied) 
			{
				this.parent.propagatedNumConstraintsSatisfied+= this.numConstraintsSatisfied;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.numConstraintsSatisfied < this.propagatedNumConstraintsSatisfied)
			{
				this.parent.propagatedNumConstraintsSatisfied+= this.propagatedNumConstraintsSatisfied;
				this.parent.propagatedStability+= this.propagatedStability;
				this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
			}

			else if (this.numConstraintsSatisfied == this.propagatedNumConstraintsSatisfied) 
			{

				if (this.stability >= this.propagatedStability)
				{
					this.parent.propagatedNumConstraintsSatisfied+= this.numConstraintsSatisfied;
					this.parent.propagatedStability+= this.stability;
					this.parent.propagatedDescendants.add(this);
				}

				else 
				{
					this.parent.propagatedNumConstraintsSatisfied+= this.propagatedNumConstraintsSatisfied;
					this.parent.propagatedStability+= this.propagatedStability;
					this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
				}	
			}	
		}
	}


	//---------------------------------------------------------------------------------------------------

	/**
	 *Created by @jadson in 13/04/2017
	 * This cluster will propagate itself to its parent if  the consistency is
	 * higher than the propagated consistency.  Otherwise, this cluster propagates its
	 * propagated descendants.  In the case of ties, stability is examined.
	 * Additionally, this cluster propagates the lowest death level of any of its descendants to its
	 * parent.
	 */
	public void propagateBCubed()
	{
		if (this.parent != null) 
		{

			//Propagate lowest death level of any descendants:
			if (this.propagatedLowestChildDeathLevel == Double.MAX_VALUE)
				this.propagatedLowestChildDeathLevel = this.deathLevel;

			if (this.propagatedLowestChildDeathLevel < this.parent.propagatedLowestChildDeathLevel)
				this.parent.propagatedLowestChildDeathLevel = this.propagatedLowestChildDeathLevel;

			//If this cluster has no children, it must propagate itself:
			if (!this.hasChildren) 
			{
				this.parent.propagatedBCubed += this.bCubed;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.bCubed > this.propagatedBCubed) 
			{
				this.parent.propagatedBCubed += this.bCubed;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.bCubed < this.propagatedBCubed) 
			{
				this.parent.propagatedBCubed += this.propagatedBCubed;
				this.parent.propagatedStability+= this.propagatedStability;
				this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
			}
			else if (this.bCubed == this.propagatedBCubed) 
			{

				if (this.stability >= this.propagatedStability)
				{
					this.parent.propagatedBCubed += this.bCubed;
					this.parent.propagatedStability+= this.stability;
					this.parent.propagatedDescendants.add(this);
				}

				else 
				{
					this.parent.propagatedBCubed += this.propagatedBCubed;
					this.parent.propagatedStability+= this.propagatedStability;
					this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
				}	
			}	
		}
	}



	/**
	 * Created by Jadson Castro Gertrudes
	 * This cluster will propagate itself to its parent if its purity index is
	 * higher or equal than the purity of propagated ones.  Otherwise, this cluster propagates its
	 * propagated descendants.
	 */

	public void propagateMixStabilityBCubed()
	{
		if (this.parent != null)
		{
			//Propagate lowest death level of any descendants:
			if (this.propagatedLowestChildDeathLevel == Double.MAX_VALUE)
				this.propagatedLowestChildDeathLevel = this.deathLevel;

			if (this.propagatedLowestChildDeathLevel < this.parent.propagatedLowestChildDeathLevel)
				this.parent.propagatedLowestChildDeathLevel = this.propagatedLowestChildDeathLevel;

			//If this cluster has no children, it must propagate itself:
			if (!this.hasChildren) 
			{
				this.parent.propagatedMixStabilityBcubed+= this.mixStabilityBcubed;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}
			else if (this.mixStabilityBcubed > this.propagatedMixStabilityBcubed)
			{
				this.parent.propagatedMixStabilityBcubed += this.mixStabilityBcubed;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.mixStabilityBcubed < this.propagatedMixStabilityBcubed)
			{
				this.parent.propagatedMixStabilityBcubed += this.propagatedMixStabilityBcubed;
				this.parent.propagatedStability+= this.propagatedStability;
				this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
			}

			else if (this.mixStabilityBcubed == this.propagatedMixStabilityBcubed) 
			{

				if (this.stability >= this.propagatedStability) 
				{
					this.parent.propagatedStability+= this.stability;
					this.parent.propagatedDescendants.add(this);
				}

				else 
				{
					this.parent.propagatedMixStabilityBcubed+= this.propagatedMixStabilityBcubed;
					this.parent.propagatedStability+= this.propagatedStability;
					this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
				}	
			}	
		}
	}	


	/**
	 * Created by Jadson Castro Gertrudes
	 * This cluster will propagate itself to its parent if its purity index is
	 * higher or equal than the purity of propagated ones.  Otherwise, this cluster propagates its
	 * propagated descendants.
	 * TODO: update function to store the propagated purity and the propagated stability
	 */

	public void propagateMixStabilityConstraints()
	{
		if (this.parent != null) 
		{

			//Propagate lowest death level of any descendants:
			if (this.propagatedLowestChildDeathLevel == Double.MAX_VALUE)
				this.propagatedLowestChildDeathLevel = this.deathLevel;

			if (this.propagatedLowestChildDeathLevel < this.parent.propagatedLowestChildDeathLevel)
				this.parent.propagatedLowestChildDeathLevel = this.propagatedLowestChildDeathLevel;

			//If this cluster has no children, it must propagate itself:
			if (!this.hasChildren) 
			{
				this.parent.propagatedMixStabilityConstraints += this.mixStabilityConstraints;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}
			else if (this.mixStabilityConstraints > this.propagatedMixStabilityConstraints)
			{
				this.parent.propagatedMixStabilityConstraints += this.mixStabilityConstraints;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.mixStabilityConstraints< this.propagatedMixStabilityConstraints) {
				this.parent.propagatedMixStabilityConstraints += this.propagatedMixStabilityConstraints;
				this.parent.propagatedStability+= this.propagatedStability;
				this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
			}

			else if (this.mixStabilityConstraints == this.propagatedMixStabilityConstraints) 
			{

				if (this.stability >= this.propagatedStability) 
				{
					this.parent.propagatedStability+= this.stability;
					this.parent.propagatedDescendants.add(this);
				}

				else{
					this.parent.propagatedMixStabilityConstraints += this.propagatedMixStabilityConstraints;
					this.parent.propagatedStability+= this.propagatedStability;
					this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
				}	
			}	
		}
	}

	//------------------------------------------------------------------------------

	public void addPointsToVirtualChildCluster(TreeSet<Integer> points) {
		this.virtualChildCluster.addAll(points);
	}


	public boolean virtualChildClusterContaintsPoint(int point) {
		return this.virtualChildCluster.contains(point);
	}


	public void addVirtualChildConstraintsSatisfied(int numConstraints) {
		this.propagatedNumConstraintsSatisfied+= numConstraints;
	}


	public void addConstraintsSatisfied(int numConstraints) {
		this.numConstraintsSatisfied+= numConstraints;
	}

	public void addChild(Integer ch)
	{
		this.children.add(ch);
	}

	/**
	 * Sets the virtual child cluster to null, thereby saving memory.  Only call this method after computing the
	 * number of constraints satisfied by the virtual child cluster.
	 */
	public void releaseVirtualChildCluster(){
		this.virtualChildCluster = null;
	}


	public void addClassInformation(Entry<Integer, Integer> labeledObject)
	{
		Integer classId = labeledObject.getValue();
		Integer idObj = labeledObject.getKey();

		this.numberOfLabeledInNode +=1; // update the number of labeled objects in the node

		if(this.classDistributionInNode.containsKey(classId))
		{
			int count = classDistributionInNode.get(classId);
			count += 1;
			this.classDistributionInNode.put(classId, count);
		}
		else
		{
			this.classDistributionInNode.put(classId, 1);
		}

		if(this.virtualChildCluster.contains(idObj)) // repeat the same process of a valid cluster
		{

			if(this.classDistributionInVirtualChild.containsKey(classId))
			{
				int count = classDistributionInVirtualChild.get(classId);
				count += 1;
				this.classDistributionInVirtualChild.put(classId, count);
			}
			else
			{
				this.classDistributionInVirtualChild.put(classId, 1);
			}

		}
	}


	/**
	 * computeBcubedIndex
	 * @param totalOfLabeled
	 * @param classDist
	 * This method compute the bcubed index given the distribution of labeled objects in the cluster
	 * It is worth to remember that we will consider each labeled object in the virtual node as a singleton.
	 * This version considers the minimum/fmeasure between the precision and the recall of each object in the cluster!!!!.
	 */
	public void computeBCubedIndex(int totalOfLabeled, TreeMap<Integer, Integer> classDist)
	{
		//		System.out.println("Computing BCubed for cluster " + this.label + " total of labeled objects: " + totalOfLabeled + " class distribution: " + this.classDistributionInNode);
		if(this.classDistributionInNode.isEmpty())
			return;

		for(Map.Entry<Integer, Integer> entry: this.classDistributionInNode.entrySet())
		{
			int totalClassLabel = classDist.get(entry.getKey());
			int count = entry.getValue();
			double precisionObject = ((double)count/numberOfLabeledInNode)/totalOfLabeled;

			double recallObject = ((double)count/totalClassLabel)/totalOfLabeled;
			this.bCubed += count*(2.0/(1.0/precisionObject + 1.0/recallObject)); // F-measure

//			System.out.println(String.format(Locale.CANADA, "Cluster %d, count: %d, precision object: %.3f, recall object: %.3f", this.label, count, precisionObject, recallObject));
		}

		//		System.out.println("index set for cluster " + this.label + ": "+ String.format(Locale.CANADA, "%.3f", this.bCubed));

		if(!this.classDistributionInVirtualChild.isEmpty() && this.hasChildren) // if the cluster is not a leaf cluster, we need to include the index value of the virtual children
		{
//			System.out.println("cluster id: "+this.label+ " Distribution of the virtual child " + classDistributionInVirtualChild);

			for(Map.Entry<Integer, Integer> entry: this.classDistributionInVirtualChild.entrySet())
			{
				int count = entry.getValue();
				int totalClassLabel = classDist.get(entry.getKey());

				double precision = 1.0/totalOfLabeled;
				double recall = (1.0/totalClassLabel)/totalOfLabeled; 
				this.propagatedBCubed += count*(2.0/(1.0/precision + 1.0/recall)); // F-measure
			}
			//			System.out.println("Including index of virtual node, cluster " + this.label + ", parent  " + this.parent.label + ": "+ String.format(Locale.CANADA, "%.3f", this.propagatedBCubed) + ".");
		}
	}


	public void releaseCluster() 
	{	
		this.propagatedStability = 0;
		this.propagatedLowestChildDeathLevel = Double.MAX_VALUE;

		this.numConstraintsSatisfied = 0;
		this.propagatedNumConstraintsSatisfied = 0;

		this.bCubed = 0.0;
		this.propagatedBCubed= 0.0;

		this.mixStabilityBcubed = 0.0;
		this.propagatedMixStabilityBcubed=0.0;

		this.mixStabilityConstraints = 0.0;
		this.propagatedMixStabilityConstraints = 0.0;

		this.classDistributionInNode = new TreeMap<Integer, Integer>();
		this.classDistributionInVirtualChild = new TreeMap<Integer, Integer>();

		this.numberOfLabeledInNode = 0;
		this.propagatedDescendants   = new ArrayList<Cluster>(1);


	}

	// ------------------------------ PRIVATE METHODS ------------------------------

	// ------------------------------ GETTERS & SETTERS ------------------------------

	public int getLabel() {
		return this.label;
	}

	public Cluster getParent() {
		return this.parent;
	}

	public double getBirthLevel() {
		return this.birthLevel;
	}

	public double getDeathLevel() {
		return this.deathLevel;
	}

	public long getFileOffset() {
		return this.fileOffset;
	}

	public void setFileOffset(long offset) {
		this.fileOffset = offset;
	}

	public double getStability() {
		return this.stability;
	}

	public double getPropagatedStability()
	{
		return this.propagatedStability;
	}

	public double getPropagatedLowestChildDeathLevel() {
		return this.propagatedLowestChildDeathLevel;
	}

	public int getNumConstraintsSatisfied() {
		return this.numConstraintsSatisfied;
	}

	public int getPropagatedNumConstraintsSatisfied() {
		return this.propagatedNumConstraintsSatisfied;
	}

	public double getBCubedAtCluster()
	{
		return this.bCubed;
	}

	public double getPropagatedBCubed()
	{
		return this.propagatedBCubed;
	}

	public void setBCubedPrecision(double bcubed)
	{
		this.bCubed = bcubed;
	}

	public double getMixStabilityBcubed()
	{
		return this.mixStabilityBcubed;
	}

	public double getPropagatedMixdStabilityBcubed()
	{
		return this.propagatedMixStabilityBcubed;
	}

	public double getPropagatedMixStabilityConstraints()
	{
		return this.propagatedMixStabilityConstraints;
	}

	public void setMixedStability(double alpha, double maxPropagatedStability, double maxBcubed)
	{
		this.mixStabilityBcubed = alpha*(this.stability/maxPropagatedStability) + (1-alpha)*(this.bCubed/maxBcubed);
		this.propagatedMixStabilityBcubed = (1-alpha)*(this.propagatedMixStabilityBcubed/maxBcubed);
	}

	/**
	 * @author jadson
	 * Function to compute the mixed function for the constrained based HDBSCAN*
	 * The function receives the total number of constraints available to perform
	 * the normalization of the number of constraints satisfactions.
	 * @param alpha
	 * @param maxPropagatedStability
	 * @param numOfConstraints
	 */
	public void setMixedForConstraint(double alpha, double maxPropagatedStability, int numOfConstraints)
	{
		this.mixStabilityConstraints= alpha*(this.stability/maxPropagatedStability) + (1-alpha)*((double)this.numConstraintsSatisfied/(2.0 * numOfConstraints));
		this.propagatedMixStabilityConstraints = (1-alpha)*((double)this.propagatedNumConstraintsSatisfied/(2.0 * numOfConstraints));
	}

	public ArrayList<Cluster> getPropagatedDescendants() 
	{
		return this.propagatedDescendants;
	}

	public boolean hasChildren() 
	{
		return this.hasChildren;
	}

	public TreeSet<Integer> getChildren()
	{
		return this.children;
	}

	public TreeSet<Integer> getObjects()
	{
		return this.objects;
	}

	public void setObjects(TreeSet<Integer> objects)
	{
		this.objects = objects;
	}

	public TreeMap<Integer, Integer> getClassDistribution()
	{
		return this.classDistributionInNode;
	}

	public String toString()
	{
		if(this.parent !=null)
			return "\n Id: " + this.label + " Parent: " + this.parent.label + ". label distribution" + this.classDistributionInNode + " virt.Child" + this.classDistributionInVirtualChild + " Stability: " + String.format(Locale.ENGLISH, "%.3f", this.stability) + " Constraints: "+ this.numConstraintsSatisfied + " propagated constraints satisfied: " + this.propagatedNumConstraintsSatisfied + " Local BCubed: " + String.format(Locale.ENGLISH, "%.3f", this.bCubed)+ " Prop. BCubed: " + String.format(Locale.ENGLISH, "%.3f", this.propagatedBCubed) + " isLeaf? " + !this.hasChildren + " Children: " + this.children + "(MIX-Const: " + String.format(Locale.ENGLISH, "%.2f", this.mixStabilityConstraints) + ", Mix-Consist: " + String.format(Locale.ENGLISH, "%.2f", this.mixStabilityBcubed) + ")"+ " Death level: " + this.deathLevel;
		else
			return "\n Id: " + this.label + " Parent: null" + ". label distribution" + this.classDistributionInNode + " virt.Child" + this.classDistributionInVirtualChild + " Stability: " + String.format(Locale.ENGLISH, "%.3f", this.stability)+ " Constraints: "+ this.numConstraintsSatisfied + " Local BCubed: " + String.format(Locale.ENGLISH, "%.3f", this.bCubed)+ " Prop. BCubed: " + String.format(Locale.ENGLISH, "%.3f", this.propagatedBCubed) + " isLeaf? " + !this.hasChildren + " Children: " + this.children + "(MIX-Const: " + String.format(Locale.ENGLISH, "%.3f", this.mixStabilityConstraints) + ", Mix-Consist: " + String.format(Locale.ENGLISH, "%.3f", this.mixStabilityBcubed) + ")" + " Death level: " + this.deathLevel;
	}
}