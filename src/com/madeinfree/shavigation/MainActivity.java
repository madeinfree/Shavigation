package com.madeinfree.shavigation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements LocationListener {
	
	protected static String TAG = "myShavigation";
	
	//Views
	private EditText origin_et, destination_et; //起點、終點
	
	private Button search_btn, 
				   doing_btn,
				   result_step_details;
	
	private TextView result_tv, 
					 resultContent_tv,
					 result_start_location_tv,
					 result_end_location_tv,
					 result_steps_tv,
					 result_steps_show_tv,
					 result_dis_tv;
	
	//WebView
	private static final String MAP_URL = "file:///android_asset/googleMap.html";
    private WebView webView;
	
	//Default url
	//
	private String path = "http://maps.googleapis.com/maps/api/directions/json?",
				   origin = "22.998787,120.253193",
				   destination = "22.997179,120.212945",
				   sensor = "yes";
	
	//Handler switch flag
	protected static final int Result_Data = 0x1;
	
	//GPS Service
	private LocationManager lms;
	private String bestProvider;
	private boolean getService = false;		//是否已開啟定位服務
	
	//Users GPS
	private Double nowLongitude, nowLatitude;
	
	//CopyData
	private boolean search_location = false;
	private int nowStep = 0;
	private int objNumber;
	private String stepInstructions[];
	private String stepEndLocation[][];
	
	//Handler 接收Thread訊息
	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case Result_Data:
						String result = null;
						String stepsResult = "\n";
						if(msg.obj instanceof String) {
							result = (String) msg.obj;
						}
						if(result != null) {
							try {
								JSONObject StoJ = new JSONObject(result);
								JSONObject array = (JSONObject) StoJ.getJSONArray("routes").opt(0);
								JSONObject legs = (JSONObject) array.getJSONArray("legs").opt(0);
								JSONArray steps = (JSONArray) legs.getJSONArray("steps");
								String start_location_address = legs.getString("start_address");
								String end_location_address = legs.getString("end_address");
								result_start_location_tv.setText("起始點：" + start_location_address.toString());
								result_end_location_tv.setText("終點：" + end_location_address.toString());
								result_steps_tv.setText("警示次數：" + String.valueOf(steps.length()) + "次");
								
								//複製資源操作
								objNumber = steps.length();
								stepInstructions = new String[objNumber];
								stepEndLocation = new String[objNumber][2];
								
								for(int i = 0; i < steps.length(); i++) {
									JSONObject steps_details = (JSONObject) steps.opt(i);
									//stepsResult += steps_details.getString("html_instructions") + "\n\n";
									stepInstructions[i] = steps_details.getString("html_instructions");
									stepEndLocation[i][0] = steps_details.getString("start_location");
									stepEndLocation[i][1] = steps_details.getString("end_location");
								}
								
								//String stepsResult_cut_b = stepsResult.replace("<b>", "");
								//String stepsResult_cut_bb = stepsResult_cut_b.replace("</b>", "");
								//String stepsResult_cut_div = stepsResult_cut_bb.replace("<div style=\"font-size:0.9em\">", "");
								//String stepsResult_cut_div_d = stepsResult_cut_div.replace("</div>", "");
								//result_steps_show_tv.setText("警示地點：" + stepsResult_cut_div_d);
								
								//Action Doing
								//
								//result_tv.setText("距離"+String.valueOf((int)getDistance(22.998748, 120.253236, 22.997844, 120.250479))+"公尺");
								//
								//
								result_tv.setText("偵測完畢，點選開始執行後，請保持GPS穩定");
								//
								search_location = true;
								
							} catch (JSONException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					break;	
			}
		}
	};
		 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		SetView();
		
		addListener();
		
		getService();
		
		setupWebView();
		
		//this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		
	}
	
	Double stepLat,
		   stepLng;
	
	private void do_worker(Location location) throws JSONException {
			stepLat = Double.valueOf(new JSONObject(stepEndLocation[nowStep][1]).getString("lat").toString());
			stepLng = Double.valueOf(new JSONObject(stepEndLocation[nowStep][1]).getString("lng").toString());
			//result_tv.setText("距離"+String.valueOf((int)getDistance(nowLatitude, nowLongitude, stepLat, stepLng))+"公尺");
			if((int)getDistance(nowLatitude, nowLongitude, stepLat, stepLng) < 200) {
				if(nowStep == objNumber-1) {
					result_tv.setText("目的地到達 !");
				} else {
					nowStep = nowStep + 1;
					stepLat = Double.valueOf(new JSONObject(stepEndLocation[nowStep][1]).getString("lat").toString());
					stepLng = Double.valueOf(new JSONObject(stepEndLocation[nowStep][1]).getString("lng").toString());
					result_tv.setText("已到達警示「" + String.valueOf(nowStep) + "」" +
							"跟下個警示距離"+String.valueOf((int)getDistance(nowLatitude, nowLongitude, stepLat, stepLng))+"公尺");
					tips("到達 !");
				}
			}
			result_dis_tv.setText("目前剩下距離：" + String.valueOf((int)getDistance(nowLatitude, nowLongitude, stepLat, stepLng))+"公尺");
				//tips(stepIns[0]);
				//tips(String.valueOf(new JSONObject(endLocation[0][0]).getString("lat")));
	}
	
	private void setupWebView(){
        webView = (WebView) findViewById(R.id.mapWebView);
        webView.getSettings().setJavaScriptEnabled(true);       
        webView.loadUrl(MAP_URL);  
    }
	
	private void markWebView(Location nowLatLng) {
		String jsurl = "javascript:mark(" +
				nowLatLng.getLatitude() + "," +
				nowLatLng.getLongitude() + ",1)";
		webView.loadUrl(jsurl);
	}
	 
	public void SetView() {
		
		//editText
		origin_et = (EditText) findViewById(R.id.origin_et);
		destination_et = (EditText) findViewById(R.id.destination_et);
		
		//Bitton
		search_btn = (Button) findViewById(R.id.search_btn);
		doing_btn = (Button) findViewById(R.id.doing_btn);
		result_step_details = (Button) findViewById(R.id.result_step_details);
		
		//TextView
		result_tv = (TextView) findViewById(R.id.result_tv);
		result_dis_tv = (TextView) findViewById(R.id.result_dis_tv);
		resultContent_tv = (TextView) findViewById(R.id.resultContent_tv);
		result_start_location_tv = (TextView) findViewById(R.id.result_start_location_tv);
		result_end_location_tv = (TextView) findViewById(R.id.result_end_location_tv);
		result_steps_tv = (TextView) findViewById(R.id.result_steps_tv);
		result_steps_show_tv = (TextView) findViewById(R.id.result_steps_show_tv);
		
	}
	
	public void addListener() {
		search_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				/*final String origin_et_str = origin_et.getText().toString();*/
				final String destination_et_str = destination_et.getText().toString();
				if(destination_et_str.equals("")) {
					tips("無法搜尋空值");
					return;
				}
				
				//Reset
				nowStep = 0;
				
				Thread bg_internet = new Thread(new Runnable() {

					@Override
					public void run() { 
						try { 
							DoInterNetConnection(path + 
									"origin=" + nowLatitude +"," + nowLongitude +
									"&destination=" + destination_et_str +
									"&language=zh-TW" +
									"&sensor=" + sensor); 
							String startForJs = nowLatitude + "," + nowLongitude;
							String jsurl = "javascript:calcRoute2(\""+startForJs+"\",\""+destination_et_str	+"\")"; 
							webView.loadUrl(jsurl); 
						} catch (MalformedURLException e) { 
							Log.e(TAG, "DoInterNetConnection, Error !");
							//e.printStackTrace(); 
						} catch (IOException e) { 
							// TODO Auto-generated catch block 
							Log.e(TAG, "DoInterNetConnection, Error !");
							//e.printStackTrace();
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
				}); 
				bg_internet.start();
			}
		});
		
		doing_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (lms.isProviderEnabled(LocationManager.GPS_PROVIDER) || lms.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
		  			//如果GPS或網路定位開啟，呼叫locationServiceInitial()更新位置
		  			getService = true;	//確認開啟定位服務
		  			try {
						locationServiceInitial();
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		  			tips("開始執行");
		  			//
		  			search_btn.setEnabled(true);
		  			doing_btn.setEnabled(false);
		  		} else {
		  			tips("請開啟定位服務");
		  			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		  		    builder.setTitle("警告訊息");
		  			builder.setMessage("您尚未開啟定位服務，要前往設定嗎？");
		  			builder.setPositiveButton("是", new DialogInterface.OnClickListener() {
		  		        @Override
		  		        public void onClick(DialogInterface dialog, int which) {
		  		        	startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));	//開啟設定頁面
		  		        }
		  		    });
		  		    builder.setNegativeButton("否", new DialogInterface.OnClickListener() {
		  		        @Override
		  		        public void onClick(DialogInterface dialog, int which) {
		  		        }
		  		    });
		  		    AlertDialog alert = builder.create();
		  		    alert.show();
		  		}
				if(getService) {
					lms.requestLocationUpdates(bestProvider, 1000, 50, MainActivity.this);
					//服務提供者、更新頻率60000毫秒=1分鐘、最短距離、地點改變時呼叫物件
				}
			}
		});
		
		result_step_details.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				String step_details_for_dialog = "";
				
				for(int i = 0;i < objNumber; i++) {
					step_details_for_dialog += stepInstructions[i] + "\n";
				}
				
				String stepsResult_cut_b = step_details_for_dialog.replace("<b>", "");
				String stepsResult_cut_bb = stepsResult_cut_b.replace("</b>", "");
				String stepsResult_cut_div = stepsResult_cut_bb.replace("<div style=\"font-size:0.9em\">", "");
				String stepsResult_cut_div_d = stepsResult_cut_div.replace("</div>", "");
				
				AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
				dialog.setTitle("警示點詳細資料");
				dialog.setMessage(stepsResult_cut_div_d);
				dialog.setIcon(android.R.drawable.ic_dialog_alert);
				dialog.setCancelable(false);
				dialog.setPositiveButton("關閉", new DialogInterface.OnClickListener() {  
				    public void onClick(DialogInterface dialog, int which) {  
				    	
				    }  
				}); 
				dialog.show();
			}
		});
	} 
	  
	private void getService() {
		
		lms = (LocationManager) getSystemService(LOCATION_SERVICE);	//取得系統定位服務
		 
	} 
	 
	private void locationServiceInitial() throws JSONException {
		Criteria criteria = new Criteria();	//資訊提供者選取標準
		bestProvider = lms.getBestProvider(criteria, true);	//選擇精準度最高的提供者
		//Location location = lms.getLastKnownLocation(LocationManager.GPS_PROVIDER);	//使用GPS定位座標
		Location location = lms.getLastKnownLocation(bestProvider);	//選擇最佳提供者
		//tips(String.valueOf(location.getLongitude())+"  "+String.valueOf(location.getLatitude()));
		getLocation(location);
		if(location != null) {
			markWebView(location);
		}
	}
	
	private void getLocation(Location location) throws JSONException {	//將定位資訊顯示在畫面中
		if(location != null) {
			//Double longitude = location.getLongitude();	//取得經度
			//Double latitude = location.getLatitude();	//取得緯度
			nowLongitude = location.getLongitude(); //取得經度
			nowLatitude = location.getLatitude(); //取得緯度
			//tips(String.valueOf(nowLongitude)+"  "+String.valueOf(nowLatitude));
			if(search_location) {
				do_worker(location);
				markWebView(location);
			}
			
		}
		else {
			Toast.makeText(this, "無法定位座標", Toast.LENGTH_LONG).show();
		}
	}
	
	private InputStream is = null;
	private String result = "";
	
	//InterNetConnection
	private void DoInterNetConnection(String path) throws IOException, JSONException {
		HttpClient httpclient = new DefaultHttpClient(); // for port 80 requests!
		HttpGet httpget = new HttpGet(path);
		HttpResponse response = httpclient.execute(httpget);
		HttpEntity entity = response.getEntity();
		is = entity.getContent();
		
		String objResult = readStream(is);
		handler.obtainMessage(Result_Data, objResult).sendToTarget();
		 
		//Log.i(TAG, ((JSONObject) new JSONObject(readStream(is)).getJSONArray("routes").opt(0)).getJSONArray("legs").opt(0).toString()); 
	}
	
	
	//Stream
	private String readStream(InputStream is) {
	    try {
	    	BufferedReader reader = new BufferedReader(new InputStreamReader(is,"utf-8"),8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			result = sb.toString();	
	      return result;
	    } catch (IOException e) {
	      return "";
	    }
	}
	
	//Helper getdis
	public double getDistance(double lat1, double lon1, double lat2, double lon2) {
		float[] results = new float[1];
		Location.distanceBetween(lat1, lon1, lat2, lon2, results);
		return results[0];
	}
	
	//Helper Tips
	public void tips(String str) {
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if(getService) {
			lms.removeUpdates(this);	//離開頁面時停止更新
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if(getService) {
			lms.removeUpdates(this);	//離開頁面時停止更新
		}
	}

	
	/** 
	 * Location implements
	 */
	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub
		try {
			getLocation(location);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub
		
	}


}
