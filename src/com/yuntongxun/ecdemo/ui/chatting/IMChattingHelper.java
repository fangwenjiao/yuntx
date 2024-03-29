/*
 *  Copyright (c) 2013 The CCP project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a Beijing Speedtong Information Technology Co.,Ltd license
 *  that can be found in the LICENSE file in the root of the web site.
 *
 *   http://www.yuntongxun.com
 *
 *  An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package com.yuntongxun.ecdemo.ui.chatting;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.yuntongxun.ecdemo.common.CCPAppManager;
import com.yuntongxun.ecdemo.common.utils.DateUtil;
import com.yuntongxun.ecdemo.common.utils.DemoUtils;
import com.yuntongxun.ecdemo.common.utils.ECNotificationManager;
import com.yuntongxun.ecdemo.common.utils.ECPreferenceSettings;
import com.yuntongxun.ecdemo.common.utils.ECPreferences;
import com.yuntongxun.ecdemo.common.utils.FileAccessor;
import com.yuntongxun.ecdemo.common.utils.LogUtil;
import com.yuntongxun.ecdemo.core.ClientUser;
import com.yuntongxun.ecdemo.storage.ContactSqlManager;
import com.yuntongxun.ecdemo.storage.GroupNoticeSqlManager;
import com.yuntongxun.ecdemo.storage.IMessageSqlManager;
import com.yuntongxun.ecdemo.storage.ImgInfoSqlManager;
import com.yuntongxun.ecdemo.ui.SDKCoreHelper;
import com.yuntongxun.ecdemo.ui.chatting.model.ImgInfo;
import com.yuntongxun.ecdemo.ui.contact.ECContacts;
import com.yuntongxun.ecdemo.ui.group.DemoGroupNotice;
import com.yuntongxun.ecdemo.ui.group.GroupNoticeHelper;
import com.yuntongxun.ecsdk.ECChatManager;
import com.yuntongxun.ecsdk.ECDevice;
import com.yuntongxun.ecsdk.ECError;
import com.yuntongxun.ecsdk.ECMessage;
import com.yuntongxun.ecsdk.OnChatReceiveListener;
import com.yuntongxun.ecsdk.PersonInfo;
import com.yuntongxun.ecsdk.SdkErrorCode;
import com.yuntongxun.ecsdk.im.ECFileMessageBody;
import com.yuntongxun.ecsdk.im.ECImageMessageBody;
import com.yuntongxun.ecsdk.im.ECTextMessageBody;
import com.yuntongxun.ecsdk.im.ECVoiceMessageBody;
import com.yuntongxun.ecsdk.im.group.ECGroupNoticeMessage;

import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


/**
 * @author Jorstin Chan@容联•云通讯
 * @date 2014-12-12
 * @version 4.0
 */
public class IMChattingHelper implements OnChatReceiveListener, ECChatManager.OnDownloadMessageListener {

    private static final String TAG = "ECSDK_Demo.IMChattingHelper";
    public static final String INTENT_ACTION_SYNC_MESSAGE = "com.yuntongxun.ecdemo_sync_message";
    public static final String GROUP_PRIVATE_TAG = "@priategroup.com";
    public static final int MAX_OFFINE_COUNT = 100;
    private static HashMap<String, SyncMsgEntry> syncMessage = new HashMap<String, SyncMsgEntry>();
    private static IMChattingHelper sInstance;
    private boolean isSyncOffline = false;
    public static IMChattingHelper getInstance(){
        if(sInstance == null) {
            sInstance = new IMChattingHelper();
        }
        return sInstance;
    }

    /**云通讯SDK聊天功能接口*/
    private ECChatManager mChatManager;
    /**全局处理所有的IM消息发送回调*/
    private ChatManagerListener mListener;
    /**是否是同步消息*/
    private boolean isFirstSync = false;
    private IMChattingHelper() {
        mChatManager = SDKCoreHelper.getECChatManager();
        mListener = new ChatManagerListener();
    }


    private void checkChatManager() {
         mChatManager = SDKCoreHelper.getECChatManager();
    }

