package com.mzbloc.springboot.redis.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 类操作工具类
 * Created by tanxw on 2019/1/31.
 */
public final class ClassUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassUtil.class);

    /**
     * 获取类加载器
     */
    public static ClassLoader getClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * 加载类
     */
    public static Class<? extends MessageListener> loadClass(String className, boolean isInitialized) {
        Class<?> cls;
        try {
            cls = Class.forName(className, isInitialized, getClassLoader());
        } catch (ClassNotFoundException e) {
            LOGGER.error("load class failure", e);
            throw new RuntimeException(e);
        }
        return (Class<? extends MessageListener>)cls;
    }

    /**
     * 加载类（默认将初始化类）
     */
    public static Class<? extends MessageListener> loadClass(String className) {
        return loadClass(className, true);
    }

    /**
     * 获取指定包名下的所有类
     */
    public static Set<Class<? extends MessageListener>> getClassSet(String packageName) {
        return getClassSet(packageName,null);
    }


    /**
     * 获取指定包名下的所有类
     */
    public static Set<Class<? extends MessageListener>> getClassSet(String packageName,Class<? extends Annotation> annotationClass) {
        Set<Class<? extends MessageListener>> classSet = new HashSet<>();
        try {
            Enumeration<URL> urls = getClassLoader().getResources(packageName.replace(".", "/"));
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (url != null) {
                    String protocol = url.getProtocol();
                    if (protocol.equals("file")) {
                        String packagePath = url.getPath().replaceAll("%20", " ");
                        addClass(classSet, packagePath, packageName,annotationClass);
                    } else if (protocol.equals("jar")) {
                        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                        if (jarURLConnection != null) {
                            JarFile jarFile = jarURLConnection.getJarFile();
                            if (jarFile != null) {
                                Enumeration<JarEntry> jarEntries = jarFile.entries();
                                while (jarEntries.hasMoreElements()) {
                                    JarEntry jarEntry = jarEntries.nextElement();
                                    String jarEntryName = jarEntry.getName();
                                    if (jarEntryName.endsWith(".class") && annotationClass!=null && annotationClass.getName().equals(jarEntryName)) {
                                        String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
                                        doAddClass(classSet, className);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("get class set failure", e);
            throw new RuntimeException(e);
        }
        return classSet;
    }

    private static void addClass(Set<Class<? extends MessageListener>> classSet, String packagePath, String packageName) {
        addClass(classSet,packagePath,packageName,null);
    }

    private static void addClass(Set<Class<? extends MessageListener>> classSet, String packagePath,
                                 String packageName,Class<? extends Annotation> annotationClass) {
        File[] files = new File(packagePath).listFiles(new FileFilter() {
            public boolean accept(File file) {
                return (file.isFile() && file.getName().endsWith(".class")) || file.isDirectory();
            }
        });
        for (File file : files) {
            String fileName = file.getName();
            if (file.isFile()) {
                String className = fileName.substring(0, fileName.lastIndexOf("."));
                if (!StringUtils.isEmpty(packageName)) {
                    className = packageName + "." + className;
                }
                doAddClass(classSet, className,annotationClass);
            } else {
                String subPackagePath = fileName;
                if (!StringUtils.isEmpty(packagePath)) {
                    subPackagePath = packagePath + "/" + subPackagePath;
                }
                String subPackageName = fileName;
                if (!StringUtils.isEmpty(packageName)) {
                    subPackageName = packageName + "." + subPackageName;
                }
                addClass(classSet, subPackagePath, subPackageName,annotationClass);
            }
        }
    }

    private static void doAddClass(Set<Class<? extends MessageListener>> classSet, String className) {
        doAddClass(classSet,className,null);
    }

    private static void doAddClass(Set<Class<? extends MessageListener>> classSet, String className,Class<? extends Annotation> annotationClass) {
        Class<? extends MessageListener> cls = loadClass(className, false);
        if(annotationClass!= null && !cls.isAnnotationPresent(annotationClass)){
            return;
        }
        classSet.add(cls);
    }
}
