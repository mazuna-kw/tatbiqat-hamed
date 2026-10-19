package com.hamed.alhasiba;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.content.SharedPreferences;
import java.text.DecimalFormat;
import java.util.*;
public class MainActivity extends Activity {
 TextView display, historyView; String expression=""; List<String> history=new ArrayList<>();
 SharedPreferences prefs; DecimalFormat fmt=new DecimalFormat("0.##########");
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(0xFF151515);getWindow().setNavigationBarColor(0xFF151515);
 prefs=getSharedPreferences("calculator",MODE_PRIVATE);String saved=prefs.getString("history","");if(!saved.isEmpty())history.addAll(Arrays.asList(saved.split("\\n")));
 LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(16,16,16,8);root.setBackgroundColor(0xFF151515);
 display=new TextView(this);display.setText("0");display.setTextColor(-1);display.setTextSize(32);display.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);root.addView(display,new LinearLayout.LayoutParams(-1,0,1.2f));
 GridLayout grid=new GridLayout(this);grid.setColumnCount(4);String[] keys={"MC","MR","M+","M-","AC","⌫","%","÷","7","8","9","×","4","5","6","−","1","2","3","+","√","0",".","="};
 for(String k:keys){Button bt=new Button(this);bt.setText(k);bt.setTextSize(17);bt.setTextColor(-1);bt.setBackgroundTintList(android.content.res.ColorStateList.valueOf(k.equals("=")?0xFFB77E34:0xFF343434));GridLayout.LayoutParams lp=new GridLayout.LayoutParams();lp.width=0;lp.height=58;lp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);lp.setMargins(3,3,3,3);grid.addView(bt,lp);bt.setOnClickListener(v->press(k));}
 root.addView(grid,new LinearLayout.LayoutParams(-1,0,3f));TextView title=new TextView(this);title.setText("سجل العمليات");title.setTextColor(0xFFD6A85F);title.setTextSize(16);root.addView(title);
 ScrollView sc=new ScrollView(this);historyView=new TextView(this);historyView.setTextColor(0xFFCCCCCC);sc.addView(historyView);root.addView(sc,new LinearLayout.LayoutParams(-1,74));
 TextView footer=new TextView(this);footer.setText("برمجة حامد الدهاش");footer.setTextColor(0xFFAAAAAA);footer.setGravity(Gravity.CENTER);root.addView(footer);setContentView(root);render();}
 void press(String k){try{switch(k){case "AC":expression="";break;case "⌫":if(!expression.isEmpty())expression=expression.substring(0,expression.length()-1);break;
 case "=":if(!expression.isEmpty()){double n=new Parser(expression).parse();String out=fmt.format(n);history.add(0,expression+" = "+out);while(history.size()>100)history.remove(history.size()-1);prefs.edit().putString("history",android.text.TextUtils.join("\\n",history)).apply();expression=out;}break;
 case "√":expression=fmt.format(Math.sqrt(Double.parseDouble(expression)));break;case "%":expression=fmt.format(Double.parseDouble(expression)/100);break;
 case "MC":prefs.edit().remove("memory").apply();break;case "MR":expression+=prefs.getString("memory","0");break;
 case "M+":prefs.edit().putString("memory",fmt.format(Double.parseDouble(expression.isEmpty()?"0":expression)+Double.parseDouble(prefs.getString("memory","0")))).apply();break;
 case "M-":prefs.edit().putString("memory",fmt.format(Double.parseDouble(prefs.getString("memory","0"))-Double.parseDouble(expression.isEmpty()?"0":expression))).apply();break;
 default:expression+=k.equals("×")?"*":k.equals("÷")?"/":k.equals("−")?"-":k;}}catch(Exception e){display.setText("خطأ");}render();}
 void render(){display.setText(expression.isEmpty()?"0":expression.replace("*","×").replace("/","÷"));historyView.setText(android.text.TextUtils.join("\\n",history));}
 static class Parser {String s;int p=0;Parser(String x){s=x.replace(" ","");}double parse(){double v=expr();if(p<s.length())throw new RuntimeException();return v;}
 double expr(){double v=term();while(p<s.length()){char c=s.charAt(p);if(c=='+'||c=='-'){p++;double n=term();v=c=='+'?v+n:v-n;}else break;}return v;}
 double term(){double v=factor();while(p<s.length()){char c=s.charAt(p);if(c=='*'||c=='/'){p++;double n=factor();if(c=='/'&&n==0)throw new ArithmeticException();v=c=='*'?v*n:v/n;}else break;}return v;}
 double factor(){if(p<s.length()&&s.charAt(p)=='('){p++;double v=expr();if(p>=s.length()||s.charAt(p++)!=')')throw new RuntimeException();return v;}int st=p;if(p<s.length()&&(s.charAt(p)=='-'||s.charAt(p)=='+'))p++;while(p<s.length()&&(Character.isDigit(s.charAt(p))||s.charAt(p)=='.'))p++;if(st==p)throw new RuntimeException();return Double.parseDouble(s.substring(st,p));}}
}
