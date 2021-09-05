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

@Field static final String device_model = 'WLPP1CFH'

@Field static final String wyze_property_push_notifications_enabled = 'P1'
@Field static final String wyze_property_power = 'P3'
@Field static final String wyze_property_device_online = 'P5'

metadata {
	definition(
		name: "Wyze Plug", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: "https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/drivers/wyzehub-meshlight-driver.groovy"
	) {
		capability "Outlet"
		capability "Refresh"
	
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

void parse(String description) {
	log.warn("Running unimplemented parse for: '${description}'")
}

def getThisCopyright(){"&copy; 2021 Jake Lehner"}

def refresh() {
	parent.logDebug("Refresh device ${device.label}")
}

def on() {
	parent.logDebug("'On' Pressed for device ${device.label}")
	parent.apiSetDeviceProperty(device.deviceNetworkId, device_model, wyze_property_power, 1)
	sendEvent(name: "switch", value: "on")
}

def off() {
	parent.logDebug("'Off' Pressed for device ${device.label}")
	parent.apiSetDeviceProperty(device.deviceNetworkId, device_model, wyze_property_power, 0)
	sendEvent(name: "switch", value: "off")
}
