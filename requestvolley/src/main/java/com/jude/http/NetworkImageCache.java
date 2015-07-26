package com.jude.http;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;

import com.jude.http.DiskLruCache.Editor;
import com.jude.volley.toolbox.ImageLoader;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class NetworkImageCache extends LruCache<String, Bitmap> implements
		ImageLoader.ImageCache {
	private DiskLruCache mDiskLruCache;
    private static final int DiskCacheSize = 100 * 1024 * 1024;//硬盘缓存大小100M

	public NetworkImageCache(Context ctx) {
		this(ctx,getDefaultLruCacheSize());
		
	}

	public NetworkImageCache(Context ctx,int sizeInKiloBytes) {
		super(sizeInKiloBytes);
		mDiskLruCache = null;  
		try {  
		    File cacheDir = getDiskCacheDir(ctx, "bitmap");  
		    if (!cacheDir.exists()) {  
		        cacheDir.mkdirs();  
		    }
		    mDiskLruCache = DiskLruCache.open(cacheDir, getAppVersion(ctx), 1, DiskCacheSize);
		} catch (IOException e) {  
		    e.printStackTrace();  
		}  
	}

	@Override
	protected int sizeOf(String key, Bitmap value) {
		return value.getRowBytes() * value.getHeight() / 1024;
	}

	@Override
	public Bitmap getBitmap(String url) {
		return get(url);
	}

	@Override
	public void putBitmap(String url, Bitmap bitmap) {
		put(url, bitmap);
		try {
			Editor editor = mDiskLruCache.edit(hashKeyForDisk(url));
			bitmap.compress(Bitmap.CompressFormat.JPEG, 100, editor.newOutputStream(0));
			editor.commit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static int getDefaultLruCacheSize() {
		final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
		final int cacheSize = maxMemory / 8;
		return cacheSize;
	}
	
	@Override
	protected Bitmap create(String url) {
        String key = hashKeyForDisk(url);
		Bitmap bitmap = null;
		try {
			DiskLruCache.Snapshot snapShot = mDiskLruCache.get(key);
			if(snapShot!=null){
				bitmap = BitmapFactory.decodeStream(snapShot.getInputStream(0));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	public File getDiskCacheDir(Context context, String uniqueName) {  
	    String cachePath;  

	    cachePath = context.getCacheDir().getPath();

	    return new File(cachePath + File.separator + uniqueName);  
	} 	
	
	public int getAppVersion(Context context) {  
	    try {  
	        PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);  
	        return info.versionCode;  
	    } catch (NameNotFoundException e) {  
	        e.printStackTrace();  
	    }  
	    return 1;  
	} 
	
	public String hashKeyForDisk(String key) {  
	    String cacheKey;  
	    try {  
	        final MessageDigest mDigest = MessageDigest.getInstance("MD5");  
	        mDigest.update(key.getBytes());  
	        cacheKey = bytesToHexString(mDigest.digest());  
	    } catch (NoSuchAlgorithmException e) {  
	        cacheKey = String.valueOf(key.hashCode());  
	    }  
	    return cacheKey;  
	}  
	  
	private String bytesToHexString(byte[] bytes) {  
	    StringBuilder sb = new StringBuilder();  
	    for (int i = 0; i < bytes.length; i++) {  
	        String hex = Integer.toHexString(0xFF & bytes[i]);  
	        if (hex.length() == 1) {  
	            sb.append('0');  
	        }  
	        sb.append(hex);  
	    }  
	    return sb.toString();  
	}  
}
