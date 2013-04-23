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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;


public class CutCopyService extends IntentService {
	
	public CutCopyService() {
		super("CutCopyService");
	}

	private int currentaction;
	private File targetfolder;
	private ArrayList<String> filelist;
	private Notification cutcopynotification;
	public NotificationManager cutcopymanager;
	private NotificationCompat.Builder cutcopybuilder;
	private RemoteViews progressview;
	private FileCopyTree tree;
	private ArrayList<FileDuplicate> duplicates;
	private static int id;
	private static int completeid=0;
	private long progressbytes;
	private int progresspercent;
	private long totalbytes;
	public static Handler mHandler;
	static int currentfileind;
	public final int[] actions=new int[] {R.string.copy, R.string.move};
	public final int[] actioning=new int[] {R.string.copyger, R.string.moveger};
	public final int[] actionspast=new int[] {R.string.copypast, R.string.movepast};
	static String actiongerund;
	PendingIntent contentIntent;

	/**
	 * Proceed to cut/copy file(s) if there is no duplicate(s)
	 */
	private void performCutCopy() {
		FileCopyNode current;
		while (currentfileind < tree.children.size()) {
			try {
				current = tree.children.get(currentfileind);
				if (current.duplicate != null && duplicates == null) {
					String waitingdup=getResources().getString(R.string.waitingduplicate);
					cutcopynotification.contentView.setTextViewText(R.id.progresstext, waitingdup);
					cutcopymanager.notify(id, cutcopynotification);
					Looper.loop();
				}
				performOperation(tree.children.get(currentfileind));
				currentfileind++;
			} catch (Exception e) {
				notifyError(R.string.unknownerror);
				e.printStackTrace();
			}
			currentfileind++;
		}
		finish();
	}

	/**
	 * Notify some error has occurred with notification
	 * @param errorRes resource to show
	 */
	private void notifyError(int errorRes) {
		String errformat,errstring;
		stopForeground(true);
		errformat= getResources().getString(errorRes);
		errstring=String.format(errformat, getResources().getString(actions[currentaction]));
		cutcopynotification=new Notification();
		cutcopynotification.contentView=new RemoteViews(
				getApplicationContext().getPackageName(),
				R.layout.errornot);
		cutcopynotification.contentView.setTextViewText(R.id.errortext, errstring);
		cutcopynotification.contentIntent=contentIntent;
		cutcopynotification.icon=R.drawable.error;
		cutcopymanager.notify(completeid, cutcopynotification);
	}

	/**
	 * Checks weither there's enough space to perform the operation
	 * @param current
	 * @return
	 */
	private boolean notEnoughSpace(FileCopyNode current) {
		StatFs targetfs=new StatFs(current.dstFile.getParent());
		return current.size > (long)targetfs.getAvailableBlocks()*(long)targetfs.getBlockSize();
	}
	
	private void finish()
	{
		NotificationCompat.Builder finishbuilder=new NotificationCompat.Builder(this);
		String completed=getResources().getString(R.string.completed);
		String actionpast=getResources().getString(actionspast[currentaction]);
		String finished=getResources().getString(R.string.succesfulcopy);
		finished=String.format(finished, FileOperations.gethumansize(totalbytes), actionpast);
		finishbuilder.setContentText(finished);
		finishbuilder.setContentTitle(completed);
		finishbuilder.setSmallIcon(R.drawable.complete);
		finishbuilder.setContentIntent(contentIntent);
		cutcopymanager.notify(completeid, finishbuilder.build());
		MainActivity.acthandler.sendEmptyMessage(Consts.MSG_FINISHED);
		stopForeground(true);
		return;
	}
	

	/**
	 * Show progress in notification
	 */
	private void updateProgress() {
		String progressstring;
		if(progresspercent !=(int) ((100 * progressbytes) / totalbytes))
		{
			progresspercent = (int) ((100 * progressbytes) / totalbytes);
			cutcopynotification.contentView.setProgressBar(R.id.progressBar, 100,
					progresspercent, false);
			progressstring = FileOperations.gethumansize(progressbytes) + "/"
					+ FileOperations.gethumansize(totalbytes);
			cutcopynotification.contentView.setTextViewText(R.id.textprogress,
					progressstring);
			cutcopymanager.notify(id, cutcopynotification);
		}
	}

	 /**
	  * Update duplicates in FileCopyNodes with duplicates received from
	  * the activity
	 * @param newduplic duplicates received
	 * @param files file nodes to update
	 */
	void updateDuplicates(ArrayList<FileDuplicate> newduplic,
			List<FileCopyNode> files) {
		FileDuplicate currentdup;
		int i, j = 0;
		for (i = 0; i < files.size(); i++) {
			FileCopyNode currentfile = files.get(i);
			if(j<newduplic.size())
			{
				currentdup = newduplic.get(j);
			}
			else
			{
				currentdup=null;
			}
			if (currentfile.duplicate != null && currentdup!=null) {
				currentfile.duplicate = currentdup;
				j++;
				if (currentfile.children.size() > 0 && currentdup.childDuplicates!=null) {
					updateDuplicates(currentdup.childDuplicates,
							currentfile.children);
				}
			}
		}

	}

