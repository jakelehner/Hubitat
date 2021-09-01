/*
 * Import URL: https://raw.githubusercontent.com/HubitatCommunity/??/master/??-Driver.groovy"
 *
 *	Copyright 2019 your Name
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

import groovy.json.JsonBuilder
import java.security.MessageDigest
import static java.util.UUID.randomUUID

public static String version() { return "v0.0.1" }
public static String apiAppName() { return "com.hualai" }
public static String apiAppVersion() { return "2.19.14" }

String wyzeAuthBaseUrl() { return "https://auth-prod.api.wyze.com" }
String wyzeApiBaseUrl() { return "https://api.wyzecam.com" }

Map wyzeRequestHeaders() {
    return [
        'x-api-key': 'WMXHYf79Nr5gIlt3r0r7p9Tcw5bvs6BB4U8O8nGJ',
        'Content-Type': 'application/json; charset=utf-8',
		'Accept': 'application/json',
		'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1 Safari/605.1.15'
    ]
}

Map wyzeRequestBody() {
    return [
        'access_token': state.access_token,
		'app_name': apiAppName(),
		'app_ver': apiAppName() + "_v" + apiAppVersion(),
		'app_version': apiAppVersion(),
		'phone_id': getPhoneId(),
		'phone_system_type': 2,
		'sc': '9f275790cab94a72bd206c8876429f3c',
		'ts': (new Date()).getTime()
    ]
}

definition(
	name: 'WyzeHub',
	namespace: 'jakelehner',
	author: 'Jake Lehner',
	description: 'Hubitat Integration for Wyze WiFi devices.',
	iconUrl: '',
	iconX2Url: '',
	iconX3Url: '',
	singleInstance: true,
	oauth: false,
	importUrl: ''
)

preferences 
{
   page(name: 'pageMenu')
   page(name: 'pageAuthSettings')
   page(name: 'pageSelectDevices')
}

//  ---------------------
// | App Control Methods |
//  ---------------------

def installed() 
{
	logDebug('installed()')

	state.serverInstalled = true
	// if (!state.devices) { state.devices = [:] }  // Maybe a good place to store devices?
	initialize()
	
	logDebug('installed() complete')
}

def updated() 
{
	logDebug('updated()')

	settings.hashedPassword = hashPassword(settings.password)
	settings.password = null
	password = null

	authenticateWyzeAccount()

	if (debugOutput) runIn(300,debugOff) //disable debug logs after 5 min
	initialize()

	logDebug('updated() complete')
}

def initialize() 
{
	logDebug('initialize()')

	if (logEnable) {
		logInfo('Logging is Enabled')
		if (debugEnabled) {
			logInfo('Debug Logging is Enabled')
		}
	}

	// TODO
	// runEvery5Minutes(checkDevices)

	logDebug('initialize() complete')
}

def uninstalled() 
{
	logDebug('uninstalled()')

	logDebug('uninstalled() complete')
}

//  -------
// | Pages |
//  -------

def getThisCopyright(){'&copy; 2021 Your Mom'}

def pageMenu() 
{
	logDebug('pageMenu()')
	initialize()

	return dynamicPage(
		name: 'pageMenu', 
		title: "", 
		install: true, 
		uninstall: true, 
		refreshInterval: 0
	) {
		section(getFormat('title', "${app.label}")) {}
		if (state?.serverInstalled == null || state.serverInstalled == false)
		{
			section("<b style=\"color:green\">${app.label} Installed!</b>")
			{
				paragraph "Click <i>Done</i> to save then return to ${app.label} to continue."
			}
			return
		}

		if (!(settings.username && settings.password)) {
			logDebug('Auth Credentials not set. Forwarding to pageAuthSettings...')
			return pageAuthSettings()
		}

		section("") {
			href(name: 'hrefSelectLights', title: 'Select Devices',
               description: '', page: 'pageSelectDevices')
        //  href(name: 'hrefSelectGroups', title: 'Select Groups',
        //        description: '', page: 'pageSelectGroups')
			href(name: 'hrefAuthSettings', title: 'Configure Login Info',
				description: '', page: 'pageAuthSettings')
        
      }

		section('Advanced Options') 
   		{
   			input 'logEnabled', 'bool', title: 'Enable Logging?', required: true, defaultValue: true
			input 'debugEnabled', 'bool', title: 'Enable Debug Mode?', required: true, defaultValue: false
		}       
      	displayFooter()
	}	
}

def pageAuthSettings() {
	logDebug('pageAuthSettings()')
	return dynamicPage(
		name: 'pageAuthSettings', 
		title: "${app.label} Account Info", 
		install: false, 
		uninstall: true, 
		refreshInterval: 0,
		nextPage: 'pageMenu'
	) {
		section(getFormat('title', "Auth Settings")) {}
		section {
            input name: 'username', type: 'text', title: 'Username', required: true, submitOnChange: true
            input name: 'password', type: 'password', title: 'Password', required: true, submitOnChange: true
			input name: 'mfaCode', type: 'password', title: 'MFA Code', required: false, submitOnChange: true
        }
   		displayFooter()
	}	
}

def pageSelectDevices() {
	
	logDebug('pageSelectLights()')
	
	Map deviceList = getDeviceList()
	logDebug(deviceList)

	List newDevices = []
	addedBulbs = false
	unclaimedBulbs = false;

	if (deviceList) {
		deviceList.each { device ->
            // com.hubitat.app.DeviceWrapper bulbChild = unclaimedBulbs.find { b -> b.deviceNetworkId == "CCH/${state.bridgeID}/Light/${cachedBulb.key}" }
            if (false) {
            //    addedBulbs.put(cachedBulb.key, [hubitatName: bulbChild.name, hubitatId: bulbChild.id, hueName: cachedBulb.value?.name])
            //    unclaimedBulbs.removeElement(bulbChild)
            } else {
               Map newDevice = [:]
               newDevice << [(device.mac): (device)]
               newDevices << newDevice
            }
         }
        //  arrNewBulbs = arrNewDevices.sort { a, b ->
            // Sort by bulb name (default would be hue ID)
            // a.entrySet().iterator().next()?.value <=> b.entrySet().iterator().next()?.value
         }
        //  addedBulbs = addedBulbs.sort { it.value.hubitatName }
	}

	return dynamicPage(
		name: 'pageSelectLights', 
		title: "${app.label} Bulb Selection", 
		install: false, 
		uninstall: true, 
		refreshInterval: 0,
		nextPage: 'pageMenu'
	) {
		section(getFormat('title', 'Select Devices')) {}

		if (!deviceCache) {
			section("No Devices Found...") {            
			input(name: "btnBulbRefresh", type: "button", title: "Refresh", submitOnChange: true)
			}
		} else {
			section("") {
				input(name: "newBulbs", type: "enum", title: "Select devices to add:",
					multiple: true, options: newDevices)

				input(name: "boolAppendDevice", type: "bool", title: "Append device to Hubitat device name")
				paragraph ""
				// paragraph("Previously added lights${addedBulbs ? ' <span style=\"font-style: italic\">(Hue Bridge device name in parentheses)</span>' : ''}:")
			
			if (addedBulbs) {
				StringBuilder bulbText = new StringBuilder()
				bulbText << "<ul>"
				addedBulbs.each {
					bulbText << "<li><a href=\"/device/edit/${it.value.hubitatId}\" target=\"_blank\">${it.value.hubitatName}</a>"
					bulbText << " <span style=\"font-style: italic\">(${it.value.hueName ?: 'not found on Hue'})</span></li>"
					//input(name: "btnRemove_Light_ID", type: "button", title: "Remove", width: 3)
				}
				bulbText << "</ul>"
				paragraph(bulbText.toString())
			}
			else {
				paragraph "<span style=\"font-style: italic\">No added lights found</span>"
			}
			if (unclaimedBulbs) {
				paragraph "Hubitat light devices not found on Hue:"
				StringBuilder bulbText = new StringBuilder()
				bulbText << "<ul>"
				unclaimedBulbs.each {
					bulbText << "<li><a href=\"/device/edit/${it.id}\" target=\"_blank\">${it.displayName}</a></li>"
				}
				bulbText << "</ul>"
				paragraph(bulbText.toString())
			}
			}
			section("Rediscover Bulbs") {
				paragraph("If you added new lights to the Hue Bridge and do not see them above, click/tap the button " +
						"below to retrieve new information from the Bridge.")
				input(name: "btnBulbRefresh", type: "button", title: "Refresh Bulb List", submitOnChange: true)
			}
		}
		
   		displayFooter()
	}	
}

def displayFooter() {
	section{
	   paragraph getFormat("line")
		paragraph "<div style='color:#1A77C9;text-align:center;font-weight:small;font-size:9px'>Developed by: Jake<br/>Version Status: $state.Status<br>Current Version: ${version()} -  ${thisCopyright}</div>"
    }
}

def getFormat(type, myText=""){
	if(type == "header-green") return "<div style='color:#ffffff;font-weight: bold;background-color:#81BC00;border: 1px solid;box-shadow: 2px 3px #A9A9A9'>${myText}</div>"
	if(type == "line") return "\n<hr style='background-color:#1A77C9; height: 1px; border: 0;'></hr>"
	if(type == "title") return "<h2 style='color:#1A77C9;font-weight: bold'>${myText}</h2>"
}

//  --------------
// | Biznas Logic |
//  --------------

def authenticateWyzeAccount() {
    logDebug('authenticateWyzeAccount()')

    if ((settings.username==null) || (settings.hashedPassword==null)) { 
		logDebug('Missing username or password')
		return false 
	}

def params = [
	'uri'                : wyzeAuthBaseUrl(),
	'headers'            : wyzeRequestHeaders(),
	'requestContentType' : "application/json; charset=utf-8",
	'path'			     : "/user/login",
	'body' : [
		'email': settings.username,
		'password': settings.hashedPassword
	]
]

try {
	httpPost(params) { response ->
		logDebug("Login Request was OK: ${response.status}")
		state.access_token   = "${response.data?.access_token}"
		state.refresh_token  = "${response.data?.refresh_token}"
		state.user_id        = "${response.data?.user_id}"
		state.mfa_options 	 = "${response.data?.mfa_options}"
		state.mfa_details 	 = "${response.data?.mfa_details}"
		state.sms_session_id = "${response.data?.sms_session_id}"
		state.statusText	 = "Success"
	}
} catch (Exception e) {
	logDebug("Login Failed with Exception: ${e}")
	state.access_token   = null
	state.refresh_token  = null
	state.user_id        = null
	state.mfa_options 	 = null
	state.mfa_details 	 = null
	state.sms_session_id = null
	state.statusText = "Login Exception: '${e}'"
	return false
}

    logDebug("authenticateWyzeAccount() complete")
    return true
}

private def getDeviceList() {
	logDebug("getDeviceList()")
	requestBody = wyzeRequestBody() + ['sv': 'c417b62d72ee44bf933054bdca183e77']
	apiPost('/app/v2/home_page/get_object_list', requestBody) { response ->
		return response.device_list
	}
}

def apiPost(String path, Map body = [], callback = {}) {
	logDebug('apiPost()')

	bodyJson = (new JsonBuilder(body)).toString()
	
	params = [
		'uri'                : wyzeApiBaseUrl(),
		'headers'            : wyzeRequestHeaders(),
		'requestContentType' : 'application/json; charset=utf-8',
		'path'               : path,
		'body'               : bodyJson
	]

	try {
		httpPost(params) { response -> callback(response.data) }
	} catch (Exception e) {
		logDebug("API Call to ${params.uri}${params.path} failed with Exception: ${e}")
		return false
	}
}

private String getPhoneId() {
	if (!state.phone_id) {
		state.phone_id = randomUUID() as String
	}
	return state.phone_id
}

private String hashPassword(String password) {
	return md5(md5(md5(password)))
}

private String md5(String str) {
	return MessageDigest.getInstance("MD5").digest(str.bytes).encodeHex().toString()
}

def debugOff()
{
	logWarn("Debug logging disabled...")
	app?.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

private void logDebug(str) {
   if (settings.logEnabled && settings.debugEnabled) log.debug("[${app.label}] " + str)
}

private void logInfo(str) {
   if (settings.logEnabled) log.info("[${app.label}] " + str)
}

private void logWarn(str) {
   if (settings.logEnabled) log.warn("[${app.label}] " + str)
}

private void logError(str) {
   if (settings.logEnabled) log.error("[${app.label}] " + str)
}
