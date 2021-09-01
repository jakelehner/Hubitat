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

public static String version()      {  return "v0.0.1"  }

public colorMode = 'RGB'
 
import groovy.transform.Field

metadata {
	definition(
		name: "Wyze Color Bulb", 
		namespace: "jakelehner", 
		author: "Jake Lehner", 
		importUrl: "https://raw.githubusercontent.com/jakelehner/hubitat-WyzeHub/master/drivers/wyzehub-meshlight-driver.groovy"
	) {
		capability "Light"
		capability "ColorControl"
		capability "ColorTemperature"
		capability "Refresh"
		capability "SwitchLevel"
		capability "ChangeLevel"
		capability "ColorMode"
		// capability "LightEffects" // TODO
		

		// command "flash"
		// command "flashOnce"
		// command "flashOff"

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

def parse(String description)
{
	log.warn("Running unimplemented parse for: '${description}'")
}

/*
	on
    
	Turns the device on.
*/
def on()
{
	// The server will update on/off status
	log.trace "Msg: $description ON"
	
}


/*
	off
    
	Turns the device off.
*/
def off()
{
	// The server will update on/off status
	log.trace "Msg: $description OFF"
}

