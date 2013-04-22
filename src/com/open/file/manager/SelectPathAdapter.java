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


import android.content.Context;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


public class SelectPathAdapter extends BaseAdapter {
	
	Context pthContext;
	static List<File> bookmarkspaths = new ArrayList<File>();

	public SelectPathAdapter(Context context) {
		pthContext=context;
		if(bookmarkspaths.isEmpty())
		{
			populatePaths();
		}
		
	}

	/**
	 * Check if sdcard is mounted, else populate paths with root (/)
	 */
	private void populatePaths() {
		String state = Environment.getExternalStorageState();
		File downloads, music, pictures;
		if (Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {

			bookmarkspaths.add(Environment.getExternalStorageDirectory());
			downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
			music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
			pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			if(downloads.exists())
			{
				bookmarkspaths.add(downloads);
			}
			if(music.exists())
			{
			bookmarkspaths.add(music);
			}
			if(pictures.exists())
			{
			bookmarkspaths.add(pictures);
			}
		}
		else {
			// The sdcard isn't mounted at all, so i just add root to the bookmarks selection list, so the user has
			//some place to start browsing folders
			bookmarkspaths.add(new File("/"));
		}
		
	}

	@Override
	public int getCount() {
		return bookmarkspaths.size();
	}

	@Override
	public File getItem(int position) {
		return bookmarkspaths.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {        
        if ( convertView == null )
        {           
           LayoutInflater li = (LayoutInflater)pthContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
           convertView = li.inflate(R.layout.textimageoriz, null);
        }
        TextView tv = (TextView)convertView.findViewById(R.id.pathtext);
        if(bookmarkspaths.size()!=1)
        {	if(bookmarkspaths.get(position)==Environment.getExternalStorageDirectory())
        	{
        		tv.setText("Sdcard");
        	}
        else
        {
        	tv.setText((bookmarkspaths.get(position)).getName());
        }
        }
        else
        {
        	tv.setText("root");
        }
        
        ImageView iv = (ImageView)convertView.findViewById(R.id.pathimage);
        
        if((bookmarkspaths.get(position)).getPath() == Environment.getExternalStorageDirectory().getPath())
        {   
            iv.setImageResource(R.drawable.sdcard);
        }
        else
        {
            iv.setImageResource(R.drawable.directory);

        }
        
        return convertView;
	}
}
