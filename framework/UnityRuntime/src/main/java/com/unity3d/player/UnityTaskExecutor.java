package com.unity3d.player;


import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.util.Log;

import java.util.LinkedList;

public final class UnityTaskExecutor implements ITaskExecutor {

    @Override
    public  void executeTaskByCheckPermission(Activity activity, final Runnable runnable) {
        if (activity == null) {
            return;
        }
        try {
            PackageManager packageManager = activity.getPackageManager();
            PackageInfo pi = packageManager.getPackageInfo(activity.getPackageName(), PackageManager.GET_PERMISSIONS);
            if (pi.requestedPermissions == null) {
                pi.requestedPermissions = new String[0];
            }
            final LinkedList<String> listPermissions = new LinkedList<String>();
            for (String permission : pi.requestedPermissions) {
                try {
                    int protectionLevel = packageManager.getPermissionInfo(permission, PackageManager.GET_META_DATA).protectionLevel;
                    int selfPermission = activity.checkCallingOrSelfPermission(permission);
                    if (protectionLevel != PermissionInfo.PROTECTION_DANGEROUS || selfPermission == PackageManager.PERMISSION_GRANTED) continue;
                    listPermissions.add(permission);
                    continue;
                }
                catch (PackageManager.NameNotFoundException v0) {
                    UnityLog.Log(UnityLog.WARN, "Failed to get permission info for " + permission + ", manifest likely missing custom permission declaration");
                    UnityLog.Log(UnityLog.WARN, "Permission " + permission + " ignored");
                }
            }
            if (listPermissions.isEmpty()) {
                runnable.run();
                return;
            }

//            final FragmentManager fragmentManager = activity.getFragmentManager();
//            final Fragment fragment = new Fragment(){
//
//                @Override
//                public void onStart() {
//                    super.onStart();
//                    requestPermissions(listPermissions.toArray(new String[0]), 15881);
//                }
//
//                @Override
//                public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//                    if (requestCode != 15881) {
//                        return;
//                    }
//                    for (requestCode = 0; requestCode < permissions.length && requestCode < grantResults.length; ++requestCode) {
//                        UnityLog.Log(Log.INFO, permissions[requestCode] + (grantResults[requestCode] == 0 ? " granted" : " denied"));
//                    }
//                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//                    fragmentTransaction.remove(this);
//                    fragmentTransaction.commit();
//                    runnable.run();
//                }
//            };
//            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
//            fragmentTransaction.add(0, fragment);
//            fragmentTransaction.commit();
//            return;
        }
        catch (Exception exception) {
            UnityLog.Log(Log.ERROR, "Unable to query for permission: " + exception.getMessage());
            return;
        }
    }
}

