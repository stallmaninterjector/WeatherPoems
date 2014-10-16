package com.smart.weatherpoems;

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

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {

	public String poem = null;
	private ProgressDialog pd;
	private ImageButton share;
	public static String apiurl=null;
	private TextView poemView;
	private static Random rand = new Random();
	private LocationManager locManager;

	/*Drawer Init stuff*/
	private DrawerLayout drawerLayout;
	private ListView drawerList;
	private String[] values = new String[] {"Generate New Poem"};

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);


		init();

		refreshPoemAndDisplay();
	}
	/**
	 * Initializes all the ui elements and 
	 */
	private void init() {

		share = (ImageButton)findViewById(R.id.imageButton1);
		share.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent sendIntent = new Intent();
				sendIntent.setAction(Intent.ACTION_SEND);
				sendIntent.putExtra(Intent.EXTRA_TEXT, poemView.getText().toString());
				sendIntent.setType("text/plain");
				startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.share)));
			}
		});
		
		poemView = (TextView)findViewById(R.id.poem);

		//start of drawer
		drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
		drawerList = (ListView)findViewById(R.id.drawer_list);


		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,android.R.id.text1,values);
		drawerList.setAdapter(adapter);


		drawerList.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view,int position, long id) {
				drawerLayout.closeDrawers();
				refreshPoemAndDisplay();

			}
		});

		drawerLayout.setDrawerListener(new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_launcher, R.string.open, R.string.title) {
			@Override
			public void onDrawerClosed(View drawerView) {
				getActionBar().setTitle(R.string.title);
				invalidateOptionsMenu();
			}
			@Override
			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle("Select an Option");
				invalidateOptionsMenu();
			}
		});

		//end of drawer stuff
		locManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		
		
	}

	/**
	 * CALL THIS METHOD TO REFRESH POEM
	 */
	private void refreshPoemAndDisplay() {
		if(locManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
			locManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,1000L,500.0f, onLocationChange);

		else if(locManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000L,500.0f, onLocationChange);

		else if(locManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER))
			locManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER,1000L,500.0f, onLocationChange);

		else
			System.out.println("Could not acquire location at all. Turn on yer damn GPS");

	}


	/**
	 * CANT CALL THIS DIRECTLY
	 * @param urlOfService -- url which has the loc data
	 * @param tv - the textview to past the poem into
	 * @throws JSONException
	 */
	public void generatePoemAndDisplay(String urlOfService,final TextView tv) throws JSONException {

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

			JSONObject json = new JSONObject(jsoncode);
			JSONObject current_conditions = json.getJSONObject("current_observation");
			final String weather =  current_conditions.getString("weather");
			final int temp = current_conditions.getInt("temp_f");
			final int windSpeed = current_conditions.getInt("wind_mph");
			final float precipToday = (float) current_conditions.getDouble("precip_today_in");
			final int visibility = current_conditions.getInt("visibility_mi");
			final int UV = current_conditions.getInt("UV");
			String humidityString = current_conditions.getString("relative_humidity");
			final int humidity = java.lang.Integer.parseInt(humidityString.replaceAll("[\\D]", ""));


			// TODO add this to the bottom 
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					if(poem.length()!=0)
						tv.setText(poem);
					else
						tv.setText("A poem for your current weather conditions could not be generated :(");
				}
			});


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
			else if ((weather.equals("Overcast")||weather.equals("Mostly Cloudy")) && temp>=32) {
				condition=6; //Cloudy day
				String line1=overcast5();
				String line2=overcast7();
				String line3=overcast5();
				poem=line1+"\n"+line2+"\n"+line3;
				System.out.println(poem);
			}
			else if (temp>=80 && humidity >70) {
				condition=7; //Hot & humid
			}
			else if(humidity>=75)
				condition=8;
			else if (UV>6) {
				condition=9; //Very Sunny!
			}
			else if (temp<=32 && windSpeed > 10) {
				condition=10; //Cold+Windy :(
			}
			else if (temp<=32) {
				condition=11; //Cold!
			}
			else if(temp>85){
				condition=12; //Hot
			}
			System.out.println(condition);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//XXX add the top piece of data here when the poem is made
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

		int choice = rand.nextInt(linePool.size());
		return linePool.get(choice);
	}
	public static String blizzard7() {
		List<String> linePool=new LinkedList<String>();
		linePool.add("Shovels will come in handy");
		linePool.add("Tree limbs are getting heavy");

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

		int choice = rand.nextInt(linePool.size());
		String lineToReturn = linePool.get(choice);
		return lineToReturn;
	}
	public static String cold5() {
		List<String> linePool=new LinkedList<String>();

		int choice = rand.nextInt(linePool.size());
		String lineToReturn = linePool.get(choice);
		return lineToReturn;
	}
	public static String cold7() {
		List<String> linePool=new LinkedList<String>();
		linePool.add("");
		linePool.add("");

		int choice = rand.nextInt(linePool.size());
		String lineToReturn = linePool.get(choice);
		return lineToReturn;
	}

	/**
	 * Location listenter
	 */
	private	LocationListener onLocationChange = new LocationListener() {
		public void onLocationChanged(final Location loc) {
			System.out.println(loc.getLatitude()+" "+loc.getLongitude());
			refreshPoemOnThread(loc);
		}

		public void onProviderDisabled(String provider) {}
		public void onProviderEnabled(String provider) {}
		public void onStatusChanged(String provider, int status, Bundle extras) {}
	};

	/**
	 * Cannot be called directly 
	 * @param loc --location
	 */
	public void refreshPoemOnThread(final Location loc) {

		AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {

			@Override
			protected void onPreExecute() {
				pd = new ProgressDialog(MainActivity.this);
				pd.setTitle("Getting your Location...");
				pd.setMessage("Please wait.");
				pd.setCancelable(false);
				pd.setIndeterminate(true);
				pd.show();
			}

			@Override
			protected Void doInBackground(Void... arg0) {

				Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
				Address returnedAddress = null;
				try {
					List<Address> address = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
					try {
						returnedAddress = address.get(0);
					} catch (IndexOutOfBoundsException ex) {
					}

					//Object[] kek=address.toArray();
					//					for (Object aKek : kek) 
					//						System.out.println(aKek);

					String ZIPCode=null;
					try{
						ZIPCode = returnedAddress.getPostalCode();
					}catch(NullPointerException e){
						System.out.println("Your location is not linked to an address.");
					}
					String apibegin="http://api.wunderground.com/api/0484e65ed3c3a0fa/conditions/q/";
					String apiend=".json";
					apiurl=apibegin+ZIPCode+apiend;
					generatePoemAndDisplay(apiurl,poemView);

				}catch(Exception e) {}
				return null;
			}

			@Override
			protected void onPostExecute(Void result) {
				if (pd!=null) 
					pd.dismiss();

			}

		};


		task.execute((Void[])null);

	}

	//debugging shit
	private void sysout(String data) {
		Toast.makeText(getApplicationContext(), data, Toast.LENGTH_SHORT).show();
	}
}
