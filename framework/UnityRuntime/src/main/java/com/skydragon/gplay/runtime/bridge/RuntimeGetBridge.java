package com.skydragon.gplay.runtime.bridge;

public final class RuntimeGetBridge implements IEngineRuntimeGetBridge {
    @Override
    public IEngineRuntimeBridge getRuntimeBridge() {
        return new CocosRuntimeBridge();
    }
}
