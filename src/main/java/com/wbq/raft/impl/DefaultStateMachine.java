package com.wbq.raft.impl;

import com.alibaba.fastjson.JSON;
import com.wbq.raft.StateMachine;
import com.wbq.raft.pojo.Command;
import com.wbq.raft.pojo.LogEntry;
import lombok.extern.slf4j.Slf4j;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

/**
 * <p>
 * 状态机默认实现
 * </p>
 *  * @author biqin.wu  * @since 14 February 2019  
 */
@Slf4j
public class DefaultStateMachine implements StateMachine {

    private static RocksDB rocksDB;

    private static String stateMachineDir;

    static {

        String dbDir = "D:\\wubiqin\\raft\\" + System.getProperty("serverPort");
        stateMachineDir = dbDir + "\\stateMachineDir";

        RocksDB.loadLibrary();
    }

    private DefaultStateMachine() {
        synchronized (this) {
            try {
                File file = new File(stateMachineDir);
                if (!file.exists()) {
                    if (!file.mkdirs()) {
                        log.warn("fail to make dirs stateMachineDir={}", stateMachineDir);
                    }
                }
                Options options = new Options();
                options.setCreateIfMissing(true);
                rocksDB = RocksDB.open(options, stateMachineDir);
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * 单例模式
     * 
     * @return
     */
    public static DefaultStateMachine getInstance() {
        return Singleton.INSTANCE;
    }

    private static class Singleton {
        private static final DefaultStateMachine INSTANCE = new DefaultStateMachine();
    }

    @Override
    public void apply(List<LogEntry> logEntries) {
        if (CollectionUtils.isEmpty(logEntries)) {
            return;
        }
        try {
            for (LogEntry logEntry : logEntries) {
                Command command = logEntry.getCommand();
                if (command == null) {
                    throw new IllegalArgumentException(String.format("command can't be null logEntry=%s", logEntry));
                }
                String key = command.getKey();
                rocksDB.put(key.getBytes(), JSON.toJSONBytes(logEntry));
            }
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public LogEntry get(String key) {
        try {
            byte[] val = rocksDB.get(key.getBytes());
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
    public String getString(String key) {
        try {
            byte[] val = rocksDB.get(key.getBytes());
            if (val == null) {
                return null;
            }
            return new String(val, Charset.forName("utf-8"));
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    @Override
    public void set(String key, String val) {
        assert key != null;
        assert val != null;

        try {
            rocksDB.put(key.getBytes(), val.getBytes());
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void del(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return;
        }
        try {
            for (String k : keys) {
                rocksDB.delete(k.getBytes());
            }
        } catch (RocksDBException e) {
            log.error(e.getMessage());
        }
    }

}
