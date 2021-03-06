package com.open.file.manager;

public class Consts
{
	//Operation indexes
	static final int ACTION_NONE=-1;
	static final int ACTION_COPY=0;
	static final int ACTION_CUT=1;
	static final int ACTION_RENAME=2;
	static final int ACTION_REMOVE=3;
	static final int ACTION_MKDIR=4;
	static final int ACTION_DUPLICATES=5;
	
	//Rename  and fileinfo index for actionbar
	static final int INDEX_RENAME=3;
	static final int INDEX_INFO=4;
	
	static final int MSG_DUPLICATES=0;
	static final int MSG_FINISHED=1;
	static final int MSG_ACTIVITYRESTART=2;

	
	static final int ICON_SIZE=48;
	
	static final int CONFLICT_DIR_DIR=1;
	static final int CONFLICT_FILE_DIR=2;
	static final int CONFLICT_FILE_FILE=3;
	
}