/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gsoc.vedantsingh.locatedvoicecms.PW.collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Collection of Physical Web URL devices and related metadata.
 */
public class PhysicalWebCollection {
  private static final int SCHEMA_VERSION = 1;
  private static final String SCHEMA_VERSION_KEY = "schema";
  private static final String DEVICES_KEY = "devices";
  private static final String METADATA_KEY = "metadata";
  private PwsClient mPwsClient;
  private Map<String, UrlDevice> mDeviceIdToUrlDeviceMap;
  private Map<String, PwsResult> mBroadcastUrlToPwsResultMap;
  private Map<String, byte[]> mIconUrlToIconMap;
  private Set<String> mPendingBroadcastUrls;
  private Set<String> mPendingIconUrls;

  /**
   * Construct a PhysicalWebCollection.
   */
  public PhysicalWebCollection() {
    mPwsClient = new PwsClient();
    mDeviceIdToUrlDeviceMap = new HashMap<>();
    mBroadcastUrlToPwsResultMap = new HashMap<>();
    mIconUrlToIconMap = new HashMap<>();
    mPendingBroadcastUrls = new HashSet<>();
    mPendingIconUrls = new HashSet<>();
  }

  /**
   * Populate this data structure with UrlDevices represented by a given JSON object.
   * @param jsonObject a serialized PhysicalWebCollection.
   * @return The PhysicalWebCollection represented by the serialized object.
   * @throws PhysicalWebCollectionException on invalid or unrecognized input
   */
  public static PhysicalWebCollection jsonDeserialize(JSONObject jsonObject)
          throws PhysicalWebCollectionException, JSONException {
    // Check the schema version
    int schemaVersion = jsonObject.getInt(SCHEMA_VERSION_KEY);
    if (schemaVersion > SCHEMA_VERSION) {
      throw new PhysicalWebCollectionException(
              "Cannot handle schema version " + schemaVersion + ".  "
                      + "This library only knows of schema version " + SCHEMA_VERSION);
    }
    PhysicalWebCollection collection = new PhysicalWebCollection();

    // Deserialize the UrlDevices
    JSONArray urlDevices = jsonObject.getJSONArray(DEVICES_KEY);
    for (int i = 0; i < urlDevices.length(); i++) {
      JSONObject urlDeviceJson = urlDevices.getJSONObject(i);
      UrlDevice urlDevice = UrlDevice.jsonDeserialize(urlDeviceJson);
      collection.addUrlDevice(urlDevice);
    }

    // Deserialize the URL metadata
    JSONArray metadata = jsonObject.getJSONArray(METADATA_KEY);
    for (int i = 0; i < metadata.length(); i++) {
      JSONObject pwsResultJson = metadata.getJSONObject(i);
      PwsResult pwsResult = PwsResult.jsonDeserialize(pwsResultJson);
      collection.addMetadata(pwsResult);
    }

    return collection;
  }

  /**
   * If a site URL appears multiple times in the pairs list, keep only the first example.
   *
   * @param allPwPairs input PwPairs list.
   * @return filtered PwPairs list with all duplicated site URLs removed.
   */
  private static List<PwPair> removeDuplicateSiteUrls(List<PwPair> allPwPairs) {
    List<PwPair> filteredPwPairs = new ArrayList<>();
    Set<String> siteUrls = new HashSet<>();
    for (PwPair pwPair : allPwPairs) {
      String siteUrl = pwPair.getPwsResult().getSiteUrl();
      if (!siteUrls.contains(siteUrl)) {
        siteUrls.add(siteUrl);
        filteredPwPairs.add(pwPair);
      }
    }
    return filteredPwPairs;
  }

  /**
   * Given a list of PwPairs, return a filtered list such that only one PwPair from each group
   * is included.
   *
   * @param allPairs    Input PwPairs list.
   * @param outGroupMap Optional output map from discovered group IDs to UrlGroups, may be null.
   * @return Filtered PwPairs list.
   */
  private static List<PwPair> removeDuplicateGroupIds(List<PwPair> allPairs,
                                                      Map<String, UrlGroup> outGroupMap) {
    List<PwPair> filteredPairs = new ArrayList<>();
    Map<String, UrlGroup> groupMap = outGroupMap;
    if (groupMap == null) {
      groupMap = new HashMap<>();
    } else {
      groupMap.clear();
    }

    for (PwPair pwPair : allPairs) {
      PwsResult pwsResult = pwPair.getPwsResult();
      String groupId = pwsResult.getGroupId();
      if (groupId == null || groupId.equals("")) {
        // Pairs without a group are always included
        filteredPairs.add(pwPair);
      } else {
        // Create the group if it doesn't exist
        UrlGroup urlGroup = groupMap.get(groupId);
        if (urlGroup == null) {
          urlGroup = new UrlGroup(groupId);
          groupMap.put(groupId, urlGroup);
        }
        urlGroup.addPair(pwPair);
      }
    }

    for (UrlGroup urlGroup : groupMap.values()) {
      filteredPairs.add(urlGroup.getTopPair());
    }

    return filteredPairs;
  }

