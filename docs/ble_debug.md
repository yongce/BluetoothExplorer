# BLE Debug

## 调试日志

调试设备：

- Client/Server：Mi 5S，MIUI 10.2，Android 8.0
- Server/Client：Nexus 5X，Android 8.1

## 升级后真机验证清单

建议至少准备两台设备，一台运行 Phone app 作为 client/scanner，另一台运行 Phone 或 Wear app 作为 advertiser/server。
如条件允许，分别覆盖 Android 8-11 和 Android 12+ 设备，确认旧定位权限和新 Bluetooth 运行时权限都能正常工作。

### 基础权限与开关

- 首次进入 BLE Scanner，确认权限弹窗出现；Android 12+ 应请求 Bluetooth scan/connect 相关权限，Android 11 及以下应请求定位权限。
- 拒绝权限后点击 Scan，不应崩溃，日志中应出现无 BLE scan 权限的提示。
- 授予权限后关闭系统蓝牙，再点击 Scan/Connect/Advertise，应跳转系统蓝牙开启界面或给出状态提示，不应抛出 SecurityException。
- 手动在系统设置中撤销 Nearby devices/Location 权限，再次执行扫描、连接、广播、断开连接，应用应保持可操作并输出权限不足日志。

### 扫描与连接

- 在 Server 设备启动 BLE Advertiser 后，Client 设备打开 BLE Scanner，确认能扫描到目标广播并能按 filter 切换结果。
- 打开 Only named devices，确认无名称设备会被过滤；Android 12+ 设备名可能来自 scan record，不能依赖 BluetoothDevice#name 一定可读。
- 选择目标设备进入 BLE Client，执行 Connect，确认状态依次进入 CONNECTING/CONNECTED，并完成 service discovery。
- 执行 Read Data 和 Send Data，确认服务端能收到写入数据，客户端能收到读取或通知结果。
- 执行 Disconnect/返回页面，重复连接 5 次，确认没有出现 “onClientRegistered - status=133 clientIf=0”。

### 广播与 GATT server

- 在 Advertiser 页面启动广播，确认日志中没有 BLUETOOTH_ADVERTISE 权限异常。
- Client 连接后，Server 端应收到 BluetoothGattServerCallback#onConnectionStateChange。
- Client 订阅通知后，Server 发送数据，确认 BluetoothGattServer#notifyCharacteristicChanged 成功并触发 onNotificationSent。
- 关闭广播/GATT server 后重新启动，确认可以再次被扫描和连接。

### 配对与地址变化

- 对未配对设备执行 Pair，确认系统配对弹窗、配对状态广播、页面状态更新正常。
- 配对后重新扫描，记录新旧 device address；如旧地址连接失败，重新扫描后使用新地址连接。
- 取消配对后重新扫描并连接，确认不会因为旧缓存地址导致持续 GATT_ERROR。

### 日志观察点

- `No Bluetooth connect permission`、`No BLE scan permissions`、`No Bluetooth advertise permission` 只应在权限缺失时出现。
- `GATT_STATUS_UNKNOWN-[0x85]` 多半与地址变化或设备状态有关，优先重新扫描再连接。
- `onClientRegistered - status=133 clientIf=0` 表示 GATT client 未释放，优先检查是否执行 BluetoothGatt#close()。
- `SecurityException` 不应出现在扫描、连接、广播、GATT response、notify、read/write 等主流程日志中。

### 扫描BLE设备

BLE协议为了保护用户隐私，所有BLE设备暴露出去的device address都是一个随机地址，会定期变化（一般15分钟左右，叠加一个随机值）。
代码是无法获取BLE设备的真实蓝牙地址，也无法获取自己的随机device address。
参考 [Bluetooth Technology Protecting Your Privacy][ble_privacy]

在通过 android.bluetooth.le.BluetoothLeAdvertiser#startAdvertising() 发送广播时，会立即更新当前设备的device address（即每次调用都不一样）。
这点，可以通过 android.bluetooth.le.BluetoothLeScanner() 的扫描结果观察到。

> 注1：已经与当前设备配对的设备，将不会出现在BLE扫描结果中，需要自己记录下已配对的设备信息（device name & device address）。
> 也可通过 android.bluetooth.BluetoothAdapter#getBondedDevices()来获得已配对设备列表，通过设备名特征来识别特定设备。

> 注2：在某些情况下，即使已经与目标设备配对，也可以在BLE扫描中发现目标设备。

### 配对BLE设备

