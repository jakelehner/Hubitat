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

public static String version()      {  return "v0.0.1"  }

@Field static final String device_model = 'WLPA19C' 

@Field static final String wyze_property_push_notifications_enabled = 'P1'
@Field static final String wyze_property_power = 'P3'
@Field static final String wyze_property_device_online = 'P5'
@Field static final String wyze_property_brightness = 'P1501' 
@Field static final String wyze_property_color_temp = 'P1502'
@Field static final String wyze_property_color = 'P1507' 
@Field static final String wyze_property_control_light = 'P1508' 
@Field static final String wyze_property_power_loss_recovery = 'P1509' 

// const WYZE_API_REMAINING_TIME = 'P1505'
// const WYZE_API_AWAY_MODE = 'P1506'
// const WYZE_API_DELAY_OFF = 'P1510'

colorMode = 'RGB'
 
import groovy.transform.Field

metadata {
	definition(
		name: "Wyze Color Bulb", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: "https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/drivers/wyzehub-meshlight-driver.groovy"
	) {
		capability "Light"
		capability "SwitchLevel"
		capability "ColorTemperature"
		// capability "ColorControl"
		// capability "ColorMode"
		// capability "Refresh"
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
    log.debug "installed()"
    initialize()
}

void updated() {
   log.debug "updated()"
   initialize()
}

void initialize() {
   log.debug "initialize()"
   Integer disableMinutes = 30
   if (enableDebug) {
      log.debug "Debug logging will be automatically disabled in ${disableMinutes} minutes"
      runIn(disableMinutes*60, debugOff)
   }
}

def getThisCopyright(){"&copy; 2021 Jake Lehner"}

def parse(String description) {
	log.warn("Running unimplemented parse for: '${description}'")
}

def on() {
	parent.logDebug("'On' Pressed for device ${device.label}")
	parent.apiRunAction(device.deviceNetworkId, device_model, 'power_on')
}

def off() {
	parent.logDebug("'Off' Pressed for device ${device.label}")
	parent.apiRunAction(device.deviceNetworkId, device_model, 'power_off')
}

def setLevel(level, durationSecs = null) {
	parent.logDebug('setColorTemperature()')

	// TODO validate inputs
	// TODO handle durationSecs

	actions = [
		[
			'pid': wyze_property_brightness,
			'pvalue': level.toString()
		]
	]
	parent.logDebug(actions)
	parent.apiRunActionList(device.deviceNetworkId, device_model, actions)
}

def setColorTemperature(colortemperature, level = null, durationSecs = null) {
	parent.logDebug('setColorTemperature()')

	// TODO validate inputs
	// TODO handle durationSecs

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
}
