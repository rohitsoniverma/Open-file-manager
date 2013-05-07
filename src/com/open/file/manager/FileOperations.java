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
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;



/**
 * The class used to do operations on files
 */
public class FileOperations {
	// Context context;
	static int currentaction;
	static List<File> operationqueue = new ArrayList<File>();
	static String currentpath;
	WeakReference<MainActivity> act;
	public static AlertDialog currentdialog;
	public static ArrayList<FileDuplicate> conflicts;
	List<Integer> duptreepath = new ArrayList<Integer>();

	/**
	 * Constructor
	 * @param myact main activity
	 */
	public FileOperations(MainActivity myact) {
		act = new WeakReference<MainActivity>(myact);
		currentaction = Consts.ACTION_NONE;
	}

	
	/*
	 * @return Is a cutcopyservice running?
	 */
	public boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) act.get().getSystemService(
				Context.ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (CutCopyService.class.getName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}


	/**
	 * Ask the user about conflicts, creating dialogs
	 * 
	 * @param duplicates the list of duplicates to ask user
	 * @param overwritefiles boolean indicating weither user has selected to overwrite all
	 * @param overwritefolders same for folders
	 * @param current index of the current file
	 */
	protected void askconflicts(final ArrayList<FileDuplicate> duplicates,
			final boolean overwritefiles, final boolean overwritefolders,
			final int current) {
		AlertDialog.Builder builder;
		currentaction = Consts.ACTION_DUPLICATES;
		if (duplicates.size() == current) {
			//if we have finished asking, send results back
			if (duplicates.equals(conflicts)) {
				Message dupmsg = Message.obtain();
				dupmsg.what=Consts.MSG_DUPLICATES;
				ArrayList<FileDuplicate> tmpduplicates = conflicts;
				Bundle dupdata = new Bundle();
				dupdata.putParcelableArrayList("duplicates", tmpduplicates);
				dupmsg.setData(dupdata);
				if (isMyServiceRunning() && CutCopyService.mHandler != null) {
					CutCopyService.mHandler.sendMessage(dupmsg);
					duptreepath.clear();
					conflicts.clear();
					currentaction = Consts.ACTION_NONE;
				}
			}
			//else we are in a sub-folder, return up
			else {
				int oldindex;
				ArrayList<FileDuplicate> restoredup = conflicts;
				for (int i = 0; i < duptreepath.size() - 1; i++) {
					oldindex = duptreepath.get(i);
					restoredup = restoredup.get(oldindex).childDuplicates;
				}
				oldindex = duptreepath.remove(duptreepath.size() - 1) + 1;
				askconflicts(restoredup, overwritefiles, overwritefolders,
						oldindex);
			}
			return;
		}
		final FileDuplicate conflict = duplicates.get(current);
		//handle already processed duplicates
		if (conflict.processed) {
			if (conflict.childDuplicates.size() > 0) {
				duptreepath.add(current);
				askconflicts(conflict.childDuplicates, overwritefiles,
						overwritefolders, 0);
			} else {
				askconflicts(duplicates, overwritefiles, overwritefolders,
						current + 1);
			}
			return;
		}
		//if conflict is between dirs, check if we selected to overwrite
		if (conflict.type == Consts.CONFLICT_DIR_DIR) {
			if (overwritefolders) {
				conflict.overwrite = true;
				conflict.processed = true;
				if (conflict.childDuplicates.size() > 0) {
					duptreepath.add(current);
					askconflicts(conflict.childDuplicates, overwritefiles,
							overwritefolders, 0);
				} else {
					askconflicts(duplicates, overwritefiles, overwritefolders,
							current + 1);
				}
				return;
			}
		}
		//same for file/file conflict
		if (conflict.type == Consts.CONFLICT_FILE_FILE) {
			if (overwritefiles) {
				conflict.overwrite = true;
				conflict.processed = true;
				askconflicts(duplicates, overwritefiles, overwritefolders,
						current + 1);
				return;
			}
		}
		
		//show appropriate dialog
		builder = act.get().showConflictdialog(conflict);
		
		if (conflict.type == Consts.CONFLICT_FILE_DIR) {
			builder.setPositiveButton(R.id.rename,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							Editable newname = ((EditText) ((AlertDialog) dialog)
									.findViewById(R.id.newfilename)).getText();
							conflict.overwrite = true;
							conflict.processed = true;
							conflict.newname = newname.toString();
							askconflicts(duplicates, overwritefiles,
									overwritefolders, current + 1);
						}
					});
		} else {
			builder.setPositiveButton(R.string.overwrite,
					new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							conflict.overwrite = true;
							conflict.processed = true;
							CheckBox overwrite = (CheckBox) ((AlertDialog) dialog)
									.findViewById(R.id.overwritecheck);
							if (conflict.src.isDirectory()) {
								if (conflict.childDuplicates.size() > 0) {
									duptreepath.add(current);
									askconflicts(conflict.childDuplicates,
											overwritefiles,
											overwrite.isChecked(), 0);
								} else {
									askconflicts(duplicates, overwritefiles,
											overwrite.isChecked(), current + 1);
								}
							} else {
								askconflicts(duplicates, overwrite.isChecked(),
										overwritefolders, current + 1);
							}
						}
					});
		}
		builder.setCancelable(false);
		builder.setNegativeButton(R.string.skip,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						conflict.overwrite = false;
						askconflicts(duplicates, overwritefiles,
								overwritefolders, current + 1);
					}
				});
		currentdialog = builder.create();
		currentdialog.show();
	}

	/**
	 * Start the service to copy or move files
	 * @param destination folder
	 */
	public void startcutcopyservice(String targetfolder) {
		List<String> cutcopylist = new ArrayList<String>();
		Intent cutcopyintent = new Intent(act.get(), CutCopyService.class);
		cutcopyintent.putExtra("action", currentaction);
		for (int i = 0; i < operationqueue.size(); i++) {
			cutcopylist.add(operationqueue.get(i).getAbsolutePath());
		}
		cutcopyintent.putStringArrayListExtra("filelist",
				(ArrayList<String>) cutcopylist);
		cutcopyintent.putExtra("targetfolder", targetfolder);
		act.get().startService(cutcopyintent);
		cutcopylist.clear();
	}

	
	/**
	 * Actually remove files
	 */
	public void performremove() {
		for (File current : operationqueue) {
			if (current.exists() && current.canWrite()) {
				DeleteRecursive(current);
			}
		}
		MainActivity.mAdapter.updatefrags();
		currentaction = Consts.ACTION_NONE;
	}

	/**
	 * Delete in a recursive way
	 * @param fileOrDirectory file or dir to delete
	 */
	void DeleteRecursive(File fileOrDirectory) {
		if (fileOrDirectory.isDirectory())
			for (File child : fileOrDirectory.listFiles())
				DeleteRecursive(child);
		fileOrDirectory.delete();
	}

	/**
	 * Checks if selected files are writeable, asks user to confirm remove
	 * @param selectedfiles
	 */
	public void removefiles(List<File> selectedfiles) {
		currentaction = Consts.ACTION_REMOVE;
		operationqueue.addAll(selectedfiles);
		final List<File> notwriteable = new ArrayList<File>();
		for (File current : selectedfiles) {
			if (!current.canWrite()) {
				notwriteable.add(current);
			}
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(act.get());
		final LayoutInflater inflater = act.get().getLayoutInflater();
		View removedialogview = inflater.inflate(R.layout.removedialog, null);
		builder.setView(removedialogview);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				if (notwriteable.size() > 0) {
					wannaremovenowriteable(notwriteable);
				} else {
					performremove();
					operationqueue.clear();
				}
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		currentdialog = builder.create();
		currentdialog.show();
	}

	/**
	 * Notifies the user that some files are not writeable, asks him what to do
	 * @param nowriteable
	 */
	public void wannaremovenowriteable(List<File> nowriteable) {
		LayoutInflater inflater = act.get().getLayoutInflater();
		AlertDialog.Builder builder = new AlertDialog.Builder(act.get());
		View wannaView = inflater
				.inflate(R.layout.wannaremovenowriteable, null);
		builder.setView(wannaView);
		ListView list = (ListView) wannaView.findViewById(R.id.listnowrite);
		list.setAdapter(new ListFileAdapter(nowriteable, act.get()));
		builder.setPositiveButton(R.string.ignore,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						performremove();
					}
				});
		builder.setNegativeButton(R.string.cancel, null);
		currentdialog = builder.create();
		currentdialog.show();
		return;
	}

	/**
	 * Rename selected file
	 * @param selectedfiles in fact a single file is in the list
	 */
	public void renamefile(List<File> selectedfiles) {
		final File rename = selectedfiles.get(0);
		operationqueue.add(rename);
		currentaction = Consts.ACTION_RENAME;
		if (rename.canWrite()) {
			String renamestring = act.get().getResources()
					.getString(R.string.rename);
			AlertDialog.Builder dialbuild = act.get().getTextDialog(
					renamestring, rename.getName());
			dialbuild.setPositiveButton(R.string.rename,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Editable newname = ((EditText) ((AlertDialog) dialog)
									.findViewById(R.id.newfilename)).getText();
							if (rename.getName() != newname.toString()
									&& newname.length() != 0) {
								File newfile = new File(rename.getParentFile(),
										newname.toString());
								if (!newfile.exists()) {
									rename.renameTo(newfile);
									operationqueue.clear();
									MainActivity.mAdapter.updatefrags();
									currentaction = Consts.ACTION_NONE;
								} else {
									operationqueue.clear();
									act.get().displaySimpleDialog(
											R.string.error,
											R.string.renameexists);
								}
							}
						}

					});
			currentdialog = dialbuild.create();
			currentdialog.show();

		} else {
			act.get().displaySimpleDialog(R.string.cantrename, R.string.error);
		}
	}

	/**
	 * Called after user click on "paste"
	 * @param filelist files to copy
	 * @param path destination folder
	 * @param currentop ACTION_CUT or ACTION_COPY
	 */
	public void handlepaste(List<File> filelist, String path, int currentop) {
		currentaction = currentop;
		currentpath = path;
		if (!(new File(currentpath)).canWrite()) {
			act.get()
					.displaySimpleDialog(R.string.cantwritedir, R.string.error);
			return;
		}
		if (currentpath.equals(filelist.get(0).getParent())) {
			act.get().displaySimpleDialog(R.string.samefolder, R.string.error);
			return;
		}
		operationqueue = filelist;
		startcutcopyservice(path);
		operationqueue.clear();
		currentaction = Consts.ACTION_NONE;
	}

	/**
	 * Ask name for folder
	 * @param message the message to be shown above the editable text
	 */
	public void createfolder(final int message) {
		currentaction = Consts.ACTION_MKDIR;
		operationqueue.add(new File(currentpath));
		String newfolder = act.get().getResources()
				.getString(R.string.newfolder);
		String typenew = act.get().getResources().getString(message);
		AlertDialog.Builder dialbuild = act.get().getTextDialog(typenew,
				newfolder);
		dialbuild.setPositiveButton("OK",
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int id) {
						Editable newname = ((EditText) ((AlertDialog) dialog)
								.findViewById(R.id.newfilename)).getText();
						File newdirfile = new File(currentpath, newname
								.toString());
						if (newdirfile.exists()) {
							createfolder(R.string.newdirexists);
						} else {
							if (newname.toString().length() > 0) {
								newdirfile.mkdir();
								operationqueue.clear();
								MainActivity.mAdapter.updatefrags();
								currentpath = null;
								currentaction = Consts.ACTION_NONE;
							} else {
								createfolder(R.string.invalidname);
							}
						}
					}
				});
		dialbuild.setNegativeButton("Cancel", null);
		currentdialog = dialbuild.create();
		currentdialog.show();
	}
	
	public static String getMimeType(File src)
	{
		String path = src.getAbsolutePath().toLowerCase(Locale.getDefault()).replace(" ", "");
		String fileExtension = MimeTypeMap.getFileExtensionFromUrl(path);
		String mimeType=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
		return mimeType;
	}

	/**
	 * @param src the file we need info from
	 * @return the formatted info string
	 */
	public Spanned getfileinfo(File src) {
		DateFormat dateform = DateFormat.getDateTimeInstance();
		String format = act.get().getResources().getString(R.string.fileinfo);
		Date srcdate = new Date(src.lastModified());
		String srcsize, mimetype;
		if (src.isDirectory()) {
			srcsize = Integer.toString(src.listFiles().length) + " elements";
			mimetype = "Directory";
		} else {
			srcsize = FileOperations.gethumansize(src.length());
			
			mimetype = getMimeType(src);
			if (mimetype == null) {
				mimetype = "unknown";
			}
		}
		String permissions = "";
		permissions += (src.canRead() ? "R" : "-");
		permissions += (src.canWrite() ? "W" : "-");
		Boolean canexec;

		// The canexecute method is supported since api level 9, we use level 8
		// so in that case we put
		// ? in the string
		try {
			Method m = File.class.getMethod("canExecute", new Class[] {});
			canexec = (Boolean) m.invoke(src);
			permissions += (canexec ? "X" : "-");
		} catch (Exception exc) {
			permissions += "?";
		}

		String srcinfo = String.format(format, src.getName(), mimetype,
				srcsize, dateform.format(srcdate), permissions);
		Spanned retval = Html.fromHtml(srcinfo);
		return retval;
	}

	/**
	 * @param bytesize the size in bytes
	 * @return a string containing size and appropriate unit
	 */
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

	/**
	 * Restore operation if activity is restored
	 */
	public void restoreOp() {
		switch (currentaction) {
		case Consts.ACTION_COPY:
			handlepaste(operationqueue, currentpath, currentaction);
			break;
		case Consts.ACTION_CUT:
			handlepaste(operationqueue, currentpath, currentaction);
			break;
		case Consts.ACTION_DUPLICATES:
			askconflicts(conflicts, false, false, 0);
			break;
		case Consts.ACTION_MKDIR:
			createfolder(R.string.newdir);
			break;
		case Consts.ACTION_REMOVE:
			removefiles(operationqueue);
			break;
		case Consts.ACTION_RENAME:
			renamefile(operationqueue);
			break;
		default:
			break;
		}
	}
}