当通过BLE扫描到设备后，可通过 android.bluetooth.BluetoothDevice#createBond() 来与目标设备配对。
当配对完成后，在发起的设备上会出现两个已配对的目标设备：其中一个是配对前的device address，另一个是新的device address。而在目标设备上，仅一个已配对设备。
这两个目标设备的device address不再变化。不确定是否所有设备上都会有此现象。

配对完成后，都可以通过 android.bluetooth.BluetoothDevice#connectGatt() 与这两个device address建立BLE连接。但有如下差异：

- 通过旧device address进行连接和断开连接（即使只执行 BluetoothGatt#disconnect()，不执行 BluetoothGatt#close()），
会分别在目标设备的 BluetoothGattServerCallback 中收到BLE连接成功和BLE连接断开的事件。
- 通过新device address进行连接和断开连接（即使执行 BluetoothGatt#close()），在目标设备只会收到一次连接成功的事件（即不会发生真正的断连）。
此外，每次的连接速度很快（应该是没有真正断连的原因）。

在某个时刻，无法再通过旧device address进行连接（GATT_ERROR，0x85），原因不明（但始终可以通过新device address进行连接）:

- 关闭/重新开启当前设备的蓝牙后，依然无法连接成功（同样错误）。
- 目标设备关闭/重新开启GATT server/BLE advertiser，依然无法连接成功（同样错误）。
- 取消配对，重新通过BLE扫描去查找设备，并进行BLE连接，会连接成功（此时device address已经改变），但随即会自动触发系统的配对对话框。
取消此对话框，BLE连接会断开（GATT_STATUS_UNKNOWN-[0x16]）。再次连接，会成功。
- 怀疑此问题跟BLE device address的变化有关。

### 连接BLE设备

需要建立BLE连接时，每次都使用 BluetoothDevice#connectGatt() 进行连接，不要使用 BluetoothGatt#connect() 来恢复连接，
否则很可能出现没有 BluetoothGattCallback 回调的情况）。

由于BLE设备的device address会经常变化，可能会导致一段时间后，无法再恢复连接。需要重新进行BLE扫描，用新的device address来连接。


### BLE数据处理

在 BluetoothGattCallback#onCharacteristicChanged() 回调中，需要即时处理 BluetoothGattCharacteristic 对象中的数据。
否则，其中的数据可能会被后面的数据更新掉。例如，如果想异步处理收到的数据，则需要先从 BluetoothGattCharacteristic 对象中取出数据，
再进行异步处理。

### BLE广播

BLE广播数据包支持多种数据类型，可以有Service UUID，Service Data，Manufacturer Data等，但最大仅能容纳31字节。
虽然可以通过Service Data, Manufacturer Data等多种匹配方式进行BLE扫描，但还是需要在广播包中添加Service UUID，
否则会导致扫到了设备，但无法发现Service。

## 一些错误

### onClientRegistered - status=133

如果在日志中发现 “BluetoothGatt/D onClientRegistered() - status=133 clientIf=0”，表明有 BluetoothGatt 连接没有被关闭，
导致达到了可建立GATT连接的上限（我测试的Android 8.0系统为clientIf=32）。
此时，需要检查代码，确保所有 BluetoothGatt 对象在不需要时调用了其 #close() 方法。

### onClientConnectionState - status=133

如果设备未配对，进行BLE连接时总是出现此错误，原因可能是目标设备的device address改变了，需要重新进行BLE扫描，然后再尝试连接。

如果remote device是Android N或者更高版本，也可能遇到这个问题。
使用 BluetoothDevice.TRANSPORT_LE 参数调用 BluetoothDevice#connectGatt() 可解决此问题。

## 16/32位UUID

由于标准的128位UUID有16字节，会占用较多数据空间（例如，BLE GATT advertise数据包总共才31字节）。
为此，SIG添加了两类特殊的UUID格式：16位和32位UUID（所有SIG定义的标准蓝牙UUID都采用这类特殊格式）。
并且，SIG定义了16位/32位UUID与128位UUID的换算关系：

* 16位UUID： 0000xxxx-0000-1000-8000-00805f9b34fb
* 32位UUID： xxxxxxxx-0000-1000-8000-00805f9b34fb

参见 [Service Discovery][bluetooth_service_discovery]

目前仅分配了16位UUID。SIG会员企业也可以花钱申请16位UUID，参见 [16 Bit UUIDs For Members][bluetooth_uuid_16bits]


[ble_privacy]: https://blog.bluetooth.com/bluetooth-technology-protecting-your-privacy
[bluetooth_service_discovery]: https://www.bluetooth.com/specifications/assigned-numbers/service-discovery
[bluetooth_uuid_16bits]: https://www.bluetooth.com/specifications/assigned-numbers/16-bit-uuids-for-members
