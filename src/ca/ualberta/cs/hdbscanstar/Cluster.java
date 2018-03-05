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
 * @author jadson (updated in 24/09/2016)
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


	private TreeMap<Integer, TreeSet<Integer>> preLabeledObjectsInNode;


	//Define the variable to store the consistency of a cluster with respect to the labeled objects
	private double consistencyIndex;
	private double propagatedConsistencyIndex;	


	//Define the mixed stability function (stability and consistency index)
	private double mixedStability;
	private double propagatedMixedStability;	

	//Define the mixed stability function for the constraint based approach (stability and consistency index)
	private double mixedForConstraints;
	private double propagatedMixedForConstraints;	


	private Cluster parent;
	private boolean hasChildren;
	public ArrayList<Cluster> propagatedDescendants;

	// Attribute include by @author jadson
	private TreeSet<Integer> objectsAtBirthLevel;
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
	public Cluster(int label, Cluster parent, double birthLevel, int numPoints, TreeSet<Integer> pointsAtBirthLevel) 
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

		this.consistencyIndex = 0.0;
		this.propagatedConsistencyIndex= 0.0;

		this.preLabeledObjectsInNode = new TreeMap<Integer, TreeSet<Integer>>();

		this.objects = new TreeSet<Integer>();

		this.parent = parent;
		if (this.parent != null)
			this.parent.hasChildren = true;

		this.hasChildren = false;
		this.propagatedDescendants = new ArrayList<Cluster>(1);

		this.children = new TreeSet<Integer>();
		this.objectsAtBirthLevel = pointsAtBirthLevel;
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
		if (this.parent != null) {

			//Propagate lowest death level of any descendants:
			if (this.propagatedLowestChildDeathLevel == Double.MAX_VALUE)
				this.propagatedLowestChildDeathLevel = this.deathLevel;

			if (this.propagatedLowestChildDeathLevel < this.parent.propagatedLowestChildDeathLevel)
				this.parent.propagatedLowestChildDeathLevel = this.propagatedLowestChildDeathLevel;

			//If this cluster has no children, it must propagate itself:
			if (!this.hasChildren) {
				this.parent.propagatedNumConstraintsSatisfied+= this.numConstraintsSatisfied;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.numConstraintsSatisfied > this.propagatedNumConstraintsSatisfied) {
				this.parent.propagatedNumConstraintsSatisfied+= this.numConstraintsSatisfied;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.numConstraintsSatisfied < this.propagatedNumConstraintsSatisfied) {
				this.parent.propagatedNumConstraintsSatisfied+= this.propagatedNumConstraintsSatisfied;
				this.parent.propagatedStability+= this.propagatedStability;
				this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
			}

			else if (this.numConstraintsSatisfied == this.propagatedNumConstraintsSatisfied) 
			{

				if (this.stability >= this.propagatedStability) {
					this.parent.propagatedNumConstraintsSatisfied+= this.numConstraintsSatisfied;
					this.parent.propagatedStability+= this.stability;
					this.parent.propagatedDescendants.add(this);
				}

				else {
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
	public void propagateSupervised()
	{
		if (this.parent != null) {

			//Propagate lowest death level of any descendants:
			if (this.propagatedLowestChildDeathLevel == Double.MAX_VALUE)
				this.propagatedLowestChildDeathLevel = this.deathLevel;

			if (this.propagatedLowestChildDeathLevel < this.parent.propagatedLowestChildDeathLevel)
				this.parent.propagatedLowestChildDeathLevel = this.propagatedLowestChildDeathLevel;

			//If this cluster has no children, it must propagate itself:
			if (!this.hasChildren) {
				this.parent.propagatedConsistencyIndex+= this.consistencyIndex;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.consistencyIndex > this.propagatedConsistencyIndex) {
				this.parent.propagatedConsistencyIndex+= this.consistencyIndex;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.consistencyIndex < this.propagatedConsistencyIndex) {
				this.parent.propagatedConsistencyIndex+= this.propagatedConsistencyIndex;
				this.parent.propagatedStability+= this.propagatedStability;
				this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
			}

			else if (this.consistencyIndex == this.propagatedConsistencyIndex) 
			{

				if (this.stability >= this.propagatedStability) {
					this.parent.propagatedStability+= this.stability;
					this.parent.propagatedDescendants.add(this);
				}

				else {
					this.parent.propagatedConsistencyIndex+= this.propagatedConsistencyIndex;
					this.parent.propagatedStability+= this.propagatedStability;
					this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
				}	
			}	
		}
	}	



	/**
	 * Created by Jadson Castro Gertrudes 26/09/2016
	 * This cluster will propagate itself to its parent if its purity index is
	 * higher or equal than the purity of propagated ones.  Otherwise, this cluster propagates its
	 * propagated descendants.
	 * TODO: update function to store the propagated purity and the propagated stability
	 */

	public void propagateMixed()
	{
		if (this.parent != null) {

			//Propagate lowest death level of any descendants:
			if (this.propagatedLowestChildDeathLevel == Double.MAX_VALUE)
				this.propagatedLowestChildDeathLevel = this.deathLevel;

			if (this.propagatedLowestChildDeathLevel < this.parent.propagatedLowestChildDeathLevel)
				this.parent.propagatedLowestChildDeathLevel = this.propagatedLowestChildDeathLevel;

			//If this cluster has no children, it must propagate itself:
			if (!this.hasChildren) {
				this.parent.propagatedMixedStability+= this.mixedStability;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}
			else if (this.mixedStability > this.propagatedMixedStability)
			{
				this.parent.propagatedMixedStability += this.mixedStability;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.mixedStability < this.propagatedMixedStability) {
				this.parent.propagatedMixedStability += this.propagatedMixedStability;
				this.parent.propagatedStability+= this.propagatedStability;
				this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
			}

			else if (this.mixedStability == this.propagatedMixedStability) 
			{

				if (this.stability >= this.propagatedStability) {
					this.parent.propagatedStability+= this.stability;
					this.parent.propagatedDescendants.add(this);
				}

				else {
					this.parent.propagatedMixedStability+= this.propagatedMixedStability;
					this.parent.propagatedStability+= this.propagatedStability;
					this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
				}	
			}	
		}
	}	


	/**
	 * Created by Jadson Castro Gertrudes 11/06/2017
	 * This cluster will propagate itself to its parent if its purity index is
	 * higher or equal than the purity of propagated ones.  Otherwise, this cluster propagates its
	 * propagated descendants.
	 * TODO: update function to store the propagated purity and the propagated stability
	 */

	public void propagateMixedForConstraints()
	{
		if (this.parent != null) {

			//Propagate lowest death level of any descendants:
			if (this.propagatedLowestChildDeathLevel == Double.MAX_VALUE)
				this.propagatedLowestChildDeathLevel = this.deathLevel;

			if (this.propagatedLowestChildDeathLevel < this.parent.propagatedLowestChildDeathLevel)
				this.parent.propagatedLowestChildDeathLevel = this.propagatedLowestChildDeathLevel;

			//If this cluster has no children, it must propagate itself:
			if (!this.hasChildren) {
				this.parent.propagatedMixedForConstraints += this.mixedForConstraints;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}
			else if (this.mixedForConstraints > this.propagatedMixedForConstraints)
			{
				this.parent.propagatedMixedForConstraints += this.mixedForConstraints;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.mixedForConstraints < this.propagatedMixedForConstraints) {
				this.parent.propagatedMixedForConstraints += this.propagatedMixedForConstraints;
				this.parent.propagatedStability+= this.propagatedStability;
				this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
			}

			else if (this.mixedForConstraints == this.propagatedMixedForConstraints) 
			{

				if (this.stability >= this.propagatedStability) {
					this.parent.propagatedStability+= this.stability;
					this.parent.propagatedDescendants.add(this);
				}

				else {
					this.parent.propagatedMixedForConstraints += this.propagatedMixedForConstraints;
					this.parent.propagatedStability+= this.propagatedStability;
					this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
				}	
			}	
		}
	}	


	/**
	 * @author jadson
	 * This cluster will propagate itself to its parent if its number of satisfied constraints is
	 * higher than the number of propagated constraints.  Otherwise, this cluster propagates its
	 * propagated descendants.  In the case of ties, stability is examined.
	 * Additionally, this cluster propagates the lowest death level of any of its descendants to its
	 * parent.
	 */
	public void propagateSub(Cluster parent)
	{
		if(this.getLabel() != parent.getLabel()) 
		{

			//Propagate lowest death level of any descendants:
			if (this.propagatedLowestChildDeathLevel == Double.MAX_VALUE)
				this.propagatedLowestChildDeathLevel = this.deathLevel;

			if (this.propagatedLowestChildDeathLevel < this.parent.propagatedLowestChildDeathLevel)
				this.parent.propagatedLowestChildDeathLevel = this.propagatedLowestChildDeathLevel;

			//If this cluster has no children, it must propagate itself:
			if (!this.hasChildren) {
				this.parent.propagatedNumConstraintsSatisfied+= this.numConstraintsSatisfied;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.numConstraintsSatisfied > this.propagatedNumConstraintsSatisfied) {
				this.parent.propagatedNumConstraintsSatisfied+= this.numConstraintsSatisfied;
				this.parent.propagatedStability+= this.stability;
				this.parent.propagatedDescendants.add(this);
			}

			else if (this.numConstraintsSatisfied < this.propagatedNumConstraintsSatisfied) {
				this.parent.propagatedNumConstraintsSatisfied+= this.propagatedNumConstraintsSatisfied;
				this.parent.propagatedStability+= this.propagatedStability;
				this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
			}

			else if (this.numConstraintsSatisfied == this.propagatedNumConstraintsSatisfied) 
			{

				if (this.stability >= this.propagatedStability) {
					this.parent.propagatedNumConstraintsSatisfied+= this.numConstraintsSatisfied;
					this.parent.propagatedStability+= this.stability;
					this.parent.propagatedDescendants.add(this);
				}

				else {
					this.parent.propagatedNumConstraintsSatisfied+= this.propagatedNumConstraintsSatisfied;
					this.parent.propagatedStability+= this.propagatedStability;
					this.parent.propagatedDescendants.addAll(this.propagatedDescendants);
				}	
			}	
		}else
		{
			//Propagate lowest death level of any descendants:
			if (this.propagatedLowestChildDeathLevel == Double.MAX_VALUE)
				this.propagatedLowestChildDeathLevel = this.deathLevel;

			if (this.stability >= this.propagatedStability) 
			{
				this.propagatedNumConstraintsSatisfied = this.numConstraintsSatisfied;
				this.propagatedStability= this.stability;
				this.propagatedDescendants.clear();
				this.propagatedDescendants.add(this);
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
		if(this.preLabeledObjectsInNode.containsKey(labeledObject.getValue()))
			this.preLabeledObjectsInNode.get(labeledObject.getValue()).add(labeledObject.getKey());
		else
		{
			this.preLabeledObjectsInNode.put(labeledObject.getValue(), new TreeSet<Integer>());
			this.preLabeledObjectsInNode.get(labeledObject.getValue()).add(labeledObject.getKey());
		}
	}


	public void releaseCluster() 
	{	
		this.propagatedStability = 0;
		this.propagatedLowestChildDeathLevel = Double.MAX_VALUE;

		this.numConstraintsSatisfied = 0;
		this.propagatedNumConstraintsSatisfied = 0;
		//		this.virtualChildCluster = new TreeSet<Integer>();

		this.consistencyIndex = 0.0;
		this.propagatedConsistencyIndex = 0.0;

		this.mixedStability = 0.0;
		this.propagatedMixedStability=0.0;
		
		this.mixedForConstraints = 0.0;
		this.propagatedMixedForConstraints = 0.0;

		this.preLabeledObjectsInNode = new TreeMap<Integer, TreeSet<Integer>>();
		this.propagatedDescendants = new ArrayList<Cluster>(1);
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

	public double getConsistencyIndex()
	{
		return this.consistencyIndex;
	}

	public double getPropagatedConsistencyIndex()
	{
		return this.propagatedConsistencyIndex;
	}

	public void setConsistencyIndex(double consistency)
	{
		this.consistencyIndex = consistency;
	}


	public double getMixedStability()
	{
		return this.mixedStability;
	}

	public double getPropagatedMixedStability()
	{
		return this.propagatedMixedStability;
	}
	
	public double getPropagatedMixedForConstraints()
	{
		return this.propagatedMixedForConstraints;
	}

	public void setMixedStability(double alpha, double maxPropagatedStability)
	{
		this.mixedStability = alpha*(this.stability/maxPropagatedStability) + (1-alpha)*(this.consistencyIndex);
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
		this.mixedForConstraints= alpha*(this.stability/maxPropagatedStability) + (1-alpha)*((double)this.numConstraintsSatisfied/(2.0 * numOfConstraints));
		this.propagatedMixedForConstraints = (1-alpha)*((double)this.propagatedNumConstraintsSatisfied/(2.0 * numOfConstraints));
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

	public TreeSet<Integer> getObjectsAtBirthLevel()
	{
		return this.objectsAtBirthLevel;
	}

	public TreeSet<Integer> getObjects()
	{
		return this.objects;
	}

	public void setObjects(TreeSet<Integer> objects)
	{
		this.objects = objects;
	}

	public TreeMap<Integer, TreeSet<Integer>> getClassInformation()
	{
		return this.preLabeledObjectsInNode;
	}

	public String toString()
	{
		if(this.parent !=null)
			return "Id: " + this.label + " Parent: " + this.parent.label + " Stability: " + String.format(Locale.ENGLISH, "%.2f", this.stability) + " Constraints: "+ this.numConstraintsSatisfied + " Consistency: " + String.format(Locale.ENGLISH, "%.2f", this.consistencyIndex) + " isLeaf? " + !this.hasChildren + " Children: " + this.children + "(MIX-Const: " + String.format(Locale.ENGLISH, "%.2f", this.mixedForConstraints) + ", Mix-Consist: " + String.format(Locale.ENGLISH, "%.2f", this.mixedStability) + ")";
		else
			return "Id: " + this.label + " Parent: null" + " Stability: " + String.format(Locale.ENGLISH, "%.2f", this.stability)+ " Constraints: "+ this.numConstraintsSatisfied + " Consistency: " + String.format(Locale.ENGLISH, "%.2f", this.consistencyIndex) + " isLeaf? " + !this.hasChildren + " Children: " + this.children + "(MIX-Const: " + String.format(Locale.ENGLISH, "%.2f", this.mixedForConstraints) + ", Mix-Consist: " + String.format(Locale.ENGLISH, "%.2f", this.mixedStability) + ")";
	}
}