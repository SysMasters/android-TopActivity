package com.willme.topactivity;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Wen on 1/14/15.
 */
public class WatchingAccessibilityService extends AccessibilityService {
    private static WatchingAccessibilityService sInstance;
    private AccessibilityEvent mEvent;

    public static WatchingAccessibilityService getInstance() {
        return sInstance;
    }

    @SuppressWarnings("all")
    @SuppressLint("NewApi")
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        this.mEvent = event;
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (SPHelper.isShowWindow(this)) {
                TasksWindow.show(this, event.getPackageName() + "\n" + event.getClassName());
            }
            autoInstall();
        }
    }

    @SuppressWarnings("all")
    private void autoInstall() {
        // 当前页面根view信息
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo == null) {
            return;
        }
        if ("android.vivo.secime.service".equals(mEvent.getPackageName())
                || "com.android.packageinstaller".equals(mEvent.getPackageName())) {
            // vivo账号密码
            String pwd = SPHelper.getVivoPwd(getApplicationContext());
            if (!TextUtils.isEmpty(pwd)) {
                fillPassWord(nodeInfo, pwd);
            }
            findAndClickView(nodeInfo);
        }
    }

    /**
     * 自动填充密码
     *
     * @param rootNode 当前窗口根节点
     * @param pwd      密码
     */
    @SuppressWarnings("all")
    private void fillPassWord(AccessibilityNodeInfo rootNode, String pwd) {
        AccessibilityNodeInfo editText = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (editText == null) {
            return;
        }
        if ("com.bbk.account".equals(editText.getPackageName()) && "android.widget.EditText".equals(editText.getClassName())) {
            Bundle arguments = new Bundle();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0);
                arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, pwd.length());
                editText.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                editText.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments);
                editText.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pwd);
                editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
            }
            // 模拟点击
            for (AccessibilityNodeInfo nodeInfo : rootNode.findAccessibilityNodeInfosByText("确定")) {
                nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    /**
     * 查找按钮并点击
     */
    private void findAndClickView(AccessibilityNodeInfo rootNode) {
        List<AccessibilityNodeInfo> nodeInfoList = new ArrayList<>();
        nodeInfoList.addAll(rootNode.findAccessibilityNodeInfosByText("继续安装"));
        nodeInfoList.addAll(rootNode.findAccessibilityNodeInfosByText("安装"));
        nodeInfoList.addAll(rootNode.findAccessibilityNodeInfosByText("打开"));

        if (nodeInfoList.size() == 0) {
            autoInstall();
            return;
        }
        for (AccessibilityNodeInfo nodeInfo : nodeInfoList) {
            nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    protected void onServiceConnected() {
        sInstance = this;
        if (SPHelper.isShowWindow(this)) {
            NotificationActionReceiver.showNotification(this, false);
        }
        sendBroadcast(new Intent(QuickSettingTileService.ACTION_UPDATE_TITLE));
        super.onServiceConnected();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sInstance = null;
        TasksWindow.dismiss(this);
        NotificationActionReceiver.cancelNotification(this);
        sendBroadcast(new Intent(QuickSettingTileService.ACTION_UPDATE_TITLE));
        return super.onUnbind(intent);
    }
}
