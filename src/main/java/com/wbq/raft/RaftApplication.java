package com.wbq.raft;

import com.google.common.collect.Sets;
import com.wbq.raft.config.NodeConfig;
import com.wbq.raft.impl.DefaultNode;

import java.util.Set;

public class RaftApplication {

    public static void main(String[] args) throws Throwable {
        Set<String> nodes = Sets.newHashSet("localhost:8001", "localhost:8002", "localhost:8003", "localhost:8004",
                "localhost:8005");

        NodeConfig config = NodeConfig.builder().addresses(nodes)
                .port(Integer.valueOf(System.getProperty("serverPort"))).build();

        Node node = DefaultNode.getInstance();

        node.setConfig(config);
        node.init();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                node.destroy();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }));
    }

}
