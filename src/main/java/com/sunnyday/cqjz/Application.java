package com.sunnyday.cqjz;

import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.StrUtil;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * @author TMW
 * @since 2020/11/17 17:38
 */
public class Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException, SchedulerException {
        ClassPathResource resource = new ClassPathResource("file.properties");
        Properties properties = new Properties();
        properties.load(resource.getStream());
        FileProcess fileProcess = new FileProcess();
        fileProcess.setProperties(properties);
        final String fileCron = properties.getProperty("copy.file.corn");
        final String isStartExecute = properties.getProperty("start.execute");
        if (StrUtil.isNotBlank(isStartExecute) && StrUtil.equalsIgnoreCase("true", isStartExecute)) {
            LOGGER.info("启动时执行复制文件");
            fileProcess.execute();
        }

        // 1、创建调度器Scheduler
        SchedulerFactory schedulerFactory = new StdSchedulerFactory();
        Scheduler scheduler = schedulerFactory.getScheduler();
        // 2、创建JobDetail实例，并与PrintWordsJob类绑定(Job执行内容)
        JobDetail jobDetail = JobBuilder.newJob(FileProcess.class)
                .withIdentity("copyFile", "group1").build();
        // 3、构建Trigger实例,每隔1s执行一次
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger1", "triggerGroup1")
                .startNow()//立即生效
                .withSchedule(
                        CronScheduleBuilder.cronSchedule(fileCron)
                ).build();//一直执行

        //4、执行
        scheduler.scheduleJob(jobDetail, trigger);
        scheduler.start();
        LOGGER.info("--------scheduler start ! ------------");
    }
}
