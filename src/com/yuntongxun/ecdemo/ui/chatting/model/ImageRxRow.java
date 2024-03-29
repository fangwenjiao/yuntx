/*
 *  Copyright (c) 2013 The CCP project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a Beijing Speedtong Information Technology Co.,Ltd license
 *  that can be found in the LICENSE file in the root of the web site.
 *
 *   http://www.cloopen.com
 *
 *  An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
package com.yuntongxun.ecdemo.ui.chatting.model;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.nostra13.universalimageloader.utils.DiskCacheUtils;
import com.yuntongxun.ecdemo.R;
import com.yuntongxun.ecdemo.common.utils.DemoUtils;
import com.yuntongxun.ecdemo.common.utils.FileAccessor;
import com.yuntongxun.ecdemo.common.utils.ResourceHelper;
import com.yuntongxun.ecdemo.storage.ImgInfoSqlManager;
import com.yuntongxun.ecdemo.ui.chatting.ChattingActivity;
import com.yuntongxun.ecdemo.ui.chatting.holder.BaseHolder;
import com.yuntongxun.ecdemo.ui.chatting.holder.ImageRowViewHolder;
import com.yuntongxun.ecdemo.ui.chatting.view.ChattingItemContainer;
import com.yuntongxun.ecsdk.ECMessage;

import java.io.File;


/**
 * <p>Title: ImageRxRow.java</p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2014</p>
 * <p>Company: Beijing Speedtong Information Technology Co.,Ltd</p>
 * @author Jorstin Chan
 * @date 2014-4-17
 * @version 1.0
 */
public class ImageRxRow extends BaseChattingRow {
	
	public ImageRxRow(int type){
		super(type);
	}
	
	
	@Override
	public View buildChatView(LayoutInflater inflater , View convertView) {
		
        //we have a don't have a converView so we'll have to create a new one
        if (convertView == null) {
        	convertView = new ChattingItemContainer(inflater, R.layout.chatting_item_from_picture);
        	
            //use the view holder pattern to save of already looked up subviews
        	ImageRowViewHolder holder = new ImageRowViewHolder(mRowType);
        	convertView.setTag(holder.initBaseHolder(convertView, true));

        }
        //actually setup the view
		return convertView;
	}
	
	@Override
	public void buildChattingData(Context context, BaseHolder baseHolder,
			ECMessage detail, int position) {

		final ImageRowViewHolder holder = (ImageRowViewHolder) baseHolder;
		String userData = detail.getUserData();
		ViewHolderTag holderTag = ViewHolderTag.createTag(detail, ViewHolderTag.TagType.TAG_VIEW_PICTURE ,position);
		View.OnClickListener onClickListener = ((ChattingActivity) context).getChattingAdapter().getOnClickListener();
		holder.chattingContentIv.setTag(holderTag);
		holder.chattingContentIv.setOnClickListener(onClickListener);
		holder.chattingContentIv.setImageBitmap(null);
        if(TextUtils.isEmpty(userData)) {
            return ;
        }
		int start = userData.indexOf("THUMBNAIL://");
		if(start != -1) {
			final String thumbnail = userData.substring(start);
			Bitmap thumbBitmap = ImgInfoSqlManager.getInstance().getThumbBitmap(thumbnail, 2);
			final ImgInfo imgInfo = ImgInfoSqlManager.getInstance().getImgInfo(detail.getMsgId());
			DisplayImageOptions.Builder optionsBuilder = DemoUtils.getChatDisplayImageOptionsBuilder();
			optionsBuilder.showImageOnLoading(new BitmapDrawable(thumbBitmap));
			if(imgInfo != null && !TextUtils.isEmpty(imgInfo.getBigImgPath())) {
				if(imgInfo.getBigImgPath().startsWith("http:")) {
					ImageLoader.getInstance().displayImage(imgInfo.getBigImgPath(), holder.chattingContentIv, optionsBuilder.build() ,new SimpleImageLoadingListener(){
						@Override
						public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
							super.onLoadingComplete(imageUri, view, loadedImage);
							if(imageUri.startsWith("http:")) {
								File imgCacheFile = DiskCacheUtils.findInCache(imageUri, ImageLoader.getInstance().getDiskCache());
								if(imgCacheFile != null) {
									imgInfo.setBigImgPath(imgCacheFile.getAbsolutePath().substring(imgCacheFile.getAbsolutePath().lastIndexOf("/")));
									ImgInfoSqlManager.getInstance().updateImageInfo(imgInfo);
								}
							}
						}
					});
				} else {
					ImageLoader.getInstance().displayImage("file://" + FileAccessor.getImagePathName() + "/"+  imgInfo.getBigImgPath(), holder.chattingContentIv, optionsBuilder.build());
				}
			}
		}

		int startWidth = userData.indexOf("outWidth://");
		int startHeight = userData.indexOf(",outHeight://");
		FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) holder.chattingContentIv.getLayoutParams();
		if(startWidth != -1 && startHeight != -1 && start != -1) {
			int imageMinWidth =  ResourceHelper.fromDPToPix(context , 100);
			int width = DemoUtils.getInt(userData.substring(startWidth + "outWidth://".length(), startHeight) , imageMinWidth);
			int height = DemoUtils.getInt(userData.substring(startHeight + ",outHeight://".length(), start - 1) , imageMinWidth);
			holder.chattingContentIv.setMinimumWidth(imageMinWidth);
			params.width = imageMinWidth;
			int _height = height * imageMinWidth /width;
			if(_height > ResourceHelper.fromDPToPix(context , 230)) {
				_height = ResourceHelper.fromDPToPix(context , 230);
				holder.chattingContentIv.setScaleType(ImageView.ScaleType.CENTER_CROP);
			}
			if(width != 0) {
				holder.chattingContentIv.setMinimumHeight(_height);
				params.height = _height;
			} else {
				holder.chattingContentIv.setMinimumHeight(imageMinWidth);
				params.height = imageMinWidth;
			}
			holder.chattingContentIv.setLayoutParams(params);

		}
	}
	
	@Override
	public int getChatViewType() {

		return ChattingRowType.IMAGE_ROW_RECEIVED.ordinal();
	}


	@Override
	public boolean onCreateRowContextMenu(ContextMenu contextMenu,
			View targetView, ECMessage detail) {
		// TODO Auto-generated method stub
		return false;
	}

}
