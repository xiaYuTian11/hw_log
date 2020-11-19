package com.sunnyday.cqjz;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.sunnyday.cqjz.entity.FileProperties;
import com.sunnyday.cqjz.entity.SSHLoginInfo;
import com.sunnyday.cqjz.utils.PropertiesUtil;
import com.sunnyday.cqjz.utils.SSHRemoteUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
            if (properties == null) {
                properties = PropertiesUtil.get();
            }
            final List<FileProperties> propertiesList = analysisProperties();
            if (CollectionUtil.isEmpty(propertiesList)) {
                return;
            }
            // 解压文件路径
            String unZipPath = properties.getProperty("unzip.path");
            delFiles(unZipPath);

            this.copyFile(propertiesList);
        } catch (SftpException | JSchException e) {
            LOGGER.error("复制文件错误：：" + e);
        }
    }

    /**
     * 解析配置
     *
     * @return
     */
    public List<FileProperties> analysisProperties() {
        List<FileProperties> propertiesList = new ArrayList<>(16);
        String ip = properties.getProperty("src.ssh.ip");
        if (StrUtil.isBlank(ip)) {
            LOGGER.error("src.ssh.ip 没有值");
            return null;
        }
        String username = properties.getProperty("src.ssh.username");
        if (StrUtil.isBlank(username)) {
            LOGGER.error("src.ssh.username 没有值");
            return null;
        }
        String password = properties.getProperty("src.ssh.password");
        if (StrUtil.isBlank(password)) {
            LOGGER.error("src.ssh.password 没有值");
            return null;
        }
        String srcDir = properties.getProperty("src.file.dir");
        if (StrUtil.isBlank(srcDir)) {
            LOGGER.error("src.file.dir 没有值");
            return null;
        }
        String fileType = properties.getProperty("src.file.type");
        if (StrUtil.isBlank(fileType)) {
            fileType = "zip";
        }

        final String destDir = properties.getProperty("dest.file.dir");
        LOGGER.info("目标文件目录：" + destDir);
        final String prefix = properties.getProperty("src.file.prefix");
        LOGGER.info("文件前缀：" + prefix);
        String srcFileDate = properties.getProperty("src.file.date");
        if (StrUtil.isBlank(srcFileDate)) {
            srcFileDate = DateUtil.format(DateUtil.yesterday(), "yyyy-MM-dd");
        }
        // 解压文件路径
        String unZipPath = properties.getProperty("unzip.path");
        final String[] ips = ip.split(",");
        final String[] usernameArray = username.split(",");
        final String[] passwordArray = password.split(",");
        final String[] srcDirArray = srcDir.split(",");
        boolean isEqual = (ips.length == usernameArray.length) && (ips.length == passwordArray.length) && (ips.length == srcDirArray.length);
        for (int i = 0; i < ips.length; i++) {
            FileProperties fileProperties;
            if (isEqual) {
                fileProperties = new FileProperties.Builder(ips[i], usernameArray[i], passwordArray[i], srcDirArray[i])
                        .srcFilePrefix(prefix)
                        .srcFileDate(srcFileDate).srcFileType(fileType)
                        .destFileDir(destDir).unzipPath(unZipPath)
                        .build();
            } else {
                fileProperties = new FileProperties.Builder(ips[i], usernameArray[0], passwordArray[0], srcDirArray[0])
                        .srcFilePrefix(prefix)
                        .srcFileDate(srcFileDate).srcFileType(fileType)
                        .destFileDir(destDir).unzipPath(unZipPath)
                        .build();
            }
            propertiesList.add(fileProperties);
        }
        return propertiesList;
    }

    /**
     * copy file
     *
     * @param propertiesList
     */
    public void copyFile(List<FileProperties> propertiesList) throws SftpException, JSchException {
        for (FileProperties fileProperties : propertiesList) {
            String ip = fileProperties.getSrcIp();
            String username = fileProperties.getSrcUsername();
            String password = fileProperties.getSrcPassword();
            String fileType = fileProperties.getSrcFileType();
            String srcDir = fileProperties.getSrcFileDir();
            String prefix = fileProperties.getSrcFilePrefix();
            String srcFileDate = fileProperties.getSrcFileDate();
            String destDir = fileProperties.getDestFileDir();
            String unZipPath = fileProperties.getUnzipPath();

            // 连接服务器
            final SSHLoginInfo sshLoginInfo = new SSHLoginInfo.Builder()
                    .setIpAddress(ip).setUserName(username).setPassWord(password).build();
            final SSHRemoteUtil remoteUtil = SSHRemoteUtil.getInstance();
            remoteUtil.sshRemoteCallLogin(sshLoginInfo);

            // copy file
            final Vector<ChannelSftp.LsEntry> lsEntries = remoteUtil.listFiles(srcDir);
            AtomicInteger atomicInteger = new AtomicInteger(0);
            for (ChannelSftp.LsEntry entry : lsEntries) {

                final String filename = entry.getFilename();
                if (StrUtil.isBlank(filename)) {
                    continue;
                }
                if (!StrUtil.startWith(filename, prefix)) {
                    continue;
                }
                if (!StrUtil.endWith(filename, fileType)) {
                    continue;
                }
                // 前一天的时间
                if (!filename.contains(srcFileDate)) {
                    continue;
                }
                // destDir 为目录时文件名称与远程名称一样
                String destFilePath = destDir + File.separator + prefix + "." + srcFileDate + System.currentTimeMillis() + "." + fileType;
                String srcFilePath = srcDir + File.separator + filename;
                remoteUtil.fileDownload(srcFilePath, destFilePath);
                this.unzipFile(destFilePath, unZipPath);
                atomicInteger.incrementAndGet();
            }
            LOGGER.info(ip + "--复制文件个数：" + atomicInteger.get());
            remoteUtil.closeSession();
        }
    }

    /**
     * 解压到当前文件目录
     *
     * @param filePath
     */
    private void unzipFile(String filePath, String unZipPath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return;
        }
        File unZipFile = new File(unZipPath);
        if (!unZipFile.exists()) {
            FileUtil.mkdir(unZipFile);
        }

        String unzipTempPath;
        if (StrUtil.isBlank(unZipPath)) {
            unzipTempPath = file.getParentFile() + File.separator + "hwtmp";
        } else {
            unzipTempPath = unZipPath + File.separator + "hwtmp";
        }
        ZipUtil.unzip(filePath, unzipTempPath, StandardCharsets.UTF_8);

        File unzipTemp = new File(unzipTempPath);
        if (unzipTemp.isDirectory()) {
            final File[] listFiles = unzipTemp.listFiles();
            if (listFiles != null && listFiles.length > 0) {
                for (File listFile : listFiles) {
                    if (listFile.isDirectory()) {
                        FileUtil.move(listFile, unzipTemp.getParentFile(), true);
                    } else if (listFile.isFile()) {
                        final File rename = FileUtil.rename(listFile, listFile.getName().substring(0, listFile.getName().lastIndexOf(".")) + System.currentTimeMillis(), true, true);
                        FileUtil.move(rename, unzipTemp.getParentFile(), true);
                    }
                }
            }
        }
        FileUtil.del(unzipTemp);
        LOGGER.info("文件解压成功：" + file.getName());
    }

    /**
     * 删除文件
     *
     * @param filePath
     */
    private void delFiles(String filePath) {
        File file = new File(filePath);
        if (file.isDirectory()) {
            final File[] files = file.listFiles();
            if (files == null || files.length == 0) {
                return;
            }
            for (File file1 : files) {
                FileUtil.del(file1);
            }
        }
        LOGGER.info("清空解压目录下文件");
    }

}
