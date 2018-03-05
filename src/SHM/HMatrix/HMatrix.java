/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SHM.HMatrix;

import ca.ualberta.cs.distance.CosineSimilarity;
import ca.ualberta.cs.distance.DistanceCalculator;
import ca.ualberta.cs.distance.EuclideanDistance;
import ca.ualberta.cs.distance.ManhattanDistance;
import ca.ualberta.cs.distance.PearsonCorrelation;
import ca.ualberta.cs.distance.SupremumDistance;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 *
 * @author fsan
 */
public class HMatrix implements java.io.Serializable
{
    protected ArrayList<Double> densities;
    protected ArrayList<ObjInstance> matrix;
    protected Integer maxClusterID;
    protected Integer maxLabelValue;    //holds the maximum label value used it starts as -1 (unlabelled).
    protected Color[] colors;
    protected HashMap<Integer, Integer> objectOrderbyID;    //HM<ID, array position>, maps an ID to its position on the ArrayList.
    protected boolean isShaved;               //says if this file is a complete or a shaved hierarchy.
    
    private boolean hasReachabilities;		//says if the reachability distances where set before saving.
    private boolean infiniteStability;		//given from HDBSCAN*
    
    //Parameters used in HDBSCAN*
    private String param_inputFile;
    private Integer param_minClSize;
    private Integer param_minPts;
    private String param_distanceFunction;
    private boolean param_distanceMatrixUsed;
    private boolean isCompact;
    
    protected HashMap<Integer, Integer> lastClusters;   //holds the clusterId where the objects become noise.
    
    private static final long serialVersionUID = 10L;
    
    public HMatrix()
    {
        this.densities = new ArrayList<Double>();
        this.matrix = new ArrayList<ObjInstance>();
        this.objectOrderbyID = new HashMap<Integer, Integer>();
        this.lastClusters = new HashMap<Integer, Integer>();
        this.maxClusterID = 0;
        this.maxLabelValue = -1;
        this.isCompact = false;
        this.hasReachabilities = false;
        this.infiniteStability = false;
        
        this.param_minClSize = 1;
        this.param_minPts = 1;
        this.param_distanceFunction = "euclidean";
        this.param_distanceMatrixUsed  = false;
    }
    
    public HashMap<Integer, Integer> getLastClusters()
    {
        return this.lastClusters;
    }
    
    public void setLastClusters(HashMap<Integer, Integer> lastClusters)
    {
        this.lastClusters = lastClusters;
    }
    
    //this function must be called if the .vis file was loaded.
    public void updateLastClusters()
    {       
        for(ObjInstance obj : this.matrix)
        {
            obj.updateDeathLevel();
            //gets the Idx of the last index that is not noise.
            int lastCluster = obj.getAllClusters().get(obj.getDeathLevel());
            this.lastClusters.put(obj.getID(), lastCluster);                
        }
    }
    
    public void setVisParams(String inputFile, Integer minClSize, Integer minPts, String distanceFunction, boolean distanceMatrixUsed, boolean isCompact)
    {
        this.setParam_inputFile(inputFile);
        this.setParam_minClSize(minClSize);
        this.setParam_minPts(minPts);
        this.setParam_distanceFunction(distanceFunction);
        this.setParam_distanceMatrixUsed(distanceMatrixUsed);
        this.setIsCompact(isCompact);
    }
    
    public void setParam_inputFile(String inputFile)
    {
        this.param_inputFile = inputFile;
    }
            
    public String getParam_inputFile()
    {
        return this.param_inputFile;
    }
    
    public void setIsCompact(boolean isCompact)
    {
        this.isCompact = isCompact;
    }
            
    public boolean getIsCompact()
    {
        return this.isCompact;
    }
    
    public void setInfiniteStability(boolean infiniteStability)
    {
        this.infiniteStability = infiniteStability;
    }
            
    public boolean getInfiniteStability()
    {
        return this.infiniteStability;
    }
    
    public void setHasReachabilities(boolean hasReachabilities)
    {
        this.hasReachabilities = hasReachabilities;
    }
            
    public boolean hasReachabilities()
    {
        return this.hasReachabilities;
    }
    
    public Integer getPositionByID(Integer ID)
    {
        return this.objectOrderbyID.getOrDefault(ID, -1);
    }
    
    public void setIsShaved(boolean shaved)
    {
        this.isShaved = shaved;
    }
    
    public boolean getIsShaved()
    {   
        return this.isShaved;
    }
    
    public ArrayList<Double> getDensities()
    {
        return this.densities;
    }
    
    public ArrayList<ObjInstance> getMatrix()
    {
        return this.matrix;
    }
    
    public Integer getMaxClusterID()
    {
        return this.maxClusterID;
    }
    
    public Integer getMaxLabelValue()
    {
        return this.maxLabelValue;
    }
    
    //this method increases the MaxLabel to the value passed as parameter, only if its greater than the old value.
    public void increaseMaxLabelValue(int newValue)
    {
        this.maxLabelValue = Math.max(newValue, this.maxLabelValue);
    }
    
