/**
 * Copyright 2018 Christian Devile
 * 
 * This file is part of FoxTrotUpscaler.
 * 
 * FoxTrotUpscaler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FoxTrotUpscaler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with FoxTrotUpscaler. If not, see <http://www.gnu.org/licenses/>.
 */

package com.Christian77777.Frame_Summoner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread
{

	private final InputStream is;
	private final boolean post;
	private String lastPost;

	public StreamGobbler(InputStream is, boolean post)
	{
		this.is = is;
		this.post = post;
	}

	public void run()
	{
		try
		{
			InputStreamReader isr;
			isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line;
			while ((line = br.readLine()) != null)
			{
				lastPost = line.trim();
				if (post)
					System.out.println(lastPost);
			}
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(20);
		}
	}
	
	public String lastPost()
	{
		return lastPost;
	}
}