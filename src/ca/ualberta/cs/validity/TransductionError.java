package ca.ualberta.cs.validity;

import java.util.ArrayList;
import java.util.TreeSet;

import javafx.util.Pair;
import ca.ualberta.cs.model.Dataset;
import ca.ualberta.cs.model.Instance;

public class TransductionError extends Validity 
{
	public double getIndex(Instance[] dataset)
	{
		int total=0;
		int inccorrect=0;
		
		for(Instance inst: dataset)
		{
			if(!inst.isPreLabeled())
			{
				total++;
				if(inst.getLabel() != inst.getTrueLabel())
					inccorrect++;
			}
		}
		
		return inccorrect/(1.0* total);
	}

	
	@Override
	public double getIndexClassification(Dataset dataset) 
	{
		// TODO Auto-generated method stub
		return 0;
	}
}