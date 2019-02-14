package com.wbq.raft.impl;

import com.alibaba.fastjson.JSON;
import com.wbq.raft.Log;
import com.wbq.raft.pojo.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author: biqin.wu
 * @Date: 2019/2/14
 * @Time: 15:02
 * @Description: 默认日志实现
 */
@Slf4j
public class DefaultLog implements Log {

    private static String dbDir = "D:\\wubiqin\\raft\\" + System.getProperty("serverPort");

    private static String logDir;

    private ReentrantLock lock = new ReentrantLock();

    private static RocksDB rocksDB;

    private static final byte[] LAST_LOG_INDEX_KEY = "last_log_index_key".getBytes();

    static {

        logDir = dbDir + "\\log";
        RocksDB.loadLibrary();
    }

    private DefaultLog() {
        Options options = new Options();
        options.setCreateIfMissing(true);

        File file = new File(logDir);
        if (!file.exists()) {
            if (file.mkdirs()) {
                log.info("success to mk dir dir={}", logDir);
            }
        }
        try {
            RocksDB.open(options, logDir);
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
    }

    public static DefaultLog getInstance() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final DefaultLog INSTANCE = new DefaultLog();
    }

    @Override
    public void write(LogEntry logEntry) {
        boolean success = false;
        LogEntry le = null;
        try {
            lock.tryLock(3000, TimeUnit.MILLISECONDS);
            LogEntry l = logEntry.withIndex(lastLogIndex() + 1);

            rocksDB.put(l.getIndex().toString().getBytes(), JSON.toJSONBytes(l));
            success = true;
            le = l;
            log.info("default log write logEntry to db success logEntry={}", l);
        } catch (InterruptedException | RocksDBException e) {
            log.error(e.getMessage());
        } finally {
            if (success) {
                updateLastLogIndex(le.getIndex());
            }
            lock.unlock();
        }
    }

    private void updateLastLogIndex(Long index) {
        try {
            rocksDB.put(LAST_LOG_INDEX_KEY, index.toString().getBytes());
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public LogEntry read(long index) {
        try {
            byte[] val = rocksDB.get(convertLongToBytes(index));
            if (val == null) {
                return null;
            }
            return JSON.parseObject(val, LogEntry.class);
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private byte[] convertLongToBytes(long index) {
        return String.valueOf(index).getBytes();
    }

    @Override
    public void removeFrom(long index) {
        boolean success = false;
        int count = 0;

        try {
            lock.tryLock(3000, TimeUnit.MILLISECONDS);
            for (long i = index; i < lastLogIndex(); i++) {
                rocksDB.delete(convertLongToBytes(index));
                count++;
            }
            success = true;
            log.warn("remove log success  startIndex={},endIndex={},count={}", index, lastLogIndex(), count);
        } catch (InterruptedException | RocksDBException e) {
            e.printStackTrace();
        } finally {
            if (success) {
                updateLastLogIndex(lastLogIndex() - count);
            }
            lock.unlock();
        }
    }

    @Override
    public LogEntry lastLog() {
        try {
            byte[] val = rocksDB.get(convertLongToBytes(lastLogIndex()));
            if (val == null) {
                return null;
            }
            return JSON.parseObject(val, LogEntry.class);
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public long lastLogIndex() {
        byte[] lastIndex = "-1".getBytes();
        try {
            lastIndex = rocksDB.get(LAST_LOG_INDEX_KEY);
            if (lastIndex == null) {
                lastIndex = "-1".getBytes();
            }
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
        return Long.valueOf(new String(lastIndex));
    }
}
