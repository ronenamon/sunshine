package com.pointech.ronen.funshine;

import android.*;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.vision.text.Text;
import com.pointech.ronen.funshine.model.DailyWeatherReport;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;


public class WeatherActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener , GoogleApiClient.ConnectionCallbacks , LocationListener {
    ////yarka
    //http://api.openweathermap.org/data/2.5/forecast/?lat=32.9554191&lon=35.2169728&units=metric&appid=12823bb54326e8cc5cbe07abbdb11fa9


    //http://api.openweathermap.org/data/2.5/forecast/?lat=32.9554191&lon=35.2169728&units=metric&appid=12823bb54326e8cc5cbe07abbdb11fa9
    final String URL_BASE = "http://api.openweathermap.org/data/2.5/forecast";
    final String URL_COORD = "/?lat=";//"/?lat=32.9554191&lon=35.2169728";
    final String URL_UNIT = "&units=metric";
    final String URL_API_KEY = "&appid=12823bb54326e8cc5cbe07abbdb11fa9";
    private GoogleApiClient mGoogleApiClient;
    private final int PERMATION_LOCATION = 111;
    private ArrayList<DailyWeatherReport> weatherReportList = new ArrayList<>();//for the 5 object reports!

    private ImageView weatherIconMini;
    private ImageView weatherIcon;
    private TextView weatherDate;
    private TextView currentTemp;
    private TextView lowTemp;
    private TextView cityCountry;
    private TextView weatherDescription;

    WeatherAdapter mAdapter;//3

    private static Context mContext;

    public static Context getContext(){
        return mContext;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        mContext = this;

        //setup
        weatherIconMini = (ImageView) findViewById(R.id.weatherIconMini);
        weatherIcon = (ImageView) findViewById(R.id.weatherIcon);
        weatherDate = (TextView) findViewById(R.id.weatherDate);
        currentTemp = (TextView) findViewById(R.id.currentTemp);
        lowTemp = (TextView) findViewById(R.id.lowTemp);
        cityCountry = (TextView) findViewById(R.id.cityCountry);
        weatherDescription = (TextView) findViewById(R.id.weatherDescription);
        //END

        //android:id="@+id/content_weather_reports at the activity_weather layot section
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.content_weather_reports);

        mAdapter = new WeatherAdapter(weatherReportList);

        recyclerView.setAdapter(mAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
       // downloadWatherData(null);//for test
    }

