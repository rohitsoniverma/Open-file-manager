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
import com.open.file.manager.GridAdapter.Gridviewholder;

public class MainActivity extends SherlockFragmentActivity implements
		SelectPathFragment.OnPathSelectedListener,
		GridFragment.Gridviewlistener {

	public static List<Boolean> firstrun = new ArrayList<Boolean>();
	boolean wannaclose;
	static int selectedcount = 0;
	public static boolean tobeclosed = false;
	public static Context actcontext;
	public static String initpath;
	public int curfrag;
	public static File root;
	public static List<File> operationqueue = new ArrayList<File>();
	public static List<File> selectedfiles = new ArrayList<File>();
	public static List<String> cutcopylist = new ArrayList<String>();
	public static FragmentAdapter mAdapter = null;
	public static ActionMode mMode;
	public static ActionMode copycutmode;
	public static ViewPager mPager;
	static FileOperations operator;
	int currentaction;
	public static Handler acthandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		actcontext = getApplicationContext();
		operator = new FileOperations(this);
		setContentView(R.layout.fragment_pager_layout);
		mPager = (ViewPager) findViewById(R.id.pager);
		mAdapter = new FragmentAdapter(getSupportFragmentManager(), this);
		if (savedInstanceState != null) {
			ArrayList<String> oldfrags = savedInstanceState
					.getStringArrayList("fragments");
			for (String curfrag : oldfrags) {
				mAdapter.addFragment(GridFragment.newInstance(curfrag));
			}
			ArrayList<String> oldselected = savedInstanceState
					.getStringArrayList("selectedfiles");
			for (String curselected : oldselected) {
				selectedfiles.add(new File(curselected));
			}
		}
		mPager.setAdapter(mAdapter);
		if (mAdapter.selectpathmissing()) {
			mAdapter.addFragment(SelectPathFragment.newInstance());
		}

		if (selectedfiles.size() > 0) {
			mMode = startActionMode(getCutCopyCallback());
		}
		curfrag = mAdapter.getcurrentfrag();
		mPager.setCurrentItem(curfrag);
		acthandler = new ActivityHandler();
		restoreOperations(savedInstanceState);
	}

	/**
	 * If an operation was not completed, restart it
	 * 
	 * @param savedInstanceState
	 */
	private void restoreOperations(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			Log.d("restoring", "operations");
			if (savedInstanceState.containsKey("conflicts")) {
				FileOperations.conflicts = savedInstanceState
						.getParcelableArrayList("conflicts");
			}
			FileOperations.currentaction = savedInstanceState
					.getInt("operation");
			if (savedInstanceState.containsKey("oldqueue")) {
				List<String> filequeue = savedInstanceState
						.getStringArrayList("oldqueue");
				for (String curfile : filequeue) {
					FileOperations.operationqueue.add(new File(curfile));
				}
			}
			if (savedInstanceState.containsKey("currentpath")) {
				FileOperations.currentpath = savedInstanceState
						.getString("currentpath");
			}
		}
		operator.restoreOp();
	}

	/**
	 * Change the path of the fragment at given position
	 * @param fragnum the position of the fragment
	 * @param newroot the new path for fragment
	 */
	public void changeFragmentPath(int fragnum, File newroot) {
		GridFragment currentfr = (GridFragment) mAdapter.getItem(fragnum);
		currentfr.ChangePath(newroot);
	}

	/* 
	 * Inteface to handle click on path from selectpathfragment
	 */
	public void onPathSelected(File clicked) {
		int fragnum = mAdapter.getCount() - 1;
		GridFragment newfrag = GridFragment.newInstance(clicked);
		mAdapter.addFragment(SelectPathFragment.newInstance());
		mAdapter.replaceFragment(newfrag, fragnum);
		mAdapter.notifyDataSetChanged();
		mPager.setAdapter(mAdapter);
		mPager.setCurrentItem(fragnum);
		setTitle(clicked.getName() == "" ? "/" : clicked.getName());
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (FileOperations.currentdialog != null
				&& FileOperations.currentdialog.isShowing()) {
			FileOperations.currentdialog.dismiss();
		}
	}

	/* 
	 * Handle long-clicks on grid files
	 */
	public void onLongclickfile(int position, int fragnum, View item,
			AdapterView<?> parent) {
		CheckedTextView tv = ((Gridviewholder) item.getTag()).filename;
		Gridviewholder holder = ((Gridviewholder) item.getTag());
		if (!tv.isChecked()) {
			selectedfiles.add(holder.associatedfile);
			tv.setChecked(true);
			selectedcount++;
			if (selectedcount == 2) {
				mMode.getMenu().getItem(Consts.INDEX_RENAME).setVisible(false);
				mMode.getMenu().getItem(Consts.INDEX_INFO).setVisible(false);

			}
			if (mMode == null) {
				mMode = startActionMode(getCutCopyCallback());
			}
		} else {
			selectedfiles.remove(holder.associatedfile);
			tv.setChecked(false);
			selectedcount--;
			if (selectedcount == 1) {
				mMode.getMenu().getItem(Consts.INDEX_RENAME).setVisible(true);
				mMode.getMenu().getItem(Consts.INDEX_INFO).setVisible(true);

			}
			if (selectedcount == 0) {
				mMode.finish();
				mMode = null;
			}
		}
	}

	/* 
	 * Save fragment paths, current operation (if any) and duplicates
	 * or operationqueue
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		ArrayList<String> oldfrags = mAdapter.getFragments();
		outState.putStringArrayList("fragments", oldfrags);
		outState.putInt("operation", FileOperations.currentaction);
		if (FileOperations.currentpath != null) {
			outState.putString("currentpath", FileOperations.currentpath);
		}
		if (FileOperations.conflicts != null
				&& FileOperations.conflicts.size() > 0) {
			outState.putParcelableArrayList("conflicts",
					FileOperations.conflicts);
		}
		if (!FileOperations.operationqueue.isEmpty()) {
			ArrayList<String> oldoperations = new ArrayList<String>();
			for (File current : FileOperations.operationqueue) {
				oldoperations.add(current.getAbsolutePath());
			}
			outState.putStringArrayList("oldqueue", oldoperations);
		}
		ArrayList<String> oldselected = new ArrayList<String>();
		for (File current : selectedfiles) {
			oldselected.add(current.getAbsolutePath());
		}
		outState.putStringArrayList("selectedfiles", oldselected);
	}

	/*
	 * On simple(short)click on file
	 */
	@Override
	public boolean onClickFile(File clicked, View item) {
		Gridviewholder holder = ((Gridviewholder) item.getTag());
		if (mMode != null) {
			if (holder.filename.isChecked()) {
				selectedfiles.remove(clicked);
				holder.filename.setChecked(false);
				selectedcount--;
				if (selectedcount == 1) {
					mMode.getMenu().getItem(Consts.INDEX_RENAME)
							.setVisible(true);
					mMode.getMenu().getItem(Consts.INDEX_INFO).setVisible(true);

				}
				if (selectedcount == 0) {
					mMode.finish();
					mMode = null;
				}
			} else {
				selectedfiles.add(clicked);
				holder.filename.setChecked(true);
				selectedcount++;
				if (selectedcount == 2) {
					mMode.getMenu().getItem(Consts.INDEX_RENAME)
							.setVisible(false);
					mMode.getMenu().getItem(Consts.INDEX_INFO)
							.setVisible(false);

				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Get a simple dialog asking for text input
	 * @param textres The text to show above the form
	 * @param fieldcontent The content of the form
	 * @return
	 */
	public Builder getTextDialog(String textres, String fieldcontent) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		View renamedialogview = inflater
				.inflate(R.layout.textinputdialog, null);
		builder.setView(renamedialogview);
		final TextView newnametext = (TextView) renamedialogview
				.findViewById(R.id.newfiletext);
		newnametext.setText(textres);
		final EditText textfieldform = (EditText) renamedialogview
				.findViewById(R.id.newfilename);
		textfieldform.setText(fieldcontent);
		return builder;
	}

	/**
	 * Display a simple dialog with an "OK" button to close it
	 * @param msgstringid Resource for the message to show
	 * @param titileid Resource for the title to show
	 */
	public void displaySimpleDialog(int msgstringid, int titleid) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(msgstringid);
		builder.setTitle(titleid);
		builder.setNegativeButton(R.string.ok, null);
		AlertDialog dialog = builder.create();
		dialog.show();
	}

	/* 
	 * Handle back press, terminating actionmodes ,going up one level
	 * in the filesystem or terminating the app
	 */
	@Override
	public void onBackPressed() {
		File nextroot;
		int fragnum = mPager.getCurrentItem();
		if (mMode != null) {
			mMode.finish();
			mMode = null;
			return;
		}
		if (copycutmode != null) {
			copycutmode.finish();
			copycutmode = null;
			return;
		}
		if (mAdapter.getItem(fragnum) instanceof GridFragment) {
			GridFragment current = (GridFragment) mAdapter.getItem(fragnum);
			nextroot = current.GetParent();
			if (nextroot != null) {
				tobeclosed = false;
				changeFragmentPath(fragnum, nextroot);
			} else {
				if (tobeclosed == true) {
					finish();
				} else {
					Toast.makeText(actcontext, "Press Back again to close",
							Toast.LENGTH_SHORT).show();
					tobeclosed = true;
				}
			}
		} else {
			if (tobeclosed == true) {
				finish();
			} else {
				Toast.makeText(actcontext, "Press Back again to close",
						Toast.LENGTH_SHORT).show();
				tobeclosed = true;
			}
		}
		return;

	}

	/**
	 * @return current shown path
	 */
	private String getCurrentPath() {
		return ((GridFragment) mAdapter.getItem(mAdapter.getcurrentfrag()))
				.getCurrentDir().getAbsolutePath();
	}

	/**
	 * @return Actionmode for cut/copy
	 */
	public Callback getCutCopyCallback() {

		return new Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				mode.getMenuInflater().inflate(R.layout.menuactions, menu);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case (R.id.cut):
					currentaction = Consts.ACTION_CUT;
					operationqueue.addAll(selectedfiles);
					delaystartpaste();
					mode.finish();
					return true;
				case (R.id.copy):
					currentaction = Consts.ACTION_COPY;
					operationqueue.addAll(selectedfiles);
					delaystartpaste();
					mode.finish();
					return true;
				case (R.id.delete):
					operator.removefiles(selectedfiles);
					mode.finish();
					return true;
				case (R.id.rename):
					operator.renamefile(selectedfiles);
					mode.finish();
					return true;
				case (R.id.info):
					getFileInfo(selectedfiles.get(0));
					mode.finish();
					return true;
				}
				return false;
			}

			/**
			 * Delay start (~10ms) of copy actionmode due to a bug in sherlock library
			 */
			public void delaystartpaste() {
				Handler modeHandler = new Handler();
				modeHandler.postDelayed(new Runnable() {

					@Override
					public void run() {
						copycutmode = startActionMode(getPasteCallback());

					}
				}, 10);
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				// clear selection
				int fragindex = MainActivity.mPager.getCurrentItem();
				MainActivity.selectedfiles.clear();
				MainActivity.selectedcount = 0;
				MainActivity.mMode = null;
				if (MainActivity.mAdapter.getItem(fragindex).getClass() == (GridFragment.class)) {
					((GridFragment) MainActivity.mAdapter.getItem(fragindex))
							.clearselection();
				}
			}

		};
	}

	/**
	 * Show info dialog for file
	 * @param file
	 */
	protected void getFileInfo(File file) {
		IconLoader loader = new IconLoader(actcontext);
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View infodialogview = inflater.inflate(R.layout.fileinfo, null);
		builder.setView(infodialogview);
		builder.setTitle(R.string.info);
		TextView infotv = (TextView) infodialogview.findViewById(R.id.infofile);
		infotv.setText(operator.getfileinfo(file));
		infotv.setCompoundDrawables(null, loader.loadConflictico(file), null,
				null);
		builder.setPositiveButton("OK", null);
		builder.create().show();
	}

	private Callback getPasteCallback() {
		return new Callback() {

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				mode.getMenuInflater().inflate(R.layout.menupaste, menu);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch (item.getItemId()) {
				case (R.id.paste):
					operator.handlepaste(operationqueue, getCurrentPath(),
							currentaction);
					mode.finish();
					return true;
				case (R.id.abs__action_mode_close_button):
					mode.finish();
					return true;
				default:
					return false;
				}
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				MainActivity.copycutmode = null;
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
			if (mAdapter.getItem(mAdapter.getcurrentfrag()) instanceof SelectPathFragment) {
				displaySimpleDialog(R.string.selectpathfirst, R.string.error);
			} else {
				FileOperations.currentpath = getCurrentPath();
				operator.createfolder(R.string.typedirname);
			}
			return true;
		case R.id.about:
			showAboutDialog();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Show dialog when "about" is clicked
	 */
	private void showAboutDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View aboutdialogview = inflater.inflate(R.layout.aboutdialog, null);
		builder.setView(aboutdialogview);
		TextView bodyview = (TextView) aboutdialogview
				.findViewById(R.id.aboutcontent);
		bodyview.setText(Html.fromHtml(getString(R.string.aboutBody)));
		bodyview.setMovementMethod(LinkMovementMethod.getInstance());
		builder.setTitle(R.string.about);
		builder.setPositiveButton("OK", null);
		builder.create().show();
	}

	/* 
	 *	Show dialog from grid
	 */
	@Override
	public void showDialog(int titlebar, int content) {
		displaySimpleDialog(titlebar, content);
	}

	static class ActivityHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Consts.MSG_DUPLICATES:
				FileOperations.conflicts = msg.getData()
						.getParcelableArrayList("duplicates");
				operator.askconflicts(FileOperations.conflicts, false, false, 0);
				break;
			case Consts.MSG_FINISHED:
				mAdapter.updatefrags();
				break;
			}

		}
	}

	/**
	 * Show current conflict in a dialog
	 * @param conflict
	 * @return the builder for the dialog
	 */
	public Builder showConflictdialog(FileDuplicate conflict) {
		AlertDialog.Builder builder;
		File src = conflict.src;
		File dst = conflict.dst;
		IconLoader loader = new IconLoader(actcontext);
		if (conflict.type == 2) {
			String fileordir, filename, messageformat, messagetxt;
			if (dst.isDirectory()) {
				fileordir = "directory";
			} else {
				fileordir = "file";
			}
			filename = src.getName();
			messageformat = getResources().getString(
					R.string.filefolderconflict);
			messagetxt = String.format(messageformat, fileordir, filename);
			builder = getTextDialog(messagetxt, filename);
		} else {
			builder = new AlertDialog.Builder(this);
			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View askdialogview = inflater
					.inflate(R.layout.conflictdialog, null);
			builder.setView(askdialogview);
			builder.setTitle(R.string.duplicatefound);
			TextView srcdescr = (TextView) askdialogview
					.findViewById(R.id.srcdescr);

			srcdescr.setText(operator.getfileinfo(src));
			srcdescr.setCompoundDrawables(null, loader.loadConflictico(src),
					null, null);
			TextView dstdescr = (TextView) askdialogview
					.findViewById(R.id.dstdescr);
			dstdescr.setText(operator.getfileinfo(dst));
			dstdescr.setCompoundDrawables(null, loader.loadConflictico(dst),
					null, null);

			CheckBox overwriteall = (CheckBox) askdialogview
					.findViewById(R.id.overwritecheck);
			overwriteall.setText(R.string.overwritefile);
		}
		return builder;
	}
}
