

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SHM.HMatrix;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

/**
 *
 * @author Fernando Soares de Aguiar Neto.
 */
public class ObjInstance implements java.io.Serializable{
    public final Integer NOISE = 0; //Cluster ID associated to noise.
    
    private Integer id; //holds the ID of an object
    private String observation; //holds tips for the objects. (Image-link, description, etc.)
    private String observation2; //holds tips for the objects. (Image-link, description, etc.) Not in use by Plotter.
    private String observation3; //holds tips for the objects. (Image-link, description, etc.) Not in use by Plotter.
    private Integer label;      //holds the label of this object, default -1. Since labels you be proided by the user and by external algorithms.
    private TreeMap<Double, Integer> clusterIDs; //holds the cluseterId values attached to the highest density.
                                                  //Notice that you must put the noise densities into the map.
    private Double reachabilityDistance;        //Holds the reachability Distance value of this object
    private Double outlierScore;				//Holds the outlier Score given to this object after applying the HDBSCAN* algorithm
    private Double coreDistance;				//Holds the core distance of this object.
    
    private Double deathLevel;           //Holds the last density where this object is not a noise.
    
    private Integer HDBSCANPartition;			//Holds the partitioning given by HDBSCAN
    
    private static final long serialVersionUID = 7L;
    
    public ObjInstance(Integer id, String observation, String observation2, String observation3)
    {
        this.id = id;
        this.observation = observation;
        this.observation2 = observation2;
        this.observation3 = observation3;
        this.label = -1;
        this.clusterIDs = new TreeMap<Double, Integer>();
        this.reachabilityDistance = -1.0;
        this.outlierScore = -1.0;
        this.coreDistance = -1.0;
        this.HDBSCANPartition = -1;
        this.deathLevel = 0.0;
    }
    
    public ObjInstance(Integer id)
    {
        this.id = id;
        this.observation = "";
        this.observation2 = "";
        this.observation3 = "";
        this.label = -1;
        this.clusterIDs = new TreeMap<Double, Integer>();
        this.reachabilityDistance = -1.0;
        this.outlierScore = -1.0;
        this.coreDistance = -1.0;
        this.HDBSCANPartition = -1;
        this.deathLevel = 0.0;
    }
    
    public void updateDeathLevel()
    {
        //gets the Idx of the last index that is not noise.
        ArrayList<Double> keys = new ArrayList<Double>(this.clusterIDs.keySet());
        this.deathLevel = keys.get(1);
    }
    
    public double getDeathLevel()
    {
        return this.deathLevel;
    }
    
    public Integer getID()
    {
        return this.id;
    }
    
    public void setOutlierScore(Double outlierScore)
    {
    	this.outlierScore = outlierScore;
    }
    
    public Double getOutlierScore()
    {
    	return this.outlierScore;   	
    }
    
    public void setHDBSCANPartition(Integer HDBSCANPartition)
    {
        this.HDBSCANPartition = HDBSCANPartition;
    }
    
    public Integer getHDBSCANPartition()
    {
        return this.HDBSCANPartition;
    }
    
    public void setCoreDistance(Double coreDistance)
    {
    	this.coreDistance = coreDistance;
    }
    
    public Double getCoreDistance()
    {
    	return this.coreDistance;   	
    }
    
    public void setLabel(Integer label)
    {
        this.label = label;
    }
    
    public Integer getLabel()
    {
        return this.label;
    }
    
    public void setReachabilityDistance(Double reachabilityDistance)
    {
        this.reachabilityDistance = reachabilityDistance;
    }
    
    public Double getReachabilityDistance()
    {
        return this.reachabilityDistance;
    }
    
    public String getObservation()
    {
        return this.observation;
    }
    
    public String getObservation2()
    {
        return this.observation2;
    }
    
    public String getObservation3()
    {
        return this.observation3;
    }
    
    public void setObservation(String observation)
    {
        this.observation = observation;
    }
    
    public void setObservation2(String observation2)
    {
        this.observation2 = observation2;
    }
    
    public void setObservation3(String observation3)
    {
        this.observation3 = observation3;
    }
    
    public TreeMap<Double, Integer> getAllClusters()
    {
        return this.clusterIDs;
    }
    
    public Set<Double> getDensities()
    {
        return this.clusterIDs.keySet();
    }
    
    public Integer getClusterID(Double d)
    {
        return this.clusterIDs.getOrDefault(this.clusterIDs.floorKey(d), NOISE);
    }
    
    public void put(Double density, Integer clusterID)
    {
        this.clusterIDs.put(density, clusterID);
    }
    
    @Override
    public String toString()
    {
       String out = "ID:"+this.id+" OBS:"+this.observation+"| "+" OBS2:"+this.observation2+"| "+" OBS3:"+this.observation3+"| ";
       for(Double density : this.clusterIDs.keySet())
       {
           out += "[D="+density+" Cl="+this.clusterIDs.get(density)+"]";
       }
       return out;
    }
}
