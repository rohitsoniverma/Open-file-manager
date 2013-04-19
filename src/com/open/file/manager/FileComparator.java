/*******************************************************************************
 * Copyright (c) 2013 Michele Corazza.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Credits:
 * 	Actionbarsherlock library: for the fragment support
 * 	Oxygen team: for the gorgeous icons
 ******************************************************************************/
package com.open.file.manager;
import java.io.File;
import java.util.Comparator;
import java.util.Locale;

class FileComparator implements Comparator<File>
{
	@Override
	public int compare(File f1, File f2)
	{
		if(f1.isDirectory() == f2.isDirectory())
		{
			Locale loc=Locale.US;
			return f1.getName().toLowerCase(loc).compareTo(f2.getName().toLowerCase(loc));
		}
		else 
		{
			if(f1.isDirectory()) return -1;
			else return 1;
		}
	}
}
