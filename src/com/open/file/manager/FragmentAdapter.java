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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragment;

public class FragmentAdapter extends FragmentStatePagerAdapter{
	// fragments to instantiate in the viewpager
	   private static List<SherlockFragment> fragments;
	   private static int currentfrag = 0;
	   private WeakReference<MainActivity> mact;
	   
	   // constructor
	   public FragmentAdapter(FragmentManager fm, MainActivity act) {
	      super(fm);
	      mact=new WeakReference<MainActivity>(act);
	      fragments=new ArrayList<SherlockFragment>();
	   }
	   
	   public void addFragment(SherlockFragment newFragment)
	   {
		   fragments.add(newFragment);
		   notifyDataSetChanged();
	   }
	   
	   // return access to fragment from position, required override
	   @Override
	   public SherlockFragment getItem(int position) {
	      return fragments.get(position);
	   }
	   
	   public int getItemPosition(SherlockFragment f)
	   {
		   if(fragments.contains(f))
		   {
			   return fragments.indexOf(f);
		   }
		   return POSITION_NONE;
	   }

	   // number of fragments in list, required override
	   @Override
	   public int getCount() {
	      return fragments.size();
	   }

	public void replaceFragment(SherlockFragment newFragment, int pos) {
		fragments.set(pos, newFragment);
		notifyDataSetChanged();
	}
	
	
	/* 
	 * Perform some actions when the user swaps between fragments
	 */
	@Override
	public void setPrimaryItem (ViewGroup container, int position, Object object)
	{
		GridFragment oldselectedgrid;
		if(currentfrag!=position)
		{
			if(MainActivity.mMode!=null)
			{
				oldselectedgrid=(GridFragment) getItem(currentfrag);
				oldselectedgrid.myimgad.notifyDataSetChanged();
				MainActivity.mMode.finish();
				MainActivity.mMode=null;
				MainActivity.mPager.setCurrentItem(position);
			}
			if(getItem(position) instanceof GridFragment)
			{
				GridFragment current=(GridFragment) getItem(position);
				mact.get().setTitle(current.currentdir.getName());
			}
			else
			{
				if(mact!=null && mact.get()!=null)
				{
				mact.get().setTitle("Open File Manager");
				}
			}
			currentfrag=position;
		}
	}
	
	/**
	 * @return current frag position
	 */
	public int getcurrentfrag()
	{
		return currentfrag;
	}

	/**
	 * @return list of paths of gridfragments
	 */
	public ArrayList<String> getFragments() {
		ArrayList<String> gridpaths= new ArrayList<String>();
		GridFragment current;
		for(int i=0; i<fragments.size(); i++)
		{
			if(fragments.get(i) instanceof GridFragment)
			{
				current=(GridFragment) fragments.get(i);
				gridpaths.add(current.currentdir.getAbsolutePath());
			}
		}
		return gridpaths;
	}
	
	@Override
	public Parcelable saveState()
	{
		return new Bundle();
	}
	
	@Override
	public void restoreState(Parcelable saved, ClassLoader loader)
	{
	}

	/**
	 * @return true if there is no selectpathfragment at the end of the fragment list
	 */
	public boolean selectpathmissing() {
		if(fragments.isEmpty()) return true;
		return!(fragments.get(fragments.size()-1) instanceof SelectPathFragment);
	}

	/**
	 * refresh all grids
	 */
	public void updatefrags() {
		for(int i=0; i<fragments.size()-1;i++)
		{
			((GridFragment)fragments.get(i)).refreshFiles();
		}
		
	}
}
