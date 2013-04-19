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
import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public class FileCopyTree
{
	public List<FileCopyNode> children= new ArrayList<FileCopyNode>();
	public ArrayList<FileDuplicate> duplicates = new ArrayList<FileDuplicate>();
	public long size=0;
	public int nfiles=0;
	public FileCopyTree(List<String> fileList, File dstdir)
	{
		for(int i=0; i<fileList.size(); i++)
		{	
			File cur=new File(fileList.get(i));
			FileCopyNode child= new FileCopyNode(cur, null, dstdir);
			nfiles+=child.nfiles;
			if(child.duplicate!=null)
			{
			duplicates.add(child.duplicate);
			}
			children.add(child);
			size+=child.size;
		}
	}
	
	public void removeChild(FileCopyNode child)
	{
		children.remove(child);
		size-=child.size;
	}
	
}

class FileCopyNode
{
	public WeakReference<FileCopyNode> parent;
	public File srcFile;
	public File dstFile;
	public List<FileCopyNode> children=new ArrayList<FileCopyNode>();
	public FileDuplicate duplicate=null;
	public long size;
	public int nfiles;
	
	public FileCopyNode(File src, FileCopyNode father, File dstdir)
	{
		srcFile=src;
		dstFile=new File(dstdir, src.getName());
		nfiles=1;
		if(dstFile.exists())
		{
			duplicate=new FileDuplicate(srcFile, dstFile, this);
		}
		if(father!=null)
		{
		parent = new WeakReference<FileCopyNode>(father);
		}
		else
		{
			father=null;
		}
		if(srcFile.isDirectory())
		{
			addChildrenArray(srcFile.listFiles(), dstFile);
		}
		else
		{
			size=srcFile.length();
		}
	}
	

	public FileCopyNode addChild(File child, File dstfile)
	{
		FileCopyNode childnode=new FileCopyNode(child, this, dstfile);
		children.add(childnode);
		if(childnode.duplicate!=null)
		{
			duplicate.childDuplicates.add(childnode.duplicate);
		}
		return childnode;
	}
	
	
	public void addChildrenArray(File[] childrenArray, File dstdir)
	{
		int ChildrenSize=0;
		int childrennum=0;
		for(int i=0; i<childrenArray.length; i++)
		{
		File childfile=childrenArray[i];
		FileCopyNode childnode = addChild(childfile, dstdir);
		ChildrenSize+=childnode.size;
		childrennum+=childnode.nfiles;
		}
		nfiles+=childrennum;
		size=ChildrenSize;
	}
	
	public void removeChild(FileCopyNode childnode)
	{
		children.remove(childnode);
		size-=childnode.size;
	}
}

class FileDuplicate implements Parcelable
{
	File src;
	File dst;
	Boolean overwrite=false;
	Boolean processed=false;
	//1 per conflitto fra dir, 2 per conflitto dir/file o viceversa, 3 per file/file
	int type;
	String newname;
	ArrayList<FileDuplicate> childDuplicates=new ArrayList<FileDuplicate>();
	
	public FileDuplicate(File source, File dest, FileCopyNode node)
	{
		src=source;
		dst=dest;
		getConfilctType();
	}
	
	public FileDuplicate(Parcel in) {
		super();
		readFromParcel(in);
	}

	public void getConfilctType()
	{
		if(src.isDirectory() && dst.isDirectory())
		{
			type=Consts.CONFLICT_DIR_DIR;
		}
		else
		{
			if(src.isDirectory() || dst.isDirectory())
			{
				type=Consts.CONFLICT_FILE_DIR;
			}
			else
			{
				type=Consts.CONFLICT_FILE_FILE;
			}
		}
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	 public static final Parcelable.Creator<FileDuplicate> CREATOR
     = new Parcelable.Creator<FileDuplicate>() {
 public FileDuplicate createFromParcel(Parcel in) {
     return new FileDuplicate(in);
 }

 public FileDuplicate[] newArray(int size) {
     return new FileDuplicate[size];
 }
};
	

	public void readFromParcel(Parcel in) {
		src=new File(in.readString());
		dst=new File(in.readString());
		overwrite=(in.readInt()==1);
		Log.d("overwrite", Boolean.toString(overwrite));
		processed=(in.readInt()==1);
		type=in.readInt();
		in.readTypedList(childDuplicates, FileDuplicate.CREATOR);
		getConfilctType();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(src.getAbsolutePath());
		dest.writeString(dst.getAbsolutePath());
		dest.writeInt(overwrite ? 1 : 0 );
		dest.writeInt(processed ? 1 : 0);
		dest.writeInt(type);
		dest.writeTypedList(childDuplicates);
	}
}

