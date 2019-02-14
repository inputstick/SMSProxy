# SMS Proxy
SMS Proxy listens to incoming SMS messages and relies them to InputStick KP2A Plugin.

### Why do I need SMS Proxy?
InputStick KP2A Plugin is no longer allowed to read SMS messages due to new Google Play Store regulations. That is why external application, distributed outside of Play Store is necessary.

### Do I need SMS Proxy?
You should install this app only if you want to use InputStick KP2A Plugin to type codes received via SMS. Codes can be also copied via clipboard but that method is not as convenient as direct access to SMS.

### Is it secure?
The app is open source and has no Internet or local storage access. Received SMS are never stored. The app listens to SMS only when requested to do so by InputStick KP2A Plugin. The plugin verifies if received data is signed by key shared between the plugin and SMS Proxy to prevent other app from spoofing SMS message.