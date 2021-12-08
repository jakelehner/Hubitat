/*
 * Import URL: https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/drivers/wyzehub-meshlight-group-driver.groovy
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

public static String version() { return "v1.3.0"  }

public String deviceModel() { return '' }

public String groupTypeId() { return 8 }

import groovy.transform.Field

metadata {
	definition(
		name: "WyzeHub Color Bulb Group", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: "https://raw.githubusercontent.com/jakelehner/Hubitat/master/WyzeHub/drivers/wyzehub-meshlight-group-driver.groovy"
	) {
		capability "Light"
		capability "SwitchLevel"
		capability "ColorTemperature"
		capability "ColorControl"
		capability "ColorMode"
		capability "Refresh"
		// capability "LightEffects"

		command(
			"setColorHEX", 
			[
				[
					"name": "HEX Color*", 
					"type": "STRING", 
					"description": "Color in HEX no #"
				]
			]
		)
		// command "toggleVacationMode"
		// command "flashOnce"
	}

}

void installed() {
    app = getApp()
	parent.logDebug("installed()")

	refresh()
	initialize()
}

void updated() {
    app = getApp()
	logDebug("updated()")
    initialize()
}

void initialize() {
    app = getApp()
	logDebug("initialize()")
}

void parse(String description) {
	app = getApp()
	logWarn("Running unimplemented parse for: '${description}'")
}

def refresh() {
	app = getApp()
	logDebug("refresh()")
	getChildDevices().each { device -> 
		device.refresh()
	}
}

def on() {
	app = getApp()
	logDebug("on()")
	getChildDevices().each { device -> 
		device.on()
	}
}

def off() {
	app = getApp()
	logDebug("off()")
	getChildDevices().each { device -> 
		device.off()
	}
}

def setLevel(level, durationSecs = null) {
	app = getApp()
	logDebug("setLevel()")
	getChildDevices().each { device -> 
		device.setLevel(level, durationSecs)
	}
}

def setColorTemperature(colortemperature, level = null, durationSecs = null) {
	app = getApp()
	logDebug("setColorTemperature()")
	getChildDevices().each { device -> 
		device.setColorTemperature(colortemperature, level, durationSecs)
	}
}

def setColor(colormap) {
	app = getApp()
	logDebug("setColor()")
	getChildDevices().each { device -> 
		device.setColor(colormap)
	}
}

def setColorHEX(String hexColor) {
	app = getApp()
	logDebug("setColor()")
	getChildDevices().each { device -> 
		device.setColorHEX(hexColor)
	}
}

def setHue(hue) {
	app = getApp()
	logDebug("setHue()")
	getChildDevices().each { device -> 
		device.setHue(hue)
	}
}

def setSaturation(saturation) {
	app = getApp()
	logDebug("setSaturation()")
	getChildDevices().each { device -> 
		device.setSaturation(saturation)
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