  /**
   * Add a UrlDevice to the collection.
   * @param urlDevice The UrlDevice to add.
   */
  public void addUrlDevice(UrlDevice urlDevice) {
    mDeviceIdToUrlDeviceMap.put(urlDevice.getId(), urlDevice);
  }

  /**
   * Add URL metadata to the collection.
   * @param pwsResult The PwsResult to add.
   */
  public void addMetadata(PwsResult pwsResult) {
    mBroadcastUrlToPwsResultMap.put(pwsResult.getRequestUrl(), pwsResult);
  }

  /**
   * Add an Icon to the collection.
   * @param url The url of the icon.
   * @param icon The bitmap of the icon.
   */
  public void addIcon(String url, byte[] icon) {
    mIconUrlToIconMap.put(url, icon);
  }

  /**
   * Get an Icon from the collection.
   * @param url The url of the icon.
   * @return The associated icon.  This will be null if there is no icon.
   */
  public byte[] getIcon(String url) {
    return mIconUrlToIconMap.get(url);
  }

  /**
   * Fetches a UrlDevice by its ID.
   * @param id The ID of the UrlDevice.
   * @return the UrlDevice with the given ID.
   */
  public UrlDevice getUrlDeviceById(String id) {
    return mDeviceIdToUrlDeviceMap.get(id);
  }

  /**
   * Fetches cached URL metadata using the URL broadcasted by the Physical Web device.
   * @param broadcastUrl The URL broadcasted by the device.
   * @return Cached metadata relevant to the given URL.
   */
  public PwsResult getMetadataByBroadcastUrl(String broadcastUrl) {
    return mBroadcastUrlToPwsResultMap.get(broadcastUrl);
  }

  /**
   * Create a JSON object that represents this data structure.
   * @return a JSON serialization of this data structure.
   */
  public JSONObject jsonSerialize() throws JSONException {
    JSONObject jsonObject = new JSONObject();

    // Serialize the UrlDevices
    JSONArray urlDevices = new JSONArray();
    for (UrlDevice urlDevice : mDeviceIdToUrlDeviceMap.values()) {
      urlDevices.put(urlDevice.jsonSerialize());
    }
    jsonObject.put(DEVICES_KEY, urlDevices);

    // Serialize the URL metadata
    JSONArray metadata = new JSONArray();
    for (PwsResult pwsResult : mBroadcastUrlToPwsResultMap.values()) {
      metadata.put(pwsResult.jsonSerialize());
    }
    jsonObject.put(METADATA_KEY, metadata);

    jsonObject.put(SCHEMA_VERSION_KEY, SCHEMA_VERSION);
    return jsonObject;
  }

  /**
   * Return a list of PwPairs sorted by rank in descending order.
   * These PwPairs will be deduplicated by siteUrls (favoring the PwPair with
   * the highest rank).
   * @return a sorted list of PwPairs.
   */
  public List<PwPair> getPwPairsSortedByRank() {
    // Get all valid PwPairs.
    List<PwPair> allPwPairs = getPwPairs();

    // Sort the list in descending order.
    Collections.sort(allPwPairs, Collections.reverseOrder());

    // Filter the list.
    return removeDuplicateSiteUrls(allPwPairs);
  }

  /**
   * Return a list of PwPairs sorted by rank in descending order, including only the top-ranked
   * pair from each group.
   * @return a sorted list of PwPairs.
   */
  public List<PwPair> getGroupedPwPairsSortedByRank() {
    // Get all valid PwPairs.
    List<PwPair> allPwPairs = getPwPairs();

    // Group pairs with the same groupId, keeping only the top-ranked PwPair.
    List<PwPair> groupedPwPairs = removeDuplicateGroupIds(allPwPairs, null);

    // Sort by descending rank.
    Collections.sort(groupedPwPairs, Collections.reverseOrder());

    // Remove duplicate site URLs.
    return removeDuplicateSiteUrls(groupedPwPairs);
  }

  /**
   * Return a list of all pairs of valid URL devices and corresponding URL metadata.
   * @return list of PwPairs.
   */
  private List<PwPair> getPwPairs() {
    List<PwPair> allPwPairs = new ArrayList<>();
    for (UrlDevice urlDevice : mDeviceIdToUrlDeviceMap.values()) {
      PwsResult pwsResult = mBroadcastUrlToPwsResultMap.get(urlDevice.getUrl());
      if (pwsResult != null) {
        allPwPairs.add(new PwPair(urlDevice, pwsResult));
      }
    }
    return allPwPairs;
  }

