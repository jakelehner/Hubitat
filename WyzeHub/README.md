# WyzeHub

This is an _unofficial_ Hubitat implementation for Wyze devices. The app offers basic control over supported devices and device groups.

## Supported Devices
Support is currently limiited to devices I have on hand. Devices will be added as I get them or as the community contributes.

### Currently Supported
* Wyze Color Bulb (Meshlight)
* Wyze Plug

## To be implemented...

* Device Group Update - currenttly after a device group is added, changes to device membership in Wyze are not reflected in Hubitat.
* Outdoor Plug - I have one but it is acting up.
* Camera Support?

## Installation w/ Hubitat Package Manager (Recommended)

In process of submitting...

## Installtion (Manual Method)
1. Back up your hub or live dangerously.

2. Install the App. In Hubitat, access the _Apps Code_ menu. Click _New App_ then _Import_. Paste the RAW URL to the WyzeHub App: 
   * https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/apps/wyzehub-app.groovy


3. Install the Drivers. In Hubitat, access the _Drivers Code_ menu. Click _New Driver_ then _Import_. Paste the URL for a driver listed below. Repeat for all drivers.
    * Wyze Color Bulb: 
      * https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/drivers/wyzehub-meshlight-driver.groovy
    * Wyze Color Bulb Group:
      * https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/drivers/wyzehub-meshlight-group-driver.groovy
    * Wyze Plug:
      * https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/drivers/wyzehub-plug-driver.groovy
    * Wyze Plug Group:
      * https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/drivers/wyzehub-plug-group-driver.groovy
      
4. Install an instance of the WyzeHub app. In Hubitat, go to _Apps > Add User App_, select _WyzeHub_, and follow the prompts to enter your credentials and import devices.

## Contributing
Contributions are welcome if you'd like to see additional devices supported or expanded features. Please do so via a pull request:

1. Fork this repository.
2. Add your driver or functionlaity.
   1. Name the device somehting accurate so to be accurate and recognizable (i.e. 'Camera v2' vs 'Camera v3')
   2. Take care to follow formatting, code styling, and general "code feel" as best as poossible.
   3. Keep the same license (if submitting back to this repo).
3. Test the hell out of your code.
4. Submit a Pull Request back to the `master` branch.
