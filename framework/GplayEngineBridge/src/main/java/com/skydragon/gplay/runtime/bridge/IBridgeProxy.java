package com.skydragon.gplay.runtime.bridge;

import java.util.Map;

public interface IBridgeProxy {
    // 同步方法调用
    Object invokeMethodSync(String method, Map<String, Object> args);
    // 异步方法调用
    void invokeMethodAsync(final String method, final Map<String, Object> args, final ICallback callback);
}
