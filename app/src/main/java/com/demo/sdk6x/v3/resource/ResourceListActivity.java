package com.demo.sdk6x.v3.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import com.demo.sdk6x.v3.R;
import com.demo.sdk6x.v3.callback.MsgCallback;
import com.demo.sdk6x.v3.callback.MsgIds;
import com.demo.sdk6x.v3.constants.Constants;
import com.demo.sdk6x.v3.data.PathBean;
import com.demo.sdk6x.v3.data.TempData;
import com.demo.sdk6x.v3.listviewlayout.ListViewForScrollView;
import com.demo.sdk6x.v3.live.LiveActivity;
import com.demo.sdk6x.v3.playback.PlayBackActivity;
import com.demo.sdk6x.v3.utils.PrintLog;
import com.demo.sdk6x.v3.utils.ThreadUtil;
import com.demo.sdk6x.v3.utils.UIUtil;
import com.hikvision.vmsnetsdk.CameraInfo;
import com.hikvision.vmsnetsdk.ControlUnitInfo;
import com.hikvision.vmsnetsdk.RegionInfo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import static com.demo.sdk6x.v3.constants.Constants.Resource.TYPE_REGION;

public class ResourceListActivity extends Activity implements MsgCallback,OnItemClickListener, AbsListView.OnScrollListener {
	private String TAG="ResourceListActivity";

	/**
	 * 资源列表
	 */
	private ListViewForScrollView resourceListView;
	private List mList;
	private ResourceListAdapter mAdapter;
//	private ScrollView sv;
	/**
	 * 父节点资源类型，TYPE_UNKNOWN表示首次获取资源列表
	 */
	private int pResType = Constants.Resource.TYPE_UNKNOWN;
	/**
	 * 父控制中心的id，只有当parentResType为TYPE_CTRL_UNIT才有用
	 */
	private String pCtrlUnitId = "0";
	/**
	 * 父区域的id，只有当parentResType为TYPE_REGION才有用
	 */
	private String pRegionId = "0";
	/**
	 * 消息处理Handler
	 */
	private MsgHandler handler;
	/**
	 * 获取资源逻辑控制类
	 */
	private ResourceControl rc;
	private Dialog mDialog;
	private TextView videoList_path;
	private static final int GOTO_LIVE_OR_PLAYBACK = 0x0b;
	private Stack<PathBean> pathStack;
	private String mPid="";
	private int firstVisibleItem=0;
	private boolean isLoading=false;
	private int currentPage=1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.resource_list_activity);

		pathStack=new Stack<PathBean>();

		resourceListView = (ListViewForScrollView) findViewById(R.id.ctrlunit_list);
		
		resourceListView.setOnItemClickListener(this);

		((ListView)resourceListView).setOnScrollListener(this);
//		sv = (ScrollView) findViewById(R.id.mScrollView);
//		sv.smoothScrollTo(0, 0);
		mList = new ArrayList();
		handler = new MsgHandler();
		rc = new ResourceControl();
		rc.setCallback(this);
