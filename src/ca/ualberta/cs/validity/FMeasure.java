package ca.ualberta.cs.validity;

import java.util.ArrayList;
import java.util.TreeSet;

import javafx.util.Pair;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;

public class FMeasure extends Validity 
{
	private static final Integer positiveLabel = 1;
	private static final Integer negativeLabel = -1;
	private static final Integer binaryClassification = 2;

	// F-measure for clustering
//	public double getIndex(Instance[] dataset) 
//	{
//		int tp = 0; //number of pairs of objects having the same classes and the same clusters
//		int tn = 0; //number of pairs of objects having different classes and different clusters
//		int fp = 0; //number of pairs of objects having different classes and the same clusters
//		int fn = 0; //number of pairs of objects having the same classes and different clusters
//
//		for (int i = 0; i < dataset.length-1; i++) 
//		{
//			if(dataset[i].isPreLabeled())
//				continue;
//
//			for (int j = i + 1; j < dataset.length; j++) 
//			{
//
//				if(dataset[j].isPreLabeled())
//					continue;
//
//				if(dataset[i].getLabel() == dataset[j].getLabel() && dataset[i].getTrueLabel() == dataset[j].getTrueLabel())
//					tp++;
//
//				if(dataset[i].getLabel() != dataset[j].getLabel() && dataset[i].getTrueLabel() != dataset[j].getTrueLabel())
//					tn++;
//
//				if(dataset[i].getLabel() == dataset[j].getLabel() && dataset[i].getTrueLabel() != dataset[j].getTrueLabel())
//					fp++;
//
//				if(dataset[i].getLabel() != dataset[j].getLabel() && dataset[i].getTrueLabel() == dataset[j].getTrueLabel())
//					fn++;
//			}
//		}
//		double precision = (double)tp/(tp + fp);
//		double recall    = (double)tp/(tp + fn);
//
//		return (2*precision*recall)/(precision + recall);
//	}


	public double getIndex(Instance[] dataset)
	{
		TreeSet<Integer> classIds = new TreeSet<Integer>();
		
		for(Instance inst: dataset)
			classIds.add(inst.getLabel());
				
		int numClasses = classIds.size();
		ArrayList<Pair<Integer, Integer>> objects = null;
		
		
		
		double fTotal = 0;
		for(Integer cl: classIds)
		{
			objects = new ArrayList<Pair<Integer,Integer>>();

			int classOne = cl;

			for(Instance inst: dataset)
			{
				if(inst.isPreLabeled())
					continue;
				
				Integer predicted, actual;

				if(inst.getLabel()==classOne) // Get labels with respect to the algorithm prediction
					predicted = positiveLabel;
				else
					predicted = negativeLabel;

				if(inst.getTrueLabel()==classOne) // Get the true labels of the objects
					actual = positiveLabel;
				else
					actual = negativeLabel;

				objects.add(new Pair<Integer, Integer>(actual, predicted));
			}
			fTotal+= computeIdx(objects);
		}
		
		return fTotal/numClasses;
	}

	private static double computeIdx(ArrayList<Pair<Integer, Integer>> objects)
	{
		int tp=0;
		int fp=0;
		int fn=0;
		int tn=0;
		
		for(Pair<Integer, Integer> obj: objects)
		{
			if(obj.getKey().equals(obj.getValue()) 		 && obj.getKey().equals(positiveLabel))
				tp++;
			else if(obj.getKey().equals(obj.getValue())  && obj.getKey().equals(negativeLabel))
				tn++;
			else if(!obj.getKey().equals(obj.getValue()) && obj.getKey().equals(positiveLabel))
				fn++;
			else if(!obj.getKey().equals(obj.getValue()) && obj.getKey().equals(negativeLabel))
				fp++;
		}
		
		double precision = (double)tp/(tp+fp);
		double recall    = (double)tp/(tp + fn);
		
		double fm;
		
		if(precision+recall > 0)
		{
			fm = (2*precision*recall)/(precision + recall);
		}else
		{
			fm = 0;
//			System.err.println("Error while computing F-measure");
		}
		return fm;
	}

	
	@Override
	public double getIndexClassification(Dataset dataset) 
	{
		// TODO Auto-generated method stub
		return 0;
	}
}