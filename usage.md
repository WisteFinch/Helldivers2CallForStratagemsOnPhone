<div align="center">
  
# Usage

[**English**](./usage.md) | [**中文**](./usage_zh_CN.md)

</div>

# 1 Usage

## 1.1 Download and run

Go to the [download page](https://github.com/WisteFinch/Helldivers2CallForStratagemsOnPhone/releases/latest), and choose the latest version.

### 1.1.1 PC server

- Download `call-for-stratagem-server-x.x.x.exe` and run it, the system must be at least Windows 10 x64(Haven't tested the lower version).
- For Linux/Mac, please refer to the [Build](./README.md#build) entry to compile on your own.

### 1.1.2 Phone client

- Downlaod `call-for-stratagem-client-x.x.x.apk` and install it, the system must be at least Android 8.0.

## 1.2 Connection

### 1.2.1 Connect and test

1. Ensure that the phone and computer are on the same network and have a smooth network connection.
2. Open the server and record the displayed IP and port information after successful startup. e.g. `Listening: 192.169.1.2:23333`, the IP is `192.168.1.2` and the port is `23333`.
3. Open the client and go to the `Settings>Connection` section.
    - Manual setting: Fill in the input box with the recorded IP and port information.
    - Scan QR code: Click the `Scan QR Code` button to take a picture of the QR code displayed on the computer, and the configuration will be automatically obtained.
4. Click the `Test Connection` button until it prompts `Success`.
    - After `v0.3.0` we added authentication to prevent malicious attacks: When connecting for the first time or every three days, the server will ask the user to confirm the connection and display `Authentication request from: XXX, sid: XXX. Do you want to authenticate this client? (Y/N):`. Enter `Y` to accept the request.

## 1.3 Configuration

After `v0.3.0`, the application configuration needs to be manually confirmed to prevent malicious attacks: After clicking the `Apply Configuration` button, the server will display `Synchronization request from: XXX.Do you want to synchronize this configuration? (Y/N):`. enter ` Y ` to accept the request.

### 1.3.1 Change port

Suggest modifying this value only when the port is occupied.

1. Open the server and ensure that the client can connect to the server normally.
2. Open the client, go to `Settings>Sync>Port`, and modify it to any value from `1` to `65535`.
3. Click the `Apply Configuration` button below, and then restart the server.

### 1.3.2 Change input speed

This value represents the delay between pressing two buttons during call stratagems, which can affect the execution speed of macro and normal input. If the computer frame rate is too low and the call fails, you can try increasing this value.

1. Open the server and ensure that the client can connect to the server normally.
2. Open the client, go to `Settings>Sync>Input Interval Time`, and modify it to the desired value (in milliseconds, default 25).
3. Click the `Apply Configuration` button below, and then restart the server.

### 1.3.3 Change input mapping

1. Open the server and ensure that the client can connect to the server normally.
2. Open the client, go to `Settings>Sync` section, and modify it to the desired value (in milliseconds, default 25).
3. Click `Input: XXX`, modify it to the desired value.
4. Click the `Apply Configuration` button below, and then restart the server.

## 1.4 Control

### 1.4.1 Enable simplified mode

This feature was added in `v0.2.1`: The simplified mode only retains the macro function and removes most of the decorations, making it suitable for users who only need macros.

1. Open the client, go to `Settings>Control>Enable Simplified Mode`, click the toggle switch

### 1.4.2 Adjust the sensitivity of swipe gestures

If you encounter difficulties in recognizing swipe gestures during input, you can lower this value appropriately.

1. Open the client, go to `Settings>Control` section.
2. Adjust `Gesture Swipe Distance Threshold`(unit is pixels, default is 100). This value represents the minimum gesture swiping distance that can be recognized.
3. Adjust `Gesture Swipe Velocity Threshold`(unit is pixels per second, default is 100). This value represents the minimum gesture swiping speed that can be recognized.

## 1.5 Group

### 1.5.1 Create stratagem group

Group is a set of user selected stratagems. It is recommended to prepare corresponding groups for different battles, which is beneficial for quickly selecting the required stratagems during combat.

1. Open the client and click the plus sign (`+`) button in the bottom right corner of the main interface.
2. enter the `Edit Stratagem Group` interface, choose a suitable name for the group, and then check the required combat readiness in the list below.
3. Click the save button in the upper right corner to save the group.

### 1.5.2 Edit stratagem group

1. Open the client, click on the group you want to edit, and enter the `Browse Stratagem Group` interface.
2. Click `...` on the top right corner, then click the `Edit` button in the pop-up menu.
3. Enter the 'Edit Stratagem Group' interface, where you can modify the group name and check the required stratagems in the list below.
4. Click the save button in the upper right corner to save the group.

### 1.5.2 Delete stratagem group

This operation is irreversible, please choose carefully.

1. Open the client, click on the group you want to edit, and enter the `Browse Stratagem Group` interface.
2. Click `...` on the top right corner, then click the `Delete` button in the pop-up menu.

## 1.6 Play

### 1.6.1 Play stratagem group

1. Open the client and click on the desired group to enter the `Browse Stratagem Group` interface.
2. Click `play(▶)` button on the bottom right corner, enter the `Play` interface.
3. In this interface, the top left corner is the `Exit` button, the top right corner is the `Free Input Mode` button, the right side is the `Stratagems List`, the bottom left is the `Connection Status`, and the middle is the `Information Area`.

- Normal input: When a stratagem is selected in the `Stratagems List`, the `Information Area` will display the stratagem information and enable gesture input. Need to swipe according to prompts to activate stratagem.
- Macro: Swipe the stratagem icon to the left to activate stratagem.
- Free Input: Click the `Free Input Mode` button to enable it, which is suitable for random input situations such as operating terminals. To call for stratagem under this mode, you need to manually press the `Open Stratagems List` button in the game.
- The `Connection Status` indicates the current connection status with the server, with green indicating success, yellow indicating connection in progress, and red indicating connection failure. When attempting to activate stratagem in the event of failure, it will immediately attempt to reconnect.

### 1.6.2 Simplified mode

This feature was added in `v0.2.1`: The simplified mode only retains the macro function and removes most of the decorations, making it suitable for users who only need macros.

1. Refer to [1.4.1-Enable simplified mode](#141-enable-simplified-mode), and turn on simplified mode.
2. Enter `Play` interface and only the stratagem icons and `Connection Status` remain. Click on the icon to activate the stratagem.

## 1.7 Database

After `v0.4.0`, the app no longer has a built-in stratagem database, but is downloaded from the network to ensure that the database is always the latest version.

### 1.7.1 Update database

1. Open the client, go to `Settings>Info>Database Version`, and wait for the app to check for the new version.
2. If there is a new version, the title will change to `Database Version (Updatable)`, click to update it.

# 2 FAQ

## 2.1 Connection

### 2.1.1 The client cannot connect to the server

1. Both mobile phones and computers must be on the same network. Campus and enterprise networks may have AP isolation enabled, preventing devices from discovering each other. It is recommended to switch to a different network or use a hotspot.
2. Check if the server address and port are filled in correctly.
3. Turn off network proxy and VPN, disable virtual network adapter.
4. Check the firewall.
5. Restart the computer.

## 2.2 Database

### 2.2.1 Database incomplete

When encountering the error prompt `Database Incomplete`, or discovering situations such as missing stratagems or icon display error, please refer to [1.7.1-Update database](#171-update-database) for updates.
