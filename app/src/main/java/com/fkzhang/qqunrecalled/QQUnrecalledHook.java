package com.fkzhang.qqunrecalled;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import static com.fkzhang.qqunrecalled.ReflectionUtil.getMethod;
import static com.fkzhang.qqunrecalled.ReflectionUtil.getObjectField;
import static com.fkzhang.qqunrecalled.ReflectionUtil.getStaticMethod;
import static com.fkzhang.qqunrecalled.ReflectionUtil.invokeMethod;
import static com.fkzhang.qqunrecalled.ReflectionUtil.invokeStaticMethod;
import static com.fkzhang.qqunrecalled.ReflectionUtil.isCallingFrom;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.setObjectField;

/**
 * Created by fkzhang on 1/20/2016.
 */
public class QQUnrecalledHook {
    private final SettingsHelper mSettings;
    private Class<?> MessageRecordFactory;
    private Object mQQAppInterface;
    private String mSelfUin;
    private Class<?> ContactUtils;
    private Class<?> MessageRecord;
    private Context mNotificationContext;
    private Class<?> NotificationClass;
    private Method mMessageGetter;
    private Object mQQMessageFacade;
    private Map mTroopMap;
    private Method mGetTroopInfo;
    private Object mTroopManager;
    private Class<?> BaseApplicationClass;
    private Class<?> TroopAssistantManagerClass;
    private boolean mDebug = false;

    public QQUnrecalledHook() {
        mSettings = new SettingsHelper("com.fkzhang.qqunrecalled");
    }

