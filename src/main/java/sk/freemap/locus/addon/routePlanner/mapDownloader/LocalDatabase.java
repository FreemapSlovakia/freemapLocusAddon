package sk.freemap.locus.addon.routePlanner.mapDownloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

public class LocalDatabase {

	private final File dir;
	private final File filesDir;
	
	public LocalDatabase(final Context context) {
		final File freemapDir = new File(Utils.findLocusDir(context), "freemap.sk");
		freemapDir.mkdir();

		dir = new File(freemapDir, "fileInfo");
		dir.mkdir();
		
		filesDir = new File(freemapDir, "fileList");
		filesDir.mkdir();
	}
	

	public static Map<String, FileInfo> createFileInfoMap(final String json) throws JSONException {
		final Map<String, FileInfo> fileInfoMap = new LinkedHashMap<String, FileInfo>();
		final JSONArray ja = new JSONArray(json);
		for (int i = 0, len = ja.length(); i < len; i++) {
			final JSONObject jo = ja.getJSONObject(i);
			fileInfoMap.put(jo.getString("id"), createFileInfo(jo));
		}
		
// DEBUG:		final FileInfo fileInfo = fileInfoMap.get("freemap_hiking_by_jurajs.zip");
//		fileInfoMap.put("freemap_hiking_by_jurajs.zip", new FileInfo(
//				fileInfo.getId(),
//				System.currentTimeMillis() - 1000,
//				fileInfo.getVersion(),
//				fileInfo.getName()
//				));
		
		return fileInfoMap;
	}


	public static FileInfo createFileInfo(final JSONObject jo) throws JSONException {
		return new FileInfo(
				jo.getString("id"),
				jo.getLong("date"),
				jo.getString("version"),
				jo.getString("name"));
	}
	
	
	public void saveFileInfo(final FileInfo fileInfo, final List<String> files) throws IOException {
		final FileOutputStream fos = new FileOutputStream(new File(dir, fileInfo.getId()));
		try {
			fos.write(new JSONObject()
				.put("id", fileInfo.getId())
				.put("name", fileInfo.getName())
				.put("version", fileInfo.getVersion())
				.put("date", fileInfo.getDate()).toString().getBytes());
		} catch (final JSONException e) {
			throw new RuntimeException(e);
		} finally {
			fos.close();
		}
		
		if (files != null) {
			final FileOutputStream fos1 = new FileOutputStream(new File(filesDir, fileInfo.getId()));
			try {
				for (final String file : files) {
					fos1.write((file + "\n").getBytes());
				}
			} finally {
				fos1.close();
			}
		}
	}
	
	
	public FileInfo getFileInfo(final String id) throws IOException {
		return getFileInfo(new File(dir, id));
	}

	
	public String[] getFiles(final String id) throws IOException {
		final File filesFile = new File(filesDir, id);
		if (!filesFile.exists()) {
			return new String[0];
		}
		final List<String> list = new ArrayList<String>();
		final BufferedReader reader = new BufferedReader(new FileReader(filesFile));
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				list.add(line);
			}
		} finally {
			reader.close();
		}
		return list.toArray(new String[list.size()]);
	}


	private FileInfo getFileInfo(final File file) throws IOException {
		final FileInputStream fis;
		try {
			fis = new FileInputStream(file);
		} catch (final FileNotFoundException e) {
			return null;
		}
		
		final String json;
		try {
			json = Utils.streamToString(fis);
		} finally {
			fis.close();
		}
		
		try {
			Log.d("JSON", json);
			return createFileInfo(new JSONObject(json));
		} catch (final JSONException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public void deleteFileInfo(final String id) {
		new File(filesDir, id).delete();
		new File(dir, id).delete();
	}
	
	
	public FileInfo[] getFileInfos() throws IOException {
		final ArrayList<FileInfo> fileInfos = new ArrayList<FileInfo>();
		final File[] files = dir.listFiles();
		if (files != null) {
			for (final File file : files) {
				fileInfos.add(getFileInfo(file));
			}
		}
		return fileInfos.toArray(new FileInfo[fileInfos.size()]);
	}
	
}
