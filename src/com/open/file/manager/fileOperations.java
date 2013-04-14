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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

public class fileOperations
{
	//Context context;
	static int currentaction;
	static List<File> operationqueue = new ArrayList<File>();
	static String currentpath;
	WeakReference<MainActivity> act;
	public static AlertDialog currentdialog;
	public static ArrayList<fileDuplicate> conflicts;
	List<Integer> duptreepath=new ArrayList<Integer>();

	
	public fileOperations(Context appcont, MainActivity myact)
	{
		act=new WeakReference<MainActivity>(myact);
		currentaction=consts.ACTION_NONE;
	}
	
	
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) act.get().getSystemService(Context.ACTIVITY_SERVICE);
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
		currentaction=consts.ACTION_DUPLICATES;
		if(duplicates.size()==current)
		{
			if(duplicates.equals(conflicts))
			{
			Log.d("i am", "here");
			Message dupmsg=Message.obtain();
			ArrayList<fileDuplicate> tmpduplicates=conflicts;
			Bundle dupdata= new Bundle();
			dupdata.putParcelableArrayList("duplicates", tmpduplicates);
			dupmsg.setData(dupdata);
			if(isMyServiceRunning() && cutcopyservice.mHandler!=null)
			{
			if(!cutcopyservice.mHandler.sendMessage(dupmsg))
			{
				Log.d("not", "sent");
			}
			Log.d("conflictsize", Integer.toString(conflicts.size()));
			Log.d("message", "sent");
			duptreepath.clear();
			conflicts.clear();
			currentaction=consts.ACTION_NONE;
			}
			}
			else
			{
				int oldindex;
				ArrayList<fileDuplicate> restoredup = conflicts;
				for(int i=0; i<duptreepath.size()-1; i++)
				{
					oldindex=duptreepath.get(i);
					restoredup=restoredup.get(oldindex).childDuplicates;
				}
				oldindex=duptreepath.remove(duptreepath.size()-1)+1;
				askconflicts(restoredup, overwritefiles, overwritefolders, oldindex);
			}
			return;
		}
		final fileDuplicate conflict= duplicates.get(current);
		if(conflict.processed)
		{
			if(conflict.childDuplicates.size()>0)
			{
				duptreepath.add(current);
				askconflicts(conflict.childDuplicates, overwritefiles, overwritefolders, 0);
			}
			else
			{
			askconflicts(duplicates, overwritefiles, overwritefolders, current+1);
			}
		}
		if(conflict.type==1)
		{
			if(overwritefolders)
			{
				conflict.overwrite=true;
            	conflict.processed=true;
				if(conflict.childDuplicates.size()>0)
				{
					duptreepath.add(current);
					askconflicts(conflict.childDuplicates, overwritefiles, overwritefolders, 0);
				}
				else
				{
				askconflicts(duplicates, overwritefiles, overwritefolders, current+1);
				}
				return;
			}
		}
		if(conflict.type==3)
		{
			if(overwritefiles)
			{
				conflict.overwrite=true;
            	conflict.processed=true;
				askconflicts(duplicates, overwritefiles, overwritefolders, current+1);
				return;
			}
		}
		builder = act.get().showConflictdialog(conflict);
		if(conflict.type==2)
		{
			builder.setPositiveButton(R.id.rename, new DialogInterface.OnClickListener() {
	            @Override
	            public void onClick(DialogInterface dialog, int id) {
	            	Editable newname=((EditText)((AlertDialog) dialog).findViewById(R.id.newfilename)).getText();
	            	conflict.overwrite=true;
	            	conflict.processed=true;
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
	            	conflict.processed=true;
            		CheckBox overwrite= (CheckBox)((AlertDialog) dialog).findViewById(R.id.overwritecheck);
	            	if(conflict.src.isDirectory())
	            	{
	            		if(conflict.childDuplicates.size()>0)
	            		{
	        				duptreepath.add(current);
	        				askconflicts(conflict.childDuplicates, overwritefiles, overwrite.isChecked(), 0);
	            		}
	            		else
	            		{
	            		askconflicts(duplicates, overwritefiles, overwrite.isChecked(), current+1);
	            		}
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
		Intent cutcopyintent=new Intent(act.get(), cutcopyservice.class);
		cutcopyintent.putExtra("action", currentaction);
		for(int i=0; i<operationqueue.size(); i++)
		{
			cutcopylist.add(operationqueue.get(i).getAbsolutePath());
		}
		cutcopyintent.putStringArrayListExtra("filelist", (ArrayList<String>) cutcopylist);
		cutcopyintent.putExtra("targetfolder", targetfolder);
		act.get().startService(cutcopyintent);
		cutcopylist.clear();
	}
	
	public void performremove()
	{
		for (File current : operationqueue)
 	   	{
 			   if(current.exists() && current.canWrite())
 			   {
 		 		   DeleteRecursive(current);
 			   }
 	   }
		act.get().refreshcurrentgrid();
		currentaction=consts.ACTION_NONE;
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
		operationqueue.addAll(selectedfiles);
		final List <File> notwriteable= new ArrayList<File>();
		for (File current : selectedfiles)
		{
			Log.d("current", current.toString());
 		   if(!current.canWrite())
 		   {
 			   notwriteable.add(current);
 		   }
 	   	}
		AlertDialog.Builder builder = new AlertDialog.Builder(act.get());
		final LayoutInflater inflater= act.get().getLayoutInflater();
		View removedialogview= inflater.inflate(R.layout.removedialog, null);
		builder.setView(removedialogview);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
               @Override
               public void onClick(DialogInterface dialog, int id) {
            	   if(notwriteable.size()>0)
            	   {
            		   Log.d("nowriteble", "what");
            		   wannaremovenowriteable(notwriteable);
            	   }
            	   else
            	   {
            		   Log.d("ok", "entering");
            		   performremove();
            			operationqueue.clear();
            	   }
               }
	});
		builder.setNegativeButton(R.string.cancel, null);
		currentdialog = builder.create();
		currentdialog.show();
	}
	
	public void wannaremovenowriteable(List<File> nowriteable)
	{
		LayoutInflater inflater=act.get().getLayoutInflater();
		AlertDialog.Builder builder = new AlertDialog.Builder(act.get());
		View wannaView = inflater.inflate(R.layout.wannaremovenowriteable, null);
		builder.setView(wannaView);
		ListView list = (ListView) wannaView.findViewById(R.id.listnowrite);
		list.setAdapter(new listfileadapter(nowriteable, act.get()));
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
			String renamestring = act.get().getResources().getString(R.string.rename);
			AlertDialog.Builder dialbuild=act.get().gettextdialog(renamestring, rename.getName());
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
						act.get().refreshcurrentgrid();
						currentaction=consts.ACTION_NONE;
						}
						else
						{
							operationqueue.clear();
							act.get().displaysimpledialog(R.string.error, R.string.renameexists);
						}
					}
				}
				
			});
			currentdialog=dialbuild.create();
			currentdialog.show();

		}
		else
		{
			act.get().displaysimpledialog(R.string.cantrename, R.string.error);
		}
	}

	public void handlepaste(List<File> filelist, String path, int currentop) {
		currentaction=currentop;
		currentpath=path;
		if(!(new File(currentpath)).canWrite())
		{
			act.get().displaysimpledialog(R.string.cantwritedir, R.string.error);
			return;
		}
		if(currentpath.equals(filelist.get(0).getParent()))
		{
			act.get().displaysimpledialog(R.string.samefolder, R.string.error);
			return;
		}
		operationqueue=filelist;
		startcutcopyservice(path);
		operationqueue.clear();
		currentaction=consts.ACTION_NONE;
	}
	

	public void createfolder(final int message) {
		currentaction=consts.ACTION_MKDIR;
		operationqueue.add(new File(currentpath));
		String newfolder=act.get().getResources().getString(R.string.newfolder);
		String typenew=act.get().getResources().getString(message);
		AlertDialog.Builder dialbuild=act.get().gettextdialog(typenew, newfolder);
		dialbuild.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id)
			{
				Editable newname=  ((EditText)((AlertDialog) dialog).findViewById(R.id.newfilename)).getText();
				File newdirfile=new File(currentpath, newname.toString());
				if(newdirfile.exists())
				{
					createfolder(R.string.newdirexists);
				}
				else
				{
					if(newname.toString().length()>0)
					{
						newdirfile.mkdir();
						operationqueue.clear();
						act.get().refreshcurrentgrid();
						currentpath=null;
						currentaction=consts.ACTION_NONE;
					}
					else
					{
						createfolder(R.string.invalidname);
					}
				}
			}
		});
		dialbuild.setNegativeButton("Cancel",null);
		currentdialog=dialbuild.create();
		currentdialog.show();
	}
	
	public Spanned getfileinfo(File src)
	{
		DateFormat dateform=DateFormat.getDateTimeInstance();
		String format=act.get().getResources().getString(R.string.fileinfo);
		Date srcdate=new Date(src.lastModified());
		String srcsize, mimetype;
		if(src.isDirectory())
		{
			srcsize=Integer.toString(src.listFiles().length)+" elements";
			mimetype="Directory";
		}
		else
		{
			srcsize=fileOperations.gethumansize(src.length());
			String fileExtension = MimeTypeMap.getFileExtensionFromUrl(src.getAbsolutePath()).toLowerCase();
			mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
			if(mimetype==null)
			{
				mimetype="unknown";
			}
		}
		
		String srcinfo= String.format(format, src.getName(),mimetype ,srcsize, dateform.format(srcdate));
		Spanned retval=Html.fromHtml(srcinfo);
		return retval;
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
			unit = "KiB";
		} else {
			dividefactor = 1;
			unit = "B";
		}
		return new String(Long.toString(bytesize / dividefactor) + " " + unit);
	}


	public void restoreOp() {
		switch(currentaction) {
		case consts.ACTION_COPY:
			handlepaste(operationqueue, currentpath, currentaction);
			break;
		case consts.ACTION_CUT:
			handlepaste(operationqueue, currentpath, currentaction);
			break;
		case consts.ACTION_DUPLICATES:
			Log.d("restoring", "duplicates");
			askconflicts(conflicts, false, false, 0);
			break;
		case consts.ACTION_MKDIR:
			createfolder(R.string.newdir);
			break;
		case consts.ACTION_REMOVE:
			removefiles(operationqueue);
			break;
		case consts.ACTION_RENAME:
			renamefile(operationqueue);
			break;
		case consts.ACTION_NONE:
			break;
		}
			
	}
}
