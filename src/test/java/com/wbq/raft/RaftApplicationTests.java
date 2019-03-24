package com.wbq.raft;

import com.wbq.raft.common.RequestType;
import com.wbq.raft.pojo.ClientRequest;
import com.wbq.raft.rpc.DefaultRpcClient;
import com.wbq.raft.rpc.RpcRequest;

public class RaftApplicationTests {

    public static void main(String[] args) {
        DefaultRpcClient client=new DefaultRpcClient();

        ClientRequest clientRequest = ClientRequest.builder().key("hello"+1).val("world").type(RequestType.PUT.getCode())
                .build();
        RpcRequest req = RpcRequest.builder().url("localhost:8003").type(RpcRequest.Type.CLIENT).data(clientRequest)
                .build();
        client.send(req);
    }

}
