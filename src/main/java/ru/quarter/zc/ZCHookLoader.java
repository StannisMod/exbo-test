package ru.quarter.zc;

import ru.quarter.zc.repack.gloomyfolken.hooklib.minecraft.HookLoader;
import ru.quarter.zc.repack.gloomyfolken.hooklib.minecraft.PrimaryClassTransformer;

//-Dfml.coreMods.load=ru.quarter.zc.ZCHookLoader
public class ZCHookLoader extends HookLoader {

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{PrimaryClassTransformer.class.getName()};
    }

    @Override
    public void registerHooks() {
        registerHookContainer("ru.quarter.zc.Hooks");
    }
}