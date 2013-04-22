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
import java.util.Hashtable;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.open.file.manager.GridAdapter.Gridviewholder;

public class IconLoader
{
	private final static int cacheSize=(int) ((Runtime.getRuntime().maxMemory())/6);;
	static final LruCache<String,Bitmap> bitmapCache=new LruCache<String, Bitmap>(cacheSize) {

		@Override
		protected int sizeOf(String key, Bitmap bitmap) {
			return (bitmap.getRowBytes()*bitmap.getHeight());
		}
		
	};;;
	private static Context mycont;
	final static Hashtable<String, Integer> icons=new Hashtable<String, Integer>(14)
			{
				private static final long serialVersionUID = -4906177634872793364L;

			{
					put("audio", R.drawable.audiogeneric);
					put("application", R.drawable.applicationgeneric);
					put("video", R.drawable.videogeneric);
					put("text", R.drawable.textgeneric);
					put("directory", R.drawable.directory);
					put("application/vnd.android.package-archive", R.drawable.apk);
					put("application/pdf", R.drawable.pdf);
					put("image", R.drawable.imagegeneric);
					put("application/zip", R.drawable.archive);
					put("application/x-tar", R.drawable.archive);
					put("application/x-gzip", R.drawable.archive);
					put("application/msword", R.drawable.msword);
					put("text/html", R.drawable.html);
					put("application/vnd.oasis.opendocument.text", R.drawable.msword);
			}};


	public IconLoader(Context ct)
	{
		mycont=ct;
	}

	private boolean cancelPotentialWork(Gridviewholder holder)
	{
		if(holder.loader!=null)
		{
			if(!holder.associatedfile.equals(holder.loader.current))
			{
				holder.loader.cancel(true);
				return true;
			}
			return false;
		}
		return true;
	}

	/**
	 * Load icon for current grid element
	 * @param holder holder associated to grid view
	 * @param position view position in the grid
	 */
	@SuppressLint("NewApi")
	public void loadIcon(Gridviewholder holder, int position)
	{
		ImageView iv=holder.fileicon;
		String key=holder.associatedfile.getAbsolutePath();
		if(bitmapCache.get(key)==null)
		{
			if(cancelPotentialWork(holder))
			{
				holder.loader=new AsyncImgLoad(mycont, holder, position);
				if(android.os.Build.VERSION.SDK_INT>=11)
				{
					holder.loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				}
				else
				{
				holder.loader.execute();
				}
			}
		}
		else
		{
			iv.setImageBitmap(bitmapCache.get(key));
		}
	}
	
	/**
	 * Get icon from the apk file
	 * @param current apk to load icon from
	 * @return icon bitmap
	 */
	private Bitmap getApkIcon(File current)
	{
		try
		{
		Bitmap icon = null;
		PackageInfo packageInfo = mycont.getPackageManager().getPackageArchiveInfo(current.getAbsolutePath(), 
				PackageManager.GET_ACTIVITIES);
		if(packageInfo != null) {
			ApplicationInfo appInfo = packageInfo.applicationInfo;
			if (Build.VERSION.SDK_INT >= 8) {
				appInfo.sourceDir = current.getAbsolutePath();
				appInfo.publicSourceDir = current.getAbsolutePath();
			}
			Drawable apkico = appInfo.loadIcon(mycont.getPackageManager());
			final float scale = mycont.getResources().getDisplayMetrics().density;
			final int targetHeight= Math.round(Consts.ICON_SIZE*scale);
			final int targetWidth=Math.round(Consts.ICON_SIZE*scale);
			icon = ((BitmapDrawable) apkico).getBitmap();
			icon=Bitmap.createScaledBitmap(icon, targetWidth, targetHeight, false);
		}
		return icon;
		}
		catch (Exception exc)
		{
			return null;
		}
	}

