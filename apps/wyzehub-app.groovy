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
import groovy.transform.Field
import java.security.MessageDigest
import static java.util.UUID.randomUUID

public static final String version() { return "v0.0.1" }
public static final String apiAppName() { return "com.hualai" }
public static final String apiAppVersion() { return "2.19.14" }
@Field static final String childNamespace = "jakelehner" 
@Field static final Map driverMap = [
   'MeshLight': [label: 'Color Bulb', driver: 'Wyze Color Bulb'],
   'Plug': [label: 'Plug', driver: 'Wyze Plug'],,
   'default': [label: 'Unsupported Type', driver: null]
]

String wyzeAuthBaseUrl() { return "https://auth-prod.api.wyze.com" }
String wyzeApiBaseUrl() { return "https://api.wyzecam.com" }

Map wyzeRequestHeaders() {
    return [
        "x-api-key": "WMXHYf79Nr5gIlt3r0r7p9Tcw5bvs6BB4U8O8nGJ",
        "Content-Type": "application/json",
		"Accept": "application/json",
		"User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.1 Safari/605.1.15"
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
	
	authenticateWyzeAccount()

	if (debugOutput) runIn(300,debugOff) //disable debug logs after 5 min
	initialize()
}

def initialize() 
{
	logDebug('initialize()')

	if(settings['addDevices']) {
		logDebug('clearing addDevices cache')
		app.removeSetting('addDevices')
	}

	if (logEnable) {
		logInfo('Logging is Enabled')
		if (debugEnabled) {
			logInfo('Debug Logging is Enabled')
		}
	}

	// TODO
	// runEvery5Minutes(checkDevices)

}

def uninstalled() 
{
	logDebug('uninstalled()')
}

//  -------
// | Pages |
//  -------

def getThisCopyright(){'&copy; 2021 Your Mom'}

def pageMenu() 
{
	logDebug('pageMenu()')
	
	if (settings["addDevices"]) {
		logDebug("New devices selected. Creating...")
		logDebug(settings["addDevices"])
		addDevices(settings["addDevices"])
		app.removeSetting('addDevices')
	}

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
               description: '', page: 'pageSelectDevices', image: '')
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
	
	updateDeviceCache()
	deviceList = getDeviceListFromCache()
	
	List newDevices = []
	List unsupportedDevices = []
	Map unclaimedDevices = [:];

	if (deviceList) {
		logDebug('pageSelectLights(): process device list')
		
		deviceList.each { mac, device ->
			productType = driverMap[device.product_type]
			childDeviceExists = getChildDevice(device.mac)
			if(!productType) {
				unsupportedDevices << device
			}else if (productType && !childDeviceExists) {
               Map newDevice = [:]
               newDevice << [(device.mac): "[${productType.label}] ${device.nickname}"]
               newDevices << newDevice
            }
		}

		// Sort
		newDevices = newDevices.sort { a, b ->
			a.entrySet().iterator().next()?.value <=> b.entrySet().iterator().next()?.value
		}
		unsupportedDevices = unsupportedDevices.sort { it.value }
	}

	return dynamicPage(
		name: 'pageSelectDevices', 
		title: "${app.label} Device Selection", 
		install: false, 
		uninstall: true, 
		refreshInterval: 0,
		nextPage: 'pageMenu'
	) {
		
		if (!deviceList) {
			section("No New Devices Found...") {            
				input(name: "btnDeviceRefresh", type: "button", title: "Refresh", submitOnChange: true)
			}
		} else {

			section(getFormat('title', 'Add Devices')) {
				input(name: "addDevices", type: "enum", title: "Select devices to add:",
					submitOnChange: false, multiple: true, options: newDevices)
			}
		}

		if(unsupportedDevices) {
			section(getFormat('title', 'Unsupported Devices')) {
				unsupportedDevices.each{ device ->
					paragraph " - [${device.product_type}] ${device.nickname}"
				}
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
	if(type == "title") return "<h4 style='color:#1A77C9;font-weight: bold'>${myText}</h4>"
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

    return true
}

private def void updateDeviceCache() {
	logDebug("updateDeviceCache()")
	state.deviceCache = [
		'groups': [:],
		'devices': [:]
	]
	requestBody = wyzeRequestBody() + ['sv': 'c417b62d72ee44bf933054bdca183e77']
	apiPost('/app/v2/home_page/get_object_list', requestBody) { response ->

		response.data.device_group_list.each { deviceGroup ->
			state.deviceCache['groups'][deviceGroup.group_id] = deviceGroup
		}

		response.data.device_list.each { device ->
			state.deviceCache['devices'][device.mac] = device
		}
		
	}
	
}

private def Map getDeviceCache() {
	return state.deviceCache
}

private def Map getDeviceFromCache(String mac) {
	return state.deviceCache['devices'][mac]
}

private def Map getDeviceListFromCache() {
	return state.deviceCache['devices']
}

private def Map getDeviceGroupListFromCache() {
	return state.deviceCache['groups']
}

private def addDevices(List deviceMacs) {
	logDebug('addDevices()')

	deviceMacs.each { mac ->
		logDebug("Add device with mac ${mac}")
		Map deviceFromCache = getDeviceFromCache(mac)
		if (deviceFromCache) {
			logDebug('Found Device in Cache. Adding...')
			driver = driverMap[deviceFromCache.product_type].driver
			if (!driver) {
				logDebug("Driver not found. Unsupported Device Type: ${deviceFromCache.product_type}")
				return
			}
			deviceProps = [
				name: (driver), 
				label: (deviceFromCache.nickname),
				deviceModel: (deviceFromCache.product_model)
			]
			addChildDevice(childNamespace, driver, deviceFromCache.mac, deviceProps)
		} else { 
			logDebug('DID NOT find Device in Cache')
		}

	}
}

def apiRunAction(String deviceMac, String deviceModel, String actionKey) {
	logDebug("apiRunActionList()")
	logDebug(deviceMac)
	logDebug(deviceModel)

	requestBody = wyzeRequestBody() + [
		'sv': '011a6b42d80a4f32b4cc24bb721c9c96', 
		'action_key': actionKey,
		'action_params': [:],
		'instance_id': deviceMac,
		'provider_key': deviceModel
	]

	apiPost('/app/v2/auto/run_action', requestBody) { response ->
		logDebug(response)
	}
}

def apiRunActionList(String deviceMac, String deviceModel, List actionList) {
	logDebug("apiRunActionList()")
	logDebug(deviceMac)
	logDebug(deviceModel)

	List apiActionList = [
		[
			'action_key': 'set_mesh_property',
			'instance_id': (deviceMac),
			'provider_key': (deviceModel),
			'action_params': [
				'list': [
						[
							'mac': (deviceMac),
							'plist': (actionList)
						]
					]
			]
		]
	]

	requestBody = wyzeRequestBody() + [
		'sv': '5e02224ae0c64d328154737602d28833', 
		'action_list': apiActionList
	]

	apiPost('/app/v2/auto/run_action_list', requestBody) { response ->
		logDebug(response)
	}
}

def apiSetDeviceProperty(String deviceMac, String deviceModel, String propertyId, value) {
	logDebug("setDeviceProperty()")

	requestBody = wyzeRequestBody() + [
		'sv': '44b6d5640c4d4978baba65c8ab9a6d6e',
		'device_mac': deviceMac,
		'device_model': deviceModel, 
		'pid': propertyId,
		'pvalue': value
	]
	
	apiPost('/app/v2/device/set_property', requestBody) { response ->
		logDebug(response)
	}
}

def apiPost(String path, Map body = [], callback = {}) {
	logDebug('apiPost()')

	bodyJson = (new JsonBuilder(body)).toString()
	logDebug(bodyJson)
	params = [
		'uri'                : wyzeApiBaseUrl(),
		'headers'            : wyzeRequestHeaders(),
		'contentType' : 'application/json',
		// 'requestContentType' : 'application/json; charset=utf-8',
		'path'               : path,
		'body'               : bodyJson
	]

	try {
		httpPost(params) { response -> 
			if (response.data.code != "1") {
				logError("apiPost error!")
			}
			callback(response.data) 
		}
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

void appButtonHandler(btn) {
   switch(btn) {
      case "btnDeviceRefresh":
         // Just want to resubmit page, so nothing
         break        
      default:
         log.warn "Unhandled app button press: $btn"
   }
}

def debugOff()
{
	logWarn("Debug logging disabled...")
	app?.updateSetting("debugEnabled",[value:"false",type:"bool"])
}

private void logDebug(message) {
   if (settings.logEnabled && settings.debugEnabled) log.debug("[${app.label}] " + message)
}

private void logInfo(message) {
   if (settings.logEnabled) log.info("[${app.label}] " + message)
}

private void logWarn(message) {
   if (settings.logEnabled) log.warn("[${app.label}] " + message)
}

private void logError(message) {
   if (settings.logEnabled) log.error("[${app.label}] " + message)
}