  /**
   * Return the top-ranked PwPair for a given group ID.
   * @return a PwPair.
   */
  public PwPair getTopRankedPwPairByGroupId(String groupId) {
    for (PwPair pwPair : getGroupedPwPairsSortedByRank()) {
      if (pwPair.getPwsResult().getGroupId().equals(groupId)) {
        return pwPair;
      }
    }
    return null;
  }

  /**
   * Set the URL for making PWS requests.
   * @param pwsEndpoint The new PWS endpoint.
   */
  public void setPwsEndpoint(String pwsEndpoint) {
    mPwsClient.setPwsEndpoint(pwsEndpoint);
  }

  /**
   * Triggers an HTTP request to be made to the PWS.
   * This method fetches a results from the PWS for all broadcast URLs,
   * depending on the supplied parameters.
   * @param pwsResultCallback The callback to run when we get an HTTPResponse.
   * If this value is null, we will not fetch the PwsResults, only icons.
   * @param pwsResultIconCallback The callback to run when we get a favicon.
   * If this value is null, we will not fetch the icons.
   */
  public void fetchPwsResults(final PwsResultCallback pwsResultCallback,
                              final PwsResultIconCallback pwsResultIconCallback) {
    // Get new URLs to fetch.
    Set<String> newResolveUrls = new HashSet<>();
    Set<String> newIconUrls = new HashSet<>();
    for (UrlDevice urlDevice : mDeviceIdToUrlDeviceMap.values()) {
      String url = urlDevice.getUrl();
      if (!mPendingBroadcastUrls.contains(url)) {
        PwsResult pwsResult = mBroadcastUrlToPwsResultMap.get(url);
        if (pwsResult == null) {
          newResolveUrls.add(url);
          mPendingBroadcastUrls.add(url);
        } else if (pwsResult.hasIconUrl()
            && !mPendingIconUrls.contains(pwsResult.getIconUrl())
            && !mIconUrlToIconMap.containsKey(pwsResult.getIconUrl())) {
          newIconUrls.add(pwsResult.getIconUrl());
          mPendingIconUrls.add(pwsResult.getIconUrl());
        }
      }
    }

    // Make the resolve request.
    final Set<String> finalResolveUrls = newResolveUrls;
    PwsResultCallback augmentedCallback = new PwsResultCallback() {
      @Override
      public void onPwsResult(PwsResult pwsResult) {
        addMetadata(pwsResult);
        if (pwsResultIconCallback != null) {
            PwsResultIconCallback augmentedIconCallback =
                new AugmentedPwsResultIconCallback(pwsResult.getIconUrl(), pwsResultIconCallback);
            mPwsClient.downloadIcon(pwsResult.getIconUrl(), augmentedIconCallback);
        }
        pwsResultCallback.onPwsResult(pwsResult);
      }

      @Override
      public void onPwsResultAbsent(String url) {
        pwsResultCallback.onPwsResultAbsent(url);
      }

      @Override
      public void onPwsResultError(Collection<String> urls, int httpResponseCode, Exception e) {
        pwsResultCallback.onPwsResultError(urls, httpResponseCode, e);
      }

      @Override
      public void onResponseReceived(long durationMillis) {
        for (String url : finalResolveUrls) {
          mPendingBroadcastUrls.remove(url);
        }
        pwsResultCallback.onResponseReceived(durationMillis);
      }
    };
    if (pwsResultCallback != null && newResolveUrls.size() > 0) {
      mPwsClient.resolve(newResolveUrls, augmentedCallback);
    }

    // Make the icon requests.
    if (pwsResultIconCallback != null) {
      for (final String iconUrl : newIconUrls) {
        PwsResultIconCallback augmentedIconCallback =
            new AugmentedPwsResultIconCallback(iconUrl, pwsResultIconCallback);
        mPwsClient.downloadIcon(iconUrl, augmentedIconCallback);
      }
    }
  }

  /**
   * Cancel all current HTTP requests.
   */
  public void cancelAllRequests() {
    mPwsClient.cancelAllRequests();
  }

  private class AugmentedPwsResultIconCallback extends PwsResultIconCallback {
    private String mUrl;
    private PwsResultIconCallback mCallback;

    AugmentedPwsResultIconCallback(String url, PwsResultIconCallback callback) {
      mUrl = url;
      mCallback = callback;
    }

    @Override
    public void onIcon(byte[] icon) {
      mPendingIconUrls.remove(mUrl);
      addIcon(mUrl, icon);
      mCallback.onIcon(icon);
    }

    @Override
    public void onError(int httpResponseCode, Exception e) {
      mPendingIconUrls.remove(mUrl);
      mCallback.onError(httpResponseCode, e);
    }
  }
}