    public void downloadWatherData(Location location) {

        final String FULLCOORDS = URL_COORD + location.getLatitude() + "&lon=" + location.getLongitude();
        final String url = URL_BASE + FULLCOORDS + URL_UNIT + URL_API_KEY;
        Log.d("url", url);

        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {//the null is mean we not send data to server!
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject city = response.getJSONObject("city");// json objcet city inside the response!
                    String cityName = city.getString("name"); // inside the city objcet we have name propertis
                    String countryName = city.getString("country");
                    Log.v("City Name", cityName);
                    Log.v("country Name ", countryName);

                    //Get the List Array and put inside objects
                    JSONArray List = response.getJSONArray("list"); // the list array inside the json

                    //get the last five object in the list
                    for (int indexOfList = 0; indexOfList < 5; indexOfList++) {

                        //temp object
                        JSONObject obj = List.getJSONObject(indexOfList);// at this index! / 0 , 1  , 2 ,3 , 4

                        //fetch the main temps from curent obj
                        JSONObject main = obj.getJSONObject("main"); // main inside the list

                        //the Temps as Double [temp , temp_min , temp_max]
                        Double currentTemp = main.getDouble("temp");
                        Double maxTemp = main.getDouble("temp_max");
                        Double minTemp = main.getDouble("temp_min");

                        //weather obj with main [Clear] ... etc
                        JSONArray weatherArr = obj.getJSONArray("weather");
                        JSONObject weather = weatherArr.getJSONObject(0); // the first object at 0 index
                        String weatherType = weather.getString("main"); // the main inside the weather object inside the current indexoflist inside all list!

                        //Get The date from the list at current object!

                        String rawDate = obj.getString("dt_txt");

                        //create new instance and add to arraylist!
                        DailyWeatherReport report = new DailyWeatherReport(cityName, countryName, currentTemp.intValue(), maxTemp.intValue(), minTemp.intValue(), weatherType, rawDate);
                        Log.v("JSON", "Printing From Class: " + report.getWeather());
                        weatherReportList.add(report);//add to the list
                    }
                    Log.v("test: ", weatherReportList.get(0).getCityName().toString() + " - " + weatherReportList.get(0).getCurrentTemp());

                } catch (JSONException ex) {
                    Log.v("Error OnResponse ", ex.getMessage());
                    Log.v("ErrorgetLocalized", ex.getLocalizedMessage());
                }
                //after all downloaded data and success
                updateUItop();
                mAdapter.notifyDataSetChanged();

            }

        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("onErrorResponse", "error!");
            }
        });

        Volley.newRequestQueue(this).add(jsonRequest);//to fire the req!

    }

    public void updateUItop() {
        if (weatherReportList.size() > 0) {
            DailyWeatherReport report = weatherReportList.get(0);

            switch (report.getWeather()) {
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.cloudy));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_WIND:
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.rainy));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_SNOW:
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.snow));
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.snow));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_CLEAR:
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.partially_cloudy));
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.partially_cloudy));
                    break;
                default:
                    weatherIconMini.setImageDrawable(getResources().getDrawable(R.drawable.sunny));
                    weatherIcon.setImageDrawable(getResources().getDrawable(R.drawable.sunny));


            }
            //update now
            weatherDate.setText("Today , July 8");
            currentTemp.setText(report.getCurrentTemp() + "");
            lowTemp.setText(Integer.toString(report.getMinTemp()));
            cityCountry.setText(report.getCityName() + ", " + report.getCountry());
            weatherDescription.setText(report.getWeather());


        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.d("onConnected", "Start!");

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMATION_LOCATION);

        } else {
            startLocationServices();
        }
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("onLocationChanged", "Start");
        downloadWatherData(location);
    }

    private void startLocationServices() {
        Log.d("startLocationServices", "Start!");
        try {
            LocationRequest req = LocationRequest.create().setPriority(LocationRequest.PRIORITY_LOW_POWER);
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, req, this);

        } catch (SecurityException exception) {

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("onRequestPermissions", "Start!");
        switch (requestCode) {
            case PERMATION_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationServices();
                } else {
                    //show a dialog saying somthing
                    Toast.makeText(this, "Can't  Run Your Location Dummy", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


}


    //for recycler view

    // Adapter (2)
     class WeatherAdapter extends RecyclerView.Adapter<WeatherReportViewHolder> {


        private ArrayList<DailyWeatherReport> mDailyWeatherReports;
        //Cots
        public WeatherAdapter(ArrayList<DailyWeatherReport> mDailyWeatherReports) {
            this.mDailyWeatherReports = mDailyWeatherReports;
        }





        //1
        @Override
        public WeatherReportViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View card = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_weather,parent,false);
            return new WeatherReportViewHolder(card);
        }

        @Override
        public void onBindViewHolder(WeatherReportViewHolder holder, int position) {

            DailyWeatherReport report = mDailyWeatherReports.get(position);
            holder.updateUI(report);
        }

        @Override
        public int getItemCount() {
            return mDailyWeatherReports.size();
        }
    }


    // View Holder (1)
      class WeatherReportViewHolder extends RecyclerView.ViewHolder{



        private ImageView _weatherIcon;
        private TextView _weatherDate;
        private TextView _weatherDescription;
        private TextView _tempHigh;
        private TextView _tempLow;
        private Context resources;

        public WeatherReportViewHolder(View itemView) {
            super(itemView);

            //reffer
            _weatherIcon = (ImageView)itemView.findViewById(R.id.list_weatherIcon);
            _weatherDate = (TextView)itemView.findViewById(R.id.list_weather_day);
            _weatherDescription = (TextView)itemView.findViewById(R.id.list_weather_description);
            _tempHigh = (TextView)itemView.findViewById(R.id.list_weather_temp_high);
            _tempLow = (TextView)itemView.findViewById(R.id.list_weather_temp_low);

        }

        public void updateUI(DailyWeatherReport report) {

            _weatherDate.setText(report.getFormattedDate());
            _weatherDescription.setText(report.getWeather());
            _tempHigh.setText(Integer.toString(report.getMaxTemp()));
            _tempLow.setText(Integer.toString(report.getMinTemp()));



            switch (report.getWeather()) {
                case DailyWeatherReport.WEATHER_TYPE_CLOUDS:
                    _weatherIcon.setImageDrawable(WeatherActivity.getContext().getResources().getDrawable(R.drawable.partially_cloudy_mini));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_RAIN:
                    _weatherIcon.setImageDrawable(WeatherActivity.getContext().getResources().getDrawable(R.drawable.rainy_mini));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_WIND:
                    _weatherIcon.setImageDrawable(WeatherActivity.getContext().getResources().getDrawable(R.drawable.rainy_mini));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_SNOW:
                    _weatherIcon.setImageDrawable(WeatherActivity.getContext().getResources().getDrawable(R.drawable.snow_mini));
                    break;
                case DailyWeatherReport.WEATHER_TYPE_CLEAR:
                    _weatherIcon.setImageDrawable(WeatherActivity.getContext().getResources().getDrawable(R.drawable.partially_cloudy_mini));
                    break;
                default:
                    _weatherIcon.setImageDrawable(WeatherActivity.getContext().getResources().getDrawable(R.drawable.sunny_mini));

            }
        }

    }