    /**
     * 消息发送报告
     */
    private OnMessageReportCallback mOnMessageReportCallback;

    /**
     * 发送ECMessage 消息
     * @param msg
     */
    public static long sendECMessage(ECMessage msg) {
       getInstance().checkChatManager();
        // 获取一个聊天管理器
        ECChatManager manager = getInstance().mChatManager;
        if(manager != null) {
            // 调用接口发送IM消息
            msg.setMsgTime(System.currentTimeMillis());
            manager.sendMessage(msg, getInstance().mListener);
            // 保存发送的消息到数据库
        } else {
            msg.setMsgStatus(ECMessage.MessageStatus.FAILED);
        }
        return IMessageSqlManager.insertIMessage(msg, ECMessage.Direction.SEND.ordinal());
    }

    /**
     * 消息重发
     * @param msg
     * @return
     */
    public static long reSendECMessage(ECMessage msg) {
        ECChatManager manager = getInstance().mChatManager;
        if(manager != null) {
            // 调用接口发送IM消息
            String oldMsgId = msg.getMsgId();
            manager.sendMessage(msg, getInstance().mListener);
            if(msg.getType() == ECMessage.Type.IMAGE) {
                ImgInfo imgInfo = ImgInfoSqlManager.getInstance().getImgInfo(oldMsgId);
                if(imgInfo == null || TextUtils.isEmpty(imgInfo.getBigImgPath())) {
                    return -1;
                }
                String bigImagePath = new File(FileAccessor.getImagePathName(), imgInfo.getBigImgPath()).getAbsolutePath();
                imgInfo.setMsglocalid(msg.getMsgId());
                ECFileMessageBody body = (ECFileMessageBody) msg.getBody();
                body.setLocalUrl(bigImagePath);
                BitmapFactory.Options options = DemoUtils.getBitmapOptions(new File(FileAccessor.IMESSAGE_IMAGE, imgInfo.getThumbImgPath()).getAbsolutePath());
                msg.setUserData("outWidth://" + options.outWidth + ",outHeight://" + options.outHeight + ",THUMBNAIL://" + msg.getMsgId());
                ImgInfoSqlManager.getInstance().updateImageInfo(imgInfo);
            }
            // 保存发送的消息到数据库
            return IMessageSqlManager.changeResendMsg(msg.getId(), msg);
        }
        return -1;
    }

    public static long sendImageMessage(ImgInfo imgInfo , ECMessage message) {

        ECChatManager manager = getInstance().mChatManager;
        if(manager != null) {
            // 调用接口发送IM消息
            manager.sendMessage(message, getInstance().mListener);

            if(TextUtils.isEmpty(message.getMsgId())) {
                return -1;
            }
            imgInfo.setMsglocalid(message.getMsgId());
            BitmapFactory.Options options = DemoUtils.getBitmapOptions(new File(FileAccessor.IMESSAGE_IMAGE, imgInfo.getThumbImgPath()).getAbsolutePath());
            message.setUserData("outWidth://" + options.outWidth + ",outHeight://" + options.outHeight + ",THUMBNAIL://" + message.getMsgId());
            long row = IMessageSqlManager.insertIMessage(message, ECMessage.Direction.SEND.ordinal());
            if(row != -1) {
                return ImgInfoSqlManager.getInstance().insertImageInfo(imgInfo);
            }
        }
        return -1;

    }

