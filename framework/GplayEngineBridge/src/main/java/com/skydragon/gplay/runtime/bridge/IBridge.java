package com.skydragon.gplay.runtime.bridge;

import java.util.Map;

/*
 * @brief Host -> Client (Dynamic loaded Jar/Apk) 的桥接接口，用于Host直接访问Jar/Apk中的函数
 * Proxy用于Client调用Host中的函数的代理类
 */
public interface IBridge {
    // 设置Bridge代理类，用于Client层访问Host层的方法
    void setBridgeProxy(IBridgeProxy proxy);

    // 获取Bridge代理类
    IBridgeProxy getBridgeProxy();

    // 设置配置键值对
    void setOption(String key, Object value);

    // 获取配置键值对
    Object getOption(String key);

    // 同步方法调用
    Object invokeMethodSync(String method, Map<String, Object> args);

    // 异步方法调用
    void invokeMethodAsync(final String method, final Map<String, Object> args, final ICallback callback);
}
