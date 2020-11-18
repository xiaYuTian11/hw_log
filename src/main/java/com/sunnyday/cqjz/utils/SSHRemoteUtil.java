package com.sunnyday.cqjz.utils;

import cn.hutool.core.io.IoUtil;
import com.jcraft.jsch.*;
import com.sunnyday.cqjz.entity.SSHLoginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Vector;

/**
 * ssh远程连接工具
 *
 * @author TMW
 * @since 2020/11/17 14:37
 */
public class SSHRemoteUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSHRemoteUtil.class);
    private static volatile SSHRemoteUtil instance = null;
    private static final int DEFAULT_PORT = 22;// 默认端口号
    private Session session;// JSCH session

    private boolean logined = false;// 是否登陆

    private SSHRemoteUtil() {
    }

    public synchronized static SSHRemoteUtil getInstance() {
        if (instance == null) {
            instance = new SSHRemoteUtil();
        }
        return instance;
    }

    /**
     * @Description: 远程登陆
     * @author: mulming
     * @param: @param ipAddress ip
     * @param: @param userName 帐号
     * @param: @param password 密码
     * @param: @throws Exception
     * @return: void
     */
    public void sshRemoteCallLogin(SSHLoginInfo loginInfo) {
        // 如果登陆就直接返回
        if (logined) {
            return;
        }
        // 创建jSch对象
        JSch jSch = new JSch();
        try {
            // 获取到jSch的session, 根据用户名、主机ip、端口号获取一个Session对象
            session = jSch.getSession(loginInfo.getUserName(), loginInfo.getIpAddress(), DEFAULT_PORT);
            // 设置密码
            session.setPassword(loginInfo.getPassWord());

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            // session.setTimeout(3000);// 设置超时
            session.connect();//通过Session建立连接

            // 设置登陆状态
            logined = true;
            LOGGER.info("SSH connection success ...");
        } catch (JSchException e) {
            // 设置登陆状态为false
            logined = false;
            LOGGER.error("主机登录失败, IP = " + loginInfo.getIpAddress() + ", USERNAME = " + loginInfo.getUserName() + ", Exception:" + e);
        }
    }

    public String copyFile() {
        // session.

        return null;
    }

    /**
     * 关闭连接
     */
    public void closeSession() {
        // 调用session的关闭连接的方法
        if (session != null) {
            // 如果session不为空,调用session的关闭连接的方法
            session.disconnect();
        }
        LOGGER.info("SSH close success ...");
    }

    public String execCommandByJSch(String command) throws IOException, JSchException {

        //1.默认方式，执行单句命令
        ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
        InputStream in = channelExec.getInputStream();
        channelExec.setCommand(command);
        channelExec.setErrStream(System.err);
        channelExec.connect();
        String result = IoUtil.read(in, StandardCharsets.UTF_8);
        channelExec.disconnect();

        return result;
    }

    public String execCommandByShell() throws IOException, JSchException {
        String result = "";
        //2.尝试解决 远程ssh只能执行一句命令的情况
        ChannelShell channelShell = (ChannelShell) session.openChannel("shell");
        InputStream inputStream = channelShell.getInputStream();//从远端到达的数据  都能从这个流读取到
        channelShell.setPty(true);
        channelShell.connect();

        OutputStream outputStream = channelShell.getOutputStream();//写入该流的数据  都将发送到远程端
        //使用PrintWriter 就是为了使用println 这个方法
        //好处就是不需要每次手动给字符加\n
        PrintWriter printWriter = new PrintWriter(outputStream);
        printWriter.println("cd /tmp");
        printWriter.println("ls");
        printWriter.println("exit");//为了结束本次交互
        printWriter.flush();//把缓冲区的数据强行输出

        /**
         shell管道本身就是交互模式的。要想停止，有两种方式：
         一、人为的发送一个exit命令，告诉程序本次交互结束
         二、使用字节流中的available方法，来获取数据的总大小，然后循环去读。
         为了避免阻塞
         */
        byte[] tmp = new byte[1024];
        while (true) {

            while (inputStream.available() > 0) {
                int i = inputStream.read(tmp, 0, 1024);
                if (i < 0) break;
                String s = new String(tmp, 0, i);
                if (s.contains("--More--")) {
                    outputStream.write((" ").getBytes());
                    outputStream.flush();
                }
                LOGGER.info(s);
            }
            if (channelShell.isClosed()) {
                LOGGER.info("exit-status:" + channelShell.getExitStatus());
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }

        }
        outputStream.close();
        inputStream.close();
        channelShell.disconnect();
        session.disconnect();
        LOGGER.info("DONE");

        return result;
    }

    public boolean execCommand(String command) {
        ChannelExec channelExec = null;
        try {
            channelExec = (ChannelExec) session.openChannel("exec");
            InputStream in = channelExec.getInputStream();
            channelExec.setCommand(command);
            channelExec.setErrStream(System.err);
            channelExec.connect();

            final String toString = IoUtil.read(in, StandardCharsets.UTF_8);
            LOGGER.info("执行结果：" + toString);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (channelExec != null) {
                channelExec.disconnect();
            }
        }
        return false;
    }

    /**
     * @Description: 执行相关的命令
     * @author: mulming
     * @param: @param command 具体的命令
     * @param: @throws IOException
     * @return: String ： 返回命令执行后的结果
     */
    // public String execCommand(String command) {
    //     InputStream in = null;// 输入流(读)
    //     Channel channel = null;// 定义channel变量
    //     try {
    //         // 如果命令command不等于null
    //         if (command != null) {
    //             // 打开channel
    //             //说明：exec用于执行命令;sftp用于文件处理
    //             channel = session.openChannel("exec");
    //             // 设置command
    //             ((ChannelExec) channel).setCommand(command);
    //             // channel进行连接
    //             channel.connect();
    //             // 获取到输入流
    //             in = channel.getInputStream();
    //             // 执行相关的命令
    //             return processDataStream(in);
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     } finally {
    //         if (in != null) {
    //             try {
    //                 in.close();
    //             } catch (IOException e) {
    //                 e.printStackTrace();
    //             }
    //         }
    //         if (channel != null) {
    //             channel.disconnect();
    //         }
    //     }
    //     return null;
    // }

    /**
     * @Description:内部使用 对将要执行的linux的命令进行遍历, 专门处理执行，以及执行后返回的命令
     * @author: mulming
     * @param: @param in
     * @param: @return
     * @param: @throws Exception
     * @return: String
     */
    private String processDataStream(InputStream in) {
        StringBuffer sb = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String result = "";
        try {
            while ((result = br.readLine()) != null) {
                sb.append(result).append("\n");
            }
        } catch (Exception e) {
            LOGGER.info("Failed to get BufferedReader: ");
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    /**
     * @Description: 上传文件 可参考:https://www.cnblogs.com/longyg/archive/2012/06/25/2556576.html
     * @author: mulming
     * @param: @param directory 上传文件的目录,即服务端的目录
     * @param: @param uploadFile 将要上传的文件，本地文件
     * @return: void
     */
    public void uploadFile(String directory, String uploadFile) {
        try {
            LOGGER.info(" Upload start .....");
            // 打开channelSftp
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            // 远程连接
            channelSftp.connect();
            // 创建一个文件名称问uploadFile的文件
            File file = new File(uploadFile);
            // 将文件进行上传(sftp协议)
            // 将本地文件名为src的文件上传到目标服务器,目标文件名为dst,若dst为目录,则目标文件名将与src文件名相同.
            // 采用默认的传输模式:OVERWRITE
            channelSftp.put(new FileInputStream(file), directory, ChannelSftp.OVERWRITE);
            // 切断远程连接
            channelSftp.exit();
            LOGGER.info(file.getName() + " Upload success .....");
        } catch (JSchException | SftpException | FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * @Description: TODO下载文件 采用默认的传输模式：OVERWRITE
     * @author: mulming
     * @param: @param src linux服务器文件地址
     * @param: @param dst 本地存放地址
     * @return: void
     */
    public void fileDownload(String src, String dst) {
        try {
            // src 是linux服务器文件地址,dst 本地存放地址
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            // 远程连接
            channelSftp.connect();
            // 下载文件,多个重载方法
            channelSftp.get(src, dst);
            // 切断远程连接,quit()等同于exit()，底层 disconnect();
            channelSftp.quit();
        } catch (JSchException | SftpException e) {
            e.printStackTrace();
        }
        LOGGER.info(src + " ,download complete.....");
    }

    /**
     * @Description: 删除文件
     * @author: mulming
     * @param: @param directoryFile 要删除文件所在目录
     * @return: void
     */
    public void deleteFile(String directoryFile) throws SftpException, JSchException {
        // 打开openChannel的sftp
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        // 远程连接
        channelSftp.connect();
        // 删除文件
        channelSftp.rm(directoryFile);
        // 切断远程连接
        channelSftp.exit();
        LOGGER.info(directoryFile + " delete complete.....");
    }

    /**
     * @Description: 列出目录下的文件
     * @author: mulming
     * @param: @param directory   要列出的目录
     * @param: @throws JSchException
     * @param: @throws SftpException
     * @return: Vector<LsEntry>
     */
    public Vector<ChannelSftp.LsEntry> listFiles(String directory) throws JSchException, SftpException {
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
        // 远程连接
        channelSftp.connect();
        // 显示目录信息
        @SuppressWarnings("unchecked")
        Vector<ChannelSftp.LsEntry> ls = channelSftp.ls(directory);
        // 切断连接
        channelSftp.exit();
        return ls;
    }
}