    public void getPersonInfo() {
        LogUtil.d(TAG , "[getPersonInfo] currentVersion :");
        final ClientUser clientUser = CCPAppManager.getClientUser();
        if(clientUser == null) {
            return ;
        }
        LogUtil.d(TAG , "[getPersonInfo] currentVersion :" + clientUser.getpVersion() + " ,ServerVersion: " + mServicePersonVersion);
        if(clientUser.getpVersion() < mServicePersonVersion) {
            SDKCoreHelper.getECChatManager().getPersonInfo(new ECChatManager.OnGetPersonInfoListener() {
                @Override
                public void onGetPersonInfoComplete(ECError e, PersonInfo p) {
                    clientUser.setpVersion(p.getVersion());
                    clientUser.setSex(p.getSex());
                    clientUser.setUserName(p.getNickName());
                    if(!TextUtils.isEmpty(p.getBirth())){
                        clientUser.setBirth(DateUtil.getActiveTimelong(p.getBirth()));
                    }
                    String newVersion = clientUser.toString();
                    LogUtil.d(TAG , "[getPersonInfo -result] ClientUser :" + newVersion);
                    try {
                        ECPreferences.savePreference(ECPreferenceSettings.SETTINGS_REGIST_AUTO, newVersion, true);
                    } catch (InvalidClassException e1) {
                        e1.printStackTrace();
                    }

                }
            });
        }

/*        // 获得SDK消息管理类
        ECChatManager manager = ECDevice.getECChatManager();
        // 调用查询个人信息接口，设置结果回调
        manager.getPersonInfo(new ECChatManager.OnGetPersonInfoListener() {
            @Override
            public void onGetPersonInfoComplete(ECError e, PersonInfo p) {
                if("000000".equals(e.errorCode)) {
                    // 个人信息获取成功
                    // 更新个人信息到本地数据库，通知UI刷新
                    return ;
                }
                Log.e("ECSDK_Demo", "get person info fail  " +
                        ", errorCode=" + e.errorCode);
            }
        });*/

    }


    private class ChatManagerListener implements ECChatManager.OnSendMessageListener {

        @Override
        public void onComplete(ECError error) {

        }

