package com.leomall.db.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 数据库备份 / 恢复工具。
 *
 * P2-5 修复：
 * 1. ProcessBuilder 参数数组替代字符串拼接，口令改走 MYSQL_PWD 环境变量，
 *    不再出现在进程命令行（同机用户无法通过进程列表窥视口令）；
 * 2. 校验进程退出码与备份文件大小，失败抛异常而不是静默吞掉；
 * 3. mysqldump 加 --single-transaction（InnoDB 一致性快照，不锁表）。
 */
public class DbUtil {

    private static final long TIMEOUT_MINUTES = 30;

    public static void backup(File file, String user, String password, String db) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "mysqldump",
                "-u" + user,
                "--single-transaction",
                "--no-tablespaces",
                "--default-character-set=utf8mb4",
                db);
        pb.environment().put("MYSQL_PWD", password);
        pb.redirectOutput(file);
        pb.redirectErrorStream(false);
        Process process = pb.start();
        try {
            String stderr = drain(process.getErrorStream());
            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("mysqldump 超时（" + TIMEOUT_MINUTES + " 分钟）");
            }
            if (process.exitValue() != 0) {
                throw new IOException("mysqldump 失败，退出码=" + process.exitValue() + "：" + stderr);
            }
            if (!file.exists() || file.length() == 0) {
                throw new IOException("mysqldump 产出为空文件：" + file.getAbsolutePath());
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("mysqldump 被中断", e);
        }
    }

    public static void load(File file, String user, String password, String db) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(
                "mysql",
                "-u" + user,
                "--default-character-set=utf8mb4",
                db);
        pb.environment().put("MYSQL_PWD", password);
        pb.redirectInput(file);
        Process process = pb.start();
        try {
            String stderr = drain(process.getErrorStream());
            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("mysql 导入超时（" + TIMEOUT_MINUTES + " 分钟）");
            }
            if (process.exitValue() != 0) {
                throw new IOException("mysql 导入失败，退出码=" + process.exitValue() + "：" + stderr);
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new IOException("mysql 导入被中断", e);
        }
    }

    private static String drain(java.io.InputStream in) throws IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
        }
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }
}