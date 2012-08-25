package net.worthington.android.maven.search.activities;

import java.util.Arrays;
import java.util.List;

import net.worthington.android.maven.search.R;
import net.worthington.android.maven.search.constants.Constants;
import net.worthington.android.maven.search.constants.OptionsMenuDialogActions;
import net.worthington.android.maven.search.restletapi.dao.MCRDoc;
import net.worthington.android.maven.search.restletapi.dao.MCRResponse;

import org.joda.time.DateTime;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class SearchResults extends Activity
{
  private int          iSearchType;
  private String       iSelectedGroup;
  private String       iSelectedArtifact;
  private String       iSelectedVersion;
  private Integer      iSelectedVersionCount;
  private MyAdapter    iAdapter;
  private List<MCRDoc> iSearchResults;
  private ListView     iLv;

  @Override
  public void onCreate(Bundle pSavedInstanceState)
  {
    super.onCreate(pSavedInstanceState);
    setContentView(R.layout.search_results);

    MCRResponse searchResults = (MCRResponse) getIntent().getExtras().getSerializable(Constants.SEARCH_RESULTS);
    iSearchType = (Integer) getIntent().getExtras().getSerializable(Constants.SEARCH_TYPE);

    if (searchResults != null)
    {
      TextView tv = (TextView) findViewById(R.id.SearchResultsTextView);
      tv.setText(searchResults.getNumFound() + " " + getSearchTypeString(iSearchType) + " Search Results:");

      iLv = (ListView) findViewById(R.id.list);

      // Creating a button - Load More
      Button btnLoadMore = new Button(this);
      btnLoadMore.setText("Load More");

      // Adding button to listview at footer
      iLv.addFooterView(btnLoadMore);

      iSearchResults = searchResults.getDocs();
      iAdapter = new MyAdapter(this, iSearchResults);
      iLv.setAdapter(iAdapter);
      
      registerForContextMenu(iLv);
      
      iLv.setOnItemClickListener(new OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> pArg0, View pV, int pArg2, long pArg3)
        {
          Log.d(Constants.LOG_TAG, "Search Result was clicked");
          setSelectedGroup(((TextView) pV.findViewById(R.id.groupIdTextView)).getText().toString());
          setSelectedArtifact(((TextView) pV.findViewById(R.id.artifactIdTextView)).getText().toString());
          setSelectedVersion(((TextView) pV.findViewById(R.id.latestVersionTextView)).getText().toString());
          setSelectedVersionCount(Integer.valueOf(((TextView) pV.findViewById(R.id.versionCountTextView)).getText()
                                                                                                         .toString()));
          // Create a progress dialog so we can see it's searching
          showDialog(Constants.PROGRESS_DIALOG_ARTIFACT_DETAILS);
        }
      });


      btnLoadMore.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View arg0)
        {
          // Starting a new async task
          Log.d(Constants.LOG_TAG, "Loading more results");
          new LoadMoreListViewThread().execute();
        }
      });
    }
    else
    {
      TextView tv = (TextView) findViewById(R.id.SearchResultsTextView);
      tv.setText("Search Results were null - check log");
    }
  }

  private String getSearchTypeString(int pSearchType)
  {
    String returnValue = "";
    switch (pSearchType)
    {
      case Constants.PROGRESS_DIALOG_QUICK_SEARCH:
        returnValue = "Quick";
        break;
      case Constants.PROGRESS_DIALOG_ADVANCED_SEARCH:
        returnValue = "Advanced";
        break;
      case Constants.PROGRESS_DIALOG_GROUPID_SEARCH:
        returnValue = "GroupId";
        break;
      case Constants.PROGRESS_DIALOG_ARTIFACTID_SEARCH:
        returnValue = "ArtifactId";
        break;
      case Constants.PROGRESS_DIALOG_VERSION_SEARCH:
        returnValue = "All Versions";
        break;
    }
    return returnValue;
  }

  private class MyAdapter extends BaseAdapter
  {

    private Activity     iActivity;
    private List<MCRDoc> iData;

    public MyAdapter(Activity pActivity, List<MCRDoc> pData)
    {
      iActivity = pActivity;
      iData = pData;
    }

    @Override
    public int getCount()
    {
      return iData.size();
    }

    @Override
    public Object getItem(int pPosition)
    {
      return pPosition;
    }

    @Override
    public long getItemId(int pPosition)
    {
      return pPosition;
    }

    @Override
    public View getView(int pPosition, View pConvertView, ViewGroup pParent)
    {
      View row = pConvertView;
      if (pConvertView == null)
      {
        LayoutInflater inflater = (LayoutInflater) iActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        row = inflater.inflate(R.layout.search_results_item, null);
      }

      TextView groupTV = (TextView) row.findViewById(R.id.groupIdTextView);
      TextView artifactTV = (TextView) row.findViewById(R.id.artifactIdTextView);
      TextView latestVersionTV = (TextView) row.findViewById(R.id.latestVersionTextView);
      TextView lastUpdateTV = (TextView) row.findViewById(R.id.lastUpdateTextView);
      TextView versionCount = (TextView) row.findViewById(R.id.versionCountTextView);

      MCRDoc mavenCentralArtifactResult = iData.get(pPosition);

      groupTV.setText(mavenCentralArtifactResult.getG());
      artifactTV.setText(mavenCentralArtifactResult.getA());

      String version = mavenCentralArtifactResult.getLatestVersion();
      if (version == null || version.trim().length() == 0)
      {
        version = mavenCentralArtifactResult.getV();
      }

      latestVersionTV.setText(version);
      lastUpdateTV.setText(mavenCentralArtifactResult.getTimestamp().toString("dd-MMM-yyyy"));
      versionCount.setText(Integer.toString(mavenCentralArtifactResult.getVersionCount()));

      return row;
    }
  }

  @Override
  protected Dialog onCreateDialog(int pId)
  {
    return OptionsMenuDialogActions.createProcessDialogHelper(pId, this);
  }

  @Override
  protected void onPrepareDialog(int pId, Dialog pDialog)
  {
    OptionsMenuDialogActions.prepareProgressDialogHelper(pId, this);
  }

  @Override
  public void onCreateContextMenu(ContextMenu pMenu, View pV, ContextMenuInfo pMenuInfo)
  {
    AdapterView.AdapterContextMenuInfo menuItem = (AdapterView.AdapterContextMenuInfo) pMenuInfo;
    View menuView = menuItem.targetView;
    setSelectedGroup(((TextView) menuView.findViewById(R.id.groupIdTextView)).getText().toString());
    setSelectedArtifact(((TextView) menuView.findViewById(R.id.artifactIdTextView)).getText().toString());
    setSelectedVersion(((TextView) menuView.findViewById(R.id.latestVersionTextView)).getText().toString());
    setSelectedVersionCount(Integer.valueOf(((TextView) menuView.findViewById(R.id.versionCountTextView)).getText()
                                                                                                   .toString()));

    super.onCreateContextMenu(pMenu, pV, pMenuInfo);
    pMenu.setHeaderTitle("Search By:");

    if (iSearchType != Constants.PROGRESS_DIALOG_GROUPID_SEARCH)
    {
      pMenu.add(Menu.NONE, R.id.contextMenuSearchGroupId, 1, "Group Id");
    }
    if (iSearchType != Constants.PROGRESS_DIALOG_ARTIFACTID_SEARCH)
    {
      pMenu.add(Menu.NONE, R.id.contextMenuSearchArtifactId, 2, "Artifact Id");
    }
    if (iSearchType != Constants.PROGRESS_DIALOG_VERSION_SEARCH)
    {
      pMenu.add(Menu.NONE, R.id.contextMenuSearchAllVersions, 3, "All " + getSelectedVersionCount() + " Versions");
    }
  }

  @Override
  public boolean onContextItemSelected(MenuItem pItem)
  {
    if (pItem.getItemId() == R.id.contextMenuSearchGroupId)
    {
      Log.d(Constants.LOG_TAG, "Search Group ID was clicked: " + getSelectedGroup());
      showDialog(Constants.PROGRESS_DIALOG_GROUPID_SEARCH);
    }
    else if (pItem.getItemId() == R.id.contextMenuSearchArtifactId)
    {
      Log.d(Constants.LOG_TAG, "Search Artifact Id was clicked: " + getSelectedArtifact());
      showDialog(Constants.PROGRESS_DIALOG_ARTIFACTID_SEARCH);
    }
    else if (pItem.getItemId() == R.id.contextMenuSearchAllVersions)
    {
      Log.d(Constants.LOG_TAG, "Search All Versions was clicked");
      showDialog(Constants.PROGRESS_DIALOG_VERSION_SEARCH);
    }

    return super.onContextItemSelected(pItem);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu pMenu)
  {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, pMenu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem pItem)
  {
    OptionsMenuDialogActions.myOptionsMenuItemSelected(this, pItem);
    return super.onOptionsItemSelected(pItem);
  }

  public Integer getSelectedVersionCount()
  {
    return iSelectedVersionCount;
  }

  private void setSelectedVersionCount(Integer selectedVersionCount)
  {
    iSelectedVersionCount = selectedVersionCount;
  }

  public String getSelectedGroup()
  {
    return iSelectedGroup;
  }

  private void setSelectedGroup(String selectedGroup)
  {
    iSelectedGroup = selectedGroup;
  }

  public String getSelectedArtifact()
  {
    return iSelectedArtifact;
  }

  private void setSelectedArtifact(String selectedArtifact)
  {
    iSelectedArtifact = selectedArtifact;
  }

  public String getSelectedVersion()
  {
    return iSelectedVersion;
  }

  private void setSelectedVersion(String selectedVersion)
  {
    iSelectedVersion = selectedVersion;
  }

  private class LoadMoreListViewThread extends AsyncTask<Void, Void, Void>
  {
    private ProgressDialog pDialog;

    @Override
    protected void onPreExecute()
    {
      // Showing progress dialog before sending http request
      pDialog = new ProgressDialog(SearchResults.this);
      pDialog.setMessage("Please wait..");
      pDialog.setIndeterminate(true);
      pDialog.setCancelable(false);
      pDialog.show();
    }

    protected Void doInBackground(Void... unused)
    {
      runOnUiThread(new Runnable() {

        public void run()
        {
          MCRDoc doc = new MCRDoc();
          doc.setG("com.searchmavenapp");
          doc.setA("utils");
          doc.setLatestVersion("1.0");
          doc.setRepositoryId("central");
          doc.setP("bundle");
          doc.setTimestamp(new DateTime(1338025419000L));
          doc.setVersionCount(2);
          doc.setText(Arrays.asList("log4j", "log4j", "-sources.jar", "-javadoc.jar", ".jar", ".zip", ".tar.gz", "pom"));
          doc.setEc(Arrays.asList("-sources.jar", "-javadoc.jar", ".jar", ".zip", ".tar.gz", "pom"));
          iSearchResults.add(doc);

          // get listview current position - used to maintain scroll position
          int currentPosition = iLv.getFirstVisiblePosition();

          // Appending new data to menuItems ArrayList
          iAdapter = new MyAdapter(SearchResults.this, iSearchResults);

          // Setting new scroll position
          iLv.setSelectionFromTop(currentPosition + 1, 0);
        }
      });
      return (null);
    }

    protected void onPostExecute(Void unused)
    {
      // closing progress dialog
      pDialog.dismiss();
    }
  }
}