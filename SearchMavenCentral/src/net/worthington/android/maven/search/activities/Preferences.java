package net.worthington.android.maven.search.activities;

import net.worthington.android.maven.search.R;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Preferences extends PreferenceActivity
{
  @Override
  protected void onCreate(Bundle pSavedInstanceState)
  {
    super.onCreate(pSavedInstanceState);
    addPreferencesFromResource(R.xml.preferences);
  }
}