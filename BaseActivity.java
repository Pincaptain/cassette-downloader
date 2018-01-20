/**
 *  {@author Pincaptain}
 *  A simple youtube search and download
 *  application specially created for a
 *  really close friend of mine.
 */

package com.cassette.akatosh.cassette;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BaseActivity extends AppCompatActivity {

    /** Application name. */
    private static final String APPLICATION_NAME = "Cassette";

    protected WebView downloadView;

    protected ListView downloadList;
    protected ArrayList<MediaView> downloadListItems;
    protected MediaArrayAdapter downloadListAdapter;

    protected SearchView downloadSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        init();
        handle();
    }

    /**
     *  {@code init()} links all the views used
     *  in the layout xml {@link R.layout}
     *  with appropriate variables inside the {@link BaseActivity}.
     */
    @SuppressLint("SetJavaScriptEnabled")
    protected void init() {
        downloadView = findViewById(R.id.downloadView);
        downloadView.getSettings().setJavaScriptEnabled(true);
        downloadView.getSettings().setLoadsImagesAutomatically(true);
        downloadView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        downloadView.setBackgroundColor(getResources().getColor(R.color.colorWebView));

        downloadList = findViewById(R.id.downloadList);
        downloadListItems = new ArrayList<>();
        downloadListAdapter = new MediaArrayAdapter(this, 0, downloadListItems);
        downloadList.setAdapter(downloadListAdapter);

        downloadSearch = findViewById(R.id.downloadSearch);
    }

    /**
     * {@code handle()} attaches functions to handle
     * the events triggered by the {@link BaseActivity}
     * variables.
     */
    protected void handle() {
        downloadList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String mediaID = ((MediaView) adapterView.getItemAtPosition(i)).getMediaID();
                downloadView.setVisibility(View.VISIBLE);
                    downloadView.loadData("<iframe src=\"https://www.yt-download.org/@api/button/mp3/"+ mediaID +"\" width=\"100%\" height=\"100px\" scrolling=\"no\" style=\"border:none;\"></iframe>",
                        "text/html; charset=utf-8", "UTF-8");
            }
        });

        downloadSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            /**
             * {@code OnQueryTextSubmit(s)} triggers the
             * {@link FetchMedia} AsyncTask class that obtains and renders
             * the data from the youtube api. Additional
             * code can be executed as long as
             * it doesn't interrupt the main task.
             */
            @Override
            public boolean onQueryTextSubmit(String s) {
                boolean result = false;
                ArrayList<SearchResult> searchResults;
                ArrayList<MediaView> structuredResults;

                try {
                    searchResults = (ArrayList<SearchResult>) new FetchMedia().execute(downloadSearch.getQuery().toString()).get();
                    structuredResults = new ArrayList<>();

                    for (SearchResult res : searchResults) {
                        structuredResults.add(new MediaView(res.getSnippet().getTitle(),res.getId().getVideoId(), res.getSnippet().getThumbnails().getDefault().getUrl()));
                    }

                    downloadListAdapter.clear();
                    downloadListAdapter.addAll(structuredResults);
                    downloadListAdapter.notifyDataSetChanged();

                    result = true;
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }

                return result;
            }

            /**
             * {@code OnQueryTextChange(s)} is ignored!
             * TODO - Create video suggestions and preferences using this event.
             */
            @Override
            public boolean onQueryTextChange(String s) {
                return false;
            }
        });
    }

}

/**
 * {@link MediaView} class encapsulates the information of
 *  a youtube video. Adding a new attribute requires you
 *  to write its get/set methods and initialize it in the
 *  constructor. Also changes must be done in {@link FetchMedia}
 *  class to obtain the required data from the api.
 */
class MediaView {

    private String mediaName;
    private String mediaID;
    private String mediaThumbnail;

    public MediaView(String mediaName, String mediaID, String mediaThumbnail) {
        this.mediaName = mediaName;
        this.mediaID = mediaID;
        this.mediaThumbnail = mediaThumbnail;
    }

    public String getMediaName() {
        return mediaName;
    }

    public void setMediaName(String mediaName) {
        this.mediaName = mediaName;
    }


    public String getMediaID() {
        return mediaID;
    }

    public void setMediaID(String mediaID) {
        this.mediaID = mediaID;
    }

    public String getMediaThumbnail() {
        return mediaThumbnail;
    }

    public void setMediaThumbnail(String mediaThumbnail) {
        this.mediaThumbnail = mediaThumbnail;
    }

    @Override
    public String toString() {
        return mediaName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MediaView mediaView = (MediaView) o;

        return mediaName.equals(mediaView.mediaName) && mediaID.equals(mediaView.mediaID);
    }

    @Override
    public int hashCode() {
        int result = mediaName.hashCode();
        result = 31 * result + mediaID.hashCode();
        return result;
    }

}

/**
 * {@link MediaArrayAdapter} is a custom Array adapter for
 * displaying the list view.
 */
class MediaArrayAdapter extends ArrayAdapter<MediaView> {

    private Context context;
    private List<MediaView> views;

    public MediaArrayAdapter(@NonNull Context context, int resource, List<MediaView> views) {
        super(context, resource);

        this.context = context;
        this.views = views;
    }

    @Override
    public int getCount() {
        return this.views.size();
    }

    /**
     * Using the information provided by the list element in position {@param position}
     * this function build a view and returns it.
     * @param position
     * @param convertView
     * @param parent
     * @return List item view
     */
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        MediaView mediaView = views.get(position);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(BaseActivity.LAYOUT_INFLATER_SERVICE);
        @SuppressLint({"InflateParams", "ViewHolder"})
        View view = inflater.inflate(R.layout.media_item, parent, false);

        ImageView mediaImage = view.findViewById(R.id.mediaThumbnail);
        TextView mediaText = view.findViewById(R.id.mediaTitle);
        mediaText.setText(mediaView.getMediaName());
        Picasso.with(context).load(mediaView.getMediaThumbnail()).into(mediaImage);

        return view;
    }

    @Override
    public void add(@Nullable MediaView object) {
        super.add(object);

        this.views.add(object);
    }

    @Override
    public void addAll(@NonNull Collection<? extends MediaView> collection) {
        super.addAll(collection);

        this.views.addAll(collection);
    }

    @Override
    public void remove(@Nullable MediaView object) {
        super.remove(object);

        this.views.remove(object);
    }

    @Override
    public void clear() {
        super.clear();

        this.views.clear();
    }

}

/**
 * {@link FetchMedia} class is in charge of fetching the media from the
 * api. Based on a certain search query and some additional parameters
 * like api key, max results and data type.
 */
class FetchMedia extends AsyncTask<String, Void, List<SearchResult>> {

    /**
     * doInBackground is an asynchronous function that
     * obtains the data directly from the youtube api.
     * Changes can be applied on the parameters below
     * if and only if necessary.
     * @param strings
     * @return ArrayList of SearchResults/ Empty ArrayList in case of error
     */
    @Override
    protected List<SearchResult> doInBackground(String... strings) {
        YouTube youtube = new YouTube.Builder(new ApacheHttpTransport(), new AndroidJsonFactory(), new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {

            }
        }).setApplicationName("Cassette").build();

        String query = strings[0];

        try {
            YouTube.Search.List search = youtube.search().list("id,snippet");
            String API_KEY = "YOUR-API-KEY";

            search.setKey(API_KEY);
            search.setQ(query);
            search.setType("video");
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
            search.setMaxResults(25L);

            SearchListResponse searchResponse = search.execute();

            return searchResponse.getItems();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ArrayList<>();
    }
    
}
