package com.logickllc.pokesensor;

import org.moe.natj.general.Pointer;
import org.moe.natj.general.ann.RegisterOnStartup;
import org.moe.natj.general.ann.Runtime;
import org.moe.natj.objc.ObjCRuntime;
import org.moe.natj.objc.ann.ObjCClassName;

import apple.uikit.UIControl;

@Runtime(ObjCRuntime.class)
@ObjCClassName("ClickableView")
@RegisterOnStartup
public class ClickableView extends UIControl {

    protected ClickableView(Pointer peer) {
        super(peer);
    }
}
