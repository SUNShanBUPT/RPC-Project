package com.mini.rpc.protocol;

import lombok.Getter;

// 记录方法调用成功与否，保存在数据包Header中的status字段
public enum MsgStatus {
    SUCCESS(0),
    FAIL(1);

    @Getter
    private final int code;

    MsgStatus(int code) {
        this.code = code;
    }

}