	/**
	 * Cut or copy file
	 * @param filenode node to process
	 * @throws IOException
	 */
	private void performOperation(FileCopyNode filenode) throws IOException {
		if (filenode.duplicate != null) {
			if (!filenode.duplicate.overwrite) {
				totalbytes -= filenode.size;
				updateProgress();
				return;
			} else {
				//dir/dir conflict, continue transferring directory content
				if (filenode.duplicate.type == Consts.CONFLICT_DIR_DIR) {
					for (int i = 0; i < filenode.children.size(); i++) {
						performOperation(filenode.children.get(i));
					}
					return;
				}
				// file/dir conflict, rename destination
				if (filenode.duplicate.type == Consts.CONFLICT_FILE_DIR) {
					filenode.dstFile = new File(filenode.dstFile.getParent(),
							filenode.duplicate.newname);
				}
				// file/file conflict, remove destination
				else {
					filenode.dstFile.delete();
				}
			}
		}
		if (filenode.srcFile.isDirectory()) {
			filenode.dstFile.mkdir();
			for (int i = 0; i < filenode.children.size(); i++) {
				performOperation(filenode.children.get(i));
			}
			return;
		}
		if(currentaction==0)
		{
			copy(filenode, true);
		}
		else
		{
			if(renameSuccessful(filenode.srcFile, filenode.dstFile))
			{
				progressbytes+=filenode.size;
				updateProgress();
			}
			else
			{
				copy(filenode, false);
			}
		}
	}

	
	/**
	 * Try to rename file to move it
	 * @param src
	 * @param dst
	 * @return true if it worked, false otherwise
	 */
	private boolean renameSuccessful(File src, File dst) {
		return src.renameTo(dst);
	}

	/**
	 * Copy files
	 * @param filenode node to process
	 * @param keeporiginal
	 * @throws IOException
	 */
	private void copy(FileCopyNode filenode, boolean keeporiginal) throws IOException {
		if(notEnoughSpace(filenode))
		{
			notifyError(R.string.notenoughspace);
			stopSelf();
		}
		if(filenode.srcFile.length()==0)
		{
			filenode.dstFile.createNewFile();
			updateProgress();
			return;
		}
		InputStream in = new FileInputStream(filenode.srcFile);
		OutputStream out = new FileOutputStream(filenode.dstFile);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			Log.d("do i", "get here?");
			out.write(buf, 0, len);
			progressbytes+=len;
			updateProgress();
		}
		in.close();
		out.close();
		if (!keeporiginal) {
			filenode.srcFile.delete();
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		progressview = new RemoteViews(
				getApplicationContext().getPackageName(),
				R.layout.progressbarlayout);

		currentaction = intent.getIntExtra("action", 0);
		filelist = intent.getStringArrayListExtra("filelist");
		targetfolder = new File(intent.getStringExtra("targetfolder"));
		tree = new FileCopyTree(filelist, targetfolder);
		duplicates=null;
		currentfileind=0;
		progressbytes=0;
		progresspercent=0;
		totalbytes=tree.size;
		mHandler = new dupresponcehandler(this);
		if (tree.duplicates.size() != 0) {
			Message dupmsg = Message.obtain();
			dupmsg.what=Consts.MSG_DUPLICATES;
			Bundle dupdata = new Bundle();
			dupdata.putParcelableArrayList("duplicates", tree.duplicates);
			dupmsg.setData(dupdata);
			MainActivity.acthandler.sendMessage(dupmsg);
		}
		id = 1;
		completeid=(completeid+2)%Integer.MAX_VALUE;
		cutcopymanager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		cutcopybuilder = new NotificationCompat.Builder(this);
		// cutcopybuilder.setProgress(100, 0, false);

		Intent notificationIntent = new Intent(this, MainActivity.class);
		notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
		contentIntent = PendingIntent.getActivity(
				this, 0, notificationIntent, 0);
		cutcopybuilder.setContent(new RemoteViews(getApplicationContext()
				.getPackageName(), R.layout.progressbarlayout));
		cutcopybuilder.setSmallIcon(R.drawable.notifyicon);
		actiongerund=getResources().getString(actioning[currentaction]);
		cutcopybuilder.setContentTitle(actiongerund+" files");
		cutcopybuilder.setContentIntent(contentIntent);
		cutcopynotification = cutcopybuilder.build();
		cutcopynotification.contentView = progressview;
		cutcopynotification.contentView.setProgressBar(R.id.progressBar, 100,
				0, false);
		cutcopynotification.contentView.setTextViewText(R.id.progresstext,
				actiongerund + " files");
		cutcopymanager.notify(id, cutcopynotification);
		startForeground(id, cutcopynotification);
		performCutCopy();
	}

	static class dupresponcehandler extends Handler
	{
		WeakReference<CutCopyService> mservice;
		
		dupresponcehandler(CutCopyService service)
		{
			mservice=new WeakReference<CutCopyService>(service);
		}
		
		@Override
		public void handleMessage(Message msg) {
			
			if(mservice!=null && mservice.get()!=null)
			{
			CutCopyService currentservice=mservice.get();
			currentservice.duplicates=msg.getData().getParcelableArrayList("duplicates");
			currentservice.tree.duplicates=currentservice.duplicates;
			currentservice.updateDuplicates(currentservice.duplicates, currentservice.tree.children);
			currentservice.cutcopynotification.contentView.setTextViewText(R.id.progresstext, actiongerund + " files");
			currentservice.cutcopymanager.notify(id, currentservice.cutcopynotification);
			currentservice.performCutCopy();
			}
			//Looper.myLooper().quit();
			
		}
	}
}