    public void hook(final ClassLoader loader) {
        try {
            hookQQMessageFacade(loader);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        try {
            hookApplicationPackageManager(loader);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    protected void hookQQMessageFacade(final ClassLoader loader) {
        findAndHookMethod("com.tencent.mobileqq.app.message.QQMessageFacade", loader,
                "a", ArrayList.class, boolean.class, new XC_MethodReplacement() {
                    @Override
                    protected Object replaceHookedMethod(MethodHookParam methodHookParam) throws Throwable {
                        try {
                            preventMsgRecall(methodHookParam, loader);
                        } catch (Throwable t) {
                            XposedBridge.log(t);
                        }
                        return null;
                    }
                });
    }


    protected void hookApplicationPackageManager(ClassLoader loader) {
        findAndHookMethod("android.app.ApplicationPackageManager", loader,
                "getInstalledApplications", int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        @SuppressWarnings("unchecked") List<ApplicationInfo> applicationInfoList
                                = (List<ApplicationInfo>) param.getResult();
                        ArrayList<ApplicationInfo> to_remove = new ArrayList<>();
                        for (ApplicationInfo info : applicationInfoList) {
                            if (info.packageName.contains("com.fkzhang") ||
                                    info.packageName.contains("de.robv.android.xposed.installer")) {
                                to_remove.add(info);
                            }
                        }
                        if (to_remove.isEmpty())
                            return;

                        applicationInfoList.removeAll(to_remove);
                    }
                });
        findAndHookMethod("android.app.ApplicationPackageManager", loader,
                "getInstalledPackages", int.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        @SuppressWarnings("unchecked") List<PackageInfo> packageInfoList
                                = (List<PackageInfo>) param.getResult();
                        ArrayList<PackageInfo> to_remove = new ArrayList<>();
                        for (PackageInfo info : packageInfoList) {
                            if (info.packageName.contains("com.fkzhang") ||
                                    info.packageName.contains("de.robv.android.xposed.installer")) {
                                to_remove.add(info);
                            }
                        }
                        if (to_remove.isEmpty())
                            return;

                        packageInfoList.removeAll(to_remove);
                    }
                });
    }

    protected void preventMsgRecall(XC_MethodHook.MethodHookParam param, ClassLoader loader) {
        ArrayList list = (ArrayList) param.args[0];
        if (list == null || list.isEmpty())
            return;

        Object obj = list.get(0);

        initObjects(param.thisObject, loader);

        try {
            setMessageTip(obj);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
    }

    protected void initObjects(Object thisObject, ClassLoader loader) {
        try {
            reload();
            mQQMessageFacade = thisObject;
            mQQAppInterface = getObjectField(thisObject, "a",
                    "com.tencent.mobileqq.app.QQAppInterface");
            if (mSelfUin == null && mQQAppInterface != null) {
                mSelfUin = getAccount();
            }
            if (MessageRecordFactory == null) {
                MessageRecordFactory = findClass("com.tencent.mobileqq.service.message.MessageRecordFactory", loader);
            }
            if (ContactUtils == null) {
                ContactUtils = findClass("com.tencent.mobileqq.utils.ContactUtils", loader);
            }
            if (BaseApplicationClass == null) {
                BaseApplicationClass = findClass("com.tencent.qphone.base.util.BaseApplication", loader);
            }
            if (mNotificationContext == null) {
                mNotificationContext = getContext();
            }
            if (NotificationClass == null) {
                NotificationClass = findClass("com.tencent.mobileqq.activity.SplashActivity", loader);
            }

            if (mMessageGetter == null) {
                mMessageGetter = getMethod(thisObject, "a", List.class, String.class, int.class,
                        long.class, long.class);
            }
            if (MessageRecord == null) {
                MessageRecord = findClass("com.tencent.mobileqq.data.MessageRecord", loader);
            }
            if (TroopAssistantManagerClass == null) {
                TroopAssistantManagerClass =
                        findClass("com.tencent.mobileqq.managers.TroopAssistantManager", loader);
            }
            if (mTroopMap == null) {
                mTroopMap = (Map) getObjectField(invokeStaticMethod(getStaticMethod(
                        TroopAssistantManagerClass, "a", TroopAssistantManagerClass)), "a",
                        Map.class);
            }
            if (mGetTroopInfo == null && mQQAppInterface != null) {
                mTroopManager = callMethod(mQQAppInterface, "getManager", 51);
                mGetTroopInfo = getMethod(mTroopManager, "a", "com.tencent.mobileqq.data.TroopMemberInfo",
                        String.class, String.class);
            }
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

    }

    private void setMessageTip(Object revokeMsgInfo) {
        long time = (long) getObjectField(revokeMsgInfo, "c", long.class);
        String friendUin = (String) getObjectField(revokeMsgInfo, "a", String.class);
        String senderUin = (String) getObjectField(revokeMsgInfo, "b", String.class);

        if (mSelfUin.equals(senderUin))
            return;

        int istroop = (int) getObjectField(revokeMsgInfo, "a", int.class);
        long msgUid = (long) getObjectField(revokeMsgInfo, "b", long.class);
        long shmsgseq = (long) getObjectField(revokeMsgInfo, "a", long.class);

        String uin = istroop == 0 ? senderUin : friendUin;
        Object msgObject = getMessage(uin, istroop, shmsgseq, msgUid);
        long id = getMessageId(msgObject);
        String msg = istroop == 0 ? getFriendName(null, senderUin) : getTroopName(friendUin, senderUin);

        mSettings.reload();
        if (id != 0) {
            if (isCallingFrom("C2CMessageProcessor"))
                return;

            msg = "\"" + msg + "\"" + mSettings.getString("qq_recalled", "尝试撤回一条消息 （已阻止)");

            String message = getMessageContent(msgObject);
            int msgtype = getMessageType(msgObject);
            if (mSettings.getBoolean("show_content", false) && msgtype == -1000 /*text msg*/) {
                if (!TextUtils.isEmpty(message)) {
                    msg += ": " + message;
                }
            }

            showMessageTip(friendUin, senderUin, msgUid, shmsgseq, time, msg, istroop);

            if (!mSettings.getBoolean("enable_recall_notification", true) ||
                    (!mSettings.getBoolean("enable_troopassistant_recall_notification", false)
                            && istroop == 1 && isInTroopAssistant(uin)))
                return;

            Intent intent = createIntent(uin, istroop);
            String title = getTroopName(istroop == 0 ? null : friendUin, senderUin) +
                    " " + mSettings.getString("qq_recalled", "尝试撤回一条消息");
            Bitmap avatar = getAvatar(senderUin);

            if (msgtype == -1000) { // text msg
                showTextNotification(title, msg, avatar, intent);
            } else if (msgtype == -2000) { // img
                showImageNotification(title, msg, avatar, getImage(msgObject), intent);
            }
        } else {
            msg = "\"" + msg + "\"" + mSettings.getString("qq_recalled_offline", "撤回了一条消息 (没收到)");
            showMessageTip(friendUin, senderUin, msgUid, shmsgseq, time, msg, istroop);
        }

    }

    private void showMessageTip(String friendUin, String senderUin, long msgUid, long shmsgseq,
                                long time, String msg, int istroop) {
        if (msgUid != 0) {
            msgUid += new Random().nextInt();
        }
        try {
            List tips = createMessageTip(friendUin, senderUin, msgUid, shmsgseq, time + 1, msg, istroop);
            if (tips == null || tips.isEmpty())
                return;

            callMethod(mQQMessageFacade, "a", tips, mSelfUin);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }

    }

    private List createMessageTip(String friendUin, String senderUin, long msgUid,
                                  long shmsgseq, long time, String msg, int istroop) {
        int msgtype = -2031; // MessageRecord.MSG_TYPE_REVOKE_GRAY_TIPS
        Object messageRecord = callStaticMethod(MessageRecordFactory, "a", msgtype);
        if (istroop == 0) { // private chat revoke
            callMethod(messageRecord, "init", mSelfUin, senderUin, senderUin, msg, time, msgtype,
                    istroop, time);
        } else { // group chat revoke
            callMethod(messageRecord, "init", mSelfUin, friendUin, senderUin, msg, time, msgtype,
                    istroop, time);
        }

        setObjectField(messageRecord, "msgUid", msgUid);
        setObjectField(messageRecord, "shmsgseq", shmsgseq);
        setObjectField(messageRecord, "isread", true);

        List<Object> list = new ArrayList<>();
        list.add(messageRecord);

        return list;
    }

    protected String getFriendName(String friendUin, String senderUin) {
        String nickname = null;

        if (friendUin != null) {
            nickname = (String) callStaticMethod(ContactUtils, "c", mQQAppInterface, friendUin, senderUin);
        }
        if (TextUtils.isEmpty(nickname)) {
            nickname = (String) callStaticMethod(ContactUtils, "b", mQQAppInterface, senderUin, true);
        }
        if (TextUtils.isEmpty(nickname)) {
            nickname = senderUin;
        }
        return nickname.replaceAll("\\u202E", "").trim();
    }

    protected String getTroopName(String friendUin, String senderUin) {
        if (mTroopManager == null || friendUin == null)
            return getFriendName(friendUin, senderUin);

        Object troopMemberInfo = invokeMethod(mGetTroopInfo, mTroopManager, friendUin, senderUin);
        if (troopMemberInfo == null) {
            return getFriendName(friendUin, senderUin);
        }
        String nickname = (String) XposedHelpers.getObjectField(troopMemberInfo, "troopnick");
        if (TextUtils.isEmpty(nickname)) {
            nickname = (String) XposedHelpers.getObjectField(troopMemberInfo, "friendnick");
        }
        if (TextUtils.isEmpty(nickname)) {
            nickname = getFriendName(friendUin, senderUin);
        }
        return nickname.replaceAll("\\u202E", "").trim();
    }

    protected Object getMessage(String uin, int istroop, long shmsgseq, long msgUid) {
        List list = (List) invokeMethod(mMessageGetter, mQQMessageFacade, uin, istroop,
                shmsgseq, msgUid);

        if (list == null || list.isEmpty())
            return null;

        return list.get(0);
    }

    protected String getMessageContent(Object msgObject) {
        return (String) getObjectField(MessageRecord, msgObject, "msg");
    }


    protected long getMessageId(Object msgObject) {
        if (msgObject == null)
            return 0;

        return (long) getObjectField(MessageRecord, msgObject, "msgUid");
    }

    protected int getMessageType(Object msgObject) {
        if (msgObject == null)
            return -1;

        return (int) getObjectField(MessageRecord, msgObject, "msgtype");
    }


    public Bitmap getAvatar(String uin) {
        if (mQQAppInterface == null)
            return null;

        Bitmap bitmap = null;
        try {
            bitmap = (Bitmap) callMethod(mQQAppInterface, "a", uin, (byte) 3, true);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        return bitmap;
    }

    protected void showTextNotification(String title, String content, Bitmap icon, Intent resultIntent) {
        if (TextUtils.isEmpty(content))
            return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mNotificationContext)
                .setContentTitle(title)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentText(content)
                .setAutoCancel(true);

        if (icon != null) {
            builder.setLargeIcon(icon);
        }

        showNotification(builder, resultIntent);
    }

    protected void showImageNotification(String title, String content, Bitmap icon, Bitmap bitmap, Intent resultIntent) {
        if (TextUtils.isEmpty(content))
            return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mNotificationContext)
                .setContentTitle(title)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true);

        if (icon != null) {
            builder.setLargeIcon(icon);
        }

        if (bitmap != null) {
            builder.setStyle(new NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap).setSummaryText(content));
        } else {
            builder.setContentText(content);
        }

        showNotification(builder, resultIntent);
    }

    protected void showNotification(NotificationCompat.Builder builder, Intent intent) {
        int notifyId = getNotificationId();
        builder.setContentIntent(PendingIntent.getActivity(mNotificationContext, notifyId,
                intent, PendingIntent.FLAG_UPDATE_CURRENT));

        Notification notification = builder.build();
        notification.flags = Notification.FLAG_SHOW_LIGHTS
                | Notification.FLAG_AUTO_CANCEL;

        notification.ledOnMS = 300;
        notification.ledOffMS = 1000;
        notification.ledARGB = Color.GREEN;

        if (mSettings.getBoolean("vibrate_enable", false)) {
            notification.defaults |= Notification.DEFAULT_VIBRATE;
        }
        if (mSettings.getBoolean("ringtone_enable", false)) {
            String uriString = mSettings.getString("ringtone", "");
            if (!TextUtils.isEmpty(uriString)) {
                notification.sound = Uri.parse(uriString);
            } else {
                notification.defaults |= Notification.DEFAULT_SOUND;
            }
        }

        NotificationManager mNotificationManager =
                (NotificationManager) mNotificationContext
                        .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notifyId, notification);
    }

    protected int getNotificationId() {
        return (int) (System.currentTimeMillis() & 0xfffffff);
    }

    protected boolean isInTroopAssistant(String uin) {
        return mTroopMap != null && mTroopMap.containsKey(uin);
    }

    protected String getAccount() {
        if (mQQAppInterface == null)
            return null;
        return (String) callMethod(mQQAppInterface, "getAccount");
    }

    protected Context getContext() {
        if (BaseApplicationClass == null)
            return null;
        return (Context) callStaticMethod(BaseApplicationClass, "getContext");
    }

    protected void reload() {
        String uin = getAccount();
        if (uin == null || mSelfUin == null || uin.equals(mSelfUin))
            return;

        mSelfUin = null;
        mTroopMap = null;
        mGetTroopInfo = null;
        mQQMessageFacade = null;
    }

    protected Intent createIntent(String frienduin, int istroop) {
        Intent intent = new Intent(mNotificationContext, NotificationClass);
        intent.putExtra("uin", frienduin);
        intent.putExtra("uintype", istroop);
        intent.setAction("com.tencent.mobileqq.action.MAINACTIVITY");
        intent.putExtra("open_chatfragment", true);
        return intent;
    }

    public void log(String msg) {
        if (mDebug) {
            XposedBridge.log("" + msg);
        }
    }

    public void log(Throwable t) {
        if (mDebug) {
            XposedBridge.log(t);
        }
    }

    protected Bitmap getImage(Object msgObject) {
        Bitmap bitmap = null;

        try {
            if ((boolean) callMethod(msgObject, "hasBigFile")) {
                bitmap = BitmapFactory.decodeFile((String) callMethod(msgObject,
                        "getFilePath", "chatimg"));
                if (bitmap != null) {
                    return bitmap;
                }
            }
        } catch (Throwable t) {
            log(t);
        }

        try {
            if ((boolean) callMethod(msgObject, "hasThumbFile")) {
                bitmap = BitmapFactory.decodeFile((String) callMethod(msgObject,
                        "getFilePath", "chatthumb"));
            }
        } catch (Throwable t) {
            log(t);
        }

        return bitmap;
    }
}
