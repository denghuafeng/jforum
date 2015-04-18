/*
 * Copyright (c) JForum Team
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, 
 * with or without modification, are permitted provided 
 * that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above 
 * copyright notice, this list of conditions and the 
 * following disclaimer.
 * 2) Redistributions in binary form must reproduce the 
 * above copyright notice, this list of conditions and 
 * the following disclaimer in the documentation and/or 
 * other materials provided with the distribution.
 * 3) Neither the name of "Rafael Steil" nor 
 * the names of its contributors may be used to endorse 
 * or promote products derived from this software without 
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT 
 * HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, 
 * BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 * 
 * Class created on Jul 15, 2005
 * The JForum Project
 * http://www.jforum.net
 */
package net.jforum.summary;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import net.jforum.util.preferences.ConfigKeys;
import net.jforum.util.preferences.SystemGlobals;

import org.apache.log4j.Logger;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Schedule the summaries to be sent to the users.
 * 
 * @see net.jforum.summary.SummaryJob
 * 
 * @author Franklin S. Dattein (<a href="mailto:franklin@portaljava.com">franklin@portaljava.com</a>)
 * @version $Id$
 */
public final class SummaryScheduler
{
	private static final Logger LOGGER = Logger.getLogger(SummaryScheduler.class);
	private static Scheduler scheduler;
	private static boolean isStarted = false;
	private static final Object MUTEX = new Object();

	/**
	 * Starts the summary Job. Conditions to start: Is not started yet and is enabled on the file
	 * SystemGlobasl.properties. The to enable it is "summary.enabled"
	 * (ConfigKeys.SUMMARY_IS_ENABLED).
	 * 
	 * @throws SchedulerException
	 */
	public static void startJob() throws SchedulerException
	{
		final boolean isEnabled = SystemGlobals.getBoolValue(ConfigKeys.SUMMARY_IS_ENABLED);

		synchronized(MUTEX) {
			if (!isStarted && isEnabled) {
				final String filename = SystemGlobals.getValue(ConfigKeys.QUARTZ_CONFIG);

				final String cronExpression = SystemGlobals.getValue("org.quartz.context.summary.cron.expression");
				scheduler = new StdSchedulerFactory(filename).getScheduler();

				final JobDetail job = newJob(SummaryJob.class).withIdentity("summaryJob", "group1").build();

				final CronTrigger trigger = newTrigger().withIdentity("trigger1", "group1").withSchedule(cronSchedule(cronExpression)).build();

				scheduler.scheduleJob(job, trigger);
				LOGGER.info("Starting quartz summary expression " + cronExpression);
				scheduler.start();
			}

			isStarted = true;
		}
	}

	/**
	 * Stops the summary Job. Conditions to stop: Is started and is enabled on the file
	 * SystemGlobasl.properties. The to enable it is "summary.enabled"
	 * (ConfigKeys.SUMMARY_IS_ENABLED).
	 * 
	 * @throws SchedulerException
	 */
	public static void stopJob() throws SchedulerException
	{
		final boolean isEnabled = SystemGlobals.getBoolValue(ConfigKeys.SUMMARY_IS_ENABLED);

		synchronized(MUTEX) {
			if (isStarted && isEnabled) {			
				final String cronExpression = SystemGlobals.getValue("org.quartz.context.summary.cron.expression");
					
				LOGGER.info("Stopping quartz summary expression " + cronExpression);
				scheduler.shutdown(true);				
			}

			isStarted = false;
		}
		
		// avoid Tomcat report memory leak
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			LOGGER.error(e.getMessage(), e);
		}
	}
	
	private SummaryScheduler() {}
}
