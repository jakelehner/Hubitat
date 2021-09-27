/*
 * Import URL: 
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

public static String version() {  return "v1.0.5"  }

public String deviceModel() { return 'WYZEC1-JZ' }

@Field static final String wyze_action_power_on = 'power_on'
@Field static final String wyze_action_power_off = 'power_off'

@Field static final String wyze_property_power = 'P3'
@Field static final String wyze_property_device_online = 'P5'
@Field static final String wyze_property_notifications = 'P1'
@Field static final String wyze_property_motion_record = 'P1047'
@Field static final String wyze_property_sound_record = 'P1048'

@Field static final String wyze_property_power_value_on = '1'
@Field static final String wyze_property_power_value_off = '0'
@Field static final String wyze_property_device_online_value_true = '1'
@Field static final String wyze_property_device_online_value_false = '0'
@Field static final String wyze_property_device_notifications_value_true = '1'
@Field static final String wyze_property_device_notifications_value_false = '0'
@Field static final String wyze_property_device_motion_record_value_true = '1'
@Field static final String wyze_property_device_motion_record_value_false = '0'
@Field static final String wyze_property_device_sound_record_value_true = '1'
@Field static final String wyze_property_device_sound_record_value_false = '0'

metadata {
	definition(
		name: "WyzeHub Camera", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: ""
	) {
		capability "Outlet"
		capability "Refresh"

		attribute "notifications_enabled", "bool"
        attribute "motion_notification", "bool"
        attribute "sound_notification", "bool"
		attribute "online", "bool"
        
        command(
             "setAllNotifications", 
             [
                [
                     "name":"all_notify*",
                     "description":"Set value to true/false to enable/disable all notifications",
                     "type":"ENUM",
                     "constraints":["true","false"]
                ]
             ]
        )
        command(
             "setMotionNotification", 
             [
                [
                     "name":"motion_notify*",
                     "description":"Set value to true/false to enable/disable notifications based on motion recording",
                     "type":"ENUM",
                     "constraints":["true","false"]
                ]
             ]
        )
        command(
             "setSoundNotification", 
             [
                [
                     "name":"sound_notify*",
                     "description":"Set value to true/false to enable/disable notifications based on sound recording",
                     "type":"ENUM",
                     "constraints":["true","false"]
                ]
             ]
        )
	}

	preferences {
        input "pollInterval", "number", title: "Polling Interval", description: "Change polling frequency (in seconds)", defaultValue:240, range: "10..86400", required: true, displayDuringSetup: true
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
    unschedule('refresh')
    refresh()
}

void parse(String description) {
    log.warn("Running unimplemented parse for: '${description}'")
}

def refresh() {
    app = getApp()
	logInfo("Refresh Device")
	app.apiGetDevicePropertyList(device.deviceNetworkId, deviceModel()) { propertyList ->
		createDeviceEventsFromPropertyList(propertyList)
	}

	runIn(pollInterval, 'refresh')
}

def on() {
	app = getApp()
	logInfo("'On' Pressed")

	app.apiRunAction(device.deviceNetworkId, deviceModel(), wyze_action_power_on)
}

def off() {
	app = getApp()
	logInfo("'Off' Pressed")

	app.apiRunAction(device.deviceNetworkId, deviceModel(), wyze_action_power_off)
}

def setAllNotifications(all_notify) {
    logInfo("setAllNotifications() Pressed")
    logDebug('Setting All Notifications to ' + all_notify)
    
    value = all_notify == "true" ? wyze_property_device_notifications_value_true : wyze_property_device_notifications_value_false
	id = wyze_property_notifications
    
    sendProperty(id, value)
    setMotionNotification(all_notify)
    setSoundNotification(all_notify)
}

def setMotionNotification(motion_notify) {
    logInfo("setMotionNotification() Pressed")
    logDebug('Setting Motion Notifications to ' + motion_notify)
    
    value = motion_notify == "true" ? wyze_property_device_motion_record_value_true : wyze_property_device_motion_record_value_false
	id = wyze_property_motion_record
    
    sendProperty(id, value)
    runIn(5,refresh)
}

def setSoundNotification(sound_notify) {
	logInfo("setSoundNotification() Pressed")
    logDebug('Setting Sound Notifications to ' + sound_notify)
    
    value = sound_notify == "true" ? wyze_property_device_sound_record_value_true : wyze_property_device_sound_record_value_false
	id = wyze_property_sound_record

    sendProperty(id, value)
    runIn(5,refresh)
}

private sendProperty(pid, pvalue) {
    app = getApp()
    app.apiSetDeviceProperty(device.deviceNetworkId, deviceModel(), pid, pvalue)
}
    
void createDeviceEventsFromPropertyList(List propertyList) {
	app = getApp()
    //logDebug(propertyList)
    String eventName, eventUnit
    def eventValue // could be String or number

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
            
            // Notifications Enabled
            case wyze_property_notifications:
                eventName = "notifications_enabled"
                eventUnit = null
                eventValue = propertyValue == wyze_property_device_notifications_value_true ? "true" : "false"

				if (device.currentValue(eventName) != eventValue) {
                    logDebug("Updating Property ${eventName} to ${eventValue}")
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // Event Recording based on motion
            case wyze_property_motion_record:
                eventName = "motion_notification"
                eventUnit = null
                eventValue = propertyValue == wyze_property_device_motion_record_value_true ? "true" : "false"

				if (device.currentValue(eventName) != eventValue) {
                    logDebug("Updating Property ${eventName} to ${eventValue}")
					app.doSendDeviceEvent(device, eventName, eventValue, eventUnit)
				}
            break

            // Event Recording based on sound
            case wyze_property_sound_record:
                eventName = "sound_notification"
                eventUnit = null
                eventValue = propertyValue == wyze_property_device_sound_record_value_true ? "true" : "false"

				if (device.currentValue(eventName) != eventValue) {
					logDebug("Updating Property ${eventName} to ${eventValue}")
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
