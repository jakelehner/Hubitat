/*
 * Import URL: https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/drivers/wyzehub-plug-driver.groovy
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

public static String version() {  return "v1.1.0"  }

public String deviceModel() { return device.getDataValue('product_model') ?: 'WLPP1CFH' }

@Field static final String wyze_action_power_on = 'power_on'
@Field static final String wyze_action_power_off = 'power_off'

@Field static final String wyze_property_power = 'P3'
@Field static final String wyze_property_device_online = 'P5'
@Field static final String wyze_property_rssi = 'P1612'
@Field static final String wyze_property_vacation_mode = 'P1614'

@Field static final String wyze_property_power_value_on = '1'
@Field static final String wyze_property_power_value_off = '0'
@Field static final String wyze_property_device_online_value_true = '1'
@Field static final String wyze_property_device_online_value_false = '0'
@Field static final String wyze_property_device_vacation_mode_value_true = '1'
@Field static final String wyze_property_device_vacation_mode_value_false = '0'

metadata {
	definition(
		name: "WyzeHub Plug", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: "https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/drivers/wyzehub-plug-driver.groovy"
	) {
		capability "Outlet"
		capability "Switch"
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

	// TODO Make Configurable
	unschedule('refresh')
	//schedule('0/10 * * * * ? *', 'refresh')

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

def refresh() {
	app = getApp()
	logInfo("Refresh Device")
	app.apiGetDevicePropertyList(device.deviceNetworkId, deviceModel())

	// TODO Make Configurable
	// keepFresh = true
	// keepFreshSeconds = 10
	// runIn(keepFreshSeconds, 'refresh')
}

def on() {
	app = getApp()
	logInfo("'On' Pressed")

	app.apiSetDeviceProperty(device.deviceNetworkId, deviceModel(), wyze_property_power, wyze_property_power_value_on)
}

def off() {
	app = getApp()
	logInfo("'Off' Pressed")

	app.apiSetDeviceProperty(device.deviceNetworkId, deviceModel(), wyze_property_power, wyze_property_power_value_off)
}

void createDeviceEventsFromPropertyList(List propertyList) {
	app = getApp()
    logDebug("createEventsFromPropertyList()")

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
					logDebug("Updating Property 'colorMode' to ${eventValue}")
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            }
        }
    }

    propertyList.each { property ->
	
		propertyValue = property.value ?: property.pvalue ?: null
		switch(property.pid) {
            // Switch State
			case wyze_action_power_on:
			case wyze_action_power_off:
            case wyze_property_power:
				eventName = "switch"
                eventUnit = null

				if(propertyValue == wyze_property_power_value_on) {
					eventValue = "on"
				} else if(propertyValue == wyze_action_power_on) {
					eventValue = "on"
				} else {
					eventValue = "off"
				}

				if (device.currentValue(eventName) != eventValue) {
					logDebug("Updating Property 'switch' to ${eventValue}")
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break
        
            // Device Online
            case wyze_property_device_online:
                eventName = "online"
                eventUnit = null
                eventValue = propertyValue == wyze_property_device_online_value_true ? "true" : "false"
                
				if (device.currentValue(eventName) != eventValue) {
					logDebug("Updating Property 'online' to ${eventValue}")
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // RSSI
            case wyze_property_rssi:
                eventName = "rssi"
                eventUnit = 'db'
                eventValue = propertyValue

				if (device.currentValue(eventName) != eventValue) {
					logDebug("Updating Property 'rssi' to ${eventValue}")
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // Vacation Mode
            case wyze_property_vacation_mode:
                eventName = "vacationMode"
                eventUnit = null
                eventValue = propertyValue == wyze_property_device_vacation_mode_value_true ? "true" : "false"

				if (device.currentValue(eventName) != eventValue) {
					logDebug("Updating Property 'vacationMode' to ${eventValue}")
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

        }
    }
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
