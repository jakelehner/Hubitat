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

@Field static final String wyze_property_push_notifications_enabled = 'P1'
@Field static final String wyze_property_power = 'P3'
@Field static final String wyze_property_device_online = 'P5'
@Field static final String wyze_property_brightness = 'P1501' 
@Field static final String wyze_property_color_temp = 'P1502'
@Field static final String wyze_property_remaing_time = 'P1505'
@Field static final String wyze_property_away_mode = 'P1506'
@Field static final String wyze_property_color = 'P1507' 
@Field static final String wyze_property_control_light = 'P1508' 
@Field static final String wyze_property_power_loss_recovery = 'P1509' 
@Field static final String wyze_property_delay_off = 'P1510' 
 
import groovy.transform.Field

metadata {
	definition(
		name: "Wyze Color Bulb", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: "https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/drivers/wyzehub-meshlight-driver.groovy"
	) {
		capability "Refresh"
		capability "Light"
		capability "SwitchLevel"
		capability "ColorTemperature"
		capability "ColorControl"
		// capability "LightEffects" // TODO
		

		// command "flash"
		// command "flashOnce"
		// command "flashOff"

		attribute "deviceModel", "string"

	}

	preferences 
	{
		
	}
}

void installed() {
    parent.logDebug("installed()")
    initialize()
}

void updated() {
   parent.logDebug("updated()")
   initialize()
}

void initialize() {
   parent.logDebug("initialize()")
   Integer disableMinutes = 30
   if (enableDebug) {
      parent.logDebug "Debug logging will be automatically disabled in ${disableMinutes} minutes"
      runIn(disableMinutes*60, debugOff)
   }
}

void parse(String description) {
	log.warn("Running unimplemented parse for: '${description}'")
}

def getThisCopyright(){"&copy; 2021 Jake Lehner"}

def refresh() {
	parent.logDebug("Refresh device ${device.label}")
}

def on() {
	parent.logDebug("'On' Pressed for device ${device.label}")
	parent.apiRunAction(device.deviceNetworkId, device_model, 'power_on')
	sendEvent(name: "switch", value: "on")
}

def off() {
	parent.logDebug("'Off' Pressed for device ${device.label}")
	parent.apiRunAction(device.deviceNetworkId, device_model, 'power_off')
	sendEvent(name: "switch", value: "off")
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
	sendEvent(name: "level", value: level)
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

	parent.apiRunActionList(device.deviceNetworkId, device_model, actions)

	sendEvent(name: "colorTemperature", value: colortemperature)
	sendEvent(name: "color", value: null)
	sendEvent(name: "hue", value: null)
	sendEvent(name: "saturation", value: null)
	sendEvent(name: "level", value: null)
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

	sendEvent(name: "colorTemperature", value: null)
	sendEvent(name: "color", value: hex)
	sendEvent(name: "hue", value: colormap.hue)
	sendEvent(name: "saturation", value: colormap.saturation)
	sendEvent(name: "level", value: colormap.level)

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
	sendEvent(name: "hue", value: hue)
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
	sendEvent(name: "saturation", value: saturation)	
}

private def hsvToHexNoHash(hue, saturation, level) {
	rgb = hubitat.helper.ColorUtils.hsvToRGB([hue, saturation, level])
	return hubitat.helper.ColorUtils.rgbToHEX(rgb).substring(1)
}
