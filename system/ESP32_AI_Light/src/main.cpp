#include <string.h>
#include <Wire.h>
#include <HardwareSerial.h>
#include <WiFi.h>
#include <WebServer.h>

WebServer  esp32_server(80);  //声明一个 WebServer 的对象，对象的名称为 esp32_server
                              //设置网络服务器响应HTTP请求的端口号为 80

#define PIN_R 25            //红色LED引脚
#define PIN_G 26            //绿色LED引脚
#define PIN_B 27            //蓝色LED引脚

#define ADDR 0b0100011      //光照传感器地址
#define POWER_ON 0b00000001 //开启传感器指令
#define RESET 0b00000111    //重置传感器指令
#define MODE 0b00100000     //采样模式——单次采样，精度为1lx

#define LX_VH 800           //超高光照门限值，达到此门限值后将关闭灯光
#define LX_H 400            //高光照门限值，达到此门限值后将开启微弱灯光
#define LX_M 200            //中等光照门限值，达到此门限值后将开启中等强度灯光
#define LX_L 100            //低光照门限值，达到此门限值后将开启强光

int ctrlAuto();             //自动控制函数，返回当前光照值
void ctrlByPhone(int);      //手动控制灯光函数，输入为光强值，无返回值
int readLight();            //读取当前光照强度函数，返回传感器光强值
void ctrlLED(int);          //LED控制函数，输入为光强值
void pwmLED(int);           //pwm控制LED光强函数，输入为光强值

void handleGet();           //服务器处理HTTP_GET请求时执行的函数
void handlePost();          //服务器处理HTTP_POST请求时执行的函数
void handleNotFound();      //服务器处理非法请求时执行的函数

#define BAUDRATE 115200     //串口波特率

//全局变量
const char *AP_SSID="WiFi_ESP32";            //WiFi名称
const char *AP_Password="";                  //WiFi密码，设为空
int light_val = 0;                           //环境光照值
int cur_val = 0;                             //当前PWM参数
String led_mode = "Auto";                    //LED模式默认值，为自动
String led_val = "0";                        //目标PWM参数

/***************************************************************************************************************
 * 主程序
 * *************************************************************************************************************/
//初始化函数
void setup()
{
  Serial.begin(BAUDRATE); //设置波特率

  while (!Serial)
  {
    ;
  }

  //初始化LED
  Wire.begin();
  Wire.beginTransmission(ADDR);
  Wire.write(POWER_ON);
  Wire.endTransmission();
  pinMode(PIN_R, OUTPUT);
  pinMode(PIN_G, OUTPUT);
  pinMode(PIN_B, OUTPUT);

  //初始化WiFi
  WiFi.softAP(AP_SSID,AP_Password);  //设置AP模式热点的名称和密码，密码可不填则发出的热点为无密码热点

  Serial.print("\n The WiFi name create by ESP32: ");
  Serial.println(AP_SSID);  //串口输出ESP32建立的wifi的名称
  Serial.print("\n The WiFi password create by ESP32: ");
  Serial.println(AP_Password);  //串口输出ESP32建立的wifi的名称
  Serial.print("\n The IP Address to ESP32 server: ");
  Serial.println(WiFi.softAPIP());  //串口输出热点的IP地址

  //启动服务器
  esp32_server.begin();  //启动网络服务器
  esp32_server.on("/Data",HTTP_GET,handleGet);  //函数处理当有HTTP请求 "/" 时执行函数 handleGet 
  esp32_server.on("/Data",HTTP_POST,handlePost);  //函数处理当有HTTP请求 "/" 时执行函数 handlePost 
  esp32_server.onNotFound(handleNotFound);  //当请求的网络资源不在服务器的时候，执行函数 handleNotFound 
}

//主函数
void loop()
{
  //检查命令状态
  if(led_mode == "Auto")                 //自动模式
  {
    light_val = ctrlAuto();
  }
  else if(led_mode == "Manual")          //手动调节模式
  {
    int val = atoi(led_val.c_str());
    ctrlByPhone(val);
  }
  else                                   //非法指令
  {
    Serial.println("ERROR!!! LED mode value is not defined. The code is: ");
    Serial.println(led_mode);
  }
  esp32_server.handleClient();
}
/************************************************************************************************************************/

/*********************
 * 自动控制函数
 * 输入值：无
 * 返回值：当前环境光强
 * *******************/
