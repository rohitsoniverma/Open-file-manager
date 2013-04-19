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
import java.lang.ref.WeakReference;
import java.util.Arrays;

import com.open.file.manager.IconLoader.AsyncImgLoad;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;


public class GridAdapter extends BaseAdapter {
	
	private File[] dirfiles;
    private Context mContext;
    private int nfrag;
    private static final FileComparator compare= new FileComparator();
    private static LruCache<String, Bitmap> bitmapCache;
    private IconLoader loader;

    public GridAdapter(Context c, File curdir) {
        mContext = c;
        loader= new IconLoader(mContext);
        dirfiles=curdir.listFiles();
        Arrays.sort(dirfiles, compare);
    }
    
    public void changepath(File newpath)
    {
    	dirfiles=newpath.listFiles();
    	Arrays.sort(dirfiles, compare);
    	notifyDataSetChanged();
    }
    
    @Override
    public boolean hasStableIds()
    {
    	return true;
    }
    
    @Override
    public int getCount() {
    	return dirfiles.length;
    }
    @Override
    public File getItem(int position) {
    	return dirfiles[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    // create a new ImageView for each item referenced by the Adapter
    public View getView(int position, View convertView, ViewGroup parent) {
    	Gridviewholder holder;
        if ( convertView == null )
        {
           LayoutInflater li = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
           convertView = li.inflate(R.layout.grid_item, null); 
           holder=new Gridviewholder();
           holder.filename=(CheckedTextView)convertView.findViewById(R.id.grid_item_text);
           holder.fileicon=(ImageView)convertView.findViewById(R.id.grid_item_image);
           convertView.setTag(holder);
        }
        else
        {
        	holder=(Gridviewholder) convertView.getTag();
        	holder.fileicon.setImageResource(R.drawable.unknownfile);
        }
        holder.rootview=convertView;
        holder.associatedfile=dirfiles[position];
        holder.position=position;
        CheckedTextView tv = holder.filename;
        File current=holder.associatedfile;
        tv.setText(current.getName());
        tv.setChecked(MainActivity.selectedfiles.contains(current));
        loader.loadIcon(holder, position);   
        if(tv.isChecked())
        {
        	convertView.setBackgroundResource(R.color.cyan);
        }
        else
        {
        	convertView.setBackgroundResource(R.color.white);
        }
        
        return convertView;
    }
    

    
    public void addToCache(String key, Bitmap value)
    {
    	if(bitmapCache.get(key)==null)
    	{
    		bitmapCache.put(key, value);
    	}
    }
    
    static public class Gridviewholder
    {
    	CheckedTextView filename;
    	ImageView fileicon;
    	View rootview;
    	Integer position;
    	File associatedfile;
    	AsyncImgLoad loader;
    }
    
    
}
