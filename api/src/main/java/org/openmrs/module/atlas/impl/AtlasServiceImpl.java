/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.atlas.impl;

import org.openmrs.module.atlas.AtlasData;
import org.openmrs.module.atlas.AtlasDataProcessor;
import org.openmrs.module.atlas.AtlasService;
import org.openmrs.module.atlas.AtlasConstants;
import org.openmrs.module.atlas.PostAtlasDataQueueTask;
import org.openmrs.scheduler.SchedulerException;
import org.openmrs.scheduler.Task;
import org.openmrs.scheduler.TaskDefinition;


import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.GlobalProperty;
import org.openmrs.OpenmrsData;
import org.openmrs.api.APIException;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;

/**
 *
 */
public class AtlasServiceImpl implements AtlasService {
	
	private Log log = LogFactory.getLog(this.getClass());	
	private AtlasDataProcessor processor = null;
	
	
    /**
     * 
     */
    public AtlasServiceImpl() {
	   processor = new AtlasDataProcessor();
    }
	/**
     * @see org.openmrs.module.atlas.AtlasService#getAtlasData()
     */
    @Override
    public AtlasData getAtlasData() throws APIException {
    	AtlasData data = processor.getAtlasData();
		return data;
    }
  	
    
	/**
     * @see org.openmrs.module.atlas.AtlasService#setAtlasData(org.openmrs.module.atlas.AtlasData)
     */
    @Override
    public void setAtlasData(AtlasData data) throws APIException {
    	processor.setAtlasData(data);
    	//processor.postAtlasData(data);
    	registerPostAtlasDataTask(60 * 60 * 24 * 7l);
    }

    public boolean registerPostAtlasDataTask(long interval) {
    	return registerTask(AtlasConstants.POST_ATLAS_DATA_TASK_NAME
    		, AtlasConstants.POST_ATLAS_DATA_TASK_DESCRIPTION
    		, PostAtlasDataQueueTask.class,
    		interval);
    }
    
	private boolean registerTask(String name, String description, Class<? extends Task> clazz, long interval) {
		try {
			Context.addProxyPrivilege("Manage Scheduler");
		
			TaskDefinition taskDef = Context.getSchedulerService().getTaskByName(name);
			if (taskDef == null) {
				taskDef = new TaskDefinition();
				taskDef.setTaskClass(clazz.getCanonicalName());
				taskDef.setStartOnStartup(true);
				taskDef.setRepeatInterval(interval);
				taskDef.setStarted(true);
				taskDef.setStartTime(getPreviousMidnight(null));
				taskDef.setName(name);
				taskDef.setUuid(UUID.randomUUID().toString()); 
				taskDef.setDescription(description);
				Context.getSchedulerService().scheduleTask(taskDef);
			}
			
		} catch (SchedulerException ex) {
			log.warn("Unable to register task '" + name + "' with scheduler", ex);
			return false;
		} finally {
			Context.removeProxyPrivilege("Manage Scheduler");
		}
		return true;
	}
	
	/**
	 * Unregisters the named task
	 * @param name the task name
	 */
	private void unregisterTask(String name) {
		TaskDefinition taskDef = Context.getSchedulerService().getTaskByName(name);
		if (taskDef != null)
			Context.getSchedulerService().deleteTask(taskDef.getId());
	}
	
	   public static Date getPreviousMidnight(Date date) {
	    	// Initialize with date if it was specified
	    	Calendar cal = new GregorianCalendar();
			if (date != null)
				cal.setTime(date);
			  
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTime();
	    }
    
}