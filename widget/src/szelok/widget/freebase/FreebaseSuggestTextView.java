package szelok.widget.freebase;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SimpleAdapter;

/**
 * Customizing AutoCompleteTextView for Freebase topic search
 */
public class FreebaseSuggestTextView extends AutoCompleteTextView {
	// Json Result Tags
	private static final String NAME = "name";
	private static final String NOTABLE = "notable";
	private static final String CATEGORY = "category";
	private static final String MID = "mid";

	// Keys used in Hashmap
	private String[] from = { NAME, CATEGORY };
	// Ids of views in listview_layout
	private int[] to = { R.id.name, R.id.category };

	public FreebaseSuggestTextView(Context context, AttributeSet attrs) {
		super(context, attrs);

		setAdapter(new FreebaseAdapter(context,
				new ArrayList<Map<String, String>>(),
				R.layout.autocomplete_layout, from, to));
		setThreshold(3);
	}

	@Override
	protected CharSequence convertSelectionToString(Object selectedItem) {
		HashMap<String, String> hm = (HashMap<String, String>) selectedItem;
		return hm.get(NAME);
	}

	private class FreebaseAdapter extends SimpleAdapter implements Filterable {
		private static final String FREEBASE_SEARCH_URL = "https://www.googleapis.com/freebase/v1/search";

		// for logging
		private static final String TAG = "FreebaseAdapter";

		private List<Map<String, String>> resultList;

		public FreebaseAdapter(Context context, List<Map<String, String>> data,
				int resource, String[] from, int[] to) {
			super(context, data, resource, from, to);
			resultList = data;
		}

		@Override
		public int getCount() {
			return resultList.size();
		}

		@Override
		public Map<String, String> getItem(int index) {
			return resultList.get(index);
		}

		@Override
		public Filter getFilter() {
			Filter filter = new Filter() {
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					FilterResults filterResults = new FilterResults();
					if (constraint != null) {
						// Retrieve the autocomplete results.
						List<Map<String, String>> results = autocomplete(constraint
								.toString());

						// Assign the data to the FilterResults
						filterResults.values = results;
						filterResults.count = ((results != null) ? results
								.size() : 0);
					}
					return filterResults;
				}

				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					resultList.clear();
					if (results != null && results.count > 0) {
						resultList
								.addAll((List<Map<String, String>>) results.values);
						notifyDataSetChanged();
					} else {
						notifyDataSetInvalidated();
					}
				}
			};
			return filter;
		}

		private List<Map<String, String>> autocomplete(String input) {
			List<Map<String, String>> results = null;
			HttpURLConnection conn = null;
			StringBuilder jsonResults = new StringBuilder();
			try {
				StringBuilder sb = new StringBuilder(FREEBASE_SEARCH_URL);
				sb.append("?query=" + URLEncoder.encode(input, "utf8"));
				sb.append("&limit=" + URLEncoder.encode("10", "utf8"));

				URL url = new URL(sb.toString());
				conn = (HttpURLConnection) url.openConnection();
				InputStreamReader in = new InputStreamReader(
						conn.getInputStream());

				// Load the results into a StringBuilder
				int read;
				char[] buff = new char[1024];
				while ((read = in.read(buff)) != -1) {
					jsonResults.append(buff, 0, read);
				}
			} catch (MalformedURLException e) {
				Log.e(TAG, "Error processing Freebase API", e);
				return results;
			} catch (IOException e) {
				// no op - it could be a valid no results
				// Log.e(TAG, "Error connecting to Freebase API", e);
				return results;
			} finally {
				if (conn != null) {
					conn.disconnect();
				}
			}

			try {
				// Create a JSON object hierarchy from the results
				JSONObject jsonObj = new JSONObject(jsonResults.toString());
				JSONArray jsonArray = jsonObj.getJSONArray("result");

				results = new ArrayList<Map<String, String>>();

				for (int i = 0; i < jsonArray.length(); i++) {
					try {
						JSONObject j = jsonArray.getJSONObject(i);

						if (j.getString(NAME).length() > 0) {
							Map<String, String> map = new HashMap<String, String>(
									3);

							map.put(NAME, j.getString(NAME));

							map.put(MID, j.getString(MID));

							JSONObject notable = j.getJSONObject(NOTABLE);
							String category = notable.has(NAME) ? notable
									.getString(NAME) : "";
							map.put(CATEGORY, category);
							results.add(map);
						}

					} catch (JSONException e) {
						// no-op skip this row
					}
				}
			} catch (JSONException e) {
				Log.e(TAG, "Cannot process JSON results", e);
			}

			return results;
		}

	}
}
