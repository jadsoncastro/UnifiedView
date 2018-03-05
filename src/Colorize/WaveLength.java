/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Colorize;

import java.awt.Color;

/**
 *
 * @author Fernando Soares de Aguiar Neto
 */
public class WaveLength
{
        //maps a number beetween 0,5 and 1,5 to an RGB color.
	public static Color toRGB(Double in)
	{
		Double R;
		Double G;
		Double B;
		
                //maps in into a interval between 0 and 1 circular, similar to a in mod 1. 
                if(in > 1)
                {
                    in = in -1;
                }
                
		//for Blue
		if(in < 0.1)
		{
			B = 0.5 + in*5;
		}
		else if((in>=0.10) && (in < 0.4))
		{
			B = 1.0;

		}else if((in >= 0.4) && (in < 0.6))
		{
			B = 1 - (in-0.4)*5;
		}
		else
		{
			B = 0.0;
		}

		//for Green
		if(in < 0.1)
		{
			G = 0.0;
		}
		else if((in>=0.1) && (in < 0.3))
		{
			G = (in-0.1)*5;

		}else if((in >= 0.3) && (in < 0.6))
		{
			G = 1.0;
		}
		else if((in >= 0.6) && (in < 0.8))
		{
			G = 1 - (in-0.6)*5;
		}
		else
		{
			G = 0.0;
		}

		//for Red
		if(in < 0.4)
		{
			R = 0.0;
		}
		else if((in>=0.4) && (in < 0.6))
		{
			R = (in - 0.4)*5;

		}else if((in >= 0.6) && (in < 0.9))
		{
			R = 1.0;
		}
		else
		{
			R = 1 - (in-0.9)*5;
		}
		
		//System.out.println(R+","+G+","+B);
		return new Color(R.floatValue(),G.floatValue(),B.floatValue());					
	}
	
}
