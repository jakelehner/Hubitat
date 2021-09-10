/*
 * Import URL: https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/src/drivers/wyzehub-meshlight-group-driver.groovy
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

public String groupTypeId() { return 8 }

import groovy.transform.Field

metadata {
	definition(
		name: "WyzeHub Color Bulb Group", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: "https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/src/drivers/wyzehub-meshlight-group-driver.groovy"
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

def getThisCopyright(){"&copy; 2021 Jake Lehner"}

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
