package com.mini.rpc.provider.facade;

import com.mini.rpc.provider.annotation.RpcService;

@RpcService(serviceInterface = RNMTuiQianFacade.class, serviceVersion = "1.0.0")
public class RNMTuiQianImpl implements RNMTuiQianFacade{
    @Override
    public String RNM(int times) {
        String ans = "";
        for (int i = 1; i <= times; i++) {
            ans = ans + i + ": 日你妈，退钱\n";

        }
        return ans;
    }
}

