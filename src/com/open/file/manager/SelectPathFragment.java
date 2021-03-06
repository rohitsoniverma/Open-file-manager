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

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;


public class SelectPathFragment extends SherlockFragment {
	int mNum;
	public View v;
	SelectPathAdapter bookmarkadapter;
	OnPathSelectedListener mCallback;
	
	public interface OnPathSelectedListener {
        public void onPathSelected(File clicked);
    }
	
	/**
	 * Create a new instance
	 */
	static SelectPathFragment newInstance() {
		SelectPathFragment f = new SelectPathFragment();
		f.setRetainInstance(false);
		return f;
	}


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bookmarkadapter = new SelectPathAdapter(getActivity().getApplicationContext());
	}
	
	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
			final Bundle savedInstanceState) {
		ListView list= new ListView(getActivity());
		v = inflater.inflate(R.layout.activity_selectpath, container, false);
		list=(ListView) v.findViewById(R.id.pathlist);
		list.setAdapter(bookmarkadapter);
		list.setOnItemClickListener(new OnItemClickListener() {
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					mCallback.onPathSelected(bookmarkadapter.getItem(position));
			}
			}
					);
		return v;
	}

	public void onAttach(Activity activity) {
        super.onAttach(activity);
        bookmarkadapter = new SelectPathAdapter(getActivity().getApplicationContext());
        try {
            mCallback = (OnPathSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnPathSelectedListener");
        }
    }
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}



}