	/**
	 * Get icon for file
	 * @param current
	 * @return icon bitmap
	 */
	public Bitmap getIcon(File current)
	{
		Bitmap icon;
		if(current.isDirectory()==true)
		{   
			icon=BitmapFactory.decodeResource(mycont.getResources(), R.drawable.directory);
		}
		else
		{
			String fileExtension = MimeTypeMap.getFileExtensionFromUrl(current.getAbsolutePath());
			fileExtension=fileExtension.toLowerCase(Locale.getDefault());
			String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
			String generictype;
			String imgregexp="image/(jpg|jpeg|png)";
			BitmapFactory.Options previewoptions=new BitmapFactory.Options();
			previewoptions.inJustDecodeBounds=true;
			try
			{
				generictype= mimetype.split("/")[0];
			}
			catch(Exception e)
			{
				generictype=null;
			}
			if(mimetype != null && mimetype.matches(imgregexp))
			{
				BitmapFactory.decodeFile(current.getAbsolutePath(), previewoptions);
				previewoptions.inSampleSize=getScaleratio(previewoptions);
				previewoptions.inJustDecodeBounds=false;
				icon=BitmapFactory.decodeFile(current.getAbsolutePath(), previewoptions);
				if(icon==null)
				{
					icon=BitmapFactory.decodeResource(mycont.getResources(), icons.get("image"));
				}
			}
			else if(mimetype=="application/vnd.android.package-archive")
			{	
				icon=getApkIcon(current);
				if(icon==null)
				{
					icon=BitmapFactory.decodeResource(mycont.getResources(), icons.get(mimetype));
				}
			}
			else if(mimetype != null && icons.containsKey(mimetype))
			{
				icon=BitmapFactory.decodeResource(mycont.getResources(), icons.get(mimetype));
			}
			else if(generictype !=null && icons.containsKey(generictype))
			{
				icon=BitmapFactory.decodeResource(mycont.getResources(), icons.get(generictype));
			}
			else
			{
				icon=BitmapFactory.decodeResource(mycont.getResources(), R.drawable.unknownfile);;
			}
		}
		return icon;
	}

	/**
	 * Get scale ratio to resize images
	 * @param bounds image options
	 * @return scale ratio
	 */
	private static int getScaleratio(Options bounds) {
		final float scale = mycont.getResources().getDisplayMetrics().density;
		final int targetHeight= Math.round(Consts.ICON_SIZE*scale);
		final int targetWidth=Math.round(Consts.ICON_SIZE*scale);
		int scaleratio=1;
		if(bounds.outHeight> targetHeight || bounds.outWidth>targetWidth)
		{
			final int heightRatio=Math.round(bounds.outHeight/targetHeight);
			final int widthRatio=Math.round(bounds.outWidth/targetWidth);

			scaleratio= heightRatio>widthRatio? heightRatio : widthRatio;
		}
		return scaleratio;
	}

	/**
	 * Load icon for conflict dialog
	 * @param current
	 * @return icon drawable
	 */
	public Drawable loadConflictico(File current)
	{
		Drawable icodraw;
		Bitmap iconbm;
		iconbm=getIcon(current);
		icodraw=new BitmapDrawable(mycont.getResources(), iconbm);
		icodraw.setBounds(new Rect(0, 0, iconbm.getWidth(), iconbm.getHeight()));
		return icodraw;
	}

	class AsyncImgLoad extends AsyncTask<Void, Void, Void>
	{
		Gridviewholder mholder;
		Bitmap icon;
		File current;
		int mposition;


		public AsyncImgLoad(Context mcontext, Gridviewholder holder, int position)
		{
			mholder=holder;
			mposition=position;
			current=holder.associatedfile;
		}

		@Override
		protected Void doInBackground(Void... params) {
			icon=getIcon(current);
			return null;
		}

		@Override
		protected void onPostExecute(Void param) {
			if(mholder.position==mposition && icon!=null && !isCancelled())
			{
				ImageView iconview = mholder.fileicon;
				iconview.setImageBitmap(icon);
				if(bitmapCache.get(current.getAbsolutePath())==null)
				{
					bitmapCache.put(current.getAbsolutePath(), icon);
				}
			}
		}
	}


}
