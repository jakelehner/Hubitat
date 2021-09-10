/*
 * Import URL: https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/src/drivers/wyzehub-meshlight-driver.groovy
 *
 *	Copyright 2021 Jake Lehner
 *
 *	Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *	use this file except in compliance with the License. You may obtain a copy
 *	of the License at:
 *
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 *	Unless required by applicable law or agreed to in writing, software
 *	distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *	WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *	License for the specific language governing permissions and limitations
 *	under the License.
 *
 *
 */

import groovy.transform.Field
import hubitat.helper.ColorUtils

public static String version() { return "v0.0.1"  }

public String deviceModel() { return 'WLPA19C' }

@Field static final String wyze_property_power = 'P3'
@Field static final String wyze_property_device_online = 'P5'
@Field static final String wyze_property_brightness = 'P1501' 
@Field static final String wyze_property_color_temp = 'P1502'
@Field static final String wyze_property_rssi = 'P1504'
@Field static final String wyze_property_remaing_time = 'P1505'
@Field static final String wyze_property_vacation_mode = 'P1506'
@Field static final String wyze_property_color = 'P1507' 
@Field static final String wyze_property_color_mode = 'P1508' 
@Field static final String wyze_property_power_loss_recovery = 'P1509' 
@Field static final String wyze_property_delay_off = 'P1510'
 
@Field static final String wyze_property_power_value_on = '1'
@Field static final String wyze_property_power_value_off = '0'
@Field static final String wyze_property_device_online_value_true = '1'
@Field static final String wyze_property_device_online_value_false = '0'
@Field static final String wyze_property_device_vacation_mode_value_true = '1'
@Field static final String wyze_property_device_vacation_mode_value_false = '0'
@Field static final String wyze_property_color_mode_value_ct = '2' 
@Field static final String wyze_property_color_mode_value_rgb = '1' 

import groovy.transform.Field

metadata {
	definition(
		name: "WyzeHub Color Bulb", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: "https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/src/drivers/wyzehub-meshlight-driver.groovy"
	) {
		capability "Light"
		capability "SwitchLevel"
		capability "ColorTemperature"
		capability "ColorControl"
		capability "ColorMode"
		capability "Refresh"
		// capability "LightEffects"

		// command "toggleVacationMode"
		// command "flashOnce"
		
		attribute "vacationMode", "bool"
		attribute "online", "bool"
		attribute "rssi", "number"
		// attrubute "lastRefreshed", "date"

	}

}

void installed() {
    app.logDebug("installed()")

	refresh()
	initialize()
}

void updated() {
    app.logDebug("updated()")
    initialize()
}

void initialize() {
    app.logDebug("initialize()")

    unschedule('refresh')
    schedule('0/10 * * * * ? *', 'refresh')
}

void parse(String description) {
	log.warn("Running unimplemented parse for: '${description}'")
}

def getThisCopyright(){"&copy; 2021 Jake Lehner"}

def refresh() {
	app = getApp()
	app.logInfo("Refresh device ${device.label}")
	app.apiGetDevicePropertyList(device.deviceNetworkId, deviceModel()) { propertyList ->
		createDeviceEventsFromPropertyList(propertyList)
	}
}

def on() {
	app = getApp()
	app.logInfo("'On' Pressed for device ${device.label}")
	actions = [
		[
			'pid': wyze_property_power,
			'pvalue': wyze_property_power_value_on
		]
	]

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)

	// ToDo Move to Callback
	createDeviceEventsFromPropertyList([
		['pid': wyze_property_power, 'value': wyze_property_power_value_on]
	])
}

def off() {
	app = getApp()
	app.logInfo("'Off' Pressed for device ${device.label}")
	actions = [
		[
			'pid': wyze_property_power,
			'pvalue': wyze_property_power_value_off
		]
	]

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)
	
	// ToDo Move to Callback
	createDeviceEventsFromPropertyList([
		['pid': wyze_property_power, 'value': wyze_property_power_value_off]
	])
}

def setLevel(level, durationSecs = null) {
	app = getApp()
	app.logInfo("setLevel() on device ${device.label}")

	level = level.min(100).max(0)

	actions = [
		[
			'pid': wyze_property_brightness,
			'pvalue': level.toString()
		]
	]

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)
	createDeviceEventsFromPropertyList([
		['pid': wyze_property_brightness, 'value': level]
	])
}

def setColorTemperature(colortemperature, level = null, durationSecs = null) {
	app = getApp()
	app.logInfo("setColorTemperature() on device ${device.label}")

	// Valid range 1800-6500
	colortemperature = colortemperature.min(6500).max(1800)

	actions = [
		[
			'pid': wyze_property_color_temp,
			'pvalue': colortemperature.toString()
		]
	]

	if (level) {
		actions << [
			'pid': wyze_property_brightness,
			'pvalue': level.toString()
		]
	}

	app.logDebug(actions)

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)

	propertyList = [
		['pid': wyze_property_color_mode, 'value': wyze_property_color_mode_value_ct],
		['pid': wyze_property_color_temp, 'value': colortemperature]	
	]

	if (level) {
		propertyList << ['pid': wyze_property_brightness, 'value': level]
	}

	createDeviceEventsFromPropertyList(propertyList)
}

def setColor(colormap) {
	app = getApp()
	app.logInfo("setColor() on device ${device.label}")
	
	hex = hsvToHexNoHash(colormap.hue, colormap.saturation, colormap.level)
	
	app.logDebug('Setting color to ' + hex)

	actions = [
		[
			'pid': wyze_property_color,
			'pvalue': hex
		]
	]

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)

	createDeviceEventsFromPropertyList([
		['pid': wyze_property_color_mode, 'value': wyze_property_color_mode_value_rgb],
		['pid': wyze_property_color, 'value': hex]
	])
}

