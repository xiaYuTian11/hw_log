package com.sunnyday.cqjz;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.sunnyday.cqjz.entity.SSHLoginInfo;
import com.sunnyday.cqjz.utils.SSHRemoteUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文件复制处理
 *
 * @author TMW
 * @since 2020/11/17 14:30
 */
public class FileProcess implements Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileProcess.class);

    private Properties properties;

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        this.execute();
        LOGGER.info("本次定时任务执行完毕");
    }

    public void execute() {
        try {
            this.copyFile(properties);
        } catch (SftpException | JSchException e) {
            LOGGER.error("复制文件错误：：" + e);
        }
    }

    /**
     * copy file
     *
     * @param properties
     */
    public void copyFile(Properties properties) throws SftpException, JSchException {
        final String ip = properties.getProperty("src.ssh.ip");
        final String username = properties.getProperty("src.ssh.username");
        final String password = properties.getProperty("src.ssh.password");
        String fileType = properties.getProperty("src.file.type");
        if (StrUtil.isBlank(fileType)) {
            fileType = "zip";
        }
        final String srcDir = properties.getProperty("src.file.dir");
        LOGGER.info("源文件目录：" + srcDir);
        final String destDir = properties.getProperty("dest.file.dir");
        LOGGER.info("目标文件目录：" + destDir);
        final String prefix = properties.getProperty("src.file.prefix");
        LOGGER.info("文件前缀：" + prefix);
        String srcFileDate = properties.getProperty("src.file.date");

        // 连接服务器
        SSHLoginInfo.Builder builder = new SSHLoginInfo.Builder();
        builder.setIpAddress(ip);
        builder.setUserName(username);
        builder.setPassWord(password);
        final SSHLoginInfo sshLoginInfo = builder.build();
        final SSHRemoteUtil remoteUtil = SSHRemoteUtil.getInstance();
        remoteUtil.sshRemoteCallLogin(sshLoginInfo);
        // copy file
        final Vector<ChannelSftp.LsEntry> lsEntries = remoteUtil.listFiles(srcDir);
        String finalFileType = fileType;
        if (StrUtil.isBlank(srcFileDate)) {
            srcFileDate = DateUtil.format(DateUtil.yesterday(), "yyyy-MM-dd");
        }
        final String format = srcFileDate;
        LOGGER.info("查找的文件日期为：" + format);
        AtomicInteger atomicInteger = new AtomicInteger(0);
        for (ChannelSftp.LsEntry entry : lsEntries) {

            final String filename = entry.getFilename();
            if (StrUtil.isBlank(filename)) {
                continue;
            }
            if (!StrUtil.startWith(filename, prefix)) {
                continue;
            }
            if (!StrUtil.endWith(filename, finalFileType)) {
                continue;
            }
            // 前一天的时间
            if (!filename.contains(format)) {
                continue;
            }
            remoteUtil.fileDownload(srcDir + File.separator + filename, destDir);
            this.unzipFile(destDir + File.separator + filename);
            atomicInteger.incrementAndGet();
        }
        LOGGER.info("复制文件个数：" + atomicInteger.get());
        remoteUtil.closeSession();
    }

    /**
     * 解压到当前文件目录
     *
     * @param filePath
     */
    private void unzipFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        final File unzip = ZipUtil.unzip(file, file.getParentFile(), StandardCharsets.UTF_8);
        LOGGER.info("文件解压成功：" + file.getName());
    }

}
