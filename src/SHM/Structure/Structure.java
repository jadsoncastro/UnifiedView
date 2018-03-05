/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package SHM.Structure;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import SHM.HMatrix.HMatrix;
import ca.ualberta.cs.hdbscanstar.Cluster;
import ca.ualberta.cs.hdbscanstar.UndirectedGraph;

/**
 *
 * @author fsan
 */
public class Structure implements java.io.Serializable{
    
    private static final long serialVersionUID = 3L;
    
    HMatrix matrix;
    ArrayList<Cluster> HDBSCANStarClusterTree;
    UndirectedGraph MST;
    HashMap<Integer, Integer> resultExpansion;
    
    public Structure()
    {
    	this.matrix = null;
        this.HDBSCANStarClusterTree = null;
        this.MST = null;
        this.resultExpansion=null;
    }
    
    public Structure(HMatrix matrix)
    {
        this.matrix = matrix;
        this.HDBSCANStarClusterTree = null;
        this.MST = null;
    }
    
    public Structure(HMatrix matrix, ArrayList<Cluster> HDBSCANStarClusterTree)
    {
        this.matrix = matrix;
        this.HDBSCANStarClusterTree = HDBSCANStarClusterTree;
    }
    
    public Structure(HMatrix matrix, ArrayList<Cluster> HDBSCANStarClusterTree, UndirectedGraph MST)
    {
        this.matrix = matrix;
        this.HDBSCANStarClusterTree = HDBSCANStarClusterTree;
        this.MST = MST;
    }
    
    public void setHDBSCANStarClusterTree(ArrayList<Cluster> HDBSCANStarClusterTree)
    {
    	this.HDBSCANStarClusterTree = HDBSCANStarClusterTree;   	
    }
    
    public ArrayList<Cluster> getHDBSCANStarClusterTree()
    {
    	return this.HDBSCANStarClusterTree;
    }
    
    public HMatrix getMatrix()
    {
        return this.matrix;
    }
    
    public void setMatrix(HMatrix matrix)
    {
        this.matrix = matrix;
    }
    
    
    public UndirectedGraph getMST()
    {
        return this.MST;
    }
    
    public void setMST(UndirectedGraph MST)
    {
        this.MST = MST;
    }
    
    public void setResultExpansion(Map<Integer, Integer> response)
    {
    	this.resultExpansion=(HashMap<Integer, Integer>)response;
    }
    
    public HashMap<Integer, Integer> getResultExpansion()
    {
    	return this.resultExpansion;
    }
    
}
