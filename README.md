
## 通过netty实现自定义协议物联网网关（单机和集群版）
最新项目地址：https://gitee.com/willbeahero/IOTGate
window笔记本电脑本地测试：**单网关**、**单前置节点**，每秒处理并发心跳6000+（根据jmeter本地最新压测统计数据），20W在线终端内存占用量1G左右

### 心跳检测
终端超过300秒无应答，则网关主动关闭通道，并清理缓存
### 高低水位参数
目前设置的网关与前置channel通讯的channel高低水位为，低水位32M，高水位64M。
new WriteBufferWaterMark(32 * 1024 * 1024, 64 * 1024 * 1024)
设置水位线的作用是：防止服务器处理能力极其低下但连接正常时，造成channel中缓存大量数据影响网关性能

***
### 入口类:
	Entrance.java
### API地址
	https://apidoc.gitee.com/willbeahero/IOTGate
### 命令行参数说明

|      参数      | 是否必选 |是否含参| 含义 |
|------------- |----------|----------|----------|
| 		-n	   |	是  | 是  | 网关编号  |
| 		-c	   |	否  | 否  | 启动集群模式  |
| 		-z	   |	否  | 是  | zookeeper集群地址  |
| 		-m	   |	否  | 是  | 前置ip地址(不含端口，前置默认8888)  |
| 		-f	   |	是  | 是  | 配置文件"iotGate.conf"的本地全路径   |

### 如何启动
自行将项目打成jar包，在linux下，执行java -jar  -n  1 [args...] iotGate.jar   默认前置端口为8888，可自行源码中修改
 - 单机方式启动 ：命令行参数使用“-m”指定前置服务地址
 - 集群方式启动：命令行参数“-c”开启zookeeper集群模式，“-z”指定zookeeper集群的地址（逗号分隔）
### 自定义网关头结构与注意事项
 
 网关报头，是网关与前置通信时，作为网关登录和传输真实报文时携带网关自身和终端响应参数的报文，报文结构是自己定义，前置按照定义好的报文格式获取数据并做相应处理。
 网关头结构如下：
 
| 报文属性      | 字节数| 含义 |
|------------- | ------------- | ----------|
| 		A8			|		 1byte | 报文头 |
| 		len			|	     2byte | 长度域：真实报文长度，包含“68”，“16”|
| 		type		|		 1byte | 报头类型|
| 		protocolType|	     1byte | 协议类型（左侧起第一个bit为0 表示IPV4, 1表示IPV6  剩余7个bit表示规约类型编号）|
| 		gateNum		|         1byte | 网关编号|
| 		00*12		|		 12byte |如果ip格式为IPV4，则当前为12字节0，反之，当前得12个byte+后续得4byte存放IPV6的值，存放顺序从左至右依次|
| 		clientIP	|		 4byte | 终端的IP地址，ip地址的每个段位占一个字节（不含符号和端口号）|
|		port		|     2byte | 终端对应的端口号|
|		count		|     4byte | 终端与网关建立连接时对应的连接序号（1-10000循环）|
 		
网关发送需要向前置发送登录报文，将自己注册到前置服务中，报文说明如下：

	 1. 登录时长度为0；有真实报文时，长度域为整个真实报文长度值
	 2. 登录时type = 03；protocolType=15;count=1都为固定值；发送真实报文时，type=01,protocolType=00;count=终端与网关的连接序号
	 3. 前置发现报头长度为0 且type = 03; protocolType=15;就不会执行解析数据的方法，否则会继续解析真实报文


***************************************************************************************************************************

### 目前网关支持的真实报文
	*“真实报文”即终端与网关通信时传输数据的报文，规约不同则报文结构差异明显

| 报文属性      | 字节数| 含义 |
|------------- | ------------- | ----------|
|68			|		 1byte  |   报头|
|		len			|		  2byte  |   长度域 "传输帧中除起始字符和结束字符之外的帧字节数"|
|		...				|	  n byte |  报文内容 |
|		16            |        1byte  |   报尾|
		
***
### 版本规划
- single Node 版本为单机版网关程序-不支持集群
- IOTGate-v1.0 版本为集群版网关程序，通过命令行参数动态配置网关为单节点或集群  网关与前置通讯时默认轮询方式负载均衡
- master 正在开发中......
### 关于多规约支持
由于物联网火起来没多久，目前业内还没有比较统一的通讯规约，MQTT是目前业界使用比较广泛的物联网通讯协议了，我看了下阿里最近弄出来的物联网平台其主要也是支持MQTT物联网设备的接入，但他毕竟不是唯一的通讯规约，光我自己知道的通讯比较完善的国家级规约都有好几种了，而且每一种规约的报文可是都各不相同，因此，所谓多规约支持，也不可能做到所有物联网规约全支持，我目前的想法是，将比较流行的使用比较多的物联网规约先支持！
那么如何实现多规约呢？
从业务的角度上说，只需要一台服务器，部署一套网关程序，就能支持多种规约类型的物理设备的接入，当然了，前提是网关的并发性能不能受太大影响（不可能不受影响）！
目前多规约的架子已经出来了，一个网关程序理论最大支持规约类型128种  可用规约编号区间[0,127]---左闭右闭

### GATE CLUSTER 结构图
![集群版IOTGate架构](https://images.gitee.com/uploads/images/2019/0125/162345_24e4fa28_1038477.png "绘图1.png")
注：GATE CLIENT（项目名称“IOTGateConsole”，项目在我的码云首页可见--正在开发中） 是一个web工程，用户登录之后可以查看当前GATE CLUSTER的运行状态监控，并可执行网关重启、关闭、启动，网关多规约支持策略等操作



### 联系方式（有问题请发邮件沟通或者直接项目下发留言）
邮箱：1012702024@qq.com

### 补充一点说明
这个项目我会结合我实际项目中遇到的问题，尽量少留坑，码代码大家都会，这个项目的目的绝对不是炫自己的代码而主要是想提供一个开源的集群网关解决方案，后续代码发布速度会有点慢，希望大家持续关注，欢迎大家多多star，star越多我的动力越足 哈哈！
关于有的同学关于文档的提问，目前希望大家多看代码注释，多多自己理解，毕竟项目刚开始，这个时候不太适合做文档编写！