        @Override
        public void onSendMessageComplete(ECError error, ECMessage message) {
            if(message == null) {
                return ;
            }
            // 处理ECMessage的发送状态
            if(message != null) {
                if(message.getType() == ECMessage.Type.VOICE) {
                    try {
                        DemoUtils.playNotifycationMusic(CCPAppManager.getContext(), "sound/voice_message_sent.mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                IMessageSqlManager.setIMessageSendStatus(message.getMsgId(), message.getMsgStatus().ordinal());
                IMessageSqlManager.notifyMsgChanged(message.getSessionId());
                if(mOnMessageReportCallback != null) {
                    mOnMessageReportCallback.onMessageReport(error , message);
                }
                return ;
            }
        }

        @Override
        public void onProgress(String  msgId ,int total, int progress) {
            // 处理发送文件IM消息的时候进度回调
            LogUtil.d(TAG , "[IMChattingHelper - onProgress] msgId：" +msgId + " ,total：" + total + " ,progress:" + progress);
        }

    }


    public static void setOnMessageReportCallback(OnMessageReportCallback callback) {
        getInstance().mOnMessageReportCallback = callback;
    }


    public interface OnMessageReportCallback{
        void onMessageReport(ECError error ,ECMessage message);
        void onPushMessage(String sessionId ,List<ECMessage> msgs);
    }

    private int getMaxVersion() {
        int maxVersion = IMessageSqlManager.getMaxVersion();
        int maxVersion1 = GroupNoticeSqlManager.getMaxVersion();
        return maxVersion > maxVersion1 ? maxVersion : maxVersion1;
    }

    /**
     * 收到新的IM文本和附件消息
     */
    @Override
    public void OnReceivedMessage(ECMessage msg) {

        if(msg == null) {
            return ;
        }
        postReceiveMessage(msg, true);

/*        // 接收到的IM消息，根据IM消息类型做不同的处理(IM消息类型：ECMessage.Type)
        ECMessage.Type type = msg.getType();
        if(type == ECMessage.Type.TXT) {
            // 在这里处理文本消息
            ECTextMessageBody textMessageBody = (ECTextMessageBody) msg.getBody();
        } else {

            String thumbnailFileUrl = null;
            String remoteUrl = null;
            if (type == ECMessage.Type.FILE) {
                // 在这里处理附件消息
                ECFileMessageBody fileMsgBody = (ECFileMessageBody) msg.getBody();
                // 获得下载地址
                remoteUrl = fileMsgBody.getRemoteUrl();
            } else if (type == ECMessage.Type.IMAGE) {
                // 在这里处理图片消息
                ECImageMessageBody imageMsgBody = (ECImageMessageBody) msg.getBody();
                // 获得缩略图地址
                thumbnailFileUrl = imageMsgBody.getThumbnailFileUrl();
                // 获得原图地址
                remoteUrl = imageMsgBody.getRemoteUrl();
            } else if (type == ECMessage.Type.VOICE) {
                // 在这里处理语音消息
                ECVoiceMessageBody voiceMsgBody = (ECVoiceMessageBody) msg.getBody();
                // 获得下载地址
                remoteUrl = voiceMsgBody.getRemoteUrl();
            } else {
                Log.e("ECSDK_Demo", "Can't handle msgType=" + type.name() + " , then ignore.");
                // 后续还会支持（地址位置、视频以及自定义等消息类型）
            }

            if(TextUtils.isEmpty(remoteUrl)) {
                return ;
            }
            if(!TextUtils.isEmpty(thumbnailFileUrl)) {
                // 先下载缩略图
            } else {
                // 下载附件
            }
        }
        // 根据不同类型处理完消息之后，将消息序列化到本地存储（sqlite）
        // 通知UI有新消息到达*/

    }


    /**
     * 处理接收消息
     * @param msg
     * @param showNotice
     */
    private synchronized void postReceiveMessage(ECMessage msg , boolean showNotice) {
        // 接收到的IM消息，根据IM消息类型做不同的处理
        // IM消息类型：ECMessage.Type
        if(msg.getType() != ECMessage.Type.TXT) {
            ECFileMessageBody body = (ECFileMessageBody) msg.getBody();
            FileAccessor.initFileAccess();
            if(!TextUtils.isEmpty(body.getRemoteUrl())) {
                boolean thumbnail = false;
                String fileExt = DemoUtils.getExtensionName(body.getRemoteUrl());
                if(msg.getType() == ECMessage.Type.VOICE) {
                    body.setLocalUrl(new File(FileAccessor.getVoicePathName(), DemoUtils.md5(String.valueOf(System.currentTimeMillis())) + ".amr").getAbsolutePath());
                } else if (msg.getType() == ECMessage.Type.IMAGE) {
                    ECImageMessageBody imageBody = (ECImageMessageBody) body;
                    thumbnail = !TextUtils.isEmpty(imageBody.getThumbnailFileUrl());
                    imageBody.setLocalUrl(new File(FileAccessor.getImagePathName() , DemoUtils.md5(thumbnail ? imageBody.getThumbnailFileUrl() :imageBody.getRemoteUrl()) + "." + fileExt).getAbsolutePath());
                } else  {
                    body.setLocalUrl(new File(FileAccessor.getFilePathName() , DemoUtils.md5(String.valueOf(System.currentTimeMillis())) + "." + fileExt).getAbsolutePath());

                }
                if(syncMessage != null) {
                    syncMessage.put(msg.getMsgId(), new SyncMsgEntry(showNotice,thumbnail, msg));
                }
                if(mChatManager != null) {
                    if(thumbnail) {
                        mChatManager.downloadThumbnailMessage(msg, this);
                    } else {
                        mChatManager.downloadMediaMessage(msg, this);
                    }
                }
                if(TextUtils.isEmpty(body.getFileName()) && !TextUtils.isEmpty(body.getRemoteUrl())) {
                    body.setFileName(FileAccessor.getFileName(body.getRemoteUrl()));
                }
                msg.setUserData("fileName=" + body.getFileName());
                if(IMessageSqlManager.insertIMessage(msg, msg.getDirection().ordinal()) > 0) {
                    return ;
                }
            } else {
                LogUtil.e(TAG, "ECMessage fileUrl: null");
            }
        }
        if(IMessageSqlManager.insertIMessage(msg, msg.getDirection().ordinal()) <= 0) {
            return ;
        }

        if(mOnMessageReportCallback != null) {
            ArrayList<ECMessage> msgs = new ArrayList<ECMessage>();
            msgs.add(msg);
            mOnMessageReportCallback.onPushMessage(msg.getSessionId(), msgs);
        }

        // 是否状态栏提示
        if(showNotice) showNotification(msg);
    }

    private static void showNotification(ECMessage msg) {
        if(checkNeedNotification(msg.getSessionId())) {
            ECNotificationManager.getInstance().forceCancelNotification();
            String lastMsg = "";
            if(msg.getType() == ECMessage.Type.TXT) {
                lastMsg = ((ECTextMessageBody) msg.getBody()).getMessage();
            }
            ECContacts contact = ContactSqlManager.getContact(msg.getForm());
            if(contact == null) {
                return ;
            }
            ECNotificationManager.getInstance().showCustomNewMessageNotification(CCPAppManager.getContext(),
                    lastMsg,
                    contact.getNickname(),
                    msg.getSessionId(),
                    msg.getType().ordinal());
        }
    }

    public static void checkDownFailMsg() {
        getInstance().postCheckDownFailMsg();
    }

    private void postCheckDownFailMsg() {
        List<ECMessage> downdFailMsg = IMessageSqlManager.getDowndFailMsg();
        if(downdFailMsg == null || downdFailMsg.isEmpty()) {
            return ;
        }
        for(ECMessage msg : downdFailMsg) {
            ECImageMessageBody body = (ECImageMessageBody) msg.getBody();
            body.setThumbnailFileUrl(body.getRemoteUrl() + "_thum");
            if(syncMessage != null) {
                syncMessage.put(msg.getMsgId(), new SyncMsgEntry(false,true, msg));
            }
            if(mChatManager != null) {
                mChatManager.downloadThumbnailMessage(msg, this);
            }
        }

    }

    /**
     * 是否需要状态栏通知
     * @param contactId
     */
    public static boolean checkNeedNotification(String contactId) {
        String currentChattingContactId = ECPreferences.getSharedPreferences().getString(
                ECPreferenceSettings.SETTING_CHATTING_CONTACTID.getId(),
                (String) ECPreferenceSettings.SETTING_CHATTING_CONTACTID.getDefaultValue());
        if(contactId == null) {
            return true;
        }
        if(contactId.equals(currentChattingContactId)) {
            return false;
        }
        return true;
    }

    @Override
    public void OnReceiveGroupNoticeMessage(ECGroupNoticeMessage notice) {
        if (notice == null) {
            return;
        }

        // 接收到的群组消息，根据群组消息类型做不同处理
        // 群组消息类型：ECGroupMessageType
        GroupNoticeHelper.insertNoticeMessage(notice, new GroupNoticeHelper.OnPushGroupNoticeMessageListener() {

            @Override
            public void onPushGroupNoticeMessage(DemoGroupNotice system) {
                IMessageSqlManager.notifyMsgChanged(GroupNoticeSqlManager.CONTACT_ID);

                ECMessage msg = ECMessage.createECMessage(ECMessage.Type.TXT);
                msg.setSessionId(system.getSender());
                msg.setForm(system.getSender());
                ECTextMessageBody tx = new ECTextMessageBody(system.getContent());
                msg.setBody(tx);
                // 是否状态栏提示
                showNotification(msg);
            }
        });
 /*       // 群组被解散
        if (notice.getType() == ECGroupNoticeMessage.ECGroupMessageType.DISMISS) {
            ECDismissGroupMsg dismissGroupMsg = (ECDismissGroupMsg) notice;
            // 处理群组被解散通知
            // 将群组从本地缓存中删除并通知UI刷新
        }
        // 有人被移除出群组
        if (notice.getType() == ECGroupNoticeMessage.ECGroupMessageType.REMOVE_MEMBER) {
            ECRemoveMemberMsg removeMemberMsg = (ECRemoveMemberMsg) notice;
            // 处理群组移除成员通知
            if ("$Smith账号".equals(removeMemberMsg.getMember())) {
                // 如果是自己则将从本地群组关联关系中移除
                // 通知UI处理刷新
            }
        }

        // 有人退出群组通知（群组成员）
        if(notice.getType() == ECGroupNoticeMessage.ECGroupMessageType.QUIT) {
            ECQuitGroupMsg quitGroupMsg = (ECQuitGroupMsg) notice;
            // 处理某人退出群组通知
        }

        // 有人加入群组通知（群组成员）
        if (notice.getType() == ECGroupNoticeMessage.ECGroupMessageType.JOIN) {
            ECJoinGroupMsg joinGroupMsg = (ECJoinGroupMsg) notice;
            // 处理某人加入群组通知
        }

        // 有人申请加入群组（仅限于管理员）
        if(notice.getType() == ECGroupNoticeMessage.ECGroupMessageType.PROPOSE) {
            ECProposerMsg proposerMsg = (ECProposerMsg) notice;
            // 处理申请加入群组请求通知
        }

        // 群组管理员邀请加入群组通知（群组成员）
        if (notice.getType() == ECGroupNoticeMessage.ECGroupMessageType.INVITE) {
            ECInviterMsg inviterMsg = (ECInviterMsg) notice;
            // 处理群组管理员邀请加入群组通知
            String groupId = inviterMsg.getGroupId();
        }
*/

    }

    private int mHistoryMsgCount = 0;

    @Override
    public void onOfflineMessageCount(int count) {
        mHistoryMsgCount = count;
    }

    /*    @Override
    public void onHistoryMessageCount(int count) {
        // 注册SDK的参数需要设置如下才能收到该回调
        // ECInitParams.setOnChatReceiveListener(new OnChatReceiveListener());
        // count参数标识当前账号的离线消息数
    }*/

/*    @Override
    public int onCountHistoryMessageToGet() {
        // 注册SDK的参数需要设置如下才能收到该回调
        // ECInitParams.setOnChatReceiveListener(new OnChatReceiveListener());
        // 建议根据onHistoryMessageCount(int count)设置接收的离线消息数
        return count;
    }*/

    @Override
    public int onGetOfflineMessage() {
        // 获取全部的离线历史消息
        return ECDevice.SYNC_OFFLINE_MSG_ALL;
    }

    @Override
    public void onReceiveOfflineMessage(List<ECMessage> msgs) {
        // 离线消息的处理可以参考 void OnReceivedMessage(ECMessage msg)方法
        // 处理逻辑完全一样
        // 参考 IMChattingHelper.java
        if(msgs != null && !msgs.isEmpty() && !isFirstSync)isFirstSync = true;
        for(ECMessage msg : msgs) {
            postReceiveMessage(msg, false);
        }
    }

    @Override
    public void onReceiveOfflineMessageCompletion() {
        // SDK离线消息拉取完成之后会通过该接口通知应用
        // 应用可以在此做类似于Loading框的关闭，Notification通知等等
        try{
            // 如果已经没有需要同步消息的请求时候，则状态栏开始提醒
            ECMessage lastECMessage = IMessageSqlManager.getLastECMessage();
            if(lastECMessage != null && mHistoryMsgCount > 0 && isFirstSync) {
                showNotification(lastECMessage);
                // lastECMessage.setSessionId(lastECMessage.getTo().startsWith("G")?lastECMessage.getTo():lastECMessage.getForm());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        isFirstSync = isSyncOffline = false;
        // 无需要同步的消息
        CCPAppManager.getContext().sendBroadcast(new Intent(INTENT_ACTION_SYNC_MESSAGE));
    }

    public int mServicePersonVersion = 0;
    @Override
    public void onServicePersonVersion(int version) {
        mServicePersonVersion = version;
    }

    /**
     * 客服消息
     * @param msg
     */
    @Override
    public void onReceiveDeskMessage(ECMessage msg) {

    }

    @Override
    public void onSoftVersion(String version, int sUpdateMode) {
        SDKCoreHelper.setSoftUpdate(version , sUpdateMode);
    }

    public static boolean isSyncOffline() {
        return getInstance().isSyncOffline;
    }

    /**
     * 下载
     */
    @Override
    public void onDownloadMessageComplete(ECError e, ECMessage message) {
        if(e.errorCode == SdkErrorCode.REQUEST_SUCCESS){
            if(message == null) return;
            // 处理发送文件IM消息的时候进度回调
            LogUtil.d(TAG , "[onDownloadMessageComplete] msgId：" +message.getMsgId());
            postDowloadMessageResult(message);
        } else {
            // 重试下载3次
            SyncMsgEntry remove = syncMessage.remove(message.getMsgId());
            if(remove == null) {
                return ;
            }
            LogUtil.d(TAG , "[onDownloadMessageComplete] download fail , retry ：" +remove.retryCount);
            retryDownload(remove);
        }
    }

    @Override
    public void onComplete(ECError error) {

    }

    @Override
    public void onProgress(String msgId, int totalByte, int progressByte) {
        // 处理发送文件IM消息的时候进度回调
        LogUtil.d(TAG , "[IMChattingHelper - onProgress] msgId: " +msgId + " , totalByte: "+totalByte+" , progressByte:" + progressByte);
    }

    /**
     * 重试下载3次
     * @param entry
     */
    private void retryDownload(SyncMsgEntry entry) {
        if(entry == null || entry.msg == null || entry.isRetryLimit()) {
            return ;
        }
        entry.increase();
        // download ..
        if(mChatManager != null) {
            if(entry.thumbnail) {
                mChatManager.downloadThumbnailMessage(entry.msg, this);
            } else {
                mChatManager.downloadMediaMessage(entry.msg, this);
            }
        }
        syncMessage.put(entry.msg.getMsgId() , entry);
    }

    private synchronized void postDowloadMessageResult(ECMessage message) {
        if(message == null) {
            return ;
        }
        if(message.getType() == ECMessage.Type.VOICE) {
            ECVoiceMessageBody voiceBody = (ECVoiceMessageBody) message.getBody();
            voiceBody.setDuration(DemoUtils.calculateVoiceTime(voiceBody.getLocalUrl()));
        } else if (message.getType() == ECMessage.Type.IMAGE){
            ImgInfo thumbImgInfo = ImgInfoSqlManager.getInstance().getThumbImgInfo(message);
            if(thumbImgInfo == null) {
                return ;
            }
            ImgInfoSqlManager.getInstance().insertImageInfo(thumbImgInfo);
            BitmapFactory.Options options = DemoUtils.getBitmapOptions(new File(FileAccessor.getImagePathName() , thumbImgInfo.getThumbImgPath()).getAbsolutePath());
            message.setUserData("outWidth://" + options.outWidth + ",outHeight://" + options.outHeight + ",THUMBNAIL://" + message.getMsgId());
        }
        if(IMessageSqlManager.updateIMessageDownload(message) <= 0) {
            return ;
        }
        if(mOnMessageReportCallback != null) {
            mOnMessageReportCallback.onMessageReport(null ,message);
        }
        boolean showNotice = true;
        SyncMsgEntry remove = syncMessage.remove(message.getMsgId());
        if(remove != null) {
            showNotice = remove.showNotice;
            if(mOnMessageReportCallback != null  && remove.msg != null) {
                ArrayList<ECMessage> msgs = new ArrayList<ECMessage>();
                msgs.add(remove.msg);
                mOnMessageReportCallback.onPushMessage(remove.msg.getSessionId() ,msgs);
            }
        }
        if(showNotice)showNotification(message);
    }

    public class SyncMsgEntry {
        // 是否是第一次初始化同步消息
        boolean showNotice = false;
        boolean thumbnail = false;

        // 重试下载次数
        private int retryCount = 1;
        ECMessage msg ;

        public SyncMsgEntry( boolean showNotice ,boolean thumbnail, ECMessage message) {
            this.showNotice = showNotice;
            this.msg = message;
            this.thumbnail = thumbnail;
        }

        public void increase() {
            retryCount ++;
        }

        public boolean isRetryLimit() {
            return retryCount >= 3;
        }
    }
}
