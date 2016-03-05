package org.remote.smartmirror.smartmirror_remote;


import android.content.Context;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.provider.Settings;
import android.util.Log;

public class NsdHelper {

    ControllerActivity mContext;

    NsdManager mNsdManager;
    NsdManager.ResolveListener mResolveListener;
    NsdManager.DiscoveryListener mDiscoveryListener;

    public static final String SERVICE_TYPE = "_http._tcp.";

    public static final String TAG = "NsdHelper";
    // Service name is given by the framework based on mDeviceName
    public String mServiceName;
    // APP_NAME is shared by both remote and mirror app
    public static final String APP_NAME = "SmartMirror";
    public String mDeviceName = APP_NAME;

    NsdServiceInfo mService;

    public NsdHelper(ControllerActivity context) {
        mContext = context;
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        mDeviceName += "_" + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        mResolveListener = new MyResolveListener();
        mDiscoveryListener = new MyDiscoveryListener();
    }

    public class MyDiscoveryListener implements NsdManager.DiscoveryListener {

        @Override
        public void onDiscoveryStarted(String regType) {
            Log.d(TAG, "Service discovery started :: " + regType);
        }

        @Override
        public void onServiceFound(NsdServiceInfo service) {
            Log.d(TAG, "onServiceFound() :: " + service);
            if (!service.getServiceType().equals(SERVICE_TYPE)) {
                Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
            } else if (service.getServiceName().equals(mServiceName)) {
                Log.d(TAG, "Same machine: \"" + mServiceName + "\"");
            } else if (service.getServiceName().contains(APP_NAME)) {
                mNsdManager.resolveService(service, mResolveListener);
                mContext.serviceDiscovered(service);
            } else {
                Log.i(TAG, "Service not matched to application.. ?!");
            }
        }

        @Override
        public void onServiceLost(NsdServiceInfo service) {
            Log.i(TAG, "service lost :: " + service);
            if (mService == service) {
                mService = null;
            }
            mContext.serviceLost(service);
        }

        @Override
        public void onDiscoveryStopped(String serviceType) {
            Log.i(TAG, "Discovery stopped: " + serviceType);
            mContext.clearServices();
        }

        @Override
        public void onStartDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }

        @Override
        public void onStopDiscoveryFailed(String serviceType, int errorCode) {
            Log.e(TAG, "Discovery failed: Error code:" + errorCode);
            mNsdManager.stopServiceDiscovery(this);
        }
    }


    private class MyResolveListener implements NsdManager.ResolveListener {
        @Override
        public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
            Log.e(TAG, "Resolve failed" + errorCode);
        }

        @Override
        public void onServiceResolved(NsdServiceInfo serviceInfo) {
            Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

            if (serviceInfo.getServiceName().equals(mServiceName)) {
                Log.d(TAG, "Same IP.");
                return;
            }
            mService = serviceInfo;
            mContext.connectToRemote();
        }
    }

    public void discoverServices() {
        Log.d(TAG, "discoverServices()");
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    public void stopDiscovery() {
        mNsdManager.stopServiceDiscovery(mDiscoveryListener);
    }

    public NsdServiceInfo getChosenServiceInfo() {
        return mService;
    }

    public void tearDown() {
        mResolveListener = null;
    }
}

