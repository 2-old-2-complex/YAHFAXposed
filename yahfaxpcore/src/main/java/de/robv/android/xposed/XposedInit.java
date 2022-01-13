/*
 * This file is part of LSPosed.
 *
 * LSPosed is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LSPosed is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LSPosed.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2020 EdXposed Contributors
 * Copyright (C) 2021 LSPosed Contributors
 */

package de.robv.android.xposed;

import static org.lsposed.lspd.deopt.PrebuiltMethodsDeopter.deoptResourceMethods;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.sInitPackageResourcesCallbacks;
import static de.robv.android.xposed.XposedBridge.sInitZygoteCallbacks;
import static de.robv.android.xposed.XposedBridge.sLoadedPackageCallbacks;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getParameterIndexByType;
import static de.robv.android.xposed.XposedHelpers.setStaticObjectField;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Resources;
import android.content.res.ResourcesImpl;
import android.content.res.TypedArray;
import android.content.res.XResources;
import android.os.Build;
import android.os.IBinder;
import android.os.Process;
import android.util.ArraySet;
import android.util.Log;

import org.lsposed.lspd.nativebridge.ResourcesHook;
import org.lsposed.lspd.yahfa.hooker.YahfaHooker;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_InitZygote;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;


public final class XposedInit {
    private static final String TAG = XposedBridge.TAG;
    public static boolean startsSystemServer = false;

    public static volatile boolean disableResources = false;
    public static AtomicBoolean resourceInit = new AtomicBoolean(false);

    private static boolean isInit=false;

    ///ADD START
    public static XC_LoadPackage.LoadPackageParam init(Context context, String processName)
    {
        if(isInit)
        {
            throw  new IllegalStateException("已经初始化了");
        }
        isInit=true;
        YahfaHooker.init();
        //XposedBridge.initXResources();
        startsSystemServer = false;
        loadedPackagesInProcess.add(context.getPackageName());
        //XResources.setPackageNameForResDir(loadedApk.getPackageName(), loadedApk.getResDir());
        XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                XposedBridge.sLoadedPackageCallbacks);
        lpparam.packageName = context.getPackageName();
        lpparam.processName =processName;
        lpparam.classLoader =context.getClassLoader();
        lpparam.appInfo = context.getApplicationInfo();
        lpparam.isFirstApplication = true;
        XC_LoadPackage.callAll(lpparam);

