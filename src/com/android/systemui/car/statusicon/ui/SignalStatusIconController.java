/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Changes from Qualcomm Innovation Center are provided under the following license:
 *
 * Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
 * SPDX-License-Identifier: BSD-3-Clause-Clear
 */

package com.android.systemui.car.statusicon.ui;

import static com.android.car.ui.utils.CarUiUtils.drawableToBitmap;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.R;
import com.android.systemui.car.statusicon.StatusIconController;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.statusbar.connectivity.NetworkController;
import com.android.systemui.statusbar.connectivity.MobileDataIndicators;
import com.android.systemui.statusbar.connectivity.SignalCallback;
import com.android.systemui.statusbar.connectivity.WifiIndicators;
import com.android.systemui.statusbar.policy.HotspotController;

import javax.inject.Inject;

/**
 * A controller for status icon about mobile data, Wi-Fi, and hotspot.
 */
public class SignalStatusIconController extends StatusIconController implements
        SignalCallback, HotspotController.Callback {

    private final Context mContext;
    private final Resources mResources;
    private final HotspotController mHotspotController;
    private final NetworkController mNetworkController;

    private SignalDrawable mMobileSignalIconDrawable;
    private Drawable mWifiSignalIconDrawable;
    private Drawable mHotSpotIconDrawable;
    private Drawable mLTEDrawable;
    private boolean mIsWifiEnabledAndConnected;
    private boolean mIsHotspotEnabled;

    //GARUDA: Boolean to check if network type LTE
    private boolean isLTE = false;

    @Inject
    SignalStatusIconController(
            Context context,
            @Main Resources resources,
            NetworkController networkController,
            HotspotController hotspotController) {
        mContext = context;
        mResources = resources;
        mHotspotController = hotspotController;
        mNetworkController = networkController;

        mMobileSignalIconDrawable = new SignalDrawable(mContext);
        mHotSpotIconDrawable = mResources.getDrawable(R.drawable.ic_hotspot, mContext.getTheme());

        mNetworkController.addCallback(this);
        mHotspotController.addCallback(this);

        // TwoWheeler: START: Add LTE icon
        mLTEDrawable = mContext.getDrawable(R.drawable.ic_two_wheeler_4g);
        // TwoWheeler: END: Add LTE icon
    }

    @Override
    protected void updateStatus() {
        if (mIsHotspotEnabled) {
            setIconDrawableToDisplay(mHotSpotIconDrawable);
        } else if (mIsWifiEnabledAndConnected) {
            setIconDrawableToDisplay(mWifiSignalIconDrawable);
        } else {
            // TwoWheeler: END: Add LTE icon
            if(isLTE && mLTEDrawable != null) {
                Bitmap lteVersionBitmap = combineBitmaps(drawableToBitmap(mLTEDrawable), drawableToBitmap(mMobileSignalIconDrawable));
                Drawable lteVersionDrawable = new BitmapDrawable(mContext.getResources(), lteVersionBitmap);
                setIconDrawableToDisplay(lteVersionDrawable);
            } else {
                setIconDrawableToDisplay(mMobileSignalIconDrawable);
            }
            // TwoWheeler: END: Add LTE icon
        }
        onStatusUpdated();
    }

    // TwoWheeler: START: Add LTE icon
    private Bitmap combineBitmaps(final Bitmap left, final Bitmap right) {
        // Get the size of the images combined side by side.
        int width = left.getWidth() + right.getWidth();
        int height = Math.max(left.getHeight(), right.getHeight());
        // Create a Bitmap large enough to hold both input images and a canvas to draw to this
        // combined bitmap.
        Bitmap combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(combined);

        // Render both input images into the combined bitmap and return it.
        canvas.drawBitmap(left, 0f, 0f, null);
        canvas.drawBitmap(right, left.getWidth(), 0f, null);

        return combined;
    }
    // TwoWheeler: END: Add LTE icon

    @Override
    public void setMobileDataIndicators(MobileDataIndicators mobileDataIndicators) {
        // TwoWheeler: START: Add LTE icon
        if(mobileDataIndicators.typeContentDescription != null) {
            String tcd = mobileDataIndicators.typeContentDescription.toString().toLowerCase();
            Log.d("SignalStatusIconController", "Current typeContentDescription is: " + tcd);
            isLTE = tcd.contains("lte") || tcd.contains("4g");
        }
        // TwoWheeler: END: Add LTE icon
        mMobileSignalIconDrawable.setLevel(mobileDataIndicators.statusIcon.icon);
        updateStatus();
    }

    @Override
    public void setWifiIndicators(WifiIndicators indicators) {
        mIsWifiEnabledAndConnected = indicators.enabled && indicators.statusIcon.visible;
        mWifiSignalIconDrawable = mResources.getDrawable(indicators.statusIcon.icon,
                mContext.getTheme());
        updateStatus();
    }

    @Override
    public void onHotspotChanged(boolean enabled, int numDevices) {
        mIsHotspotEnabled = enabled;
        updateStatus();
    }

    @Override
    protected int getPanelContentLayout() {
        return R.layout.qc_connectivity_panel;
    }

    @VisibleForTesting
    SignalDrawable getMobileSignalIconDrawable() {
        return mMobileSignalIconDrawable;
    }

    @VisibleForTesting
    Drawable getWifiSignalIconDrawable() {
        return mWifiSignalIconDrawable;
    }

    @VisibleForTesting
    Drawable getHotSpotIconDrawable() {
        return mHotSpotIconDrawable;
    }
}