int ctrlAuto()
{
  int val;
  val = readLight();          //读取当前光照值

  //根据当前光照值与预设阈值比较，改变照明强度
  if (val >= LX_VH)
  {
    ctrlLED(0);
    cur_val = 0;
  }
  else if (val >= LX_H)
  {
    ctrlLED(31);
    cur_val = 31;
  }
  else if (val >= LX_M)
  {
    ctrlLED(63);
    cur_val = 63;
  }
  else if (val >= LX_L)
  {
    ctrlLED(127);
    cur_val = 127;
  }
  else
  {
    ctrlLED(255);
    cur_val = 255;
  }
  return val;
}

/************************
 * 远程终端控制函数
 * 输入值：待设定的光照值
 * 返回值：无
 * **********************/
void ctrlByPhone(int val)
{
  ctrlLED(val);
  cur_val = val;
}

/**********************
 * 读取当前光照值
 * 输入值：无
 * 输出值：当前光照值
 * ********************/
int readLight()
{
  int lx = 0; //初始化光照值

  Wire.beginTransmission(ADDR);
  Wire.write(RESET);
  Wire.endTransmission();

  Wire.beginTransmission(ADDR);
  Wire.write(MODE);
  Wire.endTransmission();
  delay(120);
  /*计算光照*/
  Wire.requestFrom(ADDR, 2); //每次2byte
  for (lx = 0; Wire.available() >= 1;)
  {
    char c = Wire.read();
    lx = (lx << 8) + (c & 0xFF);
  }
  lx = lx / 1.2;
  return lx;
}

/********************************
 * LED光强变化控制函数
 * 输入值：设定光强值
 * 返回值：无
 * 说明：该函数用于控制LED渐变光强
 * ******************************/
void ctrlLED(int lx_RGB)
{
  if (cur_val < lx_RGB)
  {
    for (int i = cur_val; i <= lx_RGB; i++)
    {
      pwmLED(i);
      delay(2);
    }
  }
  else if (cur_val > lx_RGB)
  {
    for (int i = cur_val; i >= lx_RGB; i--)
    {
      pwmLED(i);
      delay(2);
    }
  }
}

/***********************************************
 * LED光强控制
 * 输入值：设定的光强值
 * 返回值：无
 * 说明：用以产生pwm波控制光强，控制字范围为0-255
 * *********************************************/
void pwmLED(int lx_RGB)
{
  int lx_R = lx_RGB;             //红光LED脉宽
  int lx_G = lx_RGB / 4;         //绿光LED脉宽
  int lx_B = lx_RGB / 4;         //蓝光LED脉宽
  analogWrite(PIN_R, lx_R);
  analogWrite(PIN_G, lx_G);
  analogWrite(PIN_B, lx_B);
}


/**************************************************
 * GET请求处理
 * 输入值：无
 * 返回值：无
 * 说明：服务器处理HTTP_GET请求时执行
 * 进行一次读取环境光照强度后，将光照值发送到客户端
 * ************************************************/
void handleGet()
{
  Serial.println("Client connect!");
  light_val = readLight();
  char c_val[10] = "";
  itoa(light_val,c_val,10);
  Serial.println("TEST!");
  Serial.println(c_val);
  esp32_server.send(200,"text/plain",c_val);        //向客户端发送当前环境光强值
}

/**********************************************************************
 * POST请求处理
 * 输入值：无
 * 返回值：无
 * 说明：服务器处理HTTP_POST请求时执行
 * 读取socket中“mode”与“val”的值并保存，向客户端发送请求成功的回复
 * ********************************************************************/
void handlePost()
{
  Serial.println("Client connect!");
  String key_mode = "mode";
  String key_val = "val";
  led_mode = esp32_server.arg(key_mode);
  led_val = esp32_server.arg(key_val);
  Serial.print("led_mode: ");
  Serial.println(led_mode);
  Serial.print("led_val: ");
  Serial.println(led_val);
  esp32_server.send(200,"text/plain","OK");         //向客户端发送请求成功的回复
}

/*****************************************************
 * 无效请求处理
 * 输入值：无
 * 返回值：无
 * 说明：服务器收到目录不正确的请求时，返回404报错信息
 * ***************************************************/
void handleNotFound()
{
  Serial.println("undefined request!");
  esp32_server.send(404,"text/plain","404:Not Found!");        //向客户端发送告警
}