        return lpparam;
    }
    public static XC_LoadPackage.LoadPackageParam init(Context context, final String packageName, String processName, ClassLoader classLoader, ApplicationInfo appInfo, boolean isFirstApplication)
    {
        if(isInit)
        {
            throw  new IllegalStateException("已经初始化了");
        }
        isInit=true;
        YahfaHooker.init();
        //XposedBridge.initXResources();
        startsSystemServer = false;
        loadedPackagesInProcess.add(context.getPackageName());
        //XResources.setPackageNameForResDir(loadedApk.getPackageName(), loadedApk.getResDir());
        XC_LoadPackage.LoadPackageParam lpparam = new XC_LoadPackage.LoadPackageParam(
                XposedBridge.sLoadedPackageCallbacks);
        lpparam.packageName = context.getPackageName();
        lpparam.processName =processName;
        lpparam.classLoader =classLoader;
        lpparam.appInfo = appInfo;
        lpparam.isFirstApplication = isFirstApplication;
        XC_LoadPackage.callAll(lpparam);

        return lpparam;
    }
    ///ADD END

    public static void hookResources() throws Throwable {
        if (disableResources || !resourceInit.compareAndSet(false, true)) {
            return;
        }

        deoptResourceMethods();

        if (!ResourcesHook.initXResourcesNative()) {
            Log.e(TAG, "Cannot hook resources");
            disableResources = true;
            return;
        }

        findAndHookMethod("android.app.ApplicationPackageManager", null, "getResourcesForApplication",
                ApplicationInfo.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        ApplicationInfo app = (ApplicationInfo) param.args[0];
                        XResources.setPackageNameForResDir(app.packageName,
                                app.uid == Process.myUid() ? app.sourceDir : app.publicSourceDir);
                    }
                });

        /*
         * getTopLevelResources(a)
         *   -> getTopLevelResources(b)
         *     -> key = new ResourcesKey()
         *     -> r = new Resources()
         *     -> mActiveResources.put(key, r)
         *     -> return r
         */

        final Class<?> classGTLR;
        final Class<?> classResKey;
        final ThreadLocal<Object> latestResKey = new ThreadLocal<>();
        final ArrayList<String> createResourceMethods = new ArrayList<>();

        classGTLR = android.app.ResourcesManager.class;
        classResKey = android.content.res.ResourcesKey.class;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            createResourceMethods.add("createResources");
            createResourceMethods.add("createResourcesForActivity");
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
            createResourceMethods.add("createResources");
        } else {
            createResourceMethods.add("getOrCreateResources");
        }

        final Class<?> classActivityRes = XposedHelpers.findClassIfExists("android.app.ResourcesManager$ActivityResource", classGTLR.getClassLoader());
        XC_MethodHook hooker = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                // At least on OnePlus 5, the method has an additional parameter compared to AOSP.
                Object activityToken = null;
                try {
                    final int activityTokenIdx = getParameterIndexByType(param.method, IBinder.class);
                    activityToken = param.args[activityTokenIdx];
                } catch (NoSuchFieldError ignored) {
                }
                final int resKeyIdx = getParameterIndexByType(param.method, classResKey);
                String resDir = (String) getObjectField(param.args[resKeyIdx], "mResDir");
                XResources newRes = cloneToXResources(param, resDir);
                if (newRes == null) {
                    return;
                }

                //noinspection SynchronizeOnNonFinalField
                synchronized (param.thisObject) {
                    ArrayList<Object> resourceReferences;
                    if (activityToken != null) {
                        Object activityResources = callMethod(param.thisObject, "getOrCreateActivityResourcesStructLocked", activityToken);
                        //noinspection unchecked
                        resourceReferences = (ArrayList<Object>) getObjectField(activityResources, "activityResources");
                    } else {
                        //noinspection unchecked
                        resourceReferences = (ArrayList<Object>) getObjectField(param.thisObject, "mResourceReferences");
                    }
                    if (classActivityRes == null) {
                        resourceReferences.add(new WeakReference<>(newRes));
                    } else {
                        Object activityRes = XposedHelpers.newInstance(classActivityRes);
                        XposedHelpers.setObjectField(activityRes, "resources", new WeakReference<>(newRes));
                    }
                }
            }
        };

        for (String createResourceMethod : createResourceMethods) {
            hookAllMethods(classGTLR, createResourceMethod, hooker);
        }

        findAndHookMethod(TypedArray.class, "obtain", Resources.class, int.class,
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (param.getResult() instanceof XResources.XTypedArray) {
                            return;
                        }
                        if (!(param.args[0] instanceof XResources)) {
                            return;
                        }
                        XResources.XTypedArray newResult =
                                new XResources.XTypedArray((Resources) param.args[0]);
                        int len = (int) param.args[1];
                        Method resizeMethod = XposedHelpers.findMethodBestMatch(
                                TypedArray.class, "resize", int.class);
                        resizeMethod.setAccessible(true);
                        resizeMethod.invoke(newResult, len);
                        param.setResult(newResult);
                    }
                });

        // Replace system resources
        XResources systemRes = new XResources(
                (ClassLoader) XposedHelpers.getObjectField(Resources.getSystem(), "mClassLoader"));
        //HiddenApiBridge.Resources_setImpl(systemRes, (ResourcesImpl) XposedHelpers.getObjectField(Resources.getSystem(), "mResourcesImpl"));
        systemRes.initObject(null);
        setStaticObjectField(Resources.class, "mSystem", systemRes);

        XResources.init(latestResKey);
    }

    private static XResources cloneToXResources(XC_MethodHook.MethodHookParam param, String resDir) {
        Object result = param.getResult();
        if (result == null || result instanceof XResources) {
            return null;
        }

        // Replace the returned resources with our subclass.
        XResources newRes = new XResources(
                (ClassLoader) XposedHelpers.getObjectField(param.getResult(), "mClassLoader"));
        //HiddenApiBridge.Resources_setImpl(newRes, (ResourcesImpl) XposedHelpers.getObjectField(param.getResult(), "mResourcesImpl"));
        newRes.initObject(resDir);

        // Invoke handleInitPackageResources().
        if (newRes.isFirstLoad()) {
            String packageName = newRes.getPackageName();
            XC_InitPackageResources.InitPackageResourcesParam resparam = new XC_InitPackageResources.InitPackageResourcesParam(XposedBridge.sInitPackageResourcesCallbacks);
            resparam.packageName = packageName;
            resparam.res = newRes;
            XCallback.callAll(resparam);
        }

        param.setResult(newRes);
        return newRes;
    }

    /**
     * Try to load all modules defined in <code>INSTALLER_DATA_BASE_DIR/conf/modules.list</code>
     */
    private static final AtomicBoolean modulesLoaded = new AtomicBoolean(false);
    private static final Object moduleLoadLock = new Object();
    // @GuardedBy("moduleLoadLock")
    private static final ArraySet<String> loadedModules = new ArraySet<>();

    public static ArraySet<String> getLoadedModules() {
        synchronized (moduleLoadLock) {
            return loadedModules;
        }
    }

    public static void loadModules() {
//        boolean hasLoaded = !modulesLoaded.compareAndSet(false, true);
//        if (hasLoaded) {
//            return;
//        }
//        synchronized (moduleLoadLock) {
//            List<Module> moduleList = serviceClient.getModulesList();
//            var newLoadedApk = new ArraySet<String>();
//            moduleList.forEach(module -> {
//                var apk = module.apkPath;
//                var name = module.packageName;
//                var file = module.file;
//                if (loadedModules.contains(apk)) {
//                    newLoadedApk.add(apk);
//                } else {
//                    loadedModules.add(apk); // temporarily add it for XSharedPreference
//                    boolean loadSuccess = loadModule(name, apk, file);
//                    if (loadSuccess) {
//                        newLoadedApk.add(apk);
//                    }
//                }
//
//                loadedModules.clear();
//                loadedModules.addAll(newLoadedApk);
//
//                // refresh callback according to current loaded module list
//                pruneCallbacks();
//            });
//        }
    }

    // remove deactivated or outdated module callbacks
    private static void pruneCallbacks() {
        synchronized (moduleLoadLock) {
            Object[] loadedPkgSnapshot = sLoadedPackageCallbacks.getSnapshot();
            Object[] initPkgResSnapshot = sInitPackageResourcesCallbacks.getSnapshot();
            Object[] initZygoteSnapshot = sInitZygoteCallbacks.getSnapshot();
            for (Object loadedPkg : loadedPkgSnapshot) {
                if (loadedPkg instanceof IModuleContext) {
                    if (!loadedModules.contains(((IModuleContext) loadedPkg).getApkPath())) {
                        sLoadedPackageCallbacks.remove((XC_LoadPackage) loadedPkg);
                    }
                }
            }
            for (Object initPkgRes : initPkgResSnapshot) {
                if (initPkgRes instanceof IModuleContext) {
                    if (!loadedModules.contains(((IModuleContext) initPkgRes).getApkPath())) {
                        sInitPackageResourcesCallbacks.remove((XC_InitPackageResources) initPkgRes);
                    }
                }
            }
            for (Object initZygote : initZygoteSnapshot) {
                if (initZygote instanceof IModuleContext) {
                    if (!loadedModules.contains(((IModuleContext) initZygote).getApkPath())) {
                        sInitZygoteCallbacks.remove((XC_InitZygote) initZygote);
                    }
                }
            }
        }
    }



    private static boolean initModule(ClassLoader mcl, String apk, List<String> moduleClassNames) {
        int count = 0;
        for (String moduleClassName : moduleClassNames) {
            try {
                Log.i(TAG, "  Loading class " + moduleClassName);

                Class<?> moduleClass = mcl.loadClass(moduleClassName);

                if (!IXposedMod.class.isAssignableFrom(moduleClass)) {
                    Log.e(TAG, "    This class doesn't implement any sub-interface of IXposedMod, skipping it");
                    continue;
                }

                final Object moduleInstance = moduleClass.newInstance();

                if (moduleInstance instanceof IXposedHookZygoteInit) {
                    IXposedHookZygoteInit.StartupParam param = new IXposedHookZygoteInit.StartupParam();
                    param.modulePath = apk;
                    param.startsSystemServer = startsSystemServer;

                    XposedBridge.hookInitZygote(new IXposedHookZygoteInit.Wrapper(
                            (IXposedHookZygoteInit) moduleInstance, param));
                    ((IXposedHookZygoteInit) moduleInstance).initZygote(param);
                    count++;
                }

                if (moduleInstance instanceof IXposedHookLoadPackage) {
                    XposedBridge.hookLoadPackage(new IXposedHookLoadPackage.Wrapper(
                            (IXposedHookLoadPackage) moduleInstance, apk));
                    count++;
                }

                if (moduleInstance instanceof IXposedHookInitPackageResources) {
                    hookResources();
                    XposedBridge.hookInitPackageResources(new IXposedHookInitPackageResources.Wrapper(
                            (IXposedHookInitPackageResources) moduleInstance, apk));
                    count++;
                }
            } catch (Throwable t) {
                Log.e(TAG, "    Failed to load class " + moduleClassName, t);
            }
        }
        return count > 0;
    }



    public final static Set<String> loadedPackagesInProcess = ConcurrentHashMap.newKeySet(1);
}
