package com.github.tvbox.osc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.github.tvbox.osc.event.ServerEvent;
import com.github.tvbox.osc.ui.compose.activities.SearchComposeActivity;
import com.github.tvbox.osc.util.AppManager;

import org.greenrobot.eventbus.EventBus;

/**
 * @author pj567
 * @date :2021/1/5
 * @description:
 */
public class SearchReceiver extends BroadcastReceiver {
    public static String action = "android.content.movie.search.Action";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (action.equals(intent.getAction()) && intent.getExtras() != null) {
            if (AppManager.getInstance().getActivity(SearchComposeActivity.class) != null) {
                AppManager.getInstance().backActivity(SearchComposeActivity.class);
                EventBus.getDefault().post(new ServerEvent(ServerEvent.SERVER_SEARCH, intent.getExtras().getString("title")));
            } else {
                Intent newIntent = new Intent(context, SearchComposeActivity.class);
                newIntent.putExtra("title", intent.getExtras().getString("title"));
                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                context.startActivity(newIntent);
            }
        }
    }
}