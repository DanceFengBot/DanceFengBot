package com.DanceFengBot.task;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

public class SchedulerTask {

    public static void autoRefreshToken() {
        Scheduler scheduler;
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
        } catch(SchedulerException e) {
            throw new RuntimeException(e);
        }
        JobDetail jobDetail = JobBuilder.newJob(RefreshTokenJob.class).build();
        Trigger trigger = TriggerBuilder.newTrigger().startNow()
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(3, 10))
                .build();
        try {
            scheduler.scheduleJob(jobDetail, trigger);
            scheduler.start();
        } catch(SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
    public static void addNewMusicCover() {
        Scheduler scheduler;
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
        } catch(SchedulerException e) {
            throw new RuntimeException(e);
        }
        JobDetail jobDetail = JobBuilder.newJob(AddNewMusicCoverJob.class).build();
        Trigger trigger = TriggerBuilder.newTrigger().startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule("0 30 10 15 * ?"))
                .build();
        try {
            scheduler.scheduleJob(jobDetail, trigger);
            scheduler.start();
        } catch(SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
    public static void addNewMusicAudio() {
        Scheduler scheduler;
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
        } catch(SchedulerException e) {
            throw new RuntimeException(e);
        }
        JobDetail jobDetail = JobBuilder.newJob(AddNewMusicCoverJob.class).build();
        Trigger trigger = TriggerBuilder.newTrigger().startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule("0 30 10 15 * ?"))
                .build();
        try {
            scheduler.scheduleJob(jobDetail, trigger);
            scheduler.start();
        } catch(SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
    public static void downloadNewMusicCover() {
        Scheduler scheduler;
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
        } catch(SchedulerException e) {
            throw new RuntimeException(e);
        }
        JobDetail jobDetail = JobBuilder.newJob(DownloadNewMusicCoverJob.class).build();
        Trigger trigger = TriggerBuilder.newTrigger().startNow()
                .withSchedule(CronScheduleBuilder.cronSchedule("0 30 10 15 * ?"))
                .build();
        try {
            scheduler.scheduleJob(jobDetail, trigger);
            scheduler.start();
        } catch(SchedulerException e) {
            throw new RuntimeException(e);
        }
    }
}
