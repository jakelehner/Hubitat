/*
 * Import URL: https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/drivers/wyzehub-meshlight-driver.groovy
 *
 * DON'T BE A DICK PUBLIC LICENSE
 *
 * Version 1.1, December 2016
 *
 * Copyright (C) 2021 Jake Lehner
 * 
 * Everyone is permitted to copy and distribute verbatim or modified
 * copies of this license document.
 * 
 * DON'T BE A DICK PUBLIC LICENSE
 * TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION
 *
 * 1. Do whatever you like with the original work, just don't be a dick.
 * 
 *    Being a dick includes - but is not limited to - the following instances:
 *
 *    1a. Outright copyright infringement - Don't just copy this and change the name.
 *    1b. Selling the unmodified original with no work done what-so-ever, that's REALLY being a dick.
 *    1c. Modifying the original work to contain hidden harmful content. That would make you a PROPER dick.
 *
 * 2. If you become rich through modifications, related works/services, or supporting the original work,
 *    share the love. Only a dick would make loads off this work and not buy the original work's
 *    creator(s) a pint.
 * 
 * 3. Code is provided with no warranty. Using somebody else's code and bitching when it goes wrong makes
 *    you a DONKEY dick. Fix the problem yourself. A non-dick would submit the fix back.
 *
 */

import groovy.transform.Field
import hubitat.helper.ColorUtils

public static String version() { return "v1.0.2"  }

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
		importUrl: "https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/drivers/wyzehub-meshlight-driver.groovy"
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
    logDebug("installed()")

	refresh()
	initialize()
}

void updated() {
    logDebug("updated()")
    initialize()
}

void initialize() {
    logDebug("initialize()")

    unschedule('refresh')
    schedule('0/10 * * * * ? *', 'refresh')
}

void parse(String description) {
	log.warn("Running unimplemented parse for: '${description}'")
}

def getThisCopyright(){"&copy; 2021 Jake Lehner"}

def refresh() {
	app = getApp()
	logInfo("Refresh Device")
	app.apiGetDevicePropertyList(device.deviceNetworkId, deviceModel()) { propertyList ->
		createDeviceEventsFromPropertyList(propertyList)
	}
}

def on() {
	app = getApp()
	logInfo("'On' Pressed for device ${device.label}")
	actions = [
		[
			'pid': wyze_property_power,
			'pvalue': wyze_property_power_value_on
		]
	]

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)
}

def off() {
	app = getApp()
	logInfo("'Off' Pressed for device ${device.label}")
	actions = [
		[
			'pid': wyze_property_power,
			'pvalue': wyze_property_power_value_off
		]
	]

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)
}

def setLevel(level, durationSecs = null) {
	app = getApp()
	logInfo("setLevel() on device ${device.label}")

	level = level.min(100).max(0)

	actions = [
		[
			'pid': wyze_property_brightness,
			'pvalue': level.toString()
		]
	]

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)
}

def setColorTemperature(colortemperature, level = null, durationSecs = null) {
	app = getApp()
	logInfo("setColorTemperature() on device ${device.label}")

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

	logDebug(actions)

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)
}

def setColor(colormap) {
	app = getApp()
	logInfo("setColor() on device ${device.label}")
	
	hex = hsvToHexNoHash(colormap.hue, colormap.saturation, colormap.level)
	
	logDebug('Setting color to ' + hex)

	actions = [
		[
			'pid': wyze_property_color,
			'pvalue': hex
		]
	]

	app.apiRunActionList(device.deviceNetworkId, deviceModel(), actions)
}

def setHue(hue) {
	app = getApp()
	logInfo("setHue() on device ${device.label}")

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
}

def setSaturation(saturation) {
	app = getApp()
	logInfo("setSaturation() on device ${device.label}")

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
}