//		initData();

		videoList_path= (TextView) findViewById(R.id.list_path);
		setBackEvent();

		mAdapter = new ResourceListAdapter(ResourceListActivity.this ,mList);
		resourceListView.setAdapter(mAdapter);
		
		reqResList();
	}

	//设置头部路径的span事件
	public void setBackEvent(){
		videoList_path.setText("返回上一级");
		Drawable drawable= getResources().getDrawable(R.drawable.back);
		drawable.setBounds(0,0,50,50);
		videoList_path.setCompoundDrawablePadding(10);
		videoList_path.setCompoundDrawables(drawable,null,null,null);
		videoList_path.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				backAction();
			}
		});
		videoList_path.setVisibility(View.GONE);
	}


	@Override
	public void onBackPressed() {
		super.onBackPressed();
		backAction();
	}

	//回退path状态
	private void backAction() {
		if (!pathStack.empty() && rc!=null) {
			videoList_path.setVisibility(View.VISIBLE);
			PathBean bean=pathStack.pop();
			pResType=bean.resType;
			mPid=bean.pId;
			currentPage=1;
			rc.reqResList(pResType,mPid);
			if (pathStack.empty()){
				videoList_path.setVisibility(View.GONE);
			}
		}else {
			this.finish();
		}
	}
	//前进path状态
	private void goAction(PathBean bean) {
		pathStack.push(bean);
		if (!pathStack.empty()) {
			videoList_path.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * 请求资源列表
	 */
	private void reqResList() {
		ThreadUtil.execute(new Runnable() {
			@Override
			public void run() {
				String pId = null;
				if (Constants.Resource.TYPE_CTRL_UNIT == pResType) {
					pId = pCtrlUnitId;
				} else if (TYPE_REGION == pResType) {
					pId = pRegionId;
				}
				rc.reqResList(pResType, pId);
				mPid=pId;
			}
		});
	}

	/**
	 * 加载更多数据
	 */
	private void loadMoreRegion(){
		ThreadUtil.execute(new Runnable() {
					@Override
					public void run() {
						rc.requestSubResFromRegionByPage(mPid, currentPage + 1);
					}});
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch (scrollState) {
			case SCROLL_STATE_IDLE://停止滑动
				Log.w(TAG, "停止滑动");
				break;
			case SCROLL_STATE_TOUCH_SCROLL://正在滑动
				Log.w(TAG, "正在滑动");
				break;
			case SCROLL_STATE_FLING://滑动ListView离开后，由于惯性继续滑动
				Log.w(TAG, "滑动ListView离开后，由于惯性继续滑动");
				break;
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		//可用于底部加载更多数据的判断逻辑
		if (pResType==TYPE_REGION && !isLoading && firstVisibleItem + visibleItemCount > totalItemCount-3 && totalItemCount > 0 ) {
			PrintLog.log(this,TAG,"正在加载更多列表！:mPid:"+mPid+",currentPage:"+currentPage);
			Log.e(TAG, "滑动到ListView的倒数第三个子项");
			isLoading=true;
			loadMoreRegion();
		}

		//判断ListView的滑动方向
		if (this.firstVisibleItem == firstVisibleItem) {
			Log.e(TAG, "未发生滑动");
		} else if (this.firstVisibleItem > firstVisibleItem) {
			Log.e(TAG, "发生下滑");
		} else {
			Log.e(TAG, "发生上滑");
		}
		PrintLog.log(this,TAG,"正在滑动：最后一个可见的位置是:"+(firstVisibleItem+visibleItemCount));
		this.firstVisibleItem = firstVisibleItem;
	}


	@SuppressLint("HandlerLeak")
    private final class MsgHandler extends Handler {
        @Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {

			// 获取控制中心列表成功
			case MsgIds.GET_C_F_NONE_SUC:
				
				// 从控制中心获取下级资源列表成功
			case MsgIds.GET_SUB_F_C_SUC:
				
				// 从区域获取下级列表成功
			case MsgIds.GET_SUB_F_R_SUC:
				currentPage=1;
				isLoading=false;
				PathBean bean=new PathBean(pResType,mPid);
				goAction(bean);
				refreshResList((List)msg.obj);
				break;
			case MsgIds.GET_SUB_F_R_SUC_MORE:
				currentPage++;
				isLoading=false;
				loadMoreResList((List)msg.obj);
				break;
				
			// 获取控制中心列表失败
			case MsgIds.GET_C_F_NONE_FAIL:
				
				// 调用getControlUnitList失败
			case MsgIds.GET_CU_F_CU_FAIL:
				
				// 调用getRegionListFromCtrlUnit失败
			case MsgIds.GET_R_F_C_FAIL:
				
				// 调用getCameraListFromCtrlUnit失败
			case MsgIds.GET_C_F_C_FAIL:
				
				// 从控制中心获取下级资源列表成失败
			case MsgIds.GET_SUB_F_C_FAIL:
				
				// 调用getRegionListFromRegion失败
			case MsgIds.GET_R_F_R_FAIL:
				
				// 调用getCameraListFromRegion失败
			case MsgIds.GET_C_F_R_FAIL:
				//获取更多列表失败
			case MsgIds.GET_SUB_F_R_FAIL_MORE:
				// 从区域获取下级列表失败
			case MsgIds.GET_SUB_F_R_FAILED:
				onGetResListFailed();
				isLoading=false;
				break;
			case GOTO_LIVE_OR_PLAYBACK:
				CameraInfo cInfo = (CameraInfo)msg.obj;
				gotoLiveorPlayBack(cInfo);
				break;
			default:
				break;
			}
		}
	}


	/**
	 * 调用接口失败时，界面弹出提示
	 */
	private void onGetResListFailed() {
		UIUtil.showToast(this,
				getString(R.string.fetch_reslist_failed, UIUtil.getErrorDesc()));
	}

	/**
	 * 获取数据成功后加载更多列表
	 *
	 * @param data
	 */
	private void loadMoreResList(List data) {
		if (data == null || data.isEmpty()) {
			UIUtil.showToast(this, R.string.no_more_data_tip);
			return;
		}
		UIUtil.showToast(this, R.string.fetch_resource_suc);
		if(mAdapter != null){
			mAdapter.addData(data);
		}
	}

	/**
	 * 获取数据成功后刷新列表
	 * 
	 * @param data
	 */
	private void refreshResList(List data) {
		if (data == null || data.isEmpty()) {
			UIUtil.showToast(this, R.string.no_data_tip);
			return;
		}
		UIUtil.showToast(this, R.string.fetch_resource_suc);
		if(mAdapter != null){
			mAdapter.setData(data);
		}
	}

	@Override
	public void onMsg(int msgId, Object data) {
		// TODO Auto-generated method stub
		Message msg = Message.obtain();
		msg.what = msgId;
		msg.obj = data;
		handler.sendMessage(msg);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// TODO Auto-generated method stub
		
		final Object itemData = mAdapter.getItem(position);
//		String itemName = getItemName(itemData);

		ThreadUtil.execute(new Runnable() {
			@Override
			public void run() {
				
				if (itemData instanceof CameraInfo) {
					CameraInfo info = (CameraInfo) itemData;
					//得到摄像头，进行预览或者回放
					Log.i(Constants.LOG_TAG,"get Camera:" + info.getName() + "---" + info.getDeviceID());
					PrintLog.log(ResourceListActivity.this,"onItemClick","get Camera:" + info.getName() + "---" + info.getDeviceID());
					onMsg(GOTO_LIVE_OR_PLAYBACK,info);
				}else{
					String pId = null;
					if (itemData instanceof ControlUnitInfo) {
						ControlUnitInfo info = (ControlUnitInfo) itemData;
						pResType = Constants.Resource.TYPE_CTRL_UNIT;
						pId = info.getControlUnitID();
					}

					if (itemData instanceof RegionInfo) {
						RegionInfo info = (RegionInfo) itemData;
						pResType = TYPE_REGION;
						pId = info.getRegionID();
					}

					rc.reqResList(pResType, pId);
					mPid=pId;
				}
				
			}
		});
	}

	private void gotoLiveorPlayBack(final CameraInfo info) {
		// TODO Auto-generated method stub
		String[] datas = new String[]{"预览","回放"};
		mDialog = new AlertDialog.Builder(ResourceListActivity.this).setSingleChoiceItems(datas, 0, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				mDialog.dismiss();
				switch (which) {
				case 0:
					gotoLive(info);
					break;
				case 1:
					gotoPlayback(info);
					break;
				default:
					break;
				}
			}

		}).create();
		mDialog.show();
	}

	/**实时预览
	 * @param info
	 */
	private void gotoLive(CameraInfo info) {
		// TODO Auto-generated method stub
		if(info == null){
            Log.e(Constants.LOG_TAG,"gotoLive():: fail");
			PrintLog.log(this,"gotoLive","gotoLive():: fail");
            return;
        }
		Intent intent = new Intent(ResourceListActivity.this, LiveActivity.class);
		intent.putExtra(Constants.IntentKey.CAMERA_ID, info.getId());
		TempData.getIns().setCameraInfo(info);
		ResourceListActivity.this.startActivity(intent);
	}
	
	/**远程回放
	 * @param info
	 */
	private void gotoPlayback(CameraInfo info) {
		// TODO Auto-generated method stub
		if(info == null){
	        Log.e(Constants.LOG_TAG,"gotoPlayback():: fail");
			PrintLog.log(this,"gotoPlayback","gotoPlayback():: fail");
	        return;
	    }
		Intent intent = new Intent(ResourceListActivity.this, PlayBackActivity.class);
		intent.putExtra(Constants.IntentKey.CAMERA_ID, info);
		ResourceListActivity.this.startActivity(intent);
	}

	
}