def setHue(hue) {
	app = getApp()
	app.logInfo("setHue() on device ${device.label}")

	// Must be between 0 and 100
	hue = hue.min(100).max(0)
	currentHsv = hexToHsv(device.currentValue('color'))

	hex = hsvToHexNoHash(hue, currentHsv[1], currentHsv[2])

	actions = [
		[
			'pid': wyze_property_color,
			'pvalue': hex
		]
	]

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)	

	createDeviceEventsFromPropertyList([
		['pid': wyze_property_color_mode, 'value': wyze_property_color_mode_value_rgb],
		['pid': wyze_property_color, 'value': hex]	
	])
}

def setSaturation(saturation) {
	app = getApp()
	app.logInfo("setSaturation() on device ${device.label}")

	// Must be between 0 and 100
	saturation = saturation.min(100).max(0)
	currentHsv = hexToHsv(device.currentValue('color'))

	hex = hsvToHexNoHash(currentHsv[0], saturation, currentHsv[2])

	actions = [
		[
			'pid': wyze_property_color,
			'pvalue': hex
		]
	]

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)

	createDeviceEventsFromPropertyList([
		['pid': wyze_property_color_mode, 'value': wyze_property_color_mode_value_rgb],
		['pid': wyze_property_color, 'value': hex]	
	])	
}

void createDeviceEventsFromPropertyList(List propertyList) {
    app = getApp()
	app.logDebug("createEventsFromPropertyList()")

    String eventName, eventUnit
    def eventValue // could be String or number

    // Feels silly to loop through this twice but we need colorMode early.
    // TODO Better way to search propertyList for element with pid = P1508?
    propertyList.each { property ->
        if(property.pid == wyze_property_color_mode) {
			
            deviceColorMode = (property.value == "1" ? 'RGB' : 'CT')
            
            if (device.hasCapability('ColorMode')) {
                eventName = "colorMode"
                eventUnit = null
                eventValue = deviceColorMode

				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: colorMode')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            }
        }
    }

    propertyList.each { property ->
	
        switch(property.pid) {
            // Switch State
            case wyze_property_power:
				eventName = "switch"
                eventUnit = null
                eventValue = property.value == wyze_property_power_value_on ? "on" : "off"
                
				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: switch')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break
        
            // Device Online
            case wyze_property_device_online:
                eventName = "online"
                eventUnit = null
                eventValue = property.value == wyze_property_device_online_value_true ? "true" : "false"
                
				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: online')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // Brightness
            case wyze_property_brightness:
                eventName = "level"
                eventUnit = '%'
                eventValue = property.value
                
				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: level')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // Color Temp
            case wyze_property_color_temp:
				if (deviceColorMode == 'CT') {
					// Set Temperature
					eventName = "colorTemperature"
					eventUnit = '°K'
					eventValue = property.value
					
					if (device.currentValue(eventName) != eventValue) {
						app.logDebug('Updating Property: colorTemperature')
						app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}
				}   
            break

            // RSSI
            case wyze_property_rssi:
                eventName = "rssi"
                eventUnit = 'db'
                eventValue = property.value

				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: rssi')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // Vacation Mode
            case wyze_property_vacation_mode:
                eventName = "vacationMode"
                eventUnit = null
                eventValue = property.value == wyze_property_device_vacation_mode_value_true ? "true" : "false"

				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: vacationMode')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // Color
            case wyze_property_color:
				app.logDebug(deviceColorMode)
				if (deviceColorMode == 'RGB') {
					// Set HEX Color
					eventName = "color"
					eventUnit = null
					eventValue = property.value

					if (device.currentValue(eventName) != eventValue) {
						app.logDebug('Updating Property: color')
						app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}

					hsv = hexToHsv(property.value)
					app.logDebug('hsv')
					app.logDebug(hsv)

					// Set Hue
					eventName = "hue"
					eventUnit = null
					eventValue = hsv[0]

					if (device.currentValue(eventName) != eventValue) {
						app.logDebug('Updating Property: hue')
						app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}

					// Set Saturation
					eventName = "saturation"
					eventUnit = null
					eventValue = hsv[1]

					if (device.currentValue(eventName) != eventValue) {
						app.logDebug('Updating Property: saturation')
						app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}
				}
            break

            // Vacation Mode
            case wyze_property_vacation_mode:
                eventName = "vacationMode"
                eventUnit = null
                eventValue = property.value == wyze_property_device_vacation_mode_true ? "true" : "false"

				if (device.currentValue(eventName) != eventValue) {
					app.logDebug('Updating Property: vacationMode')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            
        }
    }
}

private def hexToHsv(String hex) {
    if (hex[0] != '#') {
        hex = '#' + hex
    }
	rgb = hubitat.helper.ColorUtils.hexToRGB(hex)
	hsv = hubitat.helper.ColorUtils.rgbToHSV(rgb)
    return hsv
}

private def String hsvToHexNoHash(hue, saturation, level) {
	rgb = hubitat.helper.ColorUtils.hsvToRGB([hue, saturation, level])
	return hubitat.helper.ColorUtils.rgbToHEX(rgb).substring(1)
}

private getApp() {
	app = getParent()
	while(app && app.name != "WyzeHub") {
		app = app.getParent()
	}
	return app
}
