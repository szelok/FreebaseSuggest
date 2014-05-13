package szelok.example.freebase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import szelok.widget.freebase.FreebaseSuggestTextView;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class FreebaseSuggestExampleActivity extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		FreebaseSuggestTextView autoComplete = (FreebaseSuggestTextView) findViewById(R.id.autocomplete);

		/** Defining an itemclick event listener for the autocompletetextview */
		OnItemClickListener itemClickListener = new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1,
					int position, long id) {
				/**
				 * Each item in the adapter is a HashMap object. So this
				 * statement creates the currently clicked hashmap object
				 * */
				HashMap<String, String> hm = (HashMap<String, String>) arg0
						.getAdapter().getItem(position);

				(new FreebaseGetTask()).execute(hm.get("mid"));
			}
		};

		/** Setting the itemclick event listener */
		autoComplete.setOnItemClickListener(itemClickListener);
	}

	/**
	 * A callback method, which is executed when this activity is about to be
	 * killed This is used to save the current state of the activity ( eg :
	 * Configuration changes : portrait -> landscape )
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}

	/**
	 * A callback method, which is executed when the activity is recreated ( eg
	 * : Configuration changes : portrait -> landscape )
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	private class FreebaseGetTask extends
			AsyncTask<String, Void, Map<String, String>> {
		private static final String TAG = "FreebaseGetTask";

		private static final String URL = "https://www.googleapis.com/freebase/v1/topic";

		private AndroidHttpClient mClient = AndroidHttpClient.newInstance("");

		public FreebaseGetTask() {
			super();
		}

		@Override
		protected Map<String, String> doInBackground(String... params) {
			HttpGet request = new HttpGet(URL + params[0]);
			JSONResponseHandler responseHandler = new JSONResponseHandler();
			try {
				Map<String, String> result = mClient.execute(request,
						responseHandler);
				mClient.close();
				return result;
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Map<String, String> result) {
			TextView name = (TextView) findViewById(R.id.name);
			name.setText(result.get("name"));

			TextView description = (TextView) findViewById(R.id.description);
			if (result.containsKey("description")) {
				description.setText(result.get("description"));
			} else {
				description.setText("");
			}

			TextView website = (TextView) findViewById(R.id.website);
			if (result.containsKey("official_website_url")) {
				website.setText(Html.fromHtml(result
						.get("official_website_url")));
				website.setMovementMethod(LinkMovementMethod.getInstance());
			} else {
				website.setText("");
			}

			WebView image = (WebView) findViewById(R.id.image);
			if (result.containsKey("image")) {
				Map<String, String> headers = new HashMap<String, String>();
				image.loadData(result.get("image"), "text/html", "utf-8");
			} else {
				image.loadData("<!DOCTYPE html><html><body></body></html>", "text/html", "utf-8");
			}
		}
	}

	private class JSONResponseHandler implements
			ResponseHandler<Map<String, String>> {
		private static final String TAG = "JSONResponseHandler";

		@Override
		public Map<String, String> handleResponse(HttpResponse response)
				throws ClientProtocolException, IOException {
			Map<String, String> result = new HashMap<String, String>();
			String JSONResponse = new BasicResponseHandler()
					.handleResponse(response);

			try {
				// Get top-level JSON Object - a Map
				JSONObject responseObject = (JSONObject) new JSONTokener(
						JSONResponse).nextValue();

				JSONObject properties = responseObject
						.getJSONObject("property");

				if (properties.has("/type/object/name")) {
					JSONObject property = properties
							.getJSONObject("/type/object/name");
					if (property.has("values")) {
						JSONArray values = property.getJSONArray("values");
						if (values.length() > 0) {
							JSONObject value = ((JSONObject) values.get(0));
							if (value.has("text")) {
								result.put("name", value.getString("text"));
							}
						}
					}
				}

				if (properties.has("/common/topic/description")) {
					JSONObject property = properties
							.getJSONObject("/common/topic/description");
					if (property.has("values")) {
						JSONArray values = property.getJSONArray("values");
						if (values.length() > 0) {
							JSONObject value = ((JSONObject) values.get(0));
							if (value.has("text")) {
								result.put("description",
										value.getString("text"));
							}
						}
					}
				}

				if (properties.has("/common/topic/notable_for")) {
					JSONObject property = properties
							.getJSONObject("/common/topic/notable_for");
					if (property.has("values")) {
						JSONArray values = property.getJSONArray("values");
						if (values.length() > 0) {
							JSONObject value = ((JSONObject) values.get(0));
							if (value.has("text")) {
								result.put("notable", value.getString("text"));
							}
						}
					}
				}

				if (properties.has("/common/topic/image")) {
					JSONObject property = properties
							.getJSONObject("/common/topic/image");
					if (property.has("values")) {
						JSONArray values = property.getJSONArray("values");
						if (values.length() > 0) {
							JSONObject value = ((JSONObject) values.get(0));
							if (value.has("id")) {
								result.put(
										"image",
										"<!DOCTYPE html><html><head style=\"margin: 0px;\"><meta name=\"viewport\" content=\"width=100\" /></head><body><div style=\"width: 90px; height: 60px;\"><img src=\"https://www.googleapis.com/freebase/v1/image"
												+ value.getString("id")
												+ "\" width=\"64\"></div></body></html>");

							}
						}
					}
				}

				if (properties.has("/common/topic/official_website")) {
					JSONObject property = properties
							.getJSONObject("/common/topic/official_website");
					if (property.has("values")) {
						JSONArray values = property.getJSONArray("values");
						if (values.length() > 0) {
							JSONObject value = ((JSONObject) values.get(0));
							if (value.has("value")) {
								result.put("official_website_url", "<a href=\""
										+ value.getString("value")
										+ "\">Official Website</a>");
							}
						}
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			return result;
		}
	}
}