void createDeviceEventsFromPropertyList(List propertyList) {
    app = getApp()
	logDebug("createEventsFromPropertyList()")
	logDebug(propertyList)

    String eventName, eventUnit
    def eventValue // could be String or number

    // Feels silly to loop through this twice but we need colorMode early.
    // TODO Better way to search propertyList for element with pid = P1508?
    propertyList.each { property ->
	
		propertyValue = property.value ?: property.pvalue ?: null
        if(property.pid == wyze_property_color_mode) {
			
            deviceColorMode = (propertyValue == "1" ? 'RGB' : 'CT')
            
            if (device.hasCapability('ColorMode')) {
                eventName = "colorMode"
                eventUnit = null
                eventValue = deviceColorMode

				if (device.currentValue(eventName) != eventValue) {
					logInfo('Updating Property: colorMode')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            }
        }
    }

    propertyList.each { property ->
	
		propertyValue = property.value ?: property.pvalue ?: null
		switch(property.pid) {
            // Switch State
            case wyze_property_power:
				eventName = "switch"
                eventUnit = null
                eventValue = propertyValue == wyze_property_power_value_on ? "on" : "off"
                
				if (device.currentValue(eventName) != eventValue) {
					logInfo('Updating Property: switch')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break
        
            // Device Online
            case wyze_property_device_online:
                eventName = "online"
                eventUnit = null
                eventValue = propertyValue == wyze_property_device_online_value_true ? "true" : "false"
                
				if (device.currentValue(eventName) != eventValue) {
					logInfo('Updating Property: online')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // Brightness
            case wyze_property_brightness:
                eventName = "level"
                eventUnit = '%'
                eventValue = propertyValue
                
				if (device.currentValue(eventName).toString() != eventValue.toString()) {
					logInfo("Updating Property: level - ${device.currentValue(eventName)} != ${eventValue}")
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // Color Temp
            case wyze_property_color_temp:
				if (deviceColorMode == 'CT') {
					// Set Temperature
					eventName = "colorTemperature"
					eventUnit = '°K'
					eventValue = propertyValue
					
					if (device.currentValue(eventName).toString() != eventValue.toString()) {
						logInfo("Updating Property: colorTemperature - ${device.currentValue(eventName)} != ${eventValue}")
						app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}
				}   
            break

            // RSSI
            case wyze_property_rssi:
                eventName = "rssi"
                eventUnit = 'db'
                eventValue = propertyValue

				if (device.currentValue(eventName) != eventValue) {
					// Leaving this one on Debug since it's updated almost every time
					logDebug('Updating Property: rssi')
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // Color
            case wyze_property_color:
								
				if (deviceColorMode == 'RGB') {
					// Set HEX Color
					eventName = "color"
					eventUnit = null
					eventValue = propertyValue

					if (device.currentValue(eventName) != eventValue) {
						logInfo('Updating Property: color')
						app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}

					hsv = hexToHsv(propertyValue)
					
					// Set Hue
					eventName = "hue"
					eventUnit = null
					eventValue = hsv[0]

					if (device.currentValue(eventName) != eventValue) {
						logInfo('Updating Property: hue')
						app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}

					// Set Saturation
					eventName = "saturation"
					eventUnit = null
					eventValue = hsv[1]

					if (device.currentValue(eventName) != eventValue) {
						logInfo('Updating Property: saturation')
						app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}
				}
            break

            // Vacation Mode
            case wyze_property_vacation_mode:
                eventName = "vacationMode"
                eventUnit = null
                eventValue = propertyValue == wyze_property_device_vacation_mode_true ? "true" : "false"

				if (device.currentValue(eventName) != eventValue) {
					logInfo('Updating Property: vacationMode')
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

private void logDebug(message) {
	app = getApp()
	app.logDebug("[${device.label}] " + message)
}

private void logInfo(message) {
	app = getApp()
	app.logInfo("[${device.label}] " + message)
}

private void logWarn(message) {
	app = getApp()
	app.logWarn("[${device.label}] " + message)
}

private void logError(message) {
	app = getApp()
	app.logError("[${device.label}] " + message)
}
