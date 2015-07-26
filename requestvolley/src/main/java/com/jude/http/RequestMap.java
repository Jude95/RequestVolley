package com.jude.http;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;


public class RequestMap {
	private static String ENCODING = "UTF-8";

    protected ArrayList<Map.Entry<String, String>> urlParams;

    protected ArrayList<Map.Entry<String, FileWrapper>> fileParams;

	private final static char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
			.toCharArray();

	public RequestMap() {
		init();
	}

	public RequestMap(String key, String value) {
		init();
		put(key, value);
	}

    public RequestMap(String key, File value) {
        init();
        put(key, value);
    }

	private void init() {
		urlParams = new ArrayList();
		fileParams = new ArrayList();
	}


	public void put(String key, String value) {
		if (key != null && value != null) {
			urlParams.add(new AbstractMap.SimpleEntry<>(key, value));
		}
	}


	public void put(String key, File file) {
		try {
			put(key, new FileInputStream(file), file.getName());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}


	public void put(String key, InputStream stream, String fileName) {
		put(key, stream, fileName, null);
	}


	public void put(String key, InputStream stream, String fileName, String contentType) {
		if (key != null && stream != null) {
			fileParams.add(new AbstractMap.SimpleEntry<String, FileWrapper>(key, new FileWrapper(stream, fileName, contentType)));
		}
	}
	
	@Override
	public String toString() {
		String params = "?";
		for (Map.Entry<String,String> entry : urlParams) {
			if(!params.equals("?")){
				params+="&";
			}
			params+=entry.getKey()+"="+entry.getValue();
		}
        for (Map.Entry<String,FileWrapper> entry : fileParams) {
            if(!params.equals("?")){
                params+="&";
            }
            params+=entry.getKey()+"=File:{"+entry.getValue().fileName+"}";
        }
		return params;
	}

	public HttpEntity getEntity() {
		HttpEntity entity = null;
		if (!fileParams.isEmpty()) {
			MultipartEntity multipartEntity = new MultipartEntity();
			for (Map.Entry<String, String> entry : urlParams) {// Add  string  params
				multipartEntity.addPart(entry.getKey(), entry.getValue());
			}
			int currentIndex = 0;
			int lastIndex = fileParams.size() - 1;
			for (Map.Entry<String, FileWrapper> entry : fileParams) {//Add  file  params
				FileWrapper file = entry.getValue();
				if (file.inputStream != null) {
					boolean isLast = currentIndex == lastIndex;
					if (file.contentType != null) {
						multipartEntity.addPart(entry.getKey(), file.getFileName(), file.inputStream, file.contentType,
								isLast);
					} else {
						multipartEntity.addPart(entry.getKey(), file.getFileName(), file.inputStream, isLast);
					}
				}
				currentIndex++;
			}
			entity = multipartEntity;
		} else {
			try {
				entity = new UrlEncodedFormEntity(getParamsList(), ENCODING);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return entity;
	}

	protected List<BasicNameValuePair> getParamsList() {
		List<BasicNameValuePair> lparams = new ArrayList<BasicNameValuePair>();
		for (Map.Entry<String, String> entry : urlParams) {
			lparams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
		}
		return lparams;
	}

	private static class FileWrapper {
		public InputStream inputStream;
		public String fileName;
		public String contentType;

		public FileWrapper(InputStream inputStream, String fileName, String contentType) {
			this.inputStream = inputStream;
			this.fileName = fileName;
			this.contentType = contentType;
		}

		public String getFileName() {
			if (fileName != null) {
				return fileName;
			} else {
				return "nofilename";
			}
		}
	}

	class MultipartEntity implements HttpEntity {
		private String boundary = null;

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		boolean isSetLast = false;

		boolean isSetFirst = false;

		public MultipartEntity() {
			final StringBuffer buf = new StringBuffer();
			final Random rand = new Random();
			for (int i = 0; i < 30; i++) {
				buf.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
			}
			this.boundary = buf.toString();
		}

		public void writeFirstBoundaryIfNeeds() {
			if (!isSetFirst) {
				try {
					out.write(("--" + boundary + "\r\n").getBytes());
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			isSetFirst = true;
		}

		public void writeLastBoundaryIfNeeds() {
			if (isSetLast) {
				return;
			}
			try {
				out.write(("\r\n--" + boundary + "--\r\n").getBytes());
			} catch (final IOException e) {
				e.printStackTrace();
			}
			isSetLast = true;
		}

		public void addPart(final String key, final String value) {
			writeFirstBoundaryIfNeeds();
			try {
				out.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n").getBytes());
				out.write(value.getBytes());
				out.write(("\r\n--" + boundary + "\r\n").getBytes());
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		public void addPart(final String key, final String fileName, final InputStream fin, final boolean isLast) {
			addPart(key, fileName, fin, "application/octet-stream", isLast);
		}

		public void addPart(final String key, final String fileName, final InputStream fin, String type, final boolean isLast) {
			writeFirstBoundaryIfNeeds();
			try {
				type = "Content-Type: " + type + "\r\n";
				out.write(("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + fileName + "\"\r\n")
						.getBytes());
				out.write(type.getBytes());
				out.write("Content-Transfer-Encoding: binary\r\n\r\n".getBytes());

				final byte[] tmp = new byte[4096];
				int l = 0;
				while ((l = fin.read(tmp)) != -1) {
					out.write(tmp, 0, l);
				}
				if (!isLast)
					out.write(("\r\n--" + boundary + "\r\n").getBytes());
				else {
					writeLastBoundaryIfNeeds();
				}
				out.flush();
			} catch (final IOException e) {
				e.printStackTrace();
			} finally {
				try {
					fin.close();
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}

		public void addPart(final String key, final File value, final boolean isLast) {
			try {
				addPart(key, value.getName(), new FileInputStream(value), isLast);
			} catch (final FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		@Override
		public long getContentLength() {
			writeLastBoundaryIfNeeds();
			return out.toByteArray().length;
		}

		@Override
		public Header getContentType() {
			return new BasicHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
		}

		@Override
		public boolean isChunked() {
			return false;
		}

		@Override
		public boolean isRepeatable() {
			return false;
		}

		@Override
		public boolean isStreaming() {
			return false;
		}

		@Override
		public void writeTo(final OutputStream outstream) throws IOException {
			outstream.write(out.toByteArray());
		}

		@Override
		public Header getContentEncoding() {
			return null;
		}

		@Override
		public void consumeContent() throws IOException, UnsupportedOperationException {
			if (isStreaming()) {
				throw new UnsupportedOperationException("Streaming entity does not implement #consumeContent()");
			}
		}

		@Override
		public InputStream getContent() throws IOException, UnsupportedOperationException {
			return new ByteArrayInputStream(out.toByteArray());
		}
	}

}