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
	app.logDebug("updated()")
    initialize()
}

void initialize() {
    app = getApp()
	app.logDebug("initialize()")
}

void parse(String description) {
	app = getApp()
	app.logWarn("Running unimplemented parse for: '${description}'")
}

def getThisCopyright(){"&copy; 2021 Jake Lehner"}

def refresh() {
	app = getApp()
	app.logDebug("refresh()")
	getChildDevices().each { device -> 
		device.refresh()
	}
}

def on() {
	app = getApp()
	app.logDebug("on()")
	getChildDevices().each { device -> 
		device.on()
	}
}

def off() {
	app = getApp()
	app.logDebug("off()")
	getChildDevices().each { device -> 
		device.off()
	}
}

def setLevel(level, durationSecs = null) {
	app = getApp()
	app.logDebug("setLevel()")
	getChildDevices().each { device -> 
		device.setLevel(level, durationSecs)
	}
}

def setColorTemperature(colortemperature, level = null, durationSecs = null) {
	app = getApp()
	app.logDebug("setColorTemperature()")
	getChildDevices().each { device -> 
		device.setColorTemperature(colortemperature, level, durationSecs)
	}
}

def setColor(colormap) {
	app = getApp()
	app.logDebug("setColor()")
	getChildDevices().each { device -> 
		device.setColor(colormap)
	}
}

def setHue(hue) {
	app = getApp()
	app.logDebug("setHue()")
	getChildDevices().each { device -> 
		device.setHue(hue)
	}
}

def setSaturation(saturation) {
	app = getApp()
	app.logDebug("setSaturation()")
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
