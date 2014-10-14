/*
 * Copyright (c) 2013. Tom Casey
 * This program may only be used by the author's explicit written consent. Any descriptions, modifications, recounts, tales, attributions, mentions, representations, delineations, statements, portrayals,  and/or depictions of or pertaining to this program are strictly prohibited without the explicit written consent of Tom Casey.
 * Any actions percieved to be in violation of the above statement will be prosecuted to the full extent of US and international law.
 */

package com.Tom.weatherpoems;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.json.JSONException;
import org.json.JSONObject;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
//import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {
	public String poem=null;
	//private static final String DEBUG_TAG = null;
	public static String apiurl=null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		/*LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        boolean gpsEnabled = false, networkEnabled=false;
		try {
           gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }
        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }
		if(!gpsEnabled && !networkEnabled)
			finish();*/
		Location location = null;
        LocationManager locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if(locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
		locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000L,500.0f, onLocationChange);
		location = locManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
       else if(locManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,500.0f, onLocationChange);
            location = locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }
      else if(locManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)){
            locManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,1000L,500.0f, onLocationChange);
            location = locManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
        }
        else{
            System.out.println("Could not acquire location at all. Turn on yer damn GPS");
        }
		System.out.println(location);
		double lat = location.getLatitude();
		double lon = location.getLongitude();
		System.out.println(lat+" "+lon);
		Context c = this;
		Geocoder geocoder = new Geocoder(c, Locale.getDefault());
		Address returnedAddress = null;
		try {
			List<Address> address = geocoder.getFromLocation(lat, lon, 1);
			try {
				returnedAddress = address.get(0);
			} catch (IndexOutOfBoundsException ex) {
				AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setMessage("Unable to find your location.")
				.setNeutralButton("okay :(", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						finish();
					}
				});
				builder.create();
			}
            Object[] kek=address.toArray();
            for (Object aKek : kek) System.out.println(aKek);
			System.out.println(returnedAddress);
            String ZIPCode=null;
            try{
            ZIPCode = returnedAddress.getPostalCode();
            }catch(NullPointerException e){
                System.out.println("Your location is not linked to an address.");
            }
			String apibegin="http://api.wunderground.com/api/0484e65ed3c3a0fa/conditions/q/";
			String apiend=".json";
			apiurl=apibegin+ZIPCode+apiend;
			AsyncTask<String, Integer, String> task = new downloadWeatherData().execute(apiurl);
			System.out.println(apiurl);
			task.get(20000, TimeUnit.MILLISECONDS);
			TextView textview = new TextView(this);
			textview.setText(poem);
			setContentView(textview);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	class downloadWeatherData extends AsyncTask<String, Integer, String> {
		@Override
		protected String doInBackground(String... apiurl) {

			try {
				poem = getJsonFromService(apiurl[0]);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return apiurl[0];
		}

	}
	public /*static*/ String getJsonFromService(String urlOfService) throws JSONException {
		String jsoncode;
		try {
			int length=10000;
			URL serviceConnector = new URL(urlOfService);
			HttpURLConnection conn = (HttpURLConnection) serviceConnector.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(15000);
			conn.setRequestMethod("GET");
			conn.setDoInput(true);
			conn.connect();
			InputStream rawWeatherData = conn.getInputStream();
			jsoncode=readIt(rawWeatherData, length);
			//System.out.println(jsoncode);
			JSONObject json = new JSONObject(jsoncode);
			JSONObject current_conditions = json.getJSONObject("current_observation");
			String weather =  current_conditions.getString("weather");
			int temp = current_conditions.getInt("temp_f");
			int windSpeed = current_conditions.getInt("wind_mph");
			float precipToday = (float) current_conditions.getDouble("precip_today_in");
			int visibility = current_conditions.getInt("visibility_mi");
			int UV = current_conditions.getInt("UV");
			String humidityString = current_conditions.getString("relative_humidity");
			int humidity = java.lang.Integer.parseInt(humidityString.replaceAll("[\\D]", ""));
			System.out.println("Current Conditions: "+weather+" Temp:"+temp+" Wind speed(mph): "+windSpeed+" Rain so far today(in): "+precipToday+" Visibility(mi): "+visibility+" UV Index: "+UV+" Humidity(%): "+humidity);
			int condition=0;
			if(temp<=32 && windSpeed >=10 && precipToday > .3){
				condition=1; //Blizzard!
				String line1=blizzard5();
				String line2=blizzard7();
				String line3=blizzard5();
				poem = line1+"\n"+line2+"\n"+line3;
				System.out.println(poem);

			}
			else if(windSpeed >= 20) {
				condition=2; //Blustery Day
			}
			else if(temp>=93 && precipToday >= .2){
				condition=3; //Hot & Rainy :(
			}
			else if (precipToday>=.3 && UV <= 3) {
				condition=4; //Rainy day :(
			}
			else if (visibility >=3 && weather.equals("Clear")) {
				condition=5; //Clear day
			}
			else if (weather.equals("Overcast") && temp>=32) {
				condition=6; //Cloudy day
				String line1=overcast5();
				String line2=overcast7();
				String line3=overcast5();
				poem=line1+"\n"+line2+"\n"+line3;
				System.out.println(poem);
			}
			else if (temp>=80 && humidity >50) {
				condition=7; //Hot & humid
			}
			else if (UV>6) {
				condition=8; //Very Sunny!
			}
			else if (temp<=32 && windSpeed > 10) {
				condition=9; //Cold+Windy :(
			}
			else if (temp<=32) {
				condition=10; //Cold!
			}
            else if(temp>85){
                condition=11; //Hot
            }
			System.out.println(condition);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return poem;
	}

	public static String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
		Reader reader;
		reader = new InputStreamReader(stream, "UTF-8");        
		char[] buffer = new char[len];
		reader.read(buffer);
		return new String(buffer);	
	}
	public static String blizzard5() {
		List<String> linePool=new LinkedList<String>();
		linePool.add("Put on your jacket");
		linePool.add("No school tomorrow");
		linePool.add("Winter-Wonderland");
		linePool.add("Salt trucks are en route");
		/*linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");*/
		Random rand = new Random();
		int choice = rand.nextInt(linePool.size());
		return linePool.get(choice);
	}
	public static String blizzard7() {
		List<String> linePool=new LinkedList<String>();
		linePool.add("Shovels will come in handy");
		linePool.add("Tree limbs are getting heavy");
		/*linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");*/
		Random rand = new Random();
		int choice = rand.nextInt(linePool.size());
		String lineToReturn = linePool.get(choice);
		return lineToReturn;
	}
	public static String overcast5() {
		List<String> linePool=new LinkedList<String>();
		linePool.add("Cloudy skies now loom");
		linePool.add("No need for sunscreen");
		linePool.add("Post-pone your picnic");
		linePool.add("Clouds are upon us");
		linePool.add("No sun is in sight");
		linePool.add("Ditch the parasol");
		linePool.add("Gray blankets the sky");
		linePool.add("Holiday for sun");
		linePool.add("The sky is opaque");
		linePool.add("Suspended liquids");
		linePool.add("Sundial won\'t work");
		linePool.add("Is this Seattle?");
		linePool.add("Are we in \"Twilight?\"");
		linePool.add("Vaporized moisture");
		linePool.add("Gray spans overhead");
		linePool.add("Gray muddies the sky");
		linePool.add("Gray dominates sky");
		linePool.add("Imposing white clouds");
		linePool.add("Gloominess aloft");
		linePool.add("Gray clouds spread dispair");
		linePool.add("Sun sleeps in today");
		linePool.add("No stars out tonight");
		Random rand = new Random();
		int choice = rand.nextInt(linePool.size());
		String lineToReturn = linePool.get(choice);
		return lineToReturn;
	}
	public static String overcast7() {
		List<String> linePool=new LinkedList<String>();
		linePool.add("Disturbing lack of color");
		linePool.add("Trees stand stark against gray sky");
		linePool.add("Heavy clouds block the sunlight");
		linePool.add("No stars on the horizon");
		linePool.add("Solar panels will not work");
		linePool.add("UV index is not high");
		linePool.add("Take your vitamin D pill");
		linePool.add("Vampires will roam today");
		linePool.add("\"\'Aint no sunshine when she\'s gone\"");
		linePool.add("Bill Withers, this is your fault");
		linePool.add("Bad day for shuttle launch");
		linePool.add("No evaporation now");
		linePool.add("Photosynthetic rate slows");
		linePool.add("White sheath hovers overhead");
		linePool.add("Feel mother nature's despair");
		linePool.add("Moon is hidden by the clouds");
		linePool.add("Water particles in air");
		//linePool.add("");
		//linePool.add("");
		Random rand = new Random();
		int choice = rand.nextInt(linePool.size());
		String lineToReturn = linePool.get(choice);
		return lineToReturn;
	}
	public static String cold5() {
		List<String> linePool=new LinkedList<String>();
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		Random rand = new Random();
		int choice = rand.nextInt(linePool.size());
		String lineToReturn = linePool.get(choice);
		return lineToReturn;
	}
	public static String cold7() {
		List<String> linePool=new LinkedList<String>();
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		linePool.add("");
		//linePool.add("");
		//linePool.add("");
		Random rand = new Random();
		int choice = rand.nextInt(linePool.size());
		String lineToReturn = linePool.get(choice);
		return lineToReturn;
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
	LocationListener onLocationChange=new LocationListener() {
		public void onLocationChanged(Location loc) {}
		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	};
}
