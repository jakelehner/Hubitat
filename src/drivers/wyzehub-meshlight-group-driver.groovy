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
		// capability "Light"
		// capability "SwitchLevel"
		// capability "ColorTemperature"
		// capability "ColorControl"
		// capability "ColorMode"
		// capability "Refresh"
		// capability "LightEffects"

		// command "toggleVacationMode"
		// command "flashOnce"
		
		// attribute "vacationMode", "bool"
		// attribute "online", "bool"
		// attribute "rssi", "number"
		// attrubute "lastRefreshed", "date"

	}

}

void installed() {
    parent.logDebug("installed()")

	refresh()
	initialize()
}

void updated() {
    parent.logDebug("updated()")
    initialize()
}

void initialize() {
    parent.logDebug("initialize()")
}

void parse(String description) {
	log.warn("Running unimplemented parse for: '${description}'")
}

def getThisCopyright(){"&copy; 2021 Jake Lehner"}

def refresh() {
	
}

def on() {
	
}

def off() {
	
}

def setLevel(level, durationSecs = null) {
	
}

def setColorTemperature(colortemperature, level = null, durationSecs = null) {
	
}

def setColor(colormap) {
	
}

def setHue(hue) {
	
}

def setSaturation(saturation) {
	
}
