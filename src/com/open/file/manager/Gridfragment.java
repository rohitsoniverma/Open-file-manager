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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;

import com.actionbarsherlock.app.SherlockFragment;
import com.open.file.manager.ImageAdapter.Gridviewholder;

public class Gridfragment extends SherlockFragment
{
	int mNum;
	View v;
	public GridView grid;
	boolean firstrun;
	File currentdir;
	List<String> selectedfiles;
	ImageAdapter myimgad;
	Gridviewlistener onclickcback;
	/**
	 * Create a new instance of CountingFragment, providing "num"
	 * as an argument.
	 */

	public interface Gridviewlistener
	{
		public boolean onClickFile(File clicked, View item);
		public void onLongclickfile(int position, int fragnum, View v, AdapterView<?> parent);
		public void showdialog(int titlebar, int content);
		public void setTitle(CharSequence newtitle);
	}

	static Gridfragment newInstance(File initpath) {
		Gridfragment f = new Gridfragment();
		f.currentdir=initpath;
		return f;
	}

	static Gridfragment newInstance(String initpath) {
		Gridfragment f = new Gridfragment();
		f.currentdir=new File(initpath);
		return f;
	}

	static Gridfragment newInstance(String initpath, List<String> initselectedfiles) {
		Gridfragment f = new Gridfragment();
		f.currentdir=new File(initpath);
		f.selectedfiles=initselectedfiles;
		return f;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		firstrun=true;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		v = inflater.inflate(R.layout.activity_viewfiles, container, false);
		grid=(GridView) v.findViewById(R.id.listfilesgrid);
		return v;
	}

	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		if(currentdir==null)
		{
			currentdir=new File(savedInstanceState.getString("currentdir"));
		}
		myimgad=new ImageAdapter(MainActivity.actcontext, currentdir);
		grid.setAdapter(myimgad);
		myimgad.notifyDataSetChanged();
		grid.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				File clicked=myimgad.getItem(position);
				if(!onclickcback.onClickFile(clicked, view))

					if(clicked.isDirectory())
					{
						if(clicked.canRead())
						{
							ChangePath(clicked);
						}
						else
						{
							onclickcback.showdialog(R.string.cantopendir, R.string.error);
						}
					}
					else
					{
						String fileExtension = MimeTypeMap.getFileExtensionFromUrl(clicked.getAbsolutePath());
						fileExtension=fileExtension.toLowerCase();
						String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
						if(clicked.canRead())
						{
							Intent i = new Intent();
							i.setAction(android.content.Intent.ACTION_VIEW);
							i.setDataAndType(Uri.fromFile(clicked), mimetype);
							try
							{
								startActivity(i);
							}
							catch(Exception ex)
							{
								//IMPOSSIBILE APRIRE IL FILE!!!!!
							}
						}
						else
						{
							onclickcback.showdialog(R.string.cantread, R.string.error);

						}
					}
				myimgad.notifyDataSetChanged();
			}
		}
				);
		grid.setOnItemLongClickListener(new OnItemLongClickListener()
		{
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				onclickcback.onLongclickfile(position, mNum, view, parent);
				myimgad.notifyDataSetChanged();
				return true;
			}
		});
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString("currentdir", currentdir.getAbsolutePath());
	}

	public void ChangePath(File newroot)
	{
		currentdir=newroot;
		myimgad.changepath(newroot);
		grid.smoothScrollToPosition(0);
		updateTitle();
	}

	private void updateTitle() {
		Log.d("curr", currentdir.toString());
		if(!currentdir.equals(new File("/")))
		{
			onclickcback.setTitle(currentdir.getName());
		}
		else
		{
			onclickcback.setTitle("/");
		}
	}

	public File GetParent()
	{
		File parent = currentdir.getParentFile();
		return parent;
	}

	public File getCurrentDir()
	{
		return currentdir;
	}

	public void refreshFiles()
	{
		myimgad.changepath(currentdir);
		myimgad.notifyDataSetChanged();
		grid.invalidateViews();
	}

	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			onclickcback = (Gridviewlistener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnPathSelectedListener");
		}
	}


	public void clearselection() {
		myimgad.notifyDataSetChanged();
		int gridsize = grid.getChildCount();
		for(int i=0; i<gridsize; i++)
		{
			((Gridviewholder) grid.getChildAt(i).getTag()).filename.setChecked(false);
			grid.getChildAt(i).setBackgroundResource(R.color.white);
		}
		myimgad.notifyDataSetChanged();
		grid.invalidateViews();
		grid.setAdapter(myimgad);
	}

}
