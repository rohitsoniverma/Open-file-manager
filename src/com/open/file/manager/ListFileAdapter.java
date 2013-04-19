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
import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ListFileAdapter extends BaseAdapter
{
	List<File> mylist;
	int filecount;
	Context mContext;
	
	public ListFileAdapter(List<File> filelist, Context context)
	{
		mylist=filelist;
		filecount=mylist.size();
		mContext=context;
	}
	
	@Override
	public int getCount() {
		return filecount;
	}

	@Override
	public Object getItem(int position) {
		return mylist.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView==null)
		{
			convertView = new TextView(mContext);
		}
		File current=mylist.get(position);
		((TextView) convertView).setText(current.getName());
		return convertView;
	}
	
}
