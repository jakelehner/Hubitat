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

@Field static final String wyze_property_power = 'P3'
@Field static final String wyze_property_device_online = 'P5'
@Field static final String wyze_property_rssi = 'P1612'
@Field static final String wyze_property_vacation_mode = 'P1614'

@Field static final String wyze_property_power_value_on = '1'
@Field static final String wyze_property_power_value_off = '0'
@Field static final String wyze_property_device_online_value_true = '1'
@Field static final String wyze_property_device_online_value_false = '0'
@Field static final String wyze_property_device_vacation_mode_true = '1'
@Field static final String wyze_property_device_vacation_mode_false = '0'

metadata {
	definition(
		name: "WyzeHub Plug", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: "https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/drivers/wyzehub-meshlight-driver.groovy"
	) {
		capability "Outlet"
		capability "Refresh"

		attribute "vacationMode", "bool"
		attribute "online", "bool"
		attribute "rssi", "number"
	}

	preferences 
	{
		
	}
}

void installed() {
    log.debug "installed()"

	device.updateDataValue('deviceModel', device_model)

    refresh()
	initialize()
}

void updated() {
   log.debug "updated()"
   initialize()
}

void initialize() {
   log.debug "initialize()"
}

void parse(String description) {
	log.warn("Running unimplemented parse for: '${description}'")
}

def getThisCopyright(){"&copy; 2021 Jake Lehner"}

def refresh() {
	parent.logDebug("Refresh device ${device.label}")
	parent.apiGetDevicePropertyList(device.deviceNetworkId, device_model) { propertyList ->
		createDeviceEventsFromPropertyList(propertyList)
	}
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

void createDeviceEventsFromPropertyList(List propertyList) {
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
				parent.logDebug('Updating Property: colorMode')
				parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
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
                
				parent.logDebug('Updating Property: switch')
				parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
            break
        
            // Device Online
            case wyze_property_device_online:
                eventName = "online"
                eventUnit = null
                eventValue = property.value == wyze_property_device_online_value_true ? "true" : "false"
                
				parent.logDebug('Updating Property: online')
				parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
            break

            // RSSI
            case wyze_property_rssi:
                eventName = "rssi"
                eventUnit = 'db'
                eventValue = property.value

				parent.logDebug('Updating Property: rssi')
				parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
            break

            // Vacation Mode
            case wyze_property_vacation_mode:
                eventName = "vacationMode"
                eventUnit = null
                eventValue = property.value == wyze_property_device_vacation_mode_true ? "true" : "false"

				parent.logDebug('Updating Property: vacationMode')
				parent.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
            break

        }
    }
}
