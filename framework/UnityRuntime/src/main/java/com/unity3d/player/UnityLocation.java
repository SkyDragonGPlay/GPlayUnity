package com.unity3d.player;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

final class UnityLocation implements LocationListener {
    private Context mContext;
    private UnityPlayer mUnitPlayer;
    private Location mLocation;
    private float mMinDistance = 0.0f;
    private boolean isStartUpdatingLocation = false;
    private int mAccuracy = Criteria.NO_REQUIREMENT;
    private boolean isClosed = false;
    private int mLocationStatus = 0;

    UnityLocation(Context context, UnityPlayer unityPlayer) {
        this.mContext = context;
        this.mUnitPlayer = unityPlayer;
    }

    public boolean hasAvailableProvider() {
        if (!((LocationManager) this.mContext.getSystemService(Context.LOCATION_SERVICE)).getProviders(new Criteria(), true).isEmpty()) {
            return true;
        }
        return false;
    }

    public void setMinDistance(float minDistance) {
        this.mMinDistance = minDistance;
    }

    public void setAccuracy(float desiredAccuracy) {
        if (desiredAccuracy < 100.0f) {
            this.mAccuracy = Criteria.ACCURACY_LOW;
            return;
        }
        if (desiredAccuracy < 500.0f) {
            this.mAccuracy = Criteria.ACCURACY_LOW;
            return;
        }
        this.mAccuracy = Criteria.ACCURACY_MEDIUM;
    }

    public void startLocation() {
        this.isClosed = false;
        if (this.isStartUpdatingLocation) {
            UnityLog.Log(Log.WARN, "Location_StartUpdatingLocation already started!");
            return;
        }
        if (!this.hasAvailableProvider()) {
            this.setStatus(3);
            return;
        }
        LocationManager locationManager = (LocationManager) this.mContext.getSystemService(Context.LOCATION_SERVICE);
        this.setStatus(1);
        List<String> list = locationManager.getProviders(true);
        if (list.isEmpty()) {
            this.setStatus(3);
            return;
        }
        String currentProvider = null;
        if (this.mAccuracy == Criteria.ACCURACY_MEDIUM) {
            for (String provider : list) {
                if (locationManager.getProvider(provider).getAccuracy() != Criteria.ACCURACY_MEDIUM)
                    continue;
                currentProvider = provider;
                break;
            }
        }
        for (String provider : list) {
            if (currentProvider != null && locationManager.getProvider(provider).getAccuracy() == Criteria.ACCURACY_LOW) continue;
            this.setLocation(locationManager.getLastKnownLocation(provider));
            locationManager.requestLocationUpdates(provider, 0, this.mMinDistance, (LocationListener) this, this.mContext.getMainLooper());
            this.isStartUpdatingLocation = true;
        }
    }

    public void enforceClose() {
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);
        this.isStartUpdatingLocation = false;
        this.mLocation = null;
        this.setStatus(0);
    }

    public void safeClose() {
        if (this.mLocationStatus == 1 || this.mLocationStatus == 2) {
            this.isClosed = true;
            this.enforceClose();
        }
    }

    public void safeStartLocation() {
        if (this.isClosed) {
            this.startLocation();
        }
    }

    public void onLocationChanged(Location location) {
        this.setStatus(2);
        this.setLocation(location);
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
        this.mLocation = null;
    }

    private void setLocation(Location location) {
        if (location == null) {
            return;
        }
        if (UnityLocation.checkNeedUpdateLocation(location, this.mLocation)) {
            this.mLocation = location;
            GeomagneticField geomagneticField = new GeomagneticField((float)this.mLocation.getLatitude(), (float)this.mLocation.getLongitude(), (float)this.mLocation.getAltitude(), this.mLocation.getTime());
            this.mUnitPlayer.nativeSetLocation((float)location.getLatitude(), (float)location.getLongitude(), (float)location.getAltitude(), location.getAccuracy(), (double)location.getTime() / 1000.0, geomagneticField.getDeclination());
        }
    }

    private static boolean checkNeedUpdateLocation(Location location, Location currentLocation) {
        if (currentLocation == null) {
            return true;
        }
        long times = location.getTime() - currentLocation.getTime();
        boolean b1 = times > 120000;
        boolean b2 = times < -120000;
        if (b1) {
            return true;
        }
        if (b2) {
            return false;
        }

        boolean  bl = times > 0;
        int comparedAccuracy = (int)(location.getAccuracy() - currentLocation.getAccuracy());
        b1 = comparedAccuracy > 0;
        b2 = comparedAccuracy < 0;
        boolean b3 = comparedAccuracy > 200 | location.getAccuracy() == 0.0f;
        boolean isSameProvider = UnityLocation.isSameProvider(location.getProvider(), currentLocation.getProvider());
        if (b2) {
            return true;
        }
        if (bl && !b1) {
            return true;
        }
        if (bl && !b3 && isSameProvider) {
            return true;
        }
        return false;
    }

    private static boolean isSameProvider(String provider, String currentProvider) {
        if (provider == null) {
            if (currentProvider == null) {
                return true;
            }
            return false;
        }
        return provider.equals(currentProvider);
    }

    private void setStatus(int status) {
        this.mLocationStatus = status;
        this.mUnitPlayer.nativeSetLocationStatus(status);
    }
}

