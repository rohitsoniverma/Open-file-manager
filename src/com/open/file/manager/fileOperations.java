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
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

public class fileOperations
{
	//Context context;
	int currentaction;
	static List<File> operationqueue = new ArrayList<File>();
	String currentpath;
	MainActivity act;
	public static AlertDialog currentdialog;
	public static List<String> cutcopyqueue;
	public static ArrayList<fileDuplicate> conflicts;

	
	public fileOperations(Context appcont, MainActivity myact)
	{
		//context=appcont;
		act=myact;
	}
	
	
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) act.getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (cutcopyservice.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	protected void askconflicts(final ArrayList<fileDuplicate> duplicates,final boolean overwritefiles,
			final boolean overwritefolders, final int current) {
		AlertDialog.Builder builder;
		if(conflicts.size()<=current && duplicates.equals(conflicts))
		{
			Message dupmsg=Message.obtain();
			Bundle dupdata= new Bundle();
			dupdata.putParcelableArrayList("duplicates", duplicates);
			dupmsg.setData(dupdata);
			conflicts.clear();
			if(isMyServiceRunning() && cutcopyservice.mHandler!=null)
			{
			cutcopyservice.mHandler.sendMessage(dupmsg);
			}
			return;
		}
		final fileDuplicate conflict= duplicates.get(current);
		if(conflict.processed)
		{
			if(conflict.childDuplicates.size()>0)
			{
				askconflicts(conflict.childDuplicates, overwritefiles, overwritefolders, 0);
			}
			askconflicts(duplicates, overwritefiles, overwritefolders, current+1);
		}
		if(conflict.type==1)
		{
			if(overwritefolders)
			{
				conflict.overwrite=true;
				if(conflict.childDuplicates.size()>0)
				{
					askconflicts(conflict.childDuplicates, overwritefiles, overwritefolders, 0);
				}
				askconflicts(duplicates, overwritefiles, overwritefolders, current+1);
				return;
			}
		}
		if(conflict.type==3)
		{
			if(overwritefiles)
			{
				conflict.overwrite=true;
				askconflicts(duplicates, overwritefiles, overwritefolders, current+1);
				return;
			}
		}
		builder = act.showConflictdialog(conflict);
		if(conflict.type==2)
		{
			builder.setPositiveButton(R.id.rename, new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int id) {
	            	Editable newname=((EditText)((AlertDialog) dialog).findViewById(R.id.newfilename)).getText();
	            	conflict.overwrite=true;
	            	conflict.newname=newname.toString();
	            	askconflicts(duplicates, overwritefiles, overwritefolders, current+1);
	            }
			});
		}
		else
		{
			builder.setPositiveButton(R.string.overwrite, new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int id) {
	            	conflict.overwrite=true;
            		CheckBox overwrite= (CheckBox)((AlertDialog) dialog).findViewById(R.id.overwritecheck);
	            	if(conflict.src.isDirectory())
	            	{
	            		if(conflict.childDuplicates.size()>0)
	            		{
	            		askconflicts(conflict.childDuplicates, overwritefiles, overwrite.isChecked(), 0);
	            		}
	            		askconflicts(conflict.childDuplicates, overwritefiles, overwrite.isChecked(), current+1);
	            	}
	            	else
	            	{
	            	askconflicts(duplicates, overwrite.isChecked(), overwritefolders, current+1);
	            	}
	            }
			});
		}
		builder.setCancelable(false);
		builder.setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            	conflict.overwrite=false;
            	askconflicts(duplicates, overwritefiles, overwritefolders, current+1);
            }
		});
		currentdialog=builder.create();
		currentdialog.show();
	}
	
	public void startcutcopyservice(String targetfolder)
	{
		List<String> cutcopylist=new ArrayList<String>();
		Intent cutcopyintent=new Intent(act, cutcopyservice.class);
		cutcopyintent.putExtra("action", currentaction);
		for(int i=0; i<operationqueue.size(); i++)
		{
			cutcopylist.add(operationqueue.get(i).getAbsolutePath());
		}
		cutcopyintent.putStringArrayListExtra("filelist", (ArrayList<String>) cutcopylist);
		cutcopyintent.putExtra("targetfolder", targetfolder);
		act.startService(cutcopyintent);
		cutcopylist.clear();
	}
	
	public void performremove()
	{
		for (int i=0; i<operationqueue.size(); i++)
 	   	{
 		   File current= operationqueue.get(i);
 		   if(current.isDirectory())
 		   {
 		   DeleteRecursive(current);
 		   }
 		   else
 		   {
 			   if(current.exists())
 			   {
 			   current.delete();
 			   }
 		   }
 	   }
		operationqueue.clear();
		act.refreshcurrentgrid();
	}
	
	void DeleteRecursive(File fileOrDirectory) {
	    if (fileOrDirectory.isDirectory())
	        for (File child : fileOrDirectory.listFiles())
	            DeleteRecursive(child);
	    fileOrDirectory.delete();
	}

	public void removefiles(List<File> selectedfiles)
	{
		currentaction=consts.ACTION_REMOVE;
		final List <File> notwriteable= new ArrayList<File>();
		for (int i=0; i<selectedfiles.size(); i++)
		{
 		   File current= selectedfiles.get(i);
 		   if(!current.canWrite())
 		   {
 			   notwriteable.add(current);
 		   }
 		   else
 		   {
 			   operationqueue.add(current);
 		   }
 	   	}
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		final LayoutInflater inflater= act.getLayoutInflater();
		View removedialogview= inflater.inflate(R.layout.removedialog, null);
		builder.setView(removedialogview);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int id) {
            	   if(notwriteable.size()>0)
            	   {
            		   wannaremovenowriteable(notwriteable);
            	   }
            	   else
            	   {
            		   performremove();
            	   }
               }
	});
		builder.setNegativeButton(R.string.cancel, null);
		currentdialog = builder.create();
		currentdialog.show();
	}
	
	public void wannaremovenowriteable(List<File> nowriteable)
	{
		LayoutInflater inflater=act.getLayoutInflater();
		AlertDialog.Builder builder = new AlertDialog.Builder(act);
		View wannaView = inflater.inflate(R.layout.wannaremovenowriteable, null);
		builder.setView(wannaView);
		ListView list = (ListView) wannaView.findViewById(R.id.listnowrite);
		list.setAdapter(new listfileadapter(nowriteable, act));
		builder.setPositiveButton(R.string.ignore, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
         	   performremove();
            }
		});
		builder.setNegativeButton(R.string.cancel,null);
		currentdialog= builder.create();
		currentdialog.show();
		return;
	}
	
	public void renamefile(List<File> selectedfiles)
	{
		final File rename=selectedfiles.get(0);
		operationqueue.add(rename);
		currentaction=consts.ACTION_RENAME;
		if(rename.canWrite())
		{
			String renamestring = act.getResources().getString(R.string.rename);
			AlertDialog.Builder dialbuild=act.gettextdialog(renamestring, rename.getName());
			dialbuild.setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					Editable newname=  ((EditText)((AlertDialog) dialog).findViewById(R.id.newfilename)).getText();
					if(rename.getName()!=newname.toString() && newname.length() != 0)
					{
						File newfile=new File(rename.getParentFile(), newname.toString());
						if(!newfile.exists())
						{
						rename.renameTo(newfile);
						operationqueue.clear();
						act.refreshcurrentgrid();
						}
						else
						{
							operationqueue.clear();
							act.displaysimpledialog(R.string.error, R.string.renameexists);
						}
					}
				}
				
			});
			currentdialog=dialbuild.create();
			currentdialog.show();

		}
		else
		{
			act.displaysimpledialog(R.string.cantrename, R.string.error);
		}
	}

	public void handlepaste(List<File> filelist, String path, int currentop) {
		currentaction=currentop;
		currentpath=path;
		if(!(new File(currentpath)).canWrite())
		{
			act.displaysimpledialog(R.string.cantwritedir, R.string.error);
			return;
		}
		if(currentpath==filelist.get(0).getParent())
		{
			act.displaysimpledialog(R.string.samefolder, R.string.error);
			return;
		}
		operationqueue=filelist;
		startcutcopyservice(path);
		operationqueue.clear();
	}
	

	public void createfolder(final String currentpath, final int message) {
		currentaction=consts.ACTION_MKDIR;
		operationqueue.add(new File(currentpath));
		String newfolder=act.getResources().getString(R.string.newfolder);
		String typenew=act.getResources().getString(message);
		AlertDialog.Builder dialbuild=act.gettextdialog(typenew, newfolder);
		dialbuild.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id)
			{
				Editable newname=  ((EditText)((AlertDialog) dialog).findViewById(R.id.newfilename)).getText();
				File newdirfile=new File(currentpath, newname.toString());
				if(newdirfile.exists())
				{
					createfolder(currentpath, R.string.newdirexists);
				}
				else
				{
					if(newname.toString().length()>0)
					{
						newdirfile.mkdir();
						operationqueue.clear();
						act.refreshcurrentgrid();
					}
					else
					{
						createfolder(currentpath, R.string.invalidname);
					}
				}
			}
		});
		dialbuild.setNegativeButton("Cancel",null);
		currentdialog=dialbuild.create();
		currentdialog.show();
	}
	
	public static String gethumansize(long bytesize) {
		long dividefactor;
		String unit;
		if (bytesize >= 1073741824) {
			dividefactor = 1073741824;
			unit = "GiB";
		} else if (bytesize >= 1048576) {
			dividefactor = 1048576;
			unit = "MiB";
		} else if (bytesize >= 1024) {
			dividefactor = 1024;
			unit = "GiB";
		} else {
			dividefactor = 1;
			unit = "B";
		}
		return new String(Long.toString(bytesize / dividefactor) + " " + unit);
	}
}
