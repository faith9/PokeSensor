package com.logickllc.pokesensor.api;


import java.util.Map;

public class Message {
    public int id;
    public int minVersionIOS = Integer.MIN_VALUE;
    public int maxVersionIOS = Integer.MAX_VALUE;
    public int minVersionAndroid = Integer.MIN_VALUE;
    public int maxVersionAndroid = Integer.MAX_VALUE;
    public String title;
    public String message;
    public Map<String, String> buttons;
}
