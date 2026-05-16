package com.dhj.ingameime.core;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

public class IngameIMECorePlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getLibraryRequestClass() {
        return null;
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{"com.dhj.ingameime.core.IngameIMETransformer"};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }
}
