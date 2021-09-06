/*
 * Import URL: https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/??-Driver.groovy"
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

public static String version()      {  return "v0.0.1"  }

@Field static final String device_model = 'WLPA19C' 

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
@Field static final String wyze_property_color_mode_value_ct = '2' 
@Field static final String wyze_property_color_mode_value_rgb = '1' 

import groovy.transform.Field

metadata {
	definition(
		name: "WyzeHub Color Bulb", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: "https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/drivers/wyzehub-meshlight-driver.groovy"
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

	}

}

void installed() {
    parent.logDebug("installed()")

	parent.logDebug("before: " + device.getDataValue('deviceModel'))
	device.updateDataValue('deviceModel', device_model)
	parent.logDebug("after: " + device.getDataValue('deviceModel'))

    initialize()
}

void updated() {
   parent.logDebug("updated()")
   initialize()
}

void initialize() {
   parent.logDebug("initialize()")
   Integer disableMinutes = 30

// TODO?
//    if (enableDebug) {
//       parent.logDebug "Debug logging will be automatically disabled in ${disableMinutes} minutes"
//       runIn(disableMinutes*60, debugOff)
//    }
}

void parse(String description) {
	log.warn("Running unimplemented parse for: '${description}'")
}

def getThisCopyright(){"&copy; 2021 Jake Lehner"}

def refresh() {
	parent.logDebug("Refresh device ${device.label}")
	parent.apiGetDevicePropertyList(device.deviceNetworkId, device.getDataValue('deviceModel')) { propertyList ->
		createDeviceEventsFromPropertyList(propertyList)
	}
}


def on() {
	parent.logDebug("'On' Pressed for device ${device.label}")
	parent.apiRunAction(device.deviceNetworkId, device_model, 'power_on')
	createDeviceEventsFromPropertyList([
		['pid': wyze_property_power, 'value': wyze_property_power_value_on]
	])
}

def off() {
	parent.logDebug("'Off' Pressed for device ${device.label}")
	parent.apiRunAction(device.deviceNetworkId, device_model, 'power_off')
	createDeviceEventsFromPropertyList([
		['pid': wyze_property_power, 'value': wyze_property_power_value_off]
	])
}

def setLevel(level, durationSecs = null) {
	parent.logDebug("setLevel() on device ${device.label}")

	level = level.min(100).max(0)

	actions = [
		[
			'pid': wyze_property_brightness,
			'pvalue': level.toString()
		]
	]

	parent.apiRunActionList(device.deviceNetworkId, device_model, actions)
	createDeviceEventsFromPropertyList([
		['pid': wyze_property_brightness, 'value': level]
	])
}

def setColorTemperature(colortemperature, level = null, durationSecs = null) {
	parent.logDebug("setColorTemperature() on device ${device.label}")

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

	parent.logDebug(actions)

	parent.apiRunActionList(device.deviceNetworkId, device_model, actions)

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
	parent.logDebug("setColor() on device ${device.label}")
	
	hex = hsvToHexNoHash(colormap.hue, colormap.saturation, colormap.level)
	
	parent.logDebug('Setting color to ' + hex)

	actions = [
		[
			'pid': wyze_property_color,
			'pvalue': hex
		]
	]

	parent.apiRunActionList(device.deviceNetworkId, device_model, actions)

	createDeviceEventsFromPropertyList([
		['pid': wyze_property_color_mode, 'value': wyze_property_color_mode_value_rgb],
		['pid': wyze_property_color, 'value': hex]	
	])
}

def setHue(hue) {
	parent.logDebug("setHue() on device ${device.label}")

	// Must be between 0 and 100
	hue = hue.min(100).max(0)
	level = device.currentValue("level")
	saturation = device.currentValue("saturation")

	parent.logDebug([hue, saturation, level])

	hex = hsvToHexNoHash(hue, saturation, level)

	actions = [
		[
			'pid': wyze_property_color,
			'pvalue': hex
		]
	]

	parent.apiRunActionList(device.deviceNetworkId, device_model, actions)	

	createDeviceEventsFromPropertyList([
		['pid': wyze_property_color_mode, 'value': wyze_property_color_mode_value_rgb],
		['pid': wyze_property_color, 'value': hex]	
	])
}

def setSaturation(saturation) {
	parent.logDebug("setSaturation() on device ${device.label}")

	// Must be between 0 and 100
	setSaturation = setSaturation.min(100).setSaturation(0)
	hue = device.currentValue("hue")
	level = device.currentValue("level")

	parent.logDebug([hue, saturation, level])

	hex = hsvToHexNoHash(hue, saturation, level)

	actions = [
		[
			'pid': wyze_property_color,
			'pvalue': hex
		]
	]

	parent.apiRunActionList(device.deviceNetworkId, device_model, actions)

	createDeviceEventsFromPropertyList([
		['pid': wyze_property_color_mode, 'value': wyze_property_color_mode_value_rgb],
		['pid': wyze_property_color, 'value': hex]	
	])	
}

private void createDeviceEventsFromPropertyList(List propertyList) {
    parent.logDebug("createEventsFromPropertyList()")

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
                currentValue = device.currentValue(eventName)
                if (currentValue == null || currentValue != eventValue) {
                    parent.logDebug('Updating Property: colorMode')
                    parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
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
                eventValue = property.value == "1" ? "on" : "off"
                currentValue = device.currentValue(eventName)
				if (currentValue == null || currentValue != eventValue) {
					parent.logDebug('Updating Property: switch')
                    parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
                } 
            break
        
            // Device Online
            case wyze_property_device_online:
                eventName = "online"
                eventUnit = null
                eventValue = property.value == "1" ? "true" : "false"
                currentValue = device.currentValue(eventName)
                if (currentValue == null || currentValue != eventValue) {
                    parent.logDebug('Updating Property: online')
                    parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
                }
            break

            // Brightness
            case wyze_property_brightness:
                eventName = "level"
                eventUnit = '%'
                eventValue = property.value
                
                currentValue = device.currentValue(eventName)
                if (currentValue == null || currentValue != eventValue) {
                    parent.logDebug('Updating Property: level')
                    parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
                }
            break

            // Color Temp
            case wyze_property_color_temp:
				if (deviceColorMode == 'CT') {
					// Set Temperature
					eventName = "colorTemperature"
					eventUnit = '°K'
					eventValue = property.value
					currentValue = device.currentValue(eventName)
					if (currentValue == null || currentValue != eventValue) {
						parent.logDebug('Updating Property: colorTemperature')
						parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}

					// Set HEX Color
					eventName = "color"
					eventUnit = null
					eventValue = null
					currentValue = device.currentValue(eventName)
					if (currentValue == null || currentValue != eventValue) {
						parent.logDebug('Updating Property: color')
						parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}

					// Set Hue Color
					eventName = "hue"
					eventUnit = null
					eventValue = null
					currentValue = device.currentValue(eventName)
					if (currentValue == null || currentValue != eventValue) {
						parent.logDebug('Updating Property: hue')
						parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}

					// Set Saturation
					eventName = "saturation"
					eventUnit = null
					eventValue = null
					currentValue = device.currentValue(eventName)
					if (currentValue == null || currentValue != eventValue) {
						parent.logDebug('Updating Property: saturation')
						parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}
				}   
            break

            // RSSI
            case wyze_property_rssi:
                eventName = "rssi"
                eventUnit = 'db'
                eventValue = property.value
                currentValue = device.currentValue(eventName)
                if (currentValue == null || currentValue != eventValue) {
                    parent.logDebug('Updating Property: rssi')
                    parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
                }
            break

            // Vacation Mode
            case wyze_property_vacation_mode:
                eventName = "vacationMode"
                eventUnit = null
                eventValue = property.value == "1" ? "true" : "false"
                currentValue = device.currentValue(eventName)
                if (currentValue == null || currentValue != eventValue) {
                    parent.logDebug('Updating Property: vacationMode')
                    parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
                }
            break

            // Color
            case wyze_property_color:
				parent.logDebug(deviceColorMode)
				if (deviceColorMode == 'RGB') {
					// Set HEX Color
					eventName = "color"
					eventUnit = null
					eventValue = property.value
					currentValue = device.currentValue(eventName)
					if (currentValue == null || currentValue != eventValue) {
						parent.logDebug('Updating Property: color')
						parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}

					hsv = hexToHsv(property.value)
					parent.logDebug('hsv')
					parent.logDebug(hsv)

					// Set Hue
					eventName = "hue"
					eventUnit = null
					eventValue = hsv[0]
					currentValue = device.currentValue(eventName)
					if (currentValue == null || currentValue != eventValue) {
						parent.logDebug('Updating Property: hue')
						parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}

					// Set Saturation
					eventName = "saturation"
					eventUnit = null
					eventValue = hsv[1]
					currentValue = device.currentValue(eventName)
					// if (currentValue == null || currentValue != eventValue) {
						parent.logDebug('Updating Property: saturation')
						parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					// }

					// Set Temperature
					eventName = "colorTemperature"
					eventUnit = null
					eventValue = null
					currentValue = device.currentValue(eventName)
					if (currentValue == null || currentValue != eventValue) {
						parent.logDebug('Updating Property: colorTemperature')
						parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
					}
				}
            break

            // Vacation Mode
            case wyze_property_vacation_mode:
                eventName = "vacationMode"
                eventUnit = null
                eventValue = property.value == "1" ? "true" : "false"
                currentValue = device.currentValue(eventName)
                if (currentValue == null || currentValue != eventValue) {
                    parent.logDebug('Updating Property: vacationMode')
                    parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
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

private def String hsvToHexNoHash(Integer hue, Integer saturation, Integer level) {
	rgb = hubitat.helper.ColorUtils.hsvToRGB([hue, saturation, level])
	return hubitat.helper.ColorUtils.rgbToHEX(rgb).substring(1)
}
