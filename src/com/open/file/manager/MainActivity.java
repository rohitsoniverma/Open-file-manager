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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.ActionMode.Callback;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.open.file.manager.ImageAdapter.Gridviewholder;

public class MainActivity extends SherlockFragmentActivity 
implements Selectpathfragment.OnPathSelectedListener, Gridfragment.Gridviewlistener
{


	public static List<Boolean> firstrun=new ArrayList<Boolean>();
	boolean wannaclose;
	static int selectedcount =0;
	public static boolean tobeclosed=false;
	public static Context actcontext;
	public static String initpath;
	public int curfrag;
	public static File root;
	public static List<File> operationqueue= new ArrayList<File>();
	public static List<File> selectedfiles= new ArrayList<File>();
	public static List<String> cutcopylist= new ArrayList<String>();
	public static Fragmentadapter mAdapter=null;
	public static ActionMode mMode;
	public static ActionMode copycutmode;
	public static ViewPager mPager;
	static fileOperations operator;
	int currentaction;
	public static Handler acthandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		int i=0;
		actcontext = getApplicationContext();


		operator= new fileOperations(actcontext, this);
		/*imposto l'adapter per i fragment*/
		setContentView(R.layout.fragment_pager_layout);
		mPager = (ViewPager)findViewById(R.id.pager);
		mAdapter=new Fragmentadapter(getSupportFragmentManager(), this);
		if(savedInstanceState!=null)
		{
			ArrayList<String> oldfrags=savedInstanceState.getStringArrayList("fragments");
			for(String curfrag: oldfrags)
			{
				mAdapter.addFragment(Gridfragment.newInstance(curfrag));
			}
			ArrayList<String> oldselected=savedInstanceState.getStringArrayList("selectedfiles");
			for(String curselected: oldselected)
			{
				selectedfiles.add(new File(curselected));
			}
		}
		mPager.setAdapter(mAdapter);
		if(mAdapter.selectpathmissing())
		{
			mAdapter.addFragment(Selectpathfragment.newInstance());
		}
		
		if(selectedfiles.size() > 0)
		{
			mMode=startActionMode(getcallback());
		}
		curfrag=mAdapter.getcurrentfrag();
		mPager.setCurrentItem(curfrag);
		acthandler=new activityhandler();
		restoreOperations(savedInstanceState);
	}

	private void restoreOperations(Bundle savedInstanceState) {
		if(savedInstanceState != null)
		{
			Log.d("restoring", "operations");
			if(savedInstanceState.containsKey("conflicts"))
			{
				fileOperations.conflicts=savedInstanceState.getParcelableArrayList("conflicts");
			}
			fileOperations.currentaction=savedInstanceState.getInt("operation");
			if(savedInstanceState.containsKey("oldqueue"))
			{
				List<String> filequeue=savedInstanceState.getStringArrayList("oldqueue");
				for(String curfile: filequeue)
				{
					fileOperations.operationqueue.add(new File(curfile));
				}
			}
			if(savedInstanceState.containsKey("currentpath"))
			{
				fileOperations.currentpath=savedInstanceState.getString("currentpath");
			}
		}
		operator.restoreOp();
	}

	public void changeFragmentPath(int fragnum, File newroot)
	{
		Gridfragment currentfr = (Gridfragment) mAdapter.getItem(fragnum);
		currentfr.ChangePath(newroot);
	}



	@Override
	public void onPathSelected(File clicked) {
		int fragnum=mAdapter.getCount()-1;
		Gridfragment newfrag=Gridfragment.newInstance(clicked);
		mAdapter.addFragment(Selectpathfragment.newInstance());
		mAdapter.replaceFragment(newfrag, fragnum);
		mAdapter.notifyDataSetChanged();
		mPager.setAdapter(mAdapter);
		mPager.setCurrentItem(fragnum);
		setTitle(clicked.getName()==""? "/" : clicked.getName());
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		if(fileOperations.currentdialog!=null && fileOperations.currentdialog.isShowing())
		{
			fileOperations.currentdialog.dismiss();
		}
	}

	@Override
	public void onLongclickfile(int position, int fragnum, View item, AdapterView<?> parent) {
		CheckedTextView tv=((Gridviewholder)item.getTag()).filename;
		Gridviewholder holder= ((Gridviewholder)item.getTag());
		if(!tv.isChecked())
		{
			selectedfiles.add(holder.associatedfile);
			tv.setChecked(true);
			selectedcount++;
			if(selectedcount==2)
			{
				mMode.getMenu().getItem(consts.INDEX_RENAME).setVisible(false);
				mMode.getMenu().getItem(consts.INDEX_INFO).setVisible(false);

			}
			if(mMode == null)
			{
				mMode = startActionMode (getcallback());
			}
		}
		else
		{
			selectedfiles.remove(holder.associatedfile);
			tv.setChecked(false);
			selectedcount--;
			if(selectedcount==1)
			{
				mMode.getMenu().getItem(consts.INDEX_RENAME).setVisible(true);
				mMode.getMenu().getItem(consts.INDEX_INFO).setVisible(true);

			}
			if(selectedcount == 0)
			{
				mMode.finish();
				mMode=null;
			}
		}
	}

	@Override
	protected void onSaveInstanceState (Bundle outState)
	{
		super.onSaveInstanceState(outState);
		ArrayList<String> oldfrags=mAdapter.getFragments();
		outState.putStringArrayList("fragments", oldfrags);
		outState.putInt("operation", fileOperations.currentaction);
		if(fileOperations.currentpath!=null)
		{
			outState.putString("currentpath", fileOperations.currentpath);
		}
		if(fileOperations.conflicts!= null && fileOperations.conflicts.size()>0)
		{
			outState.putParcelableArrayList("conflicts", fileOperations.conflicts);
		}
		if(!fileOperations.operationqueue.isEmpty())
		{
			ArrayList<String> oldoperations=new ArrayList<String>();
			for(File current:fileOperations.operationqueue)
			{
				oldoperations.add(current.getAbsolutePath());
			}
			outState.putStringArrayList("oldqueue", oldoperations);
		}
		ArrayList<String> oldselected=new ArrayList<String>();
		for(File current: selectedfiles)
		{
			oldselected.add(current.getAbsolutePath());
		}
		outState.putStringArrayList("selectedfiles", oldselected);
	}


	@Override
	public boolean onClickFile(File clicked, View item) {
		Gridviewholder holder= ((Gridviewholder)item.getTag());
		if(mMode!=null)
		{
			if(holder.filename.isChecked())
			{
				selectedfiles.remove(clicked);
				holder.filename.setChecked(false);
				selectedcount--;
				if(selectedcount==1)
				{
					mMode.getMenu().getItem(consts.INDEX_RENAME).setVisible(true);
					mMode.getMenu().getItem(consts.INDEX_INFO).setVisible(true);

				}
				if(selectedcount == 0)
				{
					mMode.finish();
					mMode=null;
				}
			}
			else
			{
				selectedfiles.add(clicked);
				holder.filename.setChecked(true);
				selectedcount++;
				if(selectedcount==2)
				{
					mMode.getMenu().getItem(consts.INDEX_RENAME).setVisible(false);
					mMode.getMenu().getItem(consts.INDEX_INFO).setVisible(false);

				}
			}
			return true;
		}
		return false;
	}

	public Builder gettextdialog(String textres, String fieldcontent)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater= getLayoutInflater();
		View renamedialogview= inflater.inflate(R.layout.textinputdialog, null);
		builder.setView(renamedialogview);
		final TextView newnametext=(TextView) renamedialogview.findViewById(R.id.newfiletext);
		newnametext.setText(textres);
		final EditText textfieldform=(EditText) renamedialogview.findViewById(R.id.newfilename);
		textfieldform.setText(fieldcontent);
		return builder;
	}

	public void displaysimpledialog(int msgstringid, int errorstringid)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msgstringid);
		builder.setTitle(errorstringid);
		builder.setNegativeButton("OK", null);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	@Override
	public void onBackPressed () {
		File nextroot;
		int fragnum=mPager.getCurrentItem();
		if(mMode!=null)
		{
			mMode.finish();
			mMode=null;
			return;
		}
		if(copycutmode!=null)
		{
			copycutmode.finish();
			copycutmode=null;
			return;
		}
		if(mAdapter.getItem(fragnum) instanceof Gridfragment)
		{
			Gridfragment current = (Gridfragment) mAdapter.getItem(fragnum);
			nextroot = current.GetParent();
			if(nextroot != null)
			{
				tobeclosed=false;
				changeFragmentPath(fragnum, nextroot);
			}
			else 
			{
				if(tobeclosed==true)
				{
					finish();
				}
				else
				{
					Toast.makeText(actcontext, "Press Back again to close", Toast.LENGTH_SHORT).show();
					tobeclosed=true;
				}
			}
		}
		else 
		{
			if(tobeclosed==true)
			{
				finish();
			}
			else
			{
				Toast.makeText(actcontext, "Press Back again to close", Toast.LENGTH_SHORT).show();
				tobeclosed=true;
			}
		}
		return;

	}



	private String getcurrentpath() {
		return ((Gridfragment) mAdapter.getItem(mAdapter.getcurrentfrag())).getCurrentDir().getAbsolutePath();
	}



	public  Callback getcallback() {

		return new Callback()
		{


			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				//Used to put dark icons on light action bar
				mode.getMenuInflater().inflate(R.layout.menuactions, menu);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch(item.getItemId())
				{
				case (R.id.cut):
					currentaction=consts.ACTION_CUT;
				operationqueue.addAll(selectedfiles);
				delaystartpaste();
				mode.finish();
				return true;
				case(R.id.copy):
					currentaction=consts.ACTION_COPY;
				operationqueue.addAll(selectedfiles);
				delaystartpaste();
				mode.finish();
				return true;
				case(R.id.delete):
				operator.removefiles(selectedfiles);
				mode.finish();
				return true;
				case(R.id.rename):
					operator.renamefile(selectedfiles);
				mode.finish();
				return true;
				case(R.id.info):
					getfileinfo(selectedfiles.get(0));
				mode.finish();
				return true;
				}
				return false;
			}

			public void delaystartpaste()
			{
				//Hack per un bug nella libreria actionbar
				Handler modeHandler=new Handler();
				modeHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						copycutmode = startActionMode(getpastecallback());

					}
				}, 10);
			}


			@Override
			public void onDestroyActionMode(ActionMode mode) {
				//clear selection
				int fragindex=MainActivity.mPager.getCurrentItem();
				MainActivity.selectedfiles.clear();
				MainActivity.selectedcount=0;
				MainActivity.mMode=null;
				if(MainActivity.mAdapter.getItem(fragindex).getClass() == (Gridfragment.class))
				{
					((Gridfragment) MainActivity.mAdapter.getItem(fragindex)).clearselection();
				}
			}

		};
	}


	protected void getfileinfo(File file) {
		iconLoader loader=new iconLoader(actcontext);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater= (LayoutInflater) getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		View infodialogview= inflater.inflate(R.layout.fileinfo, null);
		builder.setView(infodialogview);
		TextView infotv=(TextView) infodialogview.findViewById(R.id.infofile);
		infotv.setText(operator.getfileinfo(file));
		infotv.setCompoundDrawables(null, loader.loadConflictico(file),null, null);
		builder.setPositiveButton("OK", null);
		builder.create().show();
	}

	private Callback getpastecallback() {
		return new Callback()
		{


			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				//Used to put dark icons on light action bar
				mode.getMenuInflater().inflate(R.layout.menupaste, menu);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch(item.getItemId())
				{
				case(R.id.paste):
					operator.handlepaste(operationqueue, getcurrentpath(),currentaction);
				mode.finish();
				return true;
				case(R.id.abs__action_mode_close_button):
					mode.finish();
				return true;
				default:
					return false;
				}
			}


			@Override
			public void onDestroyActionMode(ActionMode mode) {
				MainActivity.copycutmode=null;
				MainActivity.operationqueue.clear();
			}

		};
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.mainmenu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.newdir:
			if(mAdapter.getItem(mAdapter.getcurrentfrag()) instanceof Selectpathfragment)
			{
				displaysimpledialog(R.string.selectpathfirst, R.string.error);
			}
			else
			{
			fileOperations.currentpath=getcurrentpath();
			operator.createfolder(R.string.typedirname);
			}
			return true;
		case R.id.about:
			showaboutdialog();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void showaboutdialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater= (LayoutInflater) getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		View aboutdialogview= inflater.inflate(R.layout.aboutdialog, null);
		builder.setView(aboutdialogview);
		TextView bodyview=(TextView) aboutdialogview.findViewById(R.id.aboutcontent);
		bodyview.setText(Html.fromHtml(getString(R.string.aboutBody)));
		bodyview.setMovementMethod(LinkMovementMethod.getInstance());
		builder.setTitle(R.string.about);
		builder.setPositiveButton("OK", null);
		builder.create().show();
	}

	@Override
	public void showdialog(int titlebar, int content) {
		displaysimpledialog(titlebar, content);
	}

	static class activityhandler extends Handler
	{
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what)
			{
			case consts.MSG_DUPLICATES:
				fileOperations.conflicts=msg.getData().getParcelableArrayList("duplicates");
				operator.askconflicts(fileOperations.conflicts, false, false, 0);
				break;
			case consts.MSG_FINISHED:
				mAdapter.updatefrags();
				break;
			}
			
		}
	}
	

	public Builder showConflictdialog(fileDuplicate conflict) {
		AlertDialog.Builder builder;
		File src=conflict.src;
		File dst=conflict.dst;
		iconLoader loader=new iconLoader(actcontext);
		if(conflict.type==2)
		{
			String fileordir, filename,messageformat ,messagetxt;
			if(dst.isDirectory())
			{
				fileordir="directory";
			}
			else
			{
				fileordir="file";
			}
			filename=src.getName();
			messageformat = getResources().getString(R.string.filefolderconflict);
			messagetxt = String.format(messageformat, fileordir, filename);
			builder = gettextdialog(messagetxt, filename);
		}
		else
		{
			builder = new AlertDialog.Builder(this);
			LayoutInflater inflater= (LayoutInflater) getSystemService( Context.LAYOUT_INFLATER_SERVICE );
			View askdialogview= inflater.inflate(R.layout.conflictdialog, null);
			builder.setView(askdialogview);
			TextView srcdescr=(TextView) askdialogview.findViewById(R.id.srcdescr);
			
			srcdescr.setText(operator.getfileinfo(src));
			srcdescr.setCompoundDrawables(null, loader.loadConflictico(src), null, null);
			TextView dstdescr=(TextView) askdialogview.findViewById(R.id.dstdescr);
			dstdescr.setText(operator.getfileinfo(dst));
			dstdescr.setCompoundDrawables(null, loader.loadConflictico(dst),null, null);

			CheckBox overwriteall=(CheckBox) askdialogview.findViewById(R.id.overwritecheck);
			overwriteall.setText(R.string.overwritefile);
		}
		return builder;
	}
}

