package com.logickllc.pokesensor;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.Mapped;
import org.moe.natj.general.ann.NInt;
import org.moe.natj.general.ann.Owned;
import org.moe.natj.objc.SEL;
import org.moe.natj.objc.ann.Selector;
import org.moe.natj.objc.map.ObjCObjectMapper;

import apple.NSObject;
import apple.uikit.UIBarButtonItem;

public class CustomUIBarButtonItem extends UIBarButtonItem {
    NSObject object;

    protected CustomUIBarButtonItem(Pointer peer) {
        super(peer);
    }

    @Owned
    @Selector("alloc")
    public static native CustomUIBarButtonItem alloc();

    public CustomUIBarButtonItem initWithBarButtonSystemItemTargetActionObject(@NInt long systemItem, @Mapped(ObjCObjectMapper.class) Object target, SEL action, NSObject object) {
        super.initWithBarButtonSystemItemTargetAction(systemItem, target, action);
        this.object = object;
        return this;
    }
}
