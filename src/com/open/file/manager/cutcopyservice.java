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

public class cutcopyservice extends IntentService {
	public cutcopyservice() {
		super("cutcopyservice");
	}

	private int currentaction;
	private File targetfolder;
	private ArrayList<String> filelist;
	private Notification cutcopynotification;
	public NotificationManager cutcopymanager;
	private NotificationCompat.Builder cutcopybuilder;
	private RemoteViews progressview;
	private FileCopyTree tree;
	private ArrayList<fileDuplicate> duplicates;
	private static int id;
	private long progressbytes = 0;
	private int progresspercent = 0;
	private static long totalbytes;
	public static Handler mHandler;
	public final int[] actions=new int[] {R.string.copy, R.string.move};
	public final int[] actioning=new int[] {R.string.copyger, R.string.moveger};
	public final int[] actionspast=new int[] {R.string.copypast, R.string.movepast};
	static String actiongerund;
	PendingIntent contentIntent;

	private void performcutcopy() {
		int i = 0;
		FileCopyNode current;
		totalbytes = tree.size;
		mHandler = new dupresponcehandler(this);

		while (i < tree.children.size()) {
			try {
				current = tree.children.get(i);
				if (current.duplicate != null && duplicates == null) {
					String waitingdup=getResources().getString(R.string.waitingduplicate);
					cutcopynotification.contentView.setTextViewText(R.id.progresstext, waitingdup);
					cutcopymanager.notify(id, cutcopynotification);
					Looper.loop();
				}
				Log.d("past", "duplicates");
				performoperation(tree.children.get(i));
				i++;
			} catch (Exception e) {
				notifyerror(R.string.unknownerror);
				e.printStackTrace();
			}
			i++;
		}
	}

	private void notifyerror(int errorRes) {
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
		cutcopymanager.notify(id+1, cutcopynotification);
	}

	private boolean notenoughspace(FileCopyNode current) {
		StatFs targetfs=new StatFs(current.dstFile.getParent());
		return current.size > (long)targetfs.getAvailableBlocks()*(long)targetfs.getBlockSize();
	}

	

	private void updateprogress() {
		String progressstring;
		if (progressbytes == totalbytes) {
			NotificationCompat.Builder finishbuilder=new NotificationCompat.Builder(this);
			String completed=getResources().getString(R.string.completed);
			String actionpast=getResources().getString(actionspast[currentaction]);
			String finished=getResources().getString(R.string.succesfulcopy);
			finished=String.format(finished, fileOperations.gethumansize(totalbytes), actionpast);
			finishbuilder.setContentText(finished);
			finishbuilder.setContentTitle(completed);
			finishbuilder.setSmallIcon(R.drawable.complete);
			finishbuilder.setContentIntent(contentIntent);
			cutcopymanager.notify(id+1, finishbuilder.build());
			stopForeground(true);
			super.onDestroy();
			return;
		}
		if(progresspercent !=(int) ((100 * progressbytes) / totalbytes))
		{
			progresspercent = (int) ((100 * progressbytes) / totalbytes);
			cutcopynotification.contentView.setProgressBar(R.id.progressBar, 100,
					progresspercent, false);
			progressstring = fileOperations.gethumansize(progressbytes) + "/"
					+ fileOperations.gethumansize(totalbytes);
			cutcopynotification.contentView.setTextViewText(R.id.textprogress,
					progressstring);
			cutcopymanager.notify(id, cutcopynotification);
		}
	}

	 void updateduplicates(ArrayList<fileDuplicate> newduplic,
			List<FileCopyNode> files) {
		fileDuplicate currentdup;
		Log.d("newduplic size", Integer.toString(newduplic.size()));
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
			if (currentfile.duplicate != null) {
				currentfile.duplicate = currentdup;
				j++;
				if (currentfile.children.size() > 0 && currentdup.childDuplicates!=null) {
					updateduplicates(currentdup.childDuplicates,
							currentfile.children);
				}
			}
		}

	}

	private void performoperation(FileCopyNode filenode) throws IOException {
		if (filenode.duplicate != null) {
			if (!filenode.duplicate.overwrite) {
				Log.d("skipping", "filezz");
				totalbytes -= filenode.size;
				updateprogress();
				return;
			} else {
				// caso conflitto dir/dir, continuo semplicemente a scorrere
				// l'albero
				if (filenode.duplicate.type == 1) {
					for (int i = 0; i < filenode.children.size(); i++) {
						performoperation(filenode.children.get(i));
					}
					return;
				}
				// caso conflitto file/dir, rinomino la destinazione
				if (filenode.duplicate.type == 2) {
					filenode.dstFile = new File(filenode.dstFile.getParent(),
							filenode.duplicate.newname);
				}
				// caso conflitto file/file, rimuovo il file destinazione
				else {
					filenode.dstFile.delete();
				}
			}
		}
		if (filenode.srcFile.isDirectory()) {
			filenode.dstFile.mkdir();
			for (int i = 0; i < filenode.children.size(); i++) {
				performoperation(filenode.children.get(i));
			}
			return;
		}
		if(currentaction==0)
		{
			copy(filenode, true);
		}
		else
		{
			if(samepartition(filenode.srcFile, filenode.dstFile))
			{
				progressbytes+=filenode.size;
				updateprogress();
			}
			else
			{
				copy(filenode, false);
			}
		}
	}

	private boolean samepartition(File src, File dst) {
		return src.renameTo(dst);
	}

	private void copy(FileCopyNode filenode, boolean keeporiginal) throws IOException {
		if(notenoughspace(filenode))
		{
			notifyerror(R.string.notenoughspace);
			super.onDestroy();
		}
		InputStream in = new FileInputStream(filenode.srcFile);
		OutputStream out = new FileOutputStream(filenode.dstFile);
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
			progressbytes+=len;
			updateprogress();
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

		if (tree.duplicates.size() != 0) {
			Message dupmsg = Message.obtain();
			Bundle dupdata = new Bundle();
			dupdata.putParcelableArrayList("duplicates", tree.duplicates);
			dupmsg.setData(dupdata);
			MainActivity.dupHandler.sendMessage(dupmsg);
		}
		id = 1;
		cutcopymanager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		cutcopybuilder = new NotificationCompat.Builder(this);
		// cutcopybuilder.setProgress(100, 0, false);

		Intent notificationIntent = new Intent();
		contentIntent = PendingIntent.getActivity(
				getBaseContext(), 0, notificationIntent, 0);
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
		performcutcopy();
	}

	static class dupresponcehandler extends Handler
	{
		WeakReference<cutcopyservice> mservice;
		
		dupresponcehandler(cutcopyservice service)
		{
			mservice=new WeakReference<cutcopyservice>(service);
		}
		
		@Override
		public void handleMessage(Message msg) {
			
			if(mservice!=null && mservice.get()!=null)
			{
			cutcopyservice currentservice=mservice.get();
			currentservice.duplicates=msg.getData().getParcelableArrayList("duplicates");
			if(currentservice.duplicates.isEmpty())
			{
				Log.d("wtf", "empty");
			}
			currentservice.tree.duplicates=currentservice.duplicates;
			currentservice.updateduplicates(currentservice.duplicates, currentservice.tree.children);
			currentservice.cutcopynotification.contentView.setTextViewText(R.id.progresstext, actiongerund + " files");
			currentservice.cutcopymanager.notify(id, currentservice.cutcopynotification);
			}
			Looper.myLooper().quit();
			
		}
	}
}