    //this method forces the maximum value to be the one passed as parameter.
    public void setMaxLabelValue(Integer maxLabelValue)
    {
        this.maxLabelValue = maxLabelValue;
    }
    
    public void setColor(Color[] colors)
    {
        this.colors = colors;
    }
    
    public Color[] getColors()
    {
        return this.colors;
    }
            
    
    public void setMaxClusterID(Integer maxClusterID)
    {
        this.maxClusterID = maxClusterID;
    }
    
    //Notice that it does not use the ID, but the index (order) used on the arrayList.
    //Also it expects the actual density, not an index.
    //it returns ObjInstance.NOISE when no key was found.
    public Integer getClusterID(int i, double density)
    {
        return this.matrix.get(i).getClusterID(density);        
    }
    
    //similar to the one above, the only difference is that it automatically gets the j-esimal density (greatest to lowest order)
    //Notice that this method does not assert the existance of any of the indexes (i,j) used. This must be handled by the developer.
    public Integer getClusterID(int i, int j)
    {
        return this.matrix.get(i).getClusterID(this.densities.get(j));
    }
    
    public void add(ObjInstance newOI)
    {
        this.matrix.add(newOI);
        this.objectOrderbyID.put(newOI.getID(), this.matrix.size()-1);
    }
    
    public ObjInstance getObjInstanceByID(Integer id)
    {
        return this.matrix.get(this.objectOrderbyID.get(id));
    }
    
    public ObjInstance getObjInstance(Integer i)
    {
        return this.matrix.get(i);
    }
    
    public Double getDensity(Integer i)
    {
        return this.densities.get(i);
    }
    
    public void lexicographicSort()
    {
        for(int i = this.densities.size()-1; i >= 0; --i) {
                        final double k = this.densities.get(i);
                        Collections.sort(this.matrix, new Comparator<ObjInstance>() {
                            @Override
                            public int compare(ObjInstance t1, ObjInstance t2) {
                                return Integer.compare(t2.getClusterID(k), t1.getClusterID(k));
                            }
                        });
                    }
        
        //Taking note of the new position of the object, this way you can find it by ID directly.
        for(int i =0; i< this.matrix.size(); i++)
        {
            this.objectOrderbyID.put(this.matrix.get(i).getID(), i);
        }
    }
    
    //This methos is called to be sure that no residual value from previous iterations still into this matrix.
    public void clearAll()
    {
        this.matrix.clear();
        this.densities.clear();
    }
    
    public void print()
    {
        System.out.println("Densities");
        System.out.println(this.densities.toString());
        
        System.out.println();
        System.out.println("H Matrix");
        for(int i=0 ; i<this.matrix.size() ; i++ )
        {
            System.out.println(this.matrix.get(i).toString());
        }
    }

    /**
     * @return the param_minClSize
     */
    public Integer getParam_minClSize() {
        return param_minClSize;
    }

    /**
     * @param param_minClSize the param_minClSize to set
     */
    public void setParam_minClSize(Integer param_minClSize) {
        this.param_minClSize = param_minClSize;
    }

    /**
     * @return the param_minPts
     */
    public Integer getParam_minPts() {
        return param_minPts;
    }

    /**
     * @param param_minPts the param_minPts to set
     */
    public void setParam_minPts(Integer param_minPts) {
        this.param_minPts = param_minPts;
    }

    /**
     * @return the param_distanceFunction
     */
    public DistanceCalculator getParam_distanceFunction() {
        final String EUCLIDEAN_DISTANCE = "euclidean";
	final String COSINE_SIMILARITY = "cosine";
	final String PEARSON_CORRELATION = "pearson";
	final String MANHATTAN_DISTANCE = "manhattan";
	final String SUPREMUM_DISTANCE = "supremum";
        
        if (this.param_distanceFunction.equals(EUCLIDEAN_DISTANCE))
        {
                return new EuclideanDistance();
        }
        else if (this.param_distanceFunction.equals(COSINE_SIMILARITY))
        {
                return new CosineSimilarity();
        }
        else if (this.param_distanceFunction.equals(PEARSON_CORRELATION))
        {
                return new PearsonCorrelation();
        }
        else if (this.param_distanceFunction.equals(MANHATTAN_DISTANCE))
        {
                return new ManhattanDistance();
        }
        else if (this.param_distanceFunction.equals(SUPREMUM_DISTANCE))
        {
                return new SupremumDistance();
        }
        else
                return null;

    }

    /**
     * @param param_distanceFunction the param_distanceFunction to set
     */
    public void setParam_distanceFunction(String param_distanceFunction) {
        this.param_distanceFunction = param_distanceFunction;
    }

    /**
     * @return the param_distanceMatrixUsed
     */
    public boolean isParam_distanceMatrixUsed() {
        return param_distanceMatrixUsed;
    }

    /**
     * @param param_distanceMatrixUsed the param_distanceMatrixUsed to set
     */
    public void setParam_distanceMatrixUsed(boolean param_distanceMatrixUsed) {
        this.param_distanceMatrixUsed = param_distanceMatrixUsed;
    }
    
}
