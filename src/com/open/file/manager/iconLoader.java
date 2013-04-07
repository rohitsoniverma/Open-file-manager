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
import java.util.Hashtable;

import com.open.file.manager.ImageAdapter.Gridviewholder;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

public class iconLoader
{
	private final static int cacheSize=(int) ((Runtime.getRuntime().maxMemory() / 1024)/6);;
	private final LruCache<String,Bitmap> bitmapCache;
	private static Context mycont;
    private final Bitmap genericicon;
	static final Hashtable<String, Integer> icons=new Hashtable<String, Integer>(6);

	
	public iconLoader(Context ct)
	{
		mycont=ct;
        genericicon=BitmapFactory.decodeResource(mycont.getResources(), R.drawable.unknownfile);
		bitmapCache=new LruCache<String, Bitmap>(cacheSize) {

	        @Override
	        protected int sizeOf(String key, Bitmap bitmap) {
	            // The cache size will be measured in kilobytes rather than
	            // number of items.
	            return (bitmap.getRowBytes()*bitmap.getHeight()) / 1024;
	        }
	    };;
		//PEZZI di codice da risistemare, li ho tagliati da imageadapter... questa è l'hash delle img
	    icons.put("audio", R.drawable.audiogeneric);
	    icons.put("application", R.drawable.applicationgeneric);
	    icons.put("video", R.drawable.videogeneric);
	    icons.put("text", R.drawable.textgeneric);
	    icons.put("directory", R.drawable.directory);
	    //icons.put("application/vnd.android.package-archive", R.drawable.apk);
	    icons.put("image", R.drawable.imagegeneric);
		
	}
	
	public void loadIcon(Gridviewholder holder, int position)
	{
		ImageView iv=holder.fileicon;
		String key=holder.associatedfile.getAbsolutePath();
		if(bitmapCache.get(key)==null)
		{
			iv.setImageBitmap(genericicon);
			new asyncimgload(mycont, holder, position).execute();
		}
		else
		{
			iv.setImageBitmap(bitmapCache.get(key));
		}
	}
	
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
			String mimetype = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
			String generictype;
			String imgregexp="image/(jpg|jpeg|png)";
			BitmapFactory.Options previewoptions=new BitmapFactory.Options();
			previewoptions.inJustDecodeBounds=true;
			try
			{
				generictype= mimetype.split("/")[0];
				Log.d("generictype=", generictype);
			}
			catch(Exception e)
			{
				generictype=null;
			}
			Log.d(mimetype, imgregexp);
			if(mimetype != null && mimetype.matches(imgregexp))
			{
				BitmapFactory.decodeFile(current.getAbsolutePath(), previewoptions);
				previewoptions.inSampleSize=getScaleratio(previewoptions);
				previewoptions.inJustDecodeBounds=false;
				icon=BitmapFactory.decodeFile(current.getAbsolutePath(), previewoptions);
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
	
	private static int getScaleratio(Options bounds) {
		final float scale = mycont.getResources().getDisplayMetrics().density;
		final int targetHeight= Math.round(32*scale);
		final int targetWidth=Math.round(32*scale);
		int scaleratio=1;
		if(bounds.outHeight> targetHeight || bounds.outWidth>targetWidth)
		{
			final int heightRatio=Math.round(bounds.outHeight/targetHeight);
			final int widthRatio=Math.round(bounds.outWidth/targetWidth);
			
			scaleratio= heightRatio>widthRatio? heightRatio : widthRatio;
		}
		return scaleratio;
	}

	public Drawable loadConflictico(File current)
	{
		Drawable icodraw;
		Bitmap iconbm;
		iconbm=getIcon(current);
		icodraw=new BitmapDrawable(mycont.getResources(), iconbm);
		icodraw.setBounds(new Rect(0, 0, iconbm.getWidth(), iconbm.getHeight()));
		return icodraw;
	}
	
	class asyncimgload extends AsyncTask<Void, Void, Void>
	{
		Context cn;
		Gridviewholder mholder;
	    private WeakReference<ImageView> iv;
		Bitmap icon;
		File current;
		int mposition;
		
		
		public asyncimgload(Context mcontext, Gridviewholder holder, int position)
		{
		mholder=holder;
		mposition=position;
		iv=new WeakReference<ImageView>(holder.fileicon);
		current=holder.associatedfile;
		Log.d("fff", "wtf");
		cn = mcontext;
		}

	    
	    



		@Override
		protected Void doInBackground(Void... params) {
			//questo è il filtro che usavo sulle immagini...
		    icon=getIcon(current);
		    return null;
		}
		
		@Override
	    protected void onPostExecute(Void param) {
			if(mholder.position==mposition)
			{
			if(icon!=null)
			{
	    	ImageView iconview = iv.get();
	    	if(iconview!= null)
	    	{
	    	iconview.setImageBitmap(icon);
	    	if(bitmapCache.get(current.getAbsolutePath())==null)
	    	{
	    		bitmapCache.put(current.getAbsolutePath(), icon);
	    	}
			}
			}
			}
	    }
	}
	
	
}
