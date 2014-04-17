/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.phonegap.tasks;

import io.usersource.annoplugin.utils.AnnoUtils;

import org.apache.cordova.Config;
import org.apache.cordova.CordovaActivity;

import android.content.pm.ActivityInfo;
import android.gesture.GestureOverlayView;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class Tasks extends CordovaActivity 
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        super.init();

        GestureOverlayView view = new GestureOverlayView(this);
        view.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT, 1));

        setContentView(view);
        view.addView((View) appView.getParent()); // adds the PhoneGap browser
        view.getChildAt(0).setLayoutParams(
                new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT, 1));

        setContentView(view);
        AnnoUtils.setEnableGesture(this, view, true);

        // Set by <content src="index.html" /> in config.xml
        super.loadUrl(Config.getStartUrl(),120000);                          
        super.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //super.loadUrl("file:///android_asset/www/index.html")
    }
}

